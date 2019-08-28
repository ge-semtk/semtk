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

package com.ge.research.semtk.api.nodeGroupExecution.client;

import org.json.simple.JSONObject;

import com.ge.research.semtk.services.client.RestClientConfig;

public class NodeGroupExecutionClientConfig  extends RestClientConfig{
	protected String serviceUser = null; 
	protected String servicePassword = null;
	
	public NodeGroupExecutionClientConfig(String serviceProtocol, String serviceServer,
			int servicePort) throws Exception {
		
		
		super(serviceProtocol, serviceServer, servicePort, "fake");
		
	}
	
	public NodeGroupExecutionClientConfig(String serviceProtocol, String serviceServer, int servicePort, String user, String password) throws Exception {
		
		super(serviceProtocol, serviceServer, servicePort, "fake");
		serviceUser = user;
		servicePassword = password;
	}

	@SuppressWarnings("unchecked")
	public void addParameters(JSONObject param) {
	}
	
	public String getServiceUser() {
		return serviceUser;
	}

	public String getServicePassword() {
		return servicePassword;
	}
	
}
