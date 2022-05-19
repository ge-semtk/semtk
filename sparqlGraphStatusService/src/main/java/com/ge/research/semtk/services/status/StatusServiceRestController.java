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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.v3.oas.annotations.Operation;



/**
 * Service to get status of a query.
 */
@CrossOrigin
@RestController
@RequestMapping("/status")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class StatusServiceRestController {
 	static final String SERVICE_NAME = "StatusService";
	@Autowired
	StatusProperties prop;
	@Autowired
	ServicesGraphProperties servicesgraph_prop;
	@Autowired
	StatusLoggingProperties log_prop;
	@Autowired
	AuthProperties auth_prop;
	@Autowired 
	private ApplicationContext appContext;
	
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		
		prop.validateWithExit();
		servicesgraph_prop.validateWithExit();
		log_prop.validateWithExit();
		
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);

	}
	@RequestMapping(value="/headers", method=RequestMethod.GET)
	public String headers(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		
		return headers.toString();
	}
		
	/**
	 * Get percentComplete
	 */
	@CrossOrigin
	@RequestMapping(value="/getPercentComplete", method= RequestMethod.POST)
	public JSONObject getPercentComplete(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			String jobId = requestBody.jobId;
			    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
		    LocalLogger.logToStdOut("Status Service getPercentComplete start JobId=" + jobId);
	
	
		    try {
		    	JobTracker tracker = this.getTracker();
		    
			    res.addResult(SimpleResultSet.PERCENT_COMPLETE_RESULT_KEY, String.valueOf(tracker.getJobPercentComplete(jobId)));
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "getPercentComplete", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "getPercentComplete exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service getPercentComplete exception message=" + e.toString());
		    }
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	
	
	/**
	 * Get job status string
	 * @param requestBody
	 * @return status
	 */
	@CrossOrigin
	@RequestMapping(value="/getStatus", method= RequestMethod.POST)
	public JSONObject getStatus(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		try {
			String jobId = requestBody.jobId;
			    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
	    	LoggerRestClient.easyLog(logger, "Status Service", "getStatus start", "JobId", jobId);
	    	LocalLogger.logToStdOut("Status Service getStatus start JobId=" + jobId);
	
	    	
		    try {
		    	
		    	JobTracker tracker = this.getTracker();
		    
			    res.addResult(SimpleResultSet.STATUS_RESULT_KEY, String.valueOf(tracker.getJobStatus(jobId)));
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "getStatus", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "getStatus exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service getStatus exception message=" + e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * Get job status message (only valid on failure)
	 * @param requestBody
	 * @return statusMessage
	 */
	@CrossOrigin
	@RequestMapping(value="/getStatusMessage", method= RequestMethod.POST)
	public JSONObject getStatusMessage(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			String jobId = requestBody.jobId;
		    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
	    	LoggerRestClient.easyLog(logger, "Status Service", "getStatusMessage start", "JobId", jobId);
	    	LocalLogger.logToStdOut("Status Service getStatusMessage start JobId=" + jobId);
	
	    	
		    try {
		    	JobTracker tracker = this.getTracker();
		    
			    res.addResult(SimpleResultSet.STATUS_MESSAGE_RESULT_KEY, tracker.getJobStatusMessage(jobId));
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "getStatusMessage", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "statusMessage exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service statusMessage exception message=" + e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@Operation(
			summary="Get table of information about my (header's userName) jobs",
			description="Returns table of: creationTime, id, name, percentcomplete, statusMessage, userName, status"
			)
	@CrossOrigin
	@RequestMapping(value="/getJobsInfo", method=RequestMethod.POST)
	public JSONObject getJobsInfo(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String ENDPOINT_NAME = "getJobsInfo";
			TableResultSet res = null;
			try{
				JobTracker tracker = this.getTracker();
		    	Table jobInfoTable = tracker.getJobsInfo();
				
				res = new TableResultSet(true);
				res.addResults(jobInfoTable);
				
		    } catch (Exception e) {
		    	//   LoggerRestClient.easyLog(logger, "ResultsService", "getTableResultsCsv exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
			    res = new TableResultSet(false);
			    res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
		    } 
	
			return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@Operation(
			summary="Get seiJson SparqlEndpointInterface",
			description="This allows SemTK services to run efficient JobTrackers instead of inefficient internal calls to this service"
			)
	@CrossOrigin
	@RequestMapping(value="/getJobTrackerSei", method=RequestMethod.POST)
	public JSONObject getJobTrackerSei(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String ENDPOINT_NAME = "getJobTrackerSei";   // HERE
			SimpleResultSet res = new SimpleResultSet(true);
			try{
				res.addResult("seiJson", servicesgraph_prop.buildSei().toJson());
				
		    } catch (Exception e) {
		    	//   LoggerRestClient.easyLog(logger, "ResultsService", "getTableResultsCsv exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
			    res.setSuccess(false);
			    res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
		    } 
			return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	/**
	 * Block until status is percent complete is reached
	 */
	@CrossOrigin
	@RequestMapping(value="/waitForPercentComplete", method= RequestMethod.POST)
	public JSONObject waitForPercentComplete(@RequestBody StatusRequestBodyPercentMsec requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
	    try {
	    	String jobId = requestBody.jobId;
	    
	    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
	    	LoggerRestClient.easyLog(logger, "Status Service", "waitForPercentComplete start", "JobId", jobId);
	    	LocalLogger.logToStdOut("Status Service waitForPercentComplete " + requestBody.percentComplete + "% JobId=" + jobId);
	    	
	    	
		    try {
		    	JobTracker tracker = this.getTracker();
		    	tracker.waitForPercentComplete(jobId, requestBody.percentComplete, requestBody.maxWaitMsec);
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "waitForPercentComplete", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "waitForPercentComplete exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service waitForPercentComplete exception message=" + e.toString());
		    } 		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	
	
	/**
	 * Block until status is percent complete is reached or Msec have elapsed
	 */
	@CrossOrigin
	@RequestMapping(value="/waitForPercentOrMsec", method= RequestMethod.POST)
	public JSONObject waitForPercentOrMsec(@RequestBody StatusRequestBodyPercentMsec requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
		    String jobId = requestBody.jobId;
		    
		    LocalLogger.logToStdErr(Thread.currentThread().getName() + " PRE-LOG: waitForPercentOrMsec sees user: " + ThreadAuthenticator.getThreadUserName());
		    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
	    	LoggerRestClient.easyLog(logger, "Status Service", "waitForPercentComplete start", "JobId", jobId);
	    	LocalLogger.logToStdOut("Status Service waitForPercentComplete " + requestBody.percentComplete + "% JobId=" + jobId);
	    	
	    	LocalLogger.logToStdErr(Thread.currentThread().getName() + " POST-LOG waitForPercentOrMsec sees user: " + ThreadAuthenticator.getThreadUserName());
		    
		    try {
		    	JobTracker tracker = this.getTracker();
		    	int percentComplete = tracker.waitForPercentOrMsec(jobId, requestBody.percentComplete, requestBody.maxWaitMsec);
			    res.addResult(SimpleResultSet.PERCENT_COMPLETE_RESULT_KEY, String.valueOf(percentComplete));
			    
			    if (percentComplete == 100) {
		    		String [] statusAndMessage = tracker.getJobStatusAndMessage(jobId);
		    		res.addResult(SimpleResultSet.STATUS_RESULT_KEY, statusAndMessage[0]);
		    		res.addResult(SimpleResultSet.STATUS_MESSAGE_RESULT_KEY, statusAndMessage[1]);
		    	} else {
		    		res.addResult(SimpleResultSet.STATUS_MESSAGE_RESULT_KEY, tracker.getJobStatusMessage(jobId));
		    	}
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "waitForPercentOrMsec", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "waitForPercentOrMsec exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service waitForPercentOrMsec exception message=" + e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	/**
	 * set job to a given percent complete
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/setName", method= RequestMethod.POST)
	public JSONObject setName(@RequestBody @Valid StatusRequestBodyName requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String ENDPOINT_NAME = "setName";
		    String jobId = requestBody.jobId;
		    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
		    LocalLogger.logToStdOut("Status Service setName: " + requestBody.name );
	
		    try {
		    	JobTracker tracker = this.getTracker();
		    	tracker.setJobName(jobId, requestBody.name);
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			    LoggerRestClient.easyLog(logger, "Status Service", ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service " + ENDPOINT_NAME + " exception message=" + e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * increment percentage up to max
	 * @param requestBody
	 * @return
	 */
	@Operation(
			summary=	"Increment percent complete within specified maximum"
			)
	@CrossOrigin
	@RequestMapping(value="/incrementPercentComplete", method= RequestMethod.POST)
	public JSONObject incrementPercentComplete(@RequestBody StatusRequestBodyIncrement requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT = "incrementPercentComplete";
		HeadersManager.setHeaders(headers);
		try {
		    String jobId = requestBody.jobId;
		    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
		    LocalLogger.logToStdOut("Status Service/" + ENDPOINT + " increment=" + requestBody.increment + " JobId=" + jobId);
	
		    try {
		    	JobTracker tracker = this.getTracker();
		    	tracker.incrementPercentComplete(jobId, requestBody.increment, requestBody.max);
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, ENDPOINT, e);
			    LoggerRestClient.easyLog(logger, "Status Service", ENDPOINT + " exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service " + ENDPOINT + " exception message=" + e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
		    
	}
	
	/**
	 * set job to a given percent complete
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/setPercentComplete", method= RequestMethod.POST)
	public JSONObject setPercentComplete(@RequestBody StatusRequestBodyPercentMessage requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
		    String jobId = requestBody.jobId;
		    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
		    LocalLogger.logToStdOut("Status Service setPercentComplete " + requestBody.percentComplete + "% JobId=" + jobId);
	
		    try {
		    	JobTracker tracker = this.getTracker();
		    	tracker.setJobPercentComplete(jobId, requestBody.percentComplete, requestBody.message);
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "setPercentComplete", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "setPercentComplete exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service setPercentComplete exception message=" + e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
		    
	}
	
	/**
	 * Set job status to success and percent complete to 100
	 * @param requestBodyMessage
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/setSuccess", method= RequestMethod.POST)
	public JSONObject setSuccess(@RequestBody StatusRequestBodyMessage requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
		    String jobId = requestBody.jobId;
		    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
	    	LoggerRestClient.easyLog(logger, "Status Service", "setSuccess start", "JobId", jobId);
	    	LocalLogger.logToStdOut("Status Service setSuccess start JobId=" + jobId);
	    	try {
		    	
		    	JobTracker tracker = this.getTracker();
		    	tracker.setJobSuccess(jobId, requestBody.message);
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "setSuccess", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "setSuccess exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service setSuccess exception message=" + e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * Set job status to failure, set a message, set percent complete to 100
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/setFailure", method= RequestMethod.POST)
	public JSONObject setFailure(@RequestBody StatusRequestBodyMessage requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
		    String jobId = requestBody.jobId;
		    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
	    	LoggerRestClient.easyLog(logger, "Status Service", "setFailure start", "JobId", jobId, "Message", requestBody.message);
	    	LocalLogger.logToStdOut("Status Service setFailure start JobId=" + jobId + " Message=" + requestBody.message);
	    	
		    try {
		    	
		    	JobTracker tracker = this.getTracker();
		    	tracker.setJobFailure(jobId, requestBody.message);
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "setFailure", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "setFailure exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service setFailure exception message=" + e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
		    
	}
	
	/**
	 * deleteJob  - deletes a job
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/deleteJob", method= RequestMethod.POST)
	public JSONObject deleteJob(@RequestBody StatusRequestBodyMessage requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
		    String jobId = requestBody.jobId;
		    
		    SimpleResultSet res = new SimpleResultSet();
		    LoggerRestClient logger = LoggerRestClient.getInstance(log_prop);
	    	LoggerRestClient.easyLog(logger, "Status Service", "deleteJob start", "JobId", jobId);
	    	LocalLogger.logToStdOut("Status Service deleteJob start JobId=" + jobId);
		    try {
		    	JobTracker tracker = this.getTracker();
		    	tracker.deleteJob(jobId);
			    res.setSuccess(true);
			    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "deleteJob", e);
			    LoggerRestClient.easyLog(logger, "Status Service", "deleteJob exception", "message", e.toString());
			    LocalLogger.logToStdOut("Status Service deleteJob exception message="+ e.toString());
		    } 
		    
		    return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	private JobTracker getTracker() throws Exception {
		return new JobTracker(servicesgraph_prop.buildSei());
	}
	
}
