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
import java.net.URL;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.client.RestClient;

public class ResultsClient extends RestClient {

	public ResultsClient (ResultsClientConfig config) {
		this.conf = config;
	}
	
	@Override
	public void buildParametersJSON() throws Exception {
		// TODO: what do you think of this
		((ResultsClientConfig) this.conf).addParameters(this.parametersJSON);

	}

	@Override
	public void handleEmptyResponse() throws Exception {
		// TODO:  why is this re-implemented for all subclasses
		throw new Exception("Received empty response");
	}
	

	/**
	 * Not meant to be used.
	 * @return
	 * @throws Exception
	 */
	public SimpleResultSet execute() throws ConnectException, EndpointNotFoundException, Exception{
		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute StatusClient with no enpoint specified.");
		}
		JSONObject resultJSON = (JSONObject)super.execute();	
		
		SimpleResultSet ret = (SimpleResultSet) SimpleResultSet.fromJson(resultJSON);  
		return ret;
	}
	
	/**
	 * Store file contents.  sample is shorter csv
	 * @param contents
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execStoreCsvResults(String jobId, String contents) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/storeCsvResults");
		this.parametersJSON.put("contents", contents);
		this.parametersJSON.put("jobId", jobId);

		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("contents");
			this.parametersJSON.remove("jobId");
		}
	}
	
	/**
	 * Store Table.  fullResult is csv.  sample is json.
	 * @param contents
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execStoreTableResults(String jobId, Table table) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/storeTableResults");
		this.parametersJSON.put("contents", table.toJson().toString());
		this.parametersJSON.put("jobId", jobId);

		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("contents");
			this.parametersJSON.remove("jobId");
		}
	}
	
	/**
	 * Store Table.  fullResult is csv.  sample is json.
	 * @param contents
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execStoreSingleFileResults(String jobId, String contents, String extension) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/storeSingleFileResults");
		this.parametersJSON.put("contents", contents);
		this.parametersJSON.put("extension", extension);
		this.parametersJSON.put("jobId", jobId);

		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("contents");
			this.parametersJSON.remove("e");
			this.parametersJSON.remove("jobId");
		}
	}
	
	@SuppressWarnings("unchecked")
	public URL [] execGetResults(String jobId) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/getResults");
		this.parametersJSON.put("jobId", jobId);

		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			String sampleUrlStr = res.getResult("sampleURL");
			String fullUrlStr = res.getResult("fullURL");
			URL sampleUrl = (!sampleUrlStr.equals("")) ? new URL(sampleUrlStr) : null;
			URL fullUrl = (!fullUrlStr.equals("")) ? new URL(fullUrlStr) : null;

			URL [] ret = { sampleUrl, fullUrl };
			return ret;
			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("contents");
			this.parametersJSON.remove("jobId");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void execDeleteStorage(String jobId) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/deleteStorage");
		this.parametersJSON.put("jobId", jobId);
		
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jobId");
		}
	}
}
