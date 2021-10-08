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

import com.ge.research.semtk.connutil.EndpointNotFoundException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

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
	
	public String getJobId() {
		return ((StatusClientConfig)this.conf).getJobId();	
	}
	
	/**
	 * 
	 * @return percent as integer 0 - 100
	 * @throws Exception
	 */
	public int execGetPercentComplete() throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/getPercentComplete");
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
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
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return res.getResult("status");
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
	
	public boolean execIsSuccess() throws Exception {
		return this.execGetStatus().equals("Success");
	}
	
	/**
	 * 
	 * @return status message string
	 * @throws Exception
	 */
	public String execGetStatusMessage() throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/getStatusMessage");
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return res.getResult("statusMessage");
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
	
	public TableResultSet execGetJobsInfo() throws Exception {
		this.parametersJSON.clear();
		
		this.conf.setServiceEndpoint("status/getJobsInfo");
		this.conf.setMethod(RestClientConfig.Methods.POST);
		try {
			JSONObject jObj = (JSONObject) this.execute(false);
			TableResultSet res = new TableResultSet(jObj);
			res.throwExceptionIfUnsuccessful();
			return res;
		} finally {
			conf.setServiceEndpoint(null);
		}
	}

	
	/**
	 * Returns a table of creationTime, id, percentComplete, statusMessage, userName, status
	 * Note userName used to retrieve results is set in the ThreadAuthenticator.
	 * @return
	 * @throws Exception
	 */
	public Table getJobsInfo() throws Exception {
		Table t = this.execGetJobsInfo().getTable();
		return t;
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
			SimpleResultSet res = this.executeWithSimpleResultReturn();
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
	 * Wait until percentComplete is reached or maxWaitMsec has elapsed
	 * @param percentComplete
	 * @param maxWaitMsec
	 * @return percentComplete
	 * @throws ConnectException
	 * @throws EndpointNotFoundException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public int execWaitForPercentOrMsec(int percentComplete, int maxWaitMsec) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/waitForPercentOrMsec");
		this.parametersJSON.put("percentComplete", percentComplete);
		this.parametersJSON.put("maxWaitMsec", maxWaitMsec);
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return Integer.parseInt(res.getResult("percentComplete"));
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

		LocalLogger.logToStdErr("Set percent complete: " + String.valueOf(percentComplete) + "% " + message); // PEC TODO debugging only
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("percentComplete");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void execIncrementPercentComplete(int increment, int max) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/incrementPercentComplete");
		this.parametersJSON.put("increment", increment);
		this.parametersJSON.put("max", max);

		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("increment");
			this.parametersJSON.remove("max");

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
			SimpleResultSet res = this.executeWithSimpleResultReturn();
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
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("message");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void execSetName(String name) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/setName");
		this.parametersJSON.put("name", name);
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("name");
		}
	}
	/**
	 * 
	 * @throws Exception
	 */
	public void execDeleteJob() throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("status/deleteJob");
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return;
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public SparqlEndpointInterface getJobTrackerSei(String user, String pass) throws Exception {
		conf.setServiceEndpoint("status/getJobTrackerSei");
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(res.getResultJSON("seiJson"));
			sei.setUserAndPassword(user, pass);
			return sei;
			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
	}
	
	
	/**
	 * Create a copy with a new jobId
	 * @return
	 * @throws Exception
	 */
	public StatusClient cloneWithNewJobId() throws Exception {
		// clone the config with a new jobId
		StatusClientConfig newConfig = new StatusClientConfig(	this.conf.getServiceProtocol(), 
																this.conf.getServiceServer(), 
																this.conf.getServicePort());
		return new StatusClient(newConfig);
	}
	
	/**
	 * Create a copy with the given jobId
	 * @param jobId
	 * @return
	 * @throws Exception
	 */
	public StatusClient cloneWithNewJobId(String jobId) throws Exception {
		// clone the config with a new jobId
		StatusClientConfig newConfig = new StatusClientConfig(	this.conf.getServiceProtocol(), 
																this.conf.getServiceServer(), 
																this.conf.getServicePort(),
																jobId);
		return new StatusClient(newConfig);
	}
	
	/**
	 * Preferred way to wait for a job to complete
	 * @param jobId
	 * @param freqMsec - a ping freq such as 10,000.  Will return sooner if job finishes
	 * @param maxTries - throw exception after this many tries
	 * @throws Exception
	 */
	private void waitForCompletion(String jobId, int freqMsec, int maxTries ) throws Exception {
		int percent = 0;
		
		for (int i=0; i < maxTries; i++) {
			percent = this.execWaitForPercentOrMsec(100, freqMsec);
			if (percent == 100) {
				return;
			}
		}
		throw new Exception("Job " + jobId + " is only " + String.valueOf(percent) + "% complete after " + String.valueOf(maxTries) + " tries.");
	}
	
	// wait essentially forever
	public void waitForCompletion(String jobId) throws Exception {
		this.waitForCompletion(jobId, 9000, Integer.MAX_VALUE);
	}
	
	/**
	 * Only returns cleanly on success
	 * @throws Exception
	 */
	public void waitForCompletionSuccess() throws Exception {
		this.waitForCompletion(this.getJobId(), 10000, 36);
		if (!this.execIsSuccess()) {
			throw new Exception(this.execGetStatusMessage());
		}
	}
}
