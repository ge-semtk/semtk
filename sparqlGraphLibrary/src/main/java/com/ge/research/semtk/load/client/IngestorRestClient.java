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


package com.ge.research.semtk.load.client;

import java.net.ConnectException;
import java.net.URL;

import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;


public class IngestorRestClient extends RestClient{

	TableResultSet lastResult = null;
	
	public IngestorRestClient (IngestorClientConfig config){
		this.conf = config;
	}
	
	@Override
	public void buildParametersJSON() throws Exception {
		// TODO: what do you think of this
		((IngestorClientConfig) this.conf).addParameters(this.parametersJSON);
	}

	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response");		
	}

	/**
	 * Not meant to be used.
	 * @return
	 * @throws Exception
	 */
	public TableResultSet execute() throws ConnectException, EndpointNotFoundException, Exception {
		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute IngestionClient with no enpoint specified.");
		}
		JSONObject resultJSON = (JSONObject)super.execute();	
		
		TableResultSet ret = new TableResultSet(resultJSON); 
		return ret;
	}
	
	public void execIngestionFromCsv(String template, String data) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvPrecheck");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		
		try{
			this.lastResult = this.execute();
			this.lastResult.throwExceptionIfUnsuccessful();
	
			return;
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
		}
	}
	public TableResultSet getLastResult(){
		return this.lastResult;
	}
	
	public Boolean getLastResultSuccess(){
		if(this.lastResult != null){
			return this.lastResult.getSuccess();
		}
		else{
			return null; //valid return for a class
		}
	}
}
