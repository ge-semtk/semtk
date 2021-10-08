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

import org.json.simple.JSONObject;

import com.ge.research.semtk.connutil.EndpointNotFoundException;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.client.RestClientConfig;


public class IngestorRestClient extends SharedIngestNgeClient {

	RecordProcessResults lastResult = null;
	
	public IngestorRestClient (IngestorClientConfig config){
		super(config, "ingestion/");
	}
	public IngestorRestClient (){
		super("ingestion/");
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
	public RecordProcessResults execute() throws ConnectException, EndpointNotFoundException, Exception {
		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute IngestionClient with no enpoint specified.");
		}
		JSONObject resultJSON = (JSONObject)super.execute();	
		
		RecordProcessResults ret = new RecordProcessResults(resultJSON); 

		// the ingestor responds with a bit of extra information that should be added. 
		// the unprocessed result is available in case one wants to see everything not included in the strict table version.
		
		return ret;
	}
	
	/**
	 * execute endpoints that return simple jobId
	 * @return
	 * @throws Exception
	 */
	private String executeToJobId() throws ConnectException, EndpointNotFoundException, Exception {
		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute IngestionClient with no enpoint specified.");
		}
		
		SimpleResultSet ret = SimpleResultSet.fromJson((JSONObject)super.execute());
		ret.throwExceptionIfUnsuccessful("Ingestion error");
		
		return ret.getResult(SimpleResultSet.JOB_ID_RESULT_KEY);
	}
	
	/**
	 * Ingest from CSV, with the option to override the SPARQL connection
	 * @param template the template (as a String)
	 * @param data the data (as a String)
	 * @param sparqlConnectionOverride the SPARQL connection as a String, or null to use the connection from the template.
	 */
	public void execIngestionFromCsv(String template, String data, String sparqlConnectionOverride) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvWithNewConnectionPrecheck");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		this.parametersJSON.put("connectionOverride", sparqlConnectionOverride);
		
		try{
			this.lastResult = this.execute();	
			return;
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
			this.parametersJSON.remove("connectionOverride");
		}
	}
	
	public void execIngestionFromCsv(String template, String data, String sparqlConnectionOverride, boolean trackFlag, String overrideBaseURI) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvWithNewConnectionPrecheck");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		this.parametersJSON.put("connectionOverride", sparqlConnectionOverride);
		this.parametersJSON.put("trackFlag", trackFlag);
		this.parametersJSON.put("overrideBaseURI", overrideBaseURI);

		try{
			this.lastResult = this.execute();	
			return;
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
			this.parametersJSON.remove("connectionOverride");
			this.parametersJSON.remove("trackFlag");
			this.parametersJSON.remove("overrideBaseURI");

		}
	}
	
	public String execIngestionFromCsvAsync(String template, String data, String sparqlConnectionOverride, boolean trackFlag, String overrideBaseURI) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvWithNewConnectionPrecheckAsync");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		this.parametersJSON.put("connectionOverride", sparqlConnectionOverride);
		this.parametersJSON.put("trackFlag", trackFlag);
		this.parametersJSON.put("overrideBaseURI", overrideBaseURI);

		try{
			return this.executeToJobId();
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
			this.parametersJSON.remove("connectionOverride");
			this.parametersJSON.remove("trackFlag");
			this.parametersJSON.remove("overrideBaseURI");

		}
	}
	/**
	 * Simpler API with modern naming (not "exec" since it doesn't return a ResultSet)
	 * Should be the default ingest function
	 * @param sgJsonWithOverride
	 * @param data
	 * @return
	 * @throws ConnectException
	 * @throws EndpointNotFoundException
	 * @throws Exception
	 */
	public String ingestionFromCsvAsync(SparqlGraphJson sgJsonWithOverride, String data) throws ConnectException, EndpointNotFoundException, Exception {
		return this.execIngestionFromCsvAsync(sgJsonWithOverride.toJson().toJSONString(), data, sgJsonWithOverride.getSparqlConnJson().toJSONString());
	}
	
	/**
	 * Ingest from CSV, with the option to override the SPARQL connection
	 * @param template the template (as a String)
	 * @param data the data (as a String)
	 * @param sparqlConnectionOverride the SPARQL connection as a String, or null to use the connection from the template.
	 * @return jobId
	 */
	public String execIngestionFromCsvAsync(String template, String data, String sparqlConnectionOverride) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvWithNewConnectionPrecheckAsync");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		this.parametersJSON.put("connectionOverride", sparqlConnectionOverride);
		
		try{
			return this.executeToJobId();
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
			this.parametersJSON.remove("connectionOverride");
		}
	}
	
	/**
	 * Ingest from CSV.
	 * @param template the template (as a String)
	 * @param data the data (as a String)
	 */
	public void execIngestionFromCsv(String template, String data) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvPrecheck");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		
		try{
			this.lastResult = this.execute();	
			return;
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
		}
	}
	


	
	public RecordProcessResults getLastResult(){
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
