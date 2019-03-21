/**
 ** Copyright 2018 General Electric Company
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
import com.ge.research.semtk.resultSet.TableResultSet;

/**
 * Generic execution client - sends query only
 */
public class GenericQueryExecuteClient extends QueryExecuteClient {

	
	/**
	 * Constructor taking an ExecuteClientConfig (e.g. for use when called by Dispatcher)
	 */
	public GenericQueryExecuteClient(ExecuteClientConfig conf) {
		super(conf);
	}	
	
	/**
	 * Constructor with individual parameters
	 */
	public GenericQueryExecuteClient(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint) throws Exception{
		super(new ExecuteClientConfig(serviceProtocol, serviceServer, servicePort, serviceEndpoint, new JSONObject()));
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
			throw new Exception("Unrecognized config for GenericQueryExecuteClient");
		}
	}

	
	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response");
	}
	
	

}

