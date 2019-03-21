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


package com.ge.research.semtk.edc.client;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.client.QueryExecuteClient;
import com.ge.research.semtk.resultSet.TableResultSet;

/**
 * Client to call the HiveService
 */
public class HiveClient extends QueryExecuteClient {

	
	/**
	 * Constructor taking an ExecuteClientConfig (e.g. for use when called by Dispatcher)
	 */
	public HiveClient(ExecuteClientConfig conf) {
		super(conf);
	}	
	
	/**
	 * Constructor with individual parameters
	 */
	public HiveClient(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint, 
			String hiveHost, int hivePort, String hiveDatabase) throws Exception{
		super(new ExecuteClientConfig(serviceProtocol, serviceServer, servicePort, serviceEndpoint, new JSONObject()));
		((ExecuteClientConfig)conf).setConfigParam("host", hiveHost);
		((ExecuteClientConfig)conf).setConfigParam("port", String.valueOf(hivePort));
		((ExecuteClientConfig)conf).setConfigParam("database", hiveDatabase);
	}

	
	@Override
	public ExecuteClientConfig getConfig() {
		return (ExecuteClientConfig) this.conf;
	}

	/**
	 * Create a params JSON object
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void buildParametersJSON() throws Exception {
		
		if(! (this.conf instanceof ExecuteClientConfig)){			
			throw new Exception("Unrecognized config for HiveClient");
		}
		
		parametersJSON.put("host", ((ExecuteClientConfig)conf).getConfigParam("host"));		
		parametersJSON.put("port", ((ExecuteClientConfig)conf).getConfigParam("port"));
		parametersJSON.put("database", ((ExecuteClientConfig)conf).getConfigParam("database"));
	}

	
	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response");
	}
	


	/**
	 * Execute a call to the stat endpoint
	 */
	public TableResultSet executeStat(String table, List<ColumnOperation> columnOperations) throws Exception{

		parametersJSON.put("table", table);
		JSONArray list = new JSONArray();
		for (ColumnOperation co : columnOperations) {
			JSONObject jsonPair = new JSONObject ();
			jsonPair.put("column", co.column);
			jsonPair.put("operation", co.operation);
			list.add (jsonPair);
		}
		parametersJSON.put("columnOperations", list);

		JSONObject resultJSON = (JSONObject)super.execute();	

		TableResultSet retval = new TableResultSet();
		retval.readJson(resultJSON);
		return retval;
	}	

}

