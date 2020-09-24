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
package com.ge.research.semtk.edc.client;

import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.client.QueryExecuteClient;

/**
 * Client to call the FileStagingService
 */
public class FileStagingClient extends QueryExecuteClient {

	/**
	 * Constructor taking an ExecuteClientConfig (e.g. for use when called by Dispatcher)
	 */
	public FileStagingClient(ExecuteClientConfig conf) {
		super(conf);
	}	

	/**
	 * Constructor with individual parameters
	 */
	public FileStagingClient(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint) throws Exception{
		super(new ExecuteClientConfig(serviceProtocol, serviceServer, servicePort, serviceEndpoint, new JSONObject()));
	}

	/**
	 * Create a params JSON object
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void buildParametersJSON() throws Exception {		
		if(! (this.conf instanceof ExecuteClientConfig)){			
			throw new Exception("Unrecognized config for FileStagingClient");
		}
		parametersJSON.put("jobId", this.getJobId());
	}
	
}

