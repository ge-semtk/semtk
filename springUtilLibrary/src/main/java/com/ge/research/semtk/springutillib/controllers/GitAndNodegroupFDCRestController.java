/**
 ** Copyright 2020 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
package com.ge.research.semtk.springutillib.controllers;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.ge.research.semtk.git.GitRepoHandler;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.springutilib.requests.FdcRequest;
import com.ge.research.semtk.utility.LocalLogger;


import io.swagger.annotations.ApiOperation;

/**
 * FDC Service that pulls folders of CSV files from git.
 * @author 200001934
 *
 */
public class GitAndNodegroupFDCRestController extends NodegroupProviderRestController {
	@ApiOperation(
			value="Get csv files from a git folder",
			notes=""
			)
	@CrossOrigin
	@RequestMapping(value="/retrieveCsvsFromGit", method=RequestMethod.POST)
	public JSONObject retrieveCsvsFromGit(@RequestBody FdcRequest request) {// @RequestParam("giturl") String giturl) {
		final String ENDPOINT_NAME = "retrieveCsvsFromGit";
		
		try {
			
			// TODO: implement local based on a temp location
			// TODO: get rid of others
			// TODO: test (junit?)
			
			/*
{
 "tables": "{\"1\": {\"col_names\":[\"repo\",\"folder\",\"branch\"],\"rows\":[[\"git@github.build.ge.com:semtk/semtk-opensource.git\",\"sparqlGraphLibrary/src/test/resources/fdcTestA\",\"master\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":1}}"
}
			*/
			// TODO: config
			String local = "C:\\\\Users\\200001934\\Temp\\semtk-opensource2";
			String knownHosts = "C:\\\\Users\\200001934\\Temp\\known_hosts";
			String sshPpk = "C:\\\\Users\\200001934\\.ssh\\id_rsa.ppk";
			
			// TODO: params
			String repo = "git@github.build.ge.com:semtk/semtk-opensource.git";
			String folder = "sparqlGraphLibrary/src/test/resources/fdcTestMismatched";
			String branch = "master";
			
			File localRepo = new File(local);

			// validate the input table
			HashMap<String,Table> paramTables = request.buildTableHash();
			FdcRequest.validateTableHash(paramTables, "1", new String [] {"repo", "folder", "branch"});
			repo = paramTables.get("1").getCell(0,0);
			folder = paramTables.get("1").getCell(0,1);
			branch = paramTables.get("1").getCell(0,2);

			// get the latest from git into localRepo
			GitRepoHandler gitHandler = new GitRepoHandler(sshPpk, knownHosts);
			
			if (! localRepo.exists()) {
				gitHandler.clone(repo, localRepo, branch);
			} else {
				gitHandler.pull(localRepo, branch);
			}
			
			// open local csv folder
			File csvFolder = Paths.get(local,folder).toFile();
			if (!csvFolder.exists()) {
				throw new Exception("git repo does not contain folder: " + folder);
			}
			
			// get csv files
			File[] csvFiles = csvFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
			if (csvFiles.length == 0) {
				throw new Exception("git repo folder contains no csv files.  Expect at least one with a header line: " + folder);
			}
			
			// build table
			Table table = null;
			for (File csv : csvFiles) {
				Table t = Table.fromCsvFile(csv.getAbsolutePath());
				if (table == null) {
					table = t;
				} else {
					// will throw an Exception if table headers don't match
					table.append(t);
				}
			}
			
			// return success
			TableResultSet tResult = new TableResultSet(true);
			tResult.addResults(table);
			return tResult.toJson();
			
		} catch(Exception e){
	    	SimpleResultSet res = new SimpleResultSet();
	    	res.setSuccess(false);
	    	res.addRationaleMessage("GitAndNodegroupFDC", ENDPOINT_NAME, e);
		    LocalLogger.printStackTrace(e);
		    return res.toJson();
		}  
	}
}
