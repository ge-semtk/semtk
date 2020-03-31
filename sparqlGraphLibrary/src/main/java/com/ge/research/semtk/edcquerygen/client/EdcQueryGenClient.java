/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.edcquerygen.client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.client.QueryGenClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;

/**
 * Client for the EdcQueryGenerationService
 */
public class EdcQueryGenClient extends QueryGenClient {
		
	/**
	 * Constructor
	 */
	public EdcQueryGenClient(RestClientConfig conf){
		this.conf = conf;
	}

	/**
	 * Create a params JSON object
	 */
	@Override
	public void buildParametersJSON() throws Exception {
		// nothing to do here
	}
	
	/**
	 * Generate queries
	 */
	@SuppressWarnings("unchecked")
	public TableResultSet execute(Table locationAndValueInfoTable, JSONObject runtimeConstraints, QueryFlags flags) throws Exception {
		
		parametersJSON.put("locationAndValueInfoTableJsonStr", locationAndValueInfoTable.toJson().toString());
		if(runtimeConstraints != null){
			parametersJSON.put("constraintsJsonStr", runtimeConstraints.toJSONString());
		}
		if(flags != null && !flags.isEmpty()){
			parametersJSON.put("flagsJsonArrayStr", flags.toJSONString());
		}
		
		// execute
		JSONObject resultJSON = (JSONObject)super.execute();	
		
		// create TableResultSet to return
		TableResultSet retval = new TableResultSet();
		retval.readJson(resultJSON);
		return retval;
	}

}

