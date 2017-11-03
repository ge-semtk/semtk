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


package com.ge.research.semtk.services.results;

/*
 * TODO
 * Move properties to a common place.
 *    Do we want spring boot dependencies in sparqlGraphLibraries?  Seems bad.
 *    
 * Move query functions (anything that knows the model) to new package in libraries like
 *     com.ge.research.semtk.edc.services
 * What is the best way to use properties in this situation.
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.resultsStorage.GenericJsonBlobResultsSerializer;
import com.ge.research.semtk.edc.resultsStorage.GenericJsonBlobResultsStorage;
import com.ge.research.semtk.edc.resultsStorage.JsonLdResultsSerializer;
import com.ge.research.semtk.edc.resultsStorage.JsonLdResultsStorage;
import com.ge.research.semtk.edc.resultsStorage.TableResultsSerializer;
import com.ge.research.semtk.edc.resultsStorage.TableResultsStorage;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.Utility;

/**
 * Service to get query results.
 */
@CrossOrigin
@RestController
@RequestMapping("/results")
public class ResultsServiceRestController {
 	static final String SERVICE_NAME = "ResultsService";

	@Autowired
	ResultsProperties prop;
	@Autowired
	ResultsEdcConfigProperties edc_prop;
	@Autowired
	ResultsLoggingProperties log_prop;
	
	
	@CrossOrigin
	@RequestMapping(value="/storeJsonLdResults", method=RequestMethod.POST)
	public JSONObject storeJsonLdResults(@RequestBody JsonLdStoreRequestBody requestBody){

		// logging
		LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);	 
		LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResultsJsonInitialize start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeJsonLdResults start JobId=" + requestBody.jobId);

		SimpleResultSet res = new SimpleResultSet();
		try{
			URL url = getJsonLdResultsStorage().storeResults(requestBody.jobId, requestBody.getJsonRenderedHeader(), requestBody.getJsonRenderedGraph());
			getJobTracker().setJobResultsURL(requestBody.jobId, url);  // store URL with the job
		    res.setSuccess(true);
		} catch(Exception e){
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "storeJsonLdResults", e);
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeJsonLdResults exception", "message", e.toString());
		    e.printStackTrace();
		}    	
		return res.toJson();
	}

	@CrossOrigin
	@RequestMapping(value="/getJsonLdResults", method= RequestMethod.POST)
	public void getJsonLdResults(@RequestBody ResultsRequestBody requestBody, HttpServletResponse resp){
	
		try{
	    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
			JsonLdResultsSerializer retval = getJsonLdResultsStorage().getJsonLd(url);
			
			retval.writeToStream(resp.getWriter());
			
	    } catch (Exception e) {
	    	//   LoggerRestClient.easyLog(logger, "ResultsService", "getTableResultsCsv exception", "message", e.toString());
		    e.printStackTrace();
	    }

		System.err.println("done writing output");
	}

	@CrossOrigin
	@RequestMapping(value="/storeJsonBlobResults", method=RequestMethod.POST)
	public JSONObject storeJsonBlobResults(@RequestBody JsonBlobRequestBody requestBody){
		
		// logging
		LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);	 
		LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResultsJsonInitialize start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeJsonLdResults start JobId=" + requestBody.jobId);

		SimpleResultSet res = new SimpleResultSet();
		try{
			URL url = getJsonBlobResultsStorage().storeResults(requestBody.jobId, requestBody.getJsonBlobString());
			getJobTracker().setJobResultsURL(requestBody.jobId, url);  // store URL with the job
		    res.setSuccess(true);
		} catch(Exception e){
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "storeJsonBlobResults", e);
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeJsonLdResults exception", "message", e.toString());
		    e.printStackTrace();
		}    	
		return res.toJson();	
	}
	
	@CrossOrigin
	@RequestMapping(value="/getJsonBlobResults", method=RequestMethod.POST)
	public void getJsonBlobResults(@RequestBody ResultsRequestBody requestBody, HttpServletResponse resp){
		try{
	    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
			GenericJsonBlobResultsSerializer retval = getJsonBlobResultsStorage().getJsonBlob(url);
			
			retval.writeToStream(resp.getWriter());
			
	    } catch (Exception e) {
	    	//   LoggerRestClient.easyLog(logger, "ResultsService", "getTableResultsCsv exception", "message", e.toString());
		    e.printStackTrace();
	    }

		System.err.println("done writing output");
	}
	
	
	/**
	 * Call 1 of 3 for storing JSON results.
	 * Writes JSON start, column names, and column types.
	 */
	@CrossOrigin
	@RequestMapping(value="/storeTableResultsJsonInitialize", method=RequestMethod.POST)
	public JSONObject storeTableResultsJsonInitialize(@RequestBody ResultsRequestBodyInitializeTableResultsJson requestBody){
		
		// logging
		LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);	 
		LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResultsJsonInitialize start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeTableResultsJsonInitialize start JobId=" + requestBody.jobId);

		SimpleResultSet res = new SimpleResultSet();
		try{
			getTableResultsStorage().storeTableResultsJsonInitialize(requestBody.jobId, requestBody.getJsonRenderedHeader());
			
		    res.setSuccess(true);
		} catch(Exception e){
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "storeTableResultsJsonInitialize", e);
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResultsJsonInitialize exception", "message", e.toString());
		    e.printStackTrace();
		}    	
		return res.toJson();
	}
	
	/**
	 * Call 2 of 3 for storing JSON results.  Repeat for multiple batches.
	 * Writes rows of data.  
	 * The input data ("contents") must:
	 * 		1) be ZLIB compressed, using Utility.compress() or other
	 * 		2) omit tailing comma for the last row of the last call
	 * 
	 * Sample input (decompressed):
	 * ["a1","b1","c1"],
	 * ["a2","b2","c2"],
	 * ["a3","b3","c3"]
	 */
	@CrossOrigin
	@RequestMapping(value="/storeTableResultsJsonAddIncremental", method=RequestMethod.POST)
	public JSONObject storeTableResultsJsonAddIncremental(@RequestBody ResultsRequestBodyFileExtContents requestBody){

		// logging
		LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);	 
		LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResultsJsonAddIncremental start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeTableResultsJsonAddIncremental start JobId=" + requestBody.jobId);

		SimpleResultSet res = new SimpleResultSet();
		try{
			getTableResultsStorage().storeTableResultsJsonAddIncremental(requestBody.jobId, Utility.decompress(requestBody.getContents()));
		    res.setSuccess(true);
		}
		catch(Exception e){
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "storeTableResultsJsonAddIncremental", e);
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResultsJsonAddIncremental exception", "message", e.toString());
		    e.printStackTrace();
		}    	
		return res.toJson();
	}
	
	/**
	 * Call 3 of 3 for storing JSON results.
	 * Writes row count and JSON end.
	 */
	@CrossOrigin
	@RequestMapping(value="/storeTableResultsJsonFinalize", method=RequestMethod.POST)
	public JSONObject storeTableResultsJsonFinalize(@RequestBody ResultsRequestBodyFinalizeTableResultsJson requestBody){
		
		// logging
		LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);	 
		LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResultsJsonFinalize start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeTableResultsJsonFinalize start JobId=" + requestBody.jobId);

		SimpleResultSet res = new SimpleResultSet();
		try{
			URL url = getTableResultsStorage().storeTableResultsJsonFinalize(requestBody.jobId); 
		    getJobTracker().setJobResultsURL(requestBody.jobId, url);  // store URL with the job		
		    res.setSuccess(true);
		} catch(Exception e){
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "storeTableResultsJsonFinalize", e);
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResultsJsonFinalize exception", "message", e.toString());
		    e.printStackTrace();
		}    	
		return res.toJson();
	}
	
	/**
	 * Return a CSV file containing results (possibly truncated) for job
	 */
	@CrossOrigin
	@RequestMapping(value="/getTableResultsCsv", method= RequestMethod.POST)
	public void getTableResultsCsv(@RequestBody ResultsRequestBodyCsvMaxRows requestBody, HttpServletResponse resp){
	
		try{
	    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
			TableResultsSerializer retval = getTableResultsStorage().getCsvTable(url, requestBody.maxRows, requestBody.getStartRow()); 			
			
			if(requestBody.getAppendDownloadHeaders()){
			
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + requestBody.jobId + ".csv" + "\"; filename*=\"" + requestBody.jobId + ".csv" +"\"");
				retval.writeToStream(resp.getWriter());
			}
			else{
				retval.writeToStream(resp.getWriter());
			}
	    } catch (Exception e) {
	    	//   LoggerRestClient.easyLog(logger, "ResultsService", "getTableResultsCsv exception", "message", e.toString());
		    e.printStackTrace();
	    }

		System.err.println("done writing output");
	}

	@CrossOrigin
	@RequestMapping(value="/getTableResultsJsonForWebClient", method= RequestMethod.GET)
	public ResponseEntity<Resource> getTableResultsJsonForWebClient(@RequestParam String jobId, @RequestParam(required=false) Integer maxRows, HttpServletResponse resp){
	
		try{
			if(jobId == null){ throw new Exception("no jobId passed to endpoint."); }
			
	    	URL url = getJobTracker().getFullResultsURL(jobId);  
			TableResultsSerializer retval = getTableResultsStorage().getJsonTable(url, maxRows, 0); 			
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + jobId + ".json" + "\"; filename*=\"" + jobId + ".json" +"\"");
			retval.writeToStream(resp.getWriter());
			
	    } catch (Exception e) {
	    	//   LoggerRestClient.easyLog(logger, "ResultsService", "getTableResultsCsv exception", "message", e.toString());
		    e.printStackTrace();
	    }
		// if nothing, return nothing
		System.err.println("done writing output");
		return null;
	}

	
	@CrossOrigin
	@RequestMapping(value="/getTableResultsCsvForWebClient", method= RequestMethod.GET)
	public void getTableResultsCsvForWebClient(@RequestParam String jobId, @RequestParam(required=false) Integer maxRows, HttpServletResponse resp){
	
		try{
			if(jobId == null){ throw new Exception("no jobId passed to endpoint."); }
			
	    	URL url = getJobTracker().getFullResultsURL(jobId);  
			TableResultsSerializer retval = getTableResultsStorage().getCsvTable(url, maxRows, 0); 			

			resp.setHeader("Content-Disposition", "attachment; filename=\"" + jobId + ".csv" + "\"; filename*=\"" + jobId + ".csv" +"\"");
			retval.writeToStream(resp.getWriter());
			
	    } catch (Exception e) {
	    	//   LoggerRestClient.easyLog(logger, "ResultsService", "getTableResultsCsv exception", "message", e.toString());
		    e.printStackTrace();
	    }
		System.err.println("done writing output");
	}
	
	@CrossOrigin
	@RequestMapping(value="/getTableResultsRowCount", method= RequestMethod.POST)
	public JSONObject getTableResultsRowCount(@RequestBody ResultsRequestBody requestBody, HttpServletResponse resp){
	
		SimpleResultSet retTrue = null;
		
		try{
	    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
			int retval = getTableResultsStorage().getResultsRowCount(url);	
			
			retTrue = new SimpleResultSet(true);
			retTrue.addResult("rowCount", retval);
	    } catch (Exception e) {
		    retTrue = new SimpleResultSet(false);
		    retTrue.addRationaleMessage(SERVICE_NAME, "getTableResultsRowCount", e);
	    }
		
		return retTrue.toJson(); 
	}
	
	
	/**
	 * Return a JSON object containing results (possibly truncated) for job
	 */
	@CrossOrigin
	@RequestMapping(value="/getTableResultsJson", method= RequestMethod.POST)
	public void getTableResultsJson(@RequestBody ResultsRequestBodyMaxRows requestBody, HttpServletResponse resp){
	
		try{
	    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
			TableResultsSerializer retval = getTableResultsStorage().getJsonTable(url, requestBody.maxRows, requestBody.getStartRow());	
			
			
			wrapJsonInTableToSend(retval, resp);
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
		System.err.println("done writing output");
	}
	
	private void wrapJsonInTableToSend(TableResultsSerializer trs, HttpServletResponse resp) throws Exception, Exception{
		
		PrintWriter outPrint = resp.getWriter();
		
		// write table result set preamble.
		outPrint.write("{\"message\":\"operations succeeded.\",\"table\":{\"@table\":");
		outPrint.flush();
		trs.writeToStream(resp.getWriter());
		// close table result set.
		outPrint.write("},\"status\":\"success\"}");
		outPrint.flush();
		outPrint.close();
		
	}
	
	/**
	 * Gets a CSV URL and a JSON URL containing results for a job 
	 * Keeping this only for backwards compatibility with v1.3 or earlier
	 * "fullURL"   is the CSV  file (retaining bad label for backward compatibility)
	 * "sampleURL" is the JSON file (retaining bad label for backward compatibility)
	 */
	@CrossOrigin
	@RequestMapping(value="/getResults", method= RequestMethod.POST)
	public JSONObject getResults(@RequestBody ResultsRequestBody requestBody){
		    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Results Service", "getResults start", "JobId", requestBody.jobId);    	
    	logToStdout("Results Service getResults start JobId=" + requestBody.jobId);
    	
	    try {
	    	
	    	// check that the job id exists
	    	if(!getJobTracker().jobExists(requestBody.jobId)){
	    		throw new Exception("No job exists with id " + requestBody.jobId);
	    	}
	    	
	    	URL json = new URL(prop.getBaseURL() + "/results/getTableResultsJsonForWebClient?jobId=" + requestBody.jobId + "&maxRows=200");
	    	URL csv  =  new URL(prop.getBaseURL() + "/results/getTableResultsCsvForWebClient?jobId=" + requestBody.jobId);
	    	
	    	res.addResult("fullURL", csv.toString());  	// csv  - retain bad label for backward compatibility
		    res.addResult("sampleURL", json.toString());	// json - retain bad label for backward compatibility
		    LoggerRestClient.easyLog(logger, "ResultsService", "getResults URLs", "sampleURL", json.toString(), "fullURL", csv.toString());
		    res.setSuccess(true);		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "getResults", e);
		    LoggerRestClient.easyLog(logger, "ResultsService", "getResults exception", "message", e.toString());
		    e.printStackTrace();
	    }	    
	    return res.toJson();
	}
	
	/**
	 * Delete file and metadata associated with this jobId
	 */
	@CrossOrigin
	@RequestMapping(value="/deleteStorage", method= RequestMethod.POST)
	public JSONObject deleteStorage(@RequestBody ResultsRequestBody requestBody){
    	logToStdout("Results Service deleteStorage start JobId=" + requestBody.jobId);

	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Results Service", "deleteStorage start", "JobId", requestBody.jobId);
    	
	    try {   	
	    	URL fullURL = getJobTracker().getFullResultsURL(requestBody.jobId);
	    	if (fullURL != null) {
	    		getTableResultsStorage().deleteStoredFile(fullURL);
	    		getJobTracker().deleteJob(requestBody.jobId); 
	    		LoggerRestClient.easyLog(logger, "ResultsService", "deleteStorage URLs", "fullURL", fullURL.toString());
	    	}		    
		    res.setSuccess(true);		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "deleteStorage", e);
		    LoggerRestClient.easyLog(logger, "ResultsService", "deleteStorage exception", "message", e.toString());
	    }	    
	    return res.toJson();
	}
	
	private TableResultsStorage getTableResultsStorage() throws Exception {
		return new TableResultsStorage(prop.getFileLocation());
	}

	private JsonLdResultsStorage getJsonLdResultsStorage() throws Exception {
		return new JsonLdResultsStorage(prop.getFileLocation());
	}
	
	private GenericJsonBlobResultsStorage getJsonBlobResultsStorage() throws Exception {
		return new GenericJsonBlobResultsStorage(prop.getFileLocation());
	}
	
	private JobTracker getJobTracker() throws Exception{
		return new JobTracker(edc_prop);
	}
	
	private void logToStdout (String message) {
		System.out.println(message);
	}
}
