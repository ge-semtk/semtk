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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.ResultsStorage;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.SimpleResultSet;

/**
 * Service to get status of a query.
 */
@CrossOrigin
@RestController
@RequestMapping("/results")
public class ResultsServiceRestController {
	
	@Autowired
	ResultsProperties prop;
	@Autowired
	ResultsEdcConfigProperties edc_prop;
	@Autowired
	ResultsLoggingProperties log_prop;
	
	/**
	 * Store a chunk of text as a csv file and sample csv file save URLS with jobId
	 * @param requestBody where contents are contents of a csv file
	 * @return sampleURL, fullURL
	 */
	
	@CrossOrigin
	@RequestMapping(value="/storeSingleFileResults", method= RequestMethod.POST)
	public JSONObject storeSingleFileResults(@RequestBody ResultsRequestBodyFileContents requestBody){
		String jobId = requestBody.jobId;
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
	    LoggerRestClient.easyLog(logger, "ResultsService", "storeCsvResults start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeSingleFileResults start JobId=" + jobId);

	    
	    try {
		    ResultsStorage storage = getResultsStorage();
		     
		    // store the data
		    URL url = storage.storeSingleFile(requestBody.getContents(), requestBody.getExtension());
		    
		    // store the location via JobTracker
		    JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.setJobResultsURL(requestBody.jobId, url);
	    	
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeSingleFileResults exception", "message", e.toString());
		    e.printStackTrace();

	    }
	    
	    return res.toJson();
	}
	/**
	 * Store a chunk of text as a csv file and sample csv file save URLS with jobId
	 * @param requestBody where contents are contents of a csv file
	 * @return sampleURL, fullURL
	 */
	
	@CrossOrigin
	@RequestMapping(value="/storeCsvResults", method= RequestMethod.POST)
	public JSONObject storeCsvResults(@RequestBody ResultsRequestBodyFileContents requestBody){
		String jobId = requestBody.jobId;
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
	    LoggerRestClient.easyLog(logger, "ResultsService", "storeCsvResults start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeCsvResults start JobId=" + jobId);

	    
	    try {
		    ResultsStorage storage = getResultsStorage();
		     
		    // store the data
		    URL [] urls = storage.storeCsvFile(requestBody.getContents(), prop.getSampleLines());
		    
		    // store the location via JobTracker
		    JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.setJobResultsURLs(requestBody.jobId, urls[0], urls[1]);
	    	
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeCsvResults exception", "message", e.toString());
		    e.printStackTrace();

	    }
	    
	    return res.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/storeIncrementalCsvResults", method=RequestMethod.POST)
	public JSONObject storeIncrementalCsvResults(@RequestBody ResultsRequestBodyIncrementalFileContents requestBody){
		SimpleResultSet res = new SimpleResultSet();
		String jobId = requestBody.jobId;
		
		// logging
		LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);	 
		LoggerRestClient.easyLog(logger, "ResultsService", "storeIncrementalCsvResults start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeCsvResults start JobId=" + jobId + " with segment number: " + requestBody.getSegmentNumber());
		
		try{
			ResultsStorage storage = getResultsStorage();
			URL [] urls = storage.storeCsvFileIncremental(requestBody.getContents(), prop.getSampleLines(), requestBody.jobId, requestBody.getSegmentNumber());
			
		    // store the location via JobTracker
		    JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.setJobResultsURLs(requestBody.jobId, urls[0], urls[1]);
	    	
		    res.setSuccess(true);
		}
		catch(Exception e){
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeCsvTableResults exception", "message", e.toString());
		    e.printStackTrace();
		}
    	
		return res.toJson();
	}
	
	/**
	 * Store a chunk of text as a csv file and sample json file save URLS with jobId
	 * @param requestBody where contents are json string representing Table
	 * @return sampleURL, fullURL
	 */
	@CrossOrigin
	@RequestMapping(value="/storeTableResults", method= RequestMethod.POST)
	public JSONObject storeTableResults(@RequestBody ResultsRequestBodyFileContents requestBody){
		String jobId = requestBody.jobId;
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
	    LoggerRestClient.easyLog(logger, "ResultsService", "storeCsvResults start", "jobId", requestBody.jobId);
    	logToStdout("Results Service storeTableResults start JobId=" + jobId);

	    
	    try {
		    ResultsStorage storage = getResultsStorage();
		     
		    // make the table
		    JSONParser parser = new JSONParser();
		    JSONObject json = (JSONObject) parser.parse(requestBody.getContents());
		    Table table = Table.fromJson(json);
		    
		    URL [] urls = storage.storeTable(table, prop.getSampleLines());
		    
		    // store the location via JobTracker
		    JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.setJobResultsURLs(requestBody.jobId, urls[0], urls[1]);
	    	
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "ResultsService", "storeTableResults exception", "message", e.toString());
		    e.printStackTrace();

	    }
	    
	    return res.toJson();
	}
	
	/**
	 * 
	 * @param requestBody
	 * @return sampleURL fullURL
	 */
	@CrossOrigin
	@RequestMapping(value="/getResults", method= RequestMethod.POST)
	public JSONObject getResults(@RequestBody ResultsRequestBody requestBody){
		String jobId = requestBody.jobId;
		    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Results Service", "getResults start", "JobId", jobId);
    	
    	logToStdout("Results Service getResults start JobId=" + jobId);

	    try {
	    	JobTracker tracker = new JobTracker(edc_prop);
	    	
	    	URL sampleURL = tracker.getSampleResultsURL(jobId);
	    	URL fullURL =  tracker.getFullResultsURL(jobId);
	    	String sampleURLStr = sampleURL != null ? sampleURL.toString() : "";
		    res.addResult("fullURL", fullURL.toString());
		    res.addResult("sampleURL", sampleURLStr);
		    LoggerRestClient.easyLog(logger, "ResultsService", "getResults URLs", "sampleURL", sampleURLStr, "fullURL", fullURL.toString());

		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "ResultsService", "getResults exception", "message", e.toString());
		    e.printStackTrace();

	    }
	    
	    return res.toJson();
	}
	
	/**
	 * Delete files associated with this jobId
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/deleteStorage", method= RequestMethod.POST)
	public JSONObject deleteStorage(@RequestBody ResultsRequestBody requestBody){
	    
		String jobId = requestBody.jobId;
    	logToStdout("Results Service deleteStorage start JobId=" + jobId);

	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Results Service", "deleteStorage start", "JobId", jobId);
    	
	    try {
		    ResultsStorage storage = getResultsStorage();
	    	JobTracker tracker = new JobTracker(edc_prop);
	    	
	    	URL sampleURL = tracker.getSampleResultsURL(jobId);
	    	URL fullURL =  tracker.getFullResultsURL(jobId);
		    
	    	if (sampleURL != null) {
	    		storage.deleteStoredFile(sampleURL);
	    		LoggerRestClient.easyLog(logger, "ResultsService", "deleteStorage URLs", "sampleURL", sampleURL.toString());
	    	}
	    	if (fullURL != null) {
	    		storage.deleteStoredFile(fullURL);
	    		LoggerRestClient.easyLog(logger, "ResultsService", "deleteStorage URLs", "fullURL", fullURL.toString());
	    	}

		    
		    
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "ResultsService", "deleteStorage exception", "message", e.toString());
	    }
	    
	    return res.toJson();
	}
	private ResultsStorage getResultsStorage() throws Exception {
		return new ResultsStorage(new URL(prop.getBaseURL()), prop.getFileLocation());
	}

	private void logToStdout (String message) {
		System.out.println(message);
	}
}
