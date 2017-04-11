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

import org.json.simple.JSONObject;

import com.ge.research.semtk.services.client.RestClientConfig;

public class ExecuteClientConfig extends RestClientConfig {
	
	JSONObject configJson = null;
	
	public ExecuteClientConfig(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint, JSONObject configJson)
			throws Exception {
		super(serviceProtocol, serviceServer, servicePort, serviceEndpoint);
		this.configJson = configJson;
	}
	
	public ExecuteClientConfig clone(JSONObject configJson) throws Exception {
		return new ExecuteClientConfig(	this.serviceProtocol,
										this.serviceServer,
										this.servicePort,
										this.serviceEndpoint,
										configJson);
	}

	public void setConfigParam(String key, String value){
		configJson.put(key, value);
	}
	
	public String getConfigParam(String key) throws Exception{
		try{
			return (String) configJson.get(key);
		}catch(Exception e){
			throw new Exception("Config parameter '" + key + "' not found");
		}
	}
	
	public String getConfigJsonString() {
		return (this.configJson == null) ? "" : this.configJson.toJSONString();
	}
}
