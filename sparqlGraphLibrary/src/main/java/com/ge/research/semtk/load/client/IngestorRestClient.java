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
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.connutil.EndpointNotFoundException;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.client.RestClientConfig;


public class IngestorRestClient extends SharedIngestNgeClient {

	RecordProcessResults lastResult = null;
	ArrayList<String> warnings = null;
	
	public IngestorRestClient (IngestorClientConfig config){
		super(config, "ingestion/");
	}
	public IngestorRestClient (){
		super("ingestion/");
	}

	/**
	 * Get warnings from last async ingest call, if any.  Otherwise null.
	 * @return
	 */
	public ArrayList<String> getWarnings() {
		return warnings;
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
	private String executeToJobIdSaveWarnings() throws ConnectException, EndpointNotFoundException, Exception {
		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute IngestionClient with no enpoint specified.");
		}
		
		SimpleResultSet ret = SimpleResultSet.fromJson((JSONObject)super.execute());
		ret.throwExceptionIfUnsuccessful("Ingestion error");
		
		try {
			// check for warnings
			this.warnings = null;
			JSONArray jArr = ret.getResultJSONArray(SimpleResultSet.WARNINGS_RESULT_KEY);
			this.warnings = new ArrayList<String>();
			this.warnings.addAll(jArr);
		} catch (Exception e) {
			// leave warnings null
		}
		
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
	
	/**
	 * 
	 * Puts any warnings into getWarnings()
	 * @param template
	 * @param data
	 * @param sparqlConnectionOverride
	 * @param trackFlag
	 * @param overrideBaseURI
	 * @return
	 * @throws ConnectException
	 * @throws EndpointNotFoundException
	 * @throws Exception
	 */
	public String execIngestionFromCsvAsync(String template, String data, String sparqlConnectionOverride, boolean trackFlag, String overrideBaseURI) throws ConnectException, EndpointNotFoundException, Exception{
		return this.execIngestionFromCsvAsync(template, data, sparqlConnectionOverride, false, false, trackFlag, overrideBaseURI);
	}
	
	/**
	 * Fullest functioning Async endpoint
	 * @param template
	 * @param data
	 * @param sparqlConnectionOverride
	 * @param skipPrecheck
	 * @param skipIngest
	 * @param trackFlag
	 * @param overrideBaseURI
	 * @return
	 * @throws ConnectException
	 * @throws EndpointNotFoundException
	 * @throws Exception
	 */
	public String execIngestionFromCsvAsync(String template, String data, String sparqlConnectionOverride, boolean skipPrecheck, boolean skipIngest, boolean trackFlag, String overrideBaseURI) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvAsync");
		this.parametersJSON.put("template", template);
		this.parametersJSON.put("data", data);
		this.parametersJSON.put("connectionOverride", sparqlConnectionOverride);
		this.parametersJSON.put("skipPrecheck", skipPrecheck);
		this.parametersJSON.put("skipIngest", skipIngest);
		this.parametersJSON.put("trackFlag", trackFlag);
		this.parametersJSON.put("overrideBaseURI", overrideBaseURI);

		try{
			return this.executeToJobIdSaveWarnings();
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("template");
			this.parametersJSON.remove("data");
			this.parametersJSON.remove("connectionOverride");
			this.parametersJSON.remove("skipPrecheck");
			this.parametersJSON.remove("skipIngest");
			this.parametersJSON.remove("trackFlag");
			this.parametersJSON.remove("overrideBaseURI");
		}
	}
	
	/**
	 * Execute ingestion to jobId
	 * @param classUri
	 * @param idRegex - can be null or empty
	 * @param data
	 * @param sparqlConnectionOverride
	 * @param trackFlag
	 * @param overrideBaseURI
	 * @return
	 * @throws ConnectException
	 * @throws EndpointNotFoundException
	 * @throws Exception
	 */
	public String execFromCsvUsingClassTemplate(String classUri, String idRegex, String data, String sparqlConnection, boolean trackFlag, String overrideBaseURI) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/fromCsvUsingClassTemplate");
		this.parametersJSON.put("classURI", classUri);
		if (idRegex != null && ! idRegex.isBlank())
			this.parametersJSON.put("idRegex", idRegex);
		this.parametersJSON.put("data", data);
		this.parametersJSON.put("connection", sparqlConnection);
		this.parametersJSON.put("trackFlag", trackFlag);
		this.parametersJSON.put("overrideBaseURI", overrideBaseURI);

		try{
			return this.executeToJobIdSaveWarnings();
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("classURI");
			this.parametersJSON.remove("connection");
			this.parametersJSON.remove("idRegex");
			this.parametersJSON.remove("data");
			this.parametersJSON.remove("trackFlag");
			this.parametersJSON.remove("overrideBaseURI");
		}
	}
	
	
	public JSONObject execGetClassTemplateAndCsv(String classUri, String idRegex, String sparqlConnection) throws ConnectException, EndpointNotFoundException, Exception{
		conf.setServiceEndpoint("ingestion/getClassTemplateAndCsv");
		this.parametersJSON.put("classURI", classUri);
		if (idRegex != null && ! idRegex.isBlank())
			this.parametersJSON.put("idRegex", idRegex);
		this.parametersJSON.put("connection", sparqlConnection);
		

		try{
			return this.executeToJson();
		} 
		finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("classURI");
			this.parametersJSON.remove("connection");
			this.parametersJSON.remove("idRegex");
		}
	}
	
	/**
	 * Simpler API with modern naming (not "exec" since it doesn't return a ResultSet)
	 * Should be the default ingest function
	 * Puts any warnings into getWarnings()
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
	 * Puts any warnings into getWarnings()
	 * @param template the template (as a String)
	 * @param data the data (as a String)
	 * @param sparqlConnectionOverride the SPARQL connection as a String, or null to use the connection from the template.
	 * @return jobId
	 */
	public String execIngestionFromCsvAsync(String template, String data, String sparqlConnectionOverride) throws ConnectException, EndpointNotFoundException, Exception{
		return this.execIngestionFromCsvAsync(template, data, sparqlConnectionOverride, false, false, false, null);
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
