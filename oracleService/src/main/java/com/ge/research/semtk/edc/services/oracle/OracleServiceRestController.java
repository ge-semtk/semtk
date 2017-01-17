/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.edc.services.oracle;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.json.simple.JSONObject;

import com.ge.research.semtk.query.rdb.OracleConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;


/**
 * Service to execute queries on an Oracle database
 */
@RestController
@RequestMapping("/oracleService")
public class OracleServiceRestController {			
	
	/**
	 * Execute query on Oracle
	 */
	@CrossOrigin
	@RequestMapping(value="/queryOracle", method= RequestMethod.POST)
	public JSONObject queryOracle(@RequestBody OracleServiceRequestBody requestBody){
			
		TableResultSet tableResultSet = new TableResultSet();
		
		try {			
			
			System.out.println("Connect to " + OracleConnector.getDatabaseURL(requestBody.host, Integer.valueOf(requestBody.port), requestBody.database));
			OracleConnector oc = new OracleConnector(requestBody.host, Integer.valueOf(requestBody.port), requestBody.database, requestBody.username, requestBody.password);
			Table table = oc.query(requestBody.query);				
			tableResultSet.addResults(table);							
			tableResultSet.setSuccess(true);
			
		} catch (Exception e) {			
			e.printStackTrace();			
			tableResultSet.setSuccess(false);
			tableResultSet.addRationaleMessage(e.getMessage());
		}
		
		return tableResultSet.toJson();
	}			
	
}
