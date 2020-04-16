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


package com.ge.research.semtk.services.arangodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

import org.json.simple.JSONObject;

import com.ge.research.semtk.query.rdb.ArangoDbConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.arangodb.ArangoDbProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutilib.requests.DatabaseQueryRequest;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Service to execute AQL queries on ArangoDB
 */
@CrossOrigin
@RestController
@RequestMapping("/arangodb")
public class ArangoDbServiceRestController {

	@Autowired
	ArangoDbProperties props;
	@Autowired 
	private ApplicationContext appContext;
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();		
		props.validateWithExit();
	}
	
	/**
	 * Execute query in ArangoDB
	 */
	@CrossOrigin
	@RequestMapping(value="/query", method= RequestMethod.POST)
	public JSONObject queryArangoDb(@RequestBody DatabaseQueryRequest requestBody){
		
		// get info from the request
		String host = requestBody.host;
		String port = requestBody.port;
		String query = requestBody.query;
		String database = requestBody.database;

		// get credentials from property file
		String user = props.getUser();
		String password = props.getPassword();
		
		TableResultSet tableResultSet = new TableResultSet();
		try {
			LocalLogger.logToStdOut("Connecting to ArangoDB...");
			ArangoDbConnector connector = new ArangoDbConnector(host, Integer.valueOf(port), database, user, password);
			LocalLogger.logToStdOut("Run ArangoDB query on database " + database + ": " + query);
			Table table = connector.query(query);
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
