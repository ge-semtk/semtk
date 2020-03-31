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
package com.ge.research.semtk.fdc;


import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.client.RestClient;

/**
 * Client to call an FDC Service
 */
public class FdcClient extends RestClient {
	public static String NODEGROUP_RET_KEY = "sgjson";
	
	/**
	 * Constructor taking an FdcClientConfig (e.g. for use when called by Dispatcher)
	 */
	public FdcClient(FdcClientConfig conf) {
		super(conf);
	}	


	/**
	 * Create a params JSON object
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void buildParametersJSON() throws Exception {
		FdcClientConfig config = (FdcClientConfig)conf;
		if (config.isGetNodegroup()) {
			parametersJSON.put("id", config.getNodegroupId());
		} else {
			parametersJSON.put("tables", config.getTableHashJson().toJSONString());
		}
	}	
	
	/**
	 * Execute /getNodegroup
	 * @return sgjson or null if service call fails
	 * @throws Exception on internal error
	 */
	public SparqlGraphJson executeGetNodegroup() throws Exception {
		FdcClientConfig config = (FdcClientConfig)conf;
		if (!config.isGetNodegroup()) {
			throw new Exception("Internal error: executing getNodegroup on client without nodegroupId parameter");
		}
		
		SimpleResultSet res = this.executeWithSimpleResultReturn();
		if (res.getSuccess()) {
			return new SparqlGraphJson(res.getResultJSON(NODEGROUP_RET_KEY));
		} else {
			return null;
		}
	}
}

