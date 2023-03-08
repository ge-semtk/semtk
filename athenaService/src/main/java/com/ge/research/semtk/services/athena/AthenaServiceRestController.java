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

package com.ge.research.semtk.services.athena;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.json.simple.JSONObject;

import com.amazonaws.regions.Regions;
import com.ge.research.semtk.query.rdb.AthenaConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.services.athena.AthenaProperties;

/**
 * Service to execute queries on AWS Athena
 */
@CrossOrigin
@RestController
@RequestMapping("/athena")
public class AthenaServiceRestController {

	@Autowired
	private AthenaProperties props;
	
	/**
	 * Execute query in Athena
	 */
	@CrossOrigin
	@RequestMapping(value="/query", method= RequestMethod.POST)
	public JSONObject queryAthena(@RequestBody AthenaServiceQueryRequestBody requestBody){
		String query = requestBody.query;
		String database = requestBody.database;
		return runQuery (requestBody, query, database);
	}


	/**
	 * Execute query in Athena
	 */
	private JSONObject runQuery (AthenaServiceQueryRequestBody requestBody, String query, String database) {
		
		// get properties from property file
		String awsRegionId = props.getAwsRegionId();
		String awsS3OutputBucket = props.getAwsS3OutputBucket();
		String awsKey = props.getAwsKey();
		int awsClientExecutionTimeout = props.getAwsClientExecutionTimeout();

		TableResultSet tableResultSet = new TableResultSet();
		
		try {
			Regions awsRegion = Regions.fromName(awsRegionId);
			LocalLogger.logToStdOut("Connecting to Athena...");
			AthenaConnector athenaConnector = new AthenaConnector(awsRegion, awsS3OutputBucket, awsKey, awsClientExecutionTimeout, database);
			LocalLogger.logToStdOut("Run Athena query on database " + database + ": " + query);
			Table table = athenaConnector.query(query);
			LocalLogger.logToStdOut("Returning num rows: " + (table != null ? String.valueOf(table.getNumRows()) : "<null>"));
			tableResultSet.addResults(table);
			tableResultSet.setSuccess(true);

		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			tableResultSet.setSuccess(false);
			tableResultSet.addRationaleMessage("Failed executing query " + query + ": " + e.getMessage());
		}

		return tableResultSet.toJson();
	}

}
