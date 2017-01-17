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


package com.ge.research.semtk.services.status;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;

import com.ge.research.semtk.resultSet.SimpleResultSet;


/**
 * Service to get status of a query.
 */

@CrossOrigin
@RestController
@RequestMapping("/status")
public class StatusServiceRestController {
 	
	@Autowired
	StatusProperties prop;
	@Autowired
	StatusEdcConfigProperties edc_prop;
	@Autowired
	StatusLoggingProperties log_prop;
	
	/**
	 * Get percentComplete
	 */
	@RequestMapping(value="/getPercentComplete", method= RequestMethod.POST)
	public JSONObject getPercentComplete(@RequestBody StatusRequestBody requestBody){
		String jobId = requestBody.jobId;
		    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	logToStdout("Status Service getPercentComplete start JobId=" + jobId);


	    try {
	    	JobTracker tracker = new JobTracker(edc_prop);
	    
		    res.addResult("percentComplete", String.valueOf(tracker.getJobPercentComplete(jobId)));
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "Status Service", "getPercentComplete exception", "message", e.toString());
		    logToStdout("Status Service getPercentComplete exception message=" + e.toString());

	    }
	    
	    return res.toJson();
	}
	
	
	
	/**
	 * Get job status string
	 * @param requestBody
	 * @return status
	 */
	@RequestMapping(value="/getStatus", method= RequestMethod.POST)
	public JSONObject getStatus(@RequestBody StatusRequestBody requestBody){
		String jobId = requestBody.jobId;
		    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Status Service", "getStatus start", "JobId", jobId);
    	logToStdout("Status Service getStatus start JobId=" + jobId);

    	
	    try {
	    	
	    	JobTracker tracker = new JobTracker(edc_prop);
	    
		    res.addResult("status", String.valueOf(tracker.getJobStatus(jobId)));
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "Status Service", "getStatus exception", "message", e.toString());
		    logToStdout("Status Service getStatus exception message=" + e.toString());

	    }
	    
	    return res.toJson();
	}
	
	/**
	 * Get job status message (only valid on failure)
	 * @param requestBody
	 * @return statusMessage
	 */
	@RequestMapping(value="/getStatusMessage", method= RequestMethod.POST)
	public JSONObject getStatusMessage(@RequestBody StatusRequestBody requestBody){
		String jobId = requestBody.jobId;
		    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Status Service", "getStatusMessage start", "JobId", jobId);
    	logToStdout("Status Service getStatusMessage start JobId=" + jobId);

    	
	    try {
	    	JobTracker tracker = new JobTracker(edc_prop);
	    
		    res.addResult("statusMessage", tracker.getJobStatusMessage(jobId));
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "Status Service", "statusMessage exception", "message", e.toString());
		    logToStdout("Status Service statusMessage exception message=" + e.toString());
	    }
	    
	    return res.toJson();
	}
	
	/**
	 * Block until status is percent complete is reached
	 */
	@RequestMapping(value="/waitForPercentComplete", method= RequestMethod.POST)
	public JSONObject waitForPercentComplete(@RequestBody StatusRequestBodyPercentMsec requestBody){
	    String jobId = requestBody.jobId;
	    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Status Service", "waitForPercentComplete start", "JobId", jobId);
    	logToStdout("Status Service waitForPercentComplete " + requestBody.percentComplete + "% JobId=" + jobId);
    	
    	
	    try {
	    	JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.waitForPercentComplete(jobId, requestBody.percentComplete, requestBody.maxWaitMsec);
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "Status Service", "waitForPercentComplete exception", "message", e.toString());
		    logToStdout("Status Service waitForPercentComplete exception message=" + e.toString());
	    }
	    
	    return res.toJson();
	}
	
	/**
	 * set job to a given percent complete
	 * @param requestBody
	 * @return
	 */
	@RequestMapping(value="/setPercentComplete", method= RequestMethod.POST)
	public JSONObject setPercentComplete(@RequestBody StatusRequestBodyPercent requestBody){
	    String jobId = requestBody.jobId;
	    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	logToStdout("Status Service setPercentComplete " + requestBody.percentComplete + "% JobId=" + jobId);

	    try {
	    	JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.setJobPercentComplete(jobId, requestBody.percentComplete, requestBody.message);
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "Status Service", "setPercentComplete exception", "message", e.toString());
		    logToStdout("Status Service setPercentComplete exception message=" + e.toString());
	    }
	    
	    return res.toJson();
	}
	
	/**
	 * Set job status to success and percent complete to 100
	 * @param requestBodyMessage
	 * @return
	 */
	@RequestMapping(value="/setSuccess", method= RequestMethod.POST)
	public JSONObject setSuccess(@RequestBody StatusRequestBodyMessage requestBody){
	    String jobId = requestBody.jobId;
	    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Status Service", "setSuccess start", "JobId", jobId);
    	logToStdout("Status Service setSuccess start JobId=" + jobId);
    	try {
	    	
	    	JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.setJobSuccess(jobId, requestBody.message);
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "Status Service", "setSuccess exception", "message", e.toString());
		    logToStdout("Status Service setSuccess exception message=" + e.toString());
	    }
	    
	    return res.toJson();
	}
	
	/**
	 * Set job status to failure, set a message, set percent complete to 100
	 * @param requestBody
	 * @return
	 */
	@RequestMapping(value="/setFailure", method= RequestMethod.POST)
	public JSONObject setFailure(@RequestBody StatusRequestBodyMessage requestBody){
	    String jobId = requestBody.jobId;
	    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Status Service", "setFailure start", "JobId", jobId, "Message", requestBody.message);
    	logToStdout("Status Service setFailure start JobId=" + jobId + " Message=" + requestBody.message);
    	
	    try {
	    	
	    	JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.setJobFailure(jobId, requestBody.message);
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "Status Service", "setFailure exception", "message", e.toString());
		    logToStdout("Status Service setFailure exception message=" + e.toString());
	    }
	    
	    return res.toJson();
	}
	
	/**
	 * deleteJob  - deletes a job
	 * @param requestBody
	 * @return
	 */
	@RequestMapping(value="/deleteJob", method= RequestMethod.POST)
	public JSONObject deleteJob(@RequestBody StatusRequestBodyMessage requestBody){
	    String jobId = requestBody.jobId;
	    
	    SimpleResultSet res = new SimpleResultSet();
	    LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
    	LoggerRestClient.easyLog(logger, "Status Service", "deleteJob start", "JobId", jobId);
    	logToStdout("Status Service deleteJob start JobId=" + jobId);
	    try {
	    	JobTracker tracker = new JobTracker(edc_prop);
	    	tracker.deleteJob(jobId);
		    res.setSuccess(true);
		    
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "Status Service", "deleteJob exception", "message", e.toString());
		    logToStdout("Status Service deleteJob exception message="+ e.toString());
	    }
	    
	    return res.toJson();
	}
	
	private void logToStdout (String message) {
		System.out.println(message);
	}
}
