/** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.standaloneExecutables;

import com.opencsv.CSVReader;
import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads OWL to a semantic store.
 */
public class RetrieveFromStore {

	private static final String CSV_SPLIT_CHARACTER = ",";
	private static final String formatInfo = "Input file should be in the format: ID, comments, creationDate, creator, jsonFile";
	private static final String[] headers= {"Context","ID","comments","creationDate","creator","jsonFile"};


	/**
	 * Main method
	 */
	public static void main(String[] args) throws Exception{


		// get arguments
		if(args.length != 3) {
			System.out.println("\nUsage: http://nodegroup_store_endpoint:port nodegroup_id_regex folder_path\n");
			System.exit(0);
		}

		String endpointUrlWithPort = args[0]; 
		String regex = args[1];
		String folder = args[2];
		Pattern p = Pattern.compile(regex);
		String endpointPart[] = endpointUrlWithPort.split(":/*");

		// set up client
		NodeGroupStoreConfig config = new NodeGroupStoreConfig(endpointPart[0], endpointPart[1], Integer.parseInt(endpointPart[2]));
		NodeGroupStoreRestClient client = new NodeGroupStoreRestClient(config);

		// get list of nodegroups
		TableResultSet res = client.executeGetNodeGroupMetadata();
		res.throwExceptionIfUnsuccessful("Error retrieving nodegroup list");
		Table inTable = res.getTable();

		// build list of nodegroups we care about
		Table matchTable = new Table(inTable.getColumnNames(), inTable.getColumnTypes());
		for (int i=0; i < inTable.getNumRows(); i++) {
			String id = inTable.getCell(i, "ID");
			if (p.matcher(id).find()) {
				matchTable.addRow(inTable.getRow(i));
			}
		}

		// build outTable
		Table outTable = new Table(
				new String[] {"Context","ID","comments","creationDate","creator","jsonFile"},
				new String[] {"unknown","unknown","unknown","unknown","unknown","unknown"});

		for (int i=0; i < matchTable.getNumRows(); i++) {
			String id = matchTable.getCell(i, "ID");
			System.out.println(id);

			// write local copy of nodegroup
			SparqlGraphJson sgjson = client.executeGetNodeGroupByIdToSGJson(id);
			Path jsonPath = Paths.get(folder, id + ".json");
			Path jsonFilePath = Paths.get(    id + ".json");
			Files.write(jsonPath, sgjson.toJson().toJSONString().getBytes());

			// save metadata about nodegroup
			ArrayList<String> row = new ArrayList<String>();
			row.add("unused");
			row.add(matchTable.getCell(i, "ID"));
			row.add(matchTable.getCell(i, "comments"));
			row.add(matchTable.getCell(i, "creationDate"));
			row.add(matchTable.getCell(i, "creator"));
			row.add(jsonFilePath.toString());
			outTable.addRow(row);
		}

		// write metadata
		Files.write(Paths.get(folder, "store_data.csv"), outTable.toCSVString().getBytes());

		System.out.println("fine.");
		System.exit(0);
	}
	
}
