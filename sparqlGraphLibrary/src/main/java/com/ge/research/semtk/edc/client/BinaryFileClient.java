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

import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.lib.resultSet.TableOrJobIdResultSet;
import com.ge.research.semtk.querygen.client.QueryExecuteClient;
import com.ge.research.semtk.resultSet.TableResultSet;

/**
 * Client to call the BinaryFileService
 */
public class BinaryFileClient extends QueryExecuteClient {

	/**
	 * Constructor taking an ExecuteClientConfig (e.g. for use when called by Dispatcher)
	 */
	public BinaryFileClient(ExecuteClientConfig conf) {
		super(conf);
	}	

	/**
	 * Constructor with individual parameters
	 */
	public BinaryFileClient(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint) throws Exception{
		super(new ExecuteClientConfig(serviceProtocol, serviceServer, servicePort, serviceEndpoint, new JSONObject()));
	}

	/**
	 * Create a params JSON object
	 */
	@Override
	public void buildParametersJSON() throws Exception {
		if(! (this.conf instanceof ExecuteClientConfig)){			
			throw new Exception("Unrecognized config for BinaryFileClient");
		}
		parametersJSON.put("jobId", this.getJobId());
	}
	
	
	/**
	 * Execute a query (stage a file)
	 * Sample query: "hdfs://test1-98231834.img test1.img"
	 */

	//@SuppressWarnings("unchecked")
	//public TableResultSet execute(String query) throws Exception{
	


}

