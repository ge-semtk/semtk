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

import java.net.ConnectException;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.client.RestClient;

public class StatusClient extends RestClient {

	public StatusClient (StatusClientConfig config) {
		this.conf = config;
	}
	
	@Override
	public void buildParametersJSON() throws Exception {
		// TODO: what do you think of this
		((StatusClientConfig) this.conf).addParameters(this.parametersJSON);

	}

	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response");
	}
	
	public void setJobId(String jobId){
		((StatusClientConfig)this.conf).setJobId(jobId);
	}
	
	/**
	 * Not meant to be used.
	 * @return
	 * @throws Exception
	 */
	public SimpleResultSet execute() throws ConnectException, EndpointNotFoundException, Exception {
		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute StatusClient with no enpoint specified.");
		}
		JSONObject resultJSON = (JSONObject)super.execute();	
		
		SimpleResultSet ret = (SimpleResultSet) SimpleResultSet.fromJson(resultJSON);  
		return ret;
	}
	
	/**
	 * 
	 * @return percent as integer 0 - 100
	 * @throws Exception
	 */
	public int execGetPercentComplete() throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/getPercentComplete");
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return res.getResultInt("percentComplete");
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
	
	/**
	 * 
	 * @return status string
	 * @throws Exception
	 */
	public String execGetStatus() throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/getStatus");
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return res.getResult("status");
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
	
	/**
	 * 
	 * @return status message string
	 * @throws Exception
	 */
	public String execGetStatusMessage() throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/getStatusMessage");
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return res.getResult("statusMessage");
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
	
	/**
	 * 
	 * @param percentComplete
	 * @param maxWaitMsec
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execWaitForPercentComplete(int percentComplete, int maxWaitMsec) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/waitForPercentComplete");
		this.parametersJSON.put("percentComplete", percentComplete);
		this.parametersJSON.put("maxWaitMsec", maxWaitMsec);
		
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("percentComplete");
			this.parametersJSON.remove("maxWaitMsec");
		}
	}
	/**
	 * 
	 * @param percentComplete
	 * @throws Exception
	 */
	public void execSetPercentComplete(int percentComplete) throws ConnectException, EndpointNotFoundException, Exception {
		execSetPercentComplete(percentComplete, "");
	}
	
	@SuppressWarnings("unchecked")
	public void execSetPercentComplete(int percentComplete, String message) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/setPercentComplete");
		this.parametersJSON.put("percentComplete", percentComplete);
		this.parametersJSON.put("message", message);

		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("percentComplete");
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void execSetSuccess() throws ConnectException, EndpointNotFoundException, Exception {
		execSetSuccess("");
	}
	
	@SuppressWarnings("unchecked")
	public void execSetSuccess(String message) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/setSuccess");
		this.parametersJSON.put("message", message);
		
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
	
	/**
	 * 
	 * @param message
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execSetFailure(String message) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/setFailure");
		this.parametersJSON.put("message", message);
		
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("message");
		}
	}
	/**
	 * 
	 * @throws Exception
	 */
	public void execDeleteJob() throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/deleteJob");
		
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
}
