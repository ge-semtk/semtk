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
package com.ge.research.semtk.edc.client;

import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.client.QueryExecuteClient;
import com.ge.research.semtk.resultSet.TableOrJobIdResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Client to execute queries on the KairosDB REST service
 */
public class KairosDBClient extends QueryExecuteClient {

	public static String CONFIGJSONKEY_KAIROSDBURL = "kairosDBUrl";  // a key used in configJson.  
	
	/**
	 * Constructor taking an ExecuteClientConfig (e.g. for use when called by Dispatcher)
	 */
	public KairosDBClient(ExecuteClientConfig conf){
		super(conf);
	}	
	
	/**
	 * Constructor with individual parameters
	 */
	public KairosDBClient(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint, 
			String kairosDBUrl) throws Exception{
		super(new ExecuteClientConfig(serviceProtocol, serviceServer, servicePort, serviceEndpoint, new JSONObject()));
		((ExecuteClientConfig)conf).setConfigParam(CONFIGJSONKEY_KAIROSDBURL, kairosDBUrl); // e.g. "http://host.crd.ge.com:8080"
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
			throw new Exception("Unrecognized config for KairosDBClient");
		} 
		parametersJSON.put("connectionUrl", ((ExecuteClientConfig)conf).getConfigParam(KairosDBClient.CONFIGJSONKEY_KAIROSDBURL)); // e.g. "http://host.crd.ge.com:8080"
	}
	
	
	/**
	 * Execute a query 
	 */
	@SuppressWarnings("unchecked")
	public TableOrJobIdResultSet execute(String query) throws Exception{
		
		TableOrJobIdResultSet resultSet = new TableOrJobIdResultSet();

		try{
			LocalLogger.logToStdOut("KairosDBClient execute query " + query);				
			parametersJSON.put("queryJson", query);

			JSONObject resultJSON = (JSONObject)super.execute();	
			resultSet = new TableOrJobIdResultSet(resultJSON);		// TableResultSet will include failure/rationale if appropriate (e.g. if invalid KairosDB URL or query)

		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(e.getMessage());
		}
		return resultSet;
	}	
	
}

