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

import java.util.Arrays;

import org.json.simple.JSONObject;

import com.ge.research.semtk.services.client.RestClientConfig;

public class StatusClientConfig extends RestClientConfig {

	private String jobId;		

	public StatusClientConfig(String serviceProtocol,String serviceServer, int servicePort, 
							  String jobId) throws Exception {
		
		super(serviceProtocol, serviceServer, servicePort, "fake");
		this.setServiceEndpoint(null); // TODO this is a wonky way to circumvent the service endpoint -PEC
		
		// set non-inherited members
		this.jobId = jobId;
	}
	
	public String getJobId() {
		return jobId;
	}
	
	public void setJobId(String jobId){
		this.jobId = jobId;
	}
	
	@SuppressWarnings("unchecked")
	public void addParameters(JSONObject param) {
		param.put("jobId", jobId);
	}
}
