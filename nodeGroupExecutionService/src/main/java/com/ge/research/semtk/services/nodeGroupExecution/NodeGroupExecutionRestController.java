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

package com.ge.research.semtk.services.nodeGroupExecution;

import java.net.URL;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.api.nodeGroupExecution.SparqlExecutor;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.OntologyInfoClientConfig;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.NodeGroupResultSet;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.nodeGroupExecution.requests.ConstraintsFromIdRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.DispatchByIdRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.DispatchFromNodegroupRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.DispatchRawSparqlRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.FilterDispatchByIdRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.FilterDispatchFromNodeGroupRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.IngestByConnIdCsvStrRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.IngestByIdCsvStrRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.IngestByNodegroupCsvStrRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.InstanceDataClassesRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.InstanceDataPredicatesRequestBody;
import com.ge.research.semtk.services.nodeGroupExecution.requests.NodegroupRequestBodyPercentMsec;
import com.ge.research.semtk.services.nodeGroupExecution.requests.StatusRequestBody;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.sparqlX.asynchronousQuery.DispatcherSupportedQueryTypes;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;
import com.ge.research.semtk.springutilib.requests.IdRequest;
import com.ge.research.semtk.springutilib.requests.SparqlEndpointRequestBody;
import com.ge.research.semtk.springutilib.requests.SparqlEndpointTrackRequestBody;
import com.ge.research.semtk.springutilib.requests.TrackQueryRequestBody;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.annotations.ApiOperation;

/**
 * service to run stored nodegroups. 
 * @author 200018594
 *
 */
@RestController
@RequestMapping("/nodeGroupExecution")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class NodeGroupExecutionRestController {
	
 	static final String SERVICE_NAME = "nodeGroupExecutionService";
 	static final String JOB_ID_RESULT_KEY = SimpleResultSet.JOB_ID_RESULT_KEY;
 	
 	// updated
 	@Autowired
	private AuthProperties auth_prop;
	@Autowired
	ServicesGraphProperties servicesgraph_props;
	@Autowired
	NodegroupExecutionLoggingProperties log_prop;
	@Autowired 
	private ApplicationContext appContext;
	@Autowired
	OInfoServiceProperties oinfo_props;
	@Autowired
	NodegroupExecutionStoreProperties ngstore_prop;
	@Autowired
	NodegroupExecutionDispatchProperties dispatch_prop;
	@Autowired
	NodegroupExecutionResultsProperties results_prop;
	@Autowired
	NodegroupExecutionStatusProperties status_prop;
	@Autowired
	NodegroupExecutionIngestProperties ingest_prop;
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();

		// these are still in the older NodegroupExecutionServiceStartup
		ngstore_prop.validateWithExit();
		dispatch_prop.validateWithExit();
		results_prop.validateWithExit();
		status_prop.validateWithExit();
		ingest_prop.validateWithExit();

		servicesgraph_props.validateWithExit();
		log_prop.validateWithExit();
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);
	}
	
	@ApiOperation(
			value="Get job status",
			notes="results json contains 'status' string"
			)
	@CrossOrigin 
	@RequestMapping(value="/jobStatus", method=RequestMethod.POST)
	public JSONObject getJobStatus(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers){
		//final String ENDPOINT_NAME="jobStatus";
		HeadersManager.setHeaders(headers);
		//LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		
		try {
			SimpleResultSet retval = new SimpleResultSet();
			
			try{ 
				// create a new StoredQueryExecutor
				NodeGroupExecutor ngExecutor = this.getExecutor(requestBody.getJobID() );
				// try to get a job status
				String results = ngExecutor.getJobStatus();
				retval.setSuccess(true);
				retval.addResult(SimpleResultSet.STATUS_RESULT_KEY, results);
			}
			catch(Exception e){
				//LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "jobStatus", e);
			} 
	
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	} 
	
	@ApiOperation(
			value="Get job status message",
			notes="results json contains 'message'"
			)
	@CrossOrigin
	@RequestMapping(value="/jobStatusMessage", method=RequestMethod.POST)
	public JSONObject getJobStatusMessage(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		//final String ENDPOINT_NAME="jobStatusMessage";
		HeadersManager.setHeaders(headers);
		//LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			SimpleResultSet retval = new SimpleResultSet();
			
			try{
				// create a new StoredQueryExecutor
				NodeGroupExecutor ngExecutor = this.getExecutor(requestBody.getJobID() );
				// try to get a job status
				String results = ngExecutor.getJobStatusMessage();
				retval.setSuccess(true);
				retval.addResult("message", results);  // backwards compatible
				retval.addResult(SimpleResultSet.STATUS_MESSAGE_RESULT_KEY, results); // correct

			}
			catch(Exception e){
				//LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "jobStatusMessage", e);
			} 
		
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	} 
	
	@ApiOperation(
			value="Get job completed",
			notes="results json contains 'completed' field: true or false"
			)
	@CrossOrigin
	@RequestMapping(value="/getJobCompletionCheck", method=RequestMethod.POST)
	public JSONObject getJobCompletion(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		//final String ENDPOINT_NAME="getJobCompletionCheck";
		HeadersManager.setHeaders(headers);
		//LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			SimpleResultSet retval = new SimpleResultSet();
			
			try{
				// create a new StoredQueryExecutor
				NodeGroupExecutor ngExecutor = this.getExecutor(requestBody.getJobID() );
				// try to get a job status
				Boolean results = ngExecutor.getJobCompletion();
				retval.setSuccess(true);
				if(results){
					retval.addResult("completed", "true");
				}
				else{
					retval.addResult("completed", "false");
				}
			}
			catch(Exception e){
				//LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "getJobCompletionCheck", e);
			} 
		
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value="Get job percent complete",
			notes="results json contains 'percent' integer string"
			)
	@CrossOrigin
	@RequestMapping(value="/getJobCompletionPercentage", method=RequestMethod.POST)
	public JSONObject getJobCompletionPercent(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		//final String ENDPOINT_NAME="getJobCompletionPercentage";
		HeadersManager.setHeaders(headers);
		//LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			SimpleResultSet retval = new SimpleResultSet();
			
			try{
				// create a new StoredQueryExecutor
				NodeGroupExecutor ngExecutor = this.getExecutor(requestBody.getJobID() );
				// try to get a job status
				int results = ngExecutor.getJobPercentCompletion();
				retval.setSuccess(true);
				retval.addResult("percent", results);
	
			}
			catch(Exception e){
				//LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "getJobCompletionPercentage", e);
			} 
		
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	@ApiOperation(
			value="Wait for percent or msec",
			notes="Returns as soon as the requested Msec elapses or percent complete is reached<br>" +
			      "whichever comes first."
			)
	@CrossOrigin
	@RequestMapping(value="/waitForPercentOrMsec", method= RequestMethod.POST)
	public JSONObject waitForPercentOrMsec(@RequestBody NodegroupRequestBodyPercentMsec requestBody, @RequestHeader HttpHeaders headers) {
		// NOTE: May 2018 Paul
		// Newer / better endpoint
		// This pass-through has a signature identical to the status service
		// It uses the JobTracker, avoiding one bounce to the status service
		// copy-and-pasted the request body, though. Still needs consolodating in sparqlGraphLibrary
	    String jobId = requestBody.jobID;
	    
	    //final String ENDPOINT_NAME="getResultsTable";
		HeadersManager.setHeaders(headers);
		//LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
	    	SimpleResultSet retval = new SimpleResultSet();    	
    	
		    try {
		    	requestBody.validate();
		    	JobTracker tracker = this.getJobTracker();
		    	int percentComplete = tracker.waitForPercentOrMsec(jobId, requestBody.percentComplete, requestBody.maxWaitMsec);
		    	retval.addResult(SimpleResultSet.PERCENT_COMPLETE_RESULT_KEY, String.valueOf(percentComplete));
		    	
		    	if (percentComplete == 100) {
		    		String [] statusAndMessage = tracker.getJobStatusAndMessage(jobId);
		    		retval.addResult(SimpleResultSet.STATUS_RESULT_KEY, statusAndMessage[0]);
		    		retval.addResult(SimpleResultSet.STATUS_MESSAGE_RESULT_KEY, statusAndMessage[1]);
		    	} else {
		    		retval.addResult(SimpleResultSet.STATUS_MESSAGE_RESULT_KEY, tracker.getJobStatusMessage(jobId));
		    	}
		    	
		    	retval.setSuccess(true);
			    
		    } catch (Exception e) {
		    	//LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "waitForPercentOrMsec", e);
		    }		    
		    return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value="get results table",
			notes="Can fail if table is too big.<br>" +
			      "Results service /getTableResultsJsonForWebClient and /getTableResultsCsvForWebClient are safer "
			)
	@CrossOrigin
	@RequestMapping(value="/getResultsTable", method=RequestMethod.POST)
	public JSONObject getResultsTable(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		//final String ENDPOINT_NAME="getResultsTable";
		HeadersManager.setHeaders(headers);
		//LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			TableResultSet retval = new TableResultSet();
			
			try{
				NodeGroupExecutor nge = this.getExecutor(requestBody.getJobID());
				Table retTable = nge.getTableResults();
				retval.setSuccess(true);
				retval.addResults(retTable);
			}
			catch(Exception e){
				//LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new TableResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "getResultsTable", e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	
	@CrossOrigin
	@RequestMapping(value="/getResultsJsonLd", method=RequestMethod.POST)
	public JSONObject getResultsJsonLd(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		//final String ENDPOINT_NAME="getResultsJsonLd";
		HeadersManager.setHeaders(headers);
		//LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			NodeGroupResultSet retval = new NodeGroupResultSet();
			
			try{
				NodeGroupExecutor nge = this.getExecutor(requestBody.getJobID());
				JSONObject retLd = nge.getJsonLdResults();
				retval.setSuccess(true);
				retval.addResultsJSON(retLd);
			}
			catch(Exception e){
				//LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new NodeGroupResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "getResultsJsonLd", e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	// getJsonBlob : can't implement here because of
	//    1) can't figure out how to stream results from results service through
	//    2) don't know the results file location so can't instantiate a GenericJsonBlobResultsStorage
	// call the results service
	
	@ApiOperation(
			value=	"get results URLs",
			notes=	"DEPRECATED: URLS may not work in secure deployment of SemTK<br>" +
					"result json has 'sample' URL of sample JSON, and 'full' URL of entire CSV<br>" +
					"Results service /getTableResultsJsonForWebClient and /getTableResultsCsvForWebClient are safer<br> "
			)
	@CrossOrigin
	@RequestMapping(value="/getResultsLocation", method=RequestMethod.POST)
	public JSONObject getResultsLocation(@RequestBody StatusRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		//final String ENDPOINT_NAME="getResultsLocation";
		HeadersManager.setHeaders(headers);
		//LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			TableResultSet retval = new TableResultSet();
			
			// note: make sure the response is sane when results do not yet exist. the failure should be as graceful as we can make them.
			
			try{
				// create a new StoredQueryExecutor
				NodeGroupExecutor ngExecutor = this.getExecutor(requestBody.getJobID() );
				// try to get a job status
				URL[] results = ngExecutor.getResultsLocation();
				retval.setSuccess(true);
		
				// a little diagnostic print:
				LocalLogger.logToStdErr("results info for job (" + requestBody.getJobID() + ") : " + results.length + " records.");
				for(URL i : results){
					LocalLogger.logToStdErr("        record: " + i.toString());
				}
				
				
				// turn this into a table result.
				String[] cols = {"URL_Location", "Result_Type"};
				String[] colTypes = {"http://www.w3.org/2001/XMLSchema#string", "http://www.w3.org/2001/XMLSchema#string"};
				
				// the first is the sample. the second is the complete result.
				ArrayList<String> row0 = new ArrayList<String>();
				row0.add(results[0].toString());
				row0.add("sample");
				ArrayList<String> row1 = new ArrayList<String>();
				row1.add(results[1].toString());
				row1.add("full");
				
				ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
				
				rows.add(row0);
				rows.add(row1);
				
				Table retTable = new Table(cols, colTypes, rows);
				retval.addResults(retTable);
	
			}
			catch(Exception e){
				//LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new TableResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "getResultsLocation", e);
			} 
		
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	// base methods which others use
	
	public JSONObject dispatchAnyJobById(@RequestBody DispatchByIdRequestBody requestBody, DispatcherSupportedQueryTypes qt){
		
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			
			// make sure the request has the needed parameters
			requestBody.validate();
			
			NodeGroupExecutor ngExecutor = this.getExecutor(null );
			
			SparqlConnection connection = requestBody.getSparqlConnection();			
			// create a json object from the external data constraints. 
			
			// check if this is actually for a filter query
			String targetId = null;
			if(requestBody instanceof FilterDispatchByIdRequestBody){
				// set the target ID
				targetId = ((FilterDispatchByIdRequestBody)requestBody).getTargetObjectSparqlId();
			}
			
			// dispatch the job. 
			ngExecutor.dispatchJob(qt, connection, requestBody.getNodeGroupId(), 
					requestBody.getExternalDataConnectionConstraintsJson(), 
					requestBody.getFlags(),
					requestBody.getRuntimeConstraints(), 
					requestBody.getLimitOverride(),
					requestBody.getOffsetOverride(),
					targetId);
			String id = ngExecutor.getJobID();
			
			retval.setSuccess(true);
			retval.addResult(JOB_ID_RESULT_KEY, id); 

		}
		catch(Exception e){
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "dispatchAnyJobById exception", "message", e.toString());
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage("service: " + SERVICE_NAME + " method: dispatchAnyJobById()", e);
		} 
	
		return retval.toJson();

	}

	public JSONObject dispatchAnyJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody, DispatcherSupportedQueryTypes qt){

		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor ngExecutor = this.getExecutor(null );

			// try to create a sparql connection
			SparqlConnection connection = requestBody.getSparqlConnection();			
			// create a json object from the external data constraints. 
			
			// decode the endcodedNodeGroup
			SparqlGraphJson sgJson = new SparqlGraphJson(requestBody.getJsonNodeGroup());
			
			// swap in the connection if requested
			// "connection == null" is included for legacy.  Not sure this is correct -Paul 6/2018
			if (connection == null || NodeGroupExecutor.isUseNodegroupConn(connection)) {
				connection = sgJson.getSparqlConn();
			}			
			
			String targetId = null;
			if(requestBody instanceof FilterDispatchFromNodeGroupRequestBody){
				// set the target ID
				targetId = ((FilterDispatchFromNodeGroupRequestBody)requestBody).getTargetObjectSparqlId();
			}
			
			// dispatch the job. 
			ngExecutor.dispatchJob(qt, connection, sgJson.getNodeGroup(), 
					requestBody.getExternalDataConnectionConstraintsJson(), 
					requestBody.getFlags(),
					requestBody.getRuntimeConstraints(), 
					-1,
					-1,
					targetId);
			String id = ngExecutor.getJobID();
			
			retval.setSuccess(true);
			retval.addResult(JOB_ID_RESULT_KEY, id);

		}
		catch(Exception e){
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "dispatchAnyJobFromNodegroup exception", "message", e.toString());
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "../dispatchAnyJobById()", e);
		} 
	
		return retval.toJson();

	}

	// end base methods
	
	@ApiOperation(
			value=	"SELECT query on nodegroupID",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchById", method=RequestMethod.POST)
	public JSONObject dispatchJobById(@RequestBody DispatchByIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchById";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodeGroupId());
    	try {
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value=	"SELECT query on nodegroup",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchFromNodegroup";
		HeadersManager.setHeaders(headers);	
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);
		
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}

	@ApiOperation(
			value=	"SELECT query on nodegroupID",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchSelectById", method=RequestMethod.POST)
	public JSONObject dispatchSelectJobById(@RequestBody DispatchByIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchSelectById";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodeGroupId());
    	try {
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value="Run SELECT query by nodegroup id synchronously.",
			notes="Returns a table or times out.<br>Use only for simple queries or tests.<br><b>Preferred endpoint is /dispatchSelectById.</b>"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchSelectByIdSync", method=RequestMethod.POST)
	public JSONObject dispatchSelectByIdSync(@RequestBody DispatchByIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchSelectByIdSync";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodeGroupId());
    	try {
			final int TIMEOUT_SEC = 55;
			TableResultSet ret = null;
			try {
				// dispatch the job
				JSONObject simpleJson = dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);
				SimpleResultSet jobIdRes = SimpleResultSet.fromJson(simpleJson);
				
				if (! jobIdRes.getSuccess()) {
					// send along failure
					ret = new TableResultSet(simpleJson);
				} else {
					// wait for job to complete
					String jobId = jobIdRes.getResult(JOB_ID_RESULT_KEY);
			    	JobTracker tracker = this.getJobTracker();
			    	int percentComplete = tracker.waitForPercentOrMsec(jobId, 100, TIMEOUT_SEC * 1000);
			    	if (percentComplete < 100) {
			    		throw new Exception("Job is only " + percentComplete + "% complete after" + TIMEOUT_SEC + "seconds.  Use /dispatchSelectById instead.");
			    	}
			    	
			    	// check that job succeeded
			    	if (!tracker.jobSucceeded(jobId)) {
			    		throw new Exception("Query failed: " + tracker.getJobStatusMessage(jobId));
			    	}
			    	
			    	// get table
			    	NodeGroupExecutor nge = this.getExecutor(jobId);
					Table retTable = nge.getTableResults();
					ret = new TableResultSet(true);
					ret.addResults(retTable);
			    	
				}
			} catch (Exception e) {
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
			    ret = new TableResultSet(false);
			    ret.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			} 
			
			return ret.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value=	"SELECT query on nodegroup json",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchSelectFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchSelectJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchSelectFromNodegroup";
		HeadersManager.setHeaders(headers);	
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}
	
	@ApiOperation(
			value=	"CONSTRUCT query on nodegroup id",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchConstructById", method=RequestMethod.POST)
	public JSONObject dispatchConstructJobById(@RequestBody DispatchByIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchConstructById";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodeGroupId());
    	try {
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value=	"CONSTRUCT query on nodegroup json",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchConstructFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchConstructJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchConstructFromNodegroup";
		HeadersManager.setHeaders(headers);	
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchConstructForInstanceManipulationById", method=RequestMethod.POST)
	public JSONObject dispatchConstructInstanceJobById(@RequestBody DispatchByIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchConstructForInstanceManipulationById";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodeGroupId());
    	try {
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchConstructForInstanceManipulationFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchConstructInstanceJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchConstructForInstanceManipulationFromNodegroup";
		HeadersManager.setHeaders(headers);	
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}
	
	@ApiOperation(
			value=	"COUNT query on nodegroup id",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchCountById", method=RequestMethod.POST)
	public JSONObject dispatchCountJobById(@RequestBody DispatchByIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchCountById";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodeGroupId());
    	try {
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.COUNT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value=	"COUNT query on nodegroup json",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchCountFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchCountJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchCountFromNodegroup";
		HeadersManager.setHeaders(headers);	
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.COUNT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}

	@ApiOperation(
			value=	"FILTER query nodegroup id",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchFilterById", method=RequestMethod.POST)
	public JSONObject dispatchFilterJobById(@RequestBody FilterDispatchByIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchFilterById";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodeGroupId());
    	try {
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.FILTERCONSTRAINT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value=	"FILTER query nodegroup json",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchFilterFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchFilterJobFromNodegroup(@RequestBody FilterDispatchFromNodeGroupRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchFilterFromNodegroup";
		HeadersManager.setHeaders(headers);	
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.FILTERCONSTRAINT);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}

    	
	@ApiOperation(
			value=	"get instance data predicates",
			notes=	"returns job id.  Resulting table will have columns: ?s ?s_class ?p ?o ?o_class"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchSelectInstanceDataPredicates", method=RequestMethod.POST)
	public JSONObject dispatchSelectInstanceDataPredicates(@RequestBody InstanceDataPredicatesRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchSelectInstanceDataPredicates";
		HeadersManager.setHeaders(headers);	
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);

		try {		    

			SimpleResultSet retval = new SimpleResultSet();

			try {
    			SparqlConnection conn = new SparqlConnection(requestBody.getConn());
    			OntologyInfo oInfo = retrieveOInfo(conn);
    			ArrayList<String[]> pairsList = requestBody.buildPredicateListPairs();
    			if (pairsList.size() == 0) {
    				pairsList = oInfo.getPropertyPairs();
    			}
				String sparql = SparqlToXUtils.generateSelectInstanceDataPredicates(	
						conn, 
						oInfo,
						pairsList,
						requestBody.getLimitOverride(),
						requestBody.getOffsetOverride(),
						requestBody.getCountOnly());

				// execute
				NodeGroupExecutor ngExecutor = this.getExecutor(null );		
				ngExecutor.dispatchRawSparql(conn, sparql);
				String id = ngExecutor.getJobID();

				retval.setSuccess(true);
				retval.addResult(JOB_ID_RESULT_KEY, id);

			}
			catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
				LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			} 

			return retval.toJson();

		} finally {
			HeadersManager.clearHeaders();
		}

	}
	
	@ApiOperation(
			value=	"get instance data subjects",
			notes=	"returns job id.  Resulting table will have columns: ?s ?s_class " 
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchSelectInstanceDataSubjects", method=RequestMethod.POST)
	public JSONObject dispatchSelectInstanceDataSubjects(@RequestBody InstanceDataClassesRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchSelectInstanceDataSubjects";
		HeadersManager.setHeaders(headers);	
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);

		try {		    

			SimpleResultSet retval = new SimpleResultSet();

			try {
    			SparqlConnection conn = new SparqlConnection(requestBody.getConn());
    			OntologyInfo oInfo = retrieveOInfo(conn);
    			ArrayList<String> classList = requestBody.getClassValues();
    			if (classList.size() == 0) {
    				classList = oInfo.getClassNames();
    			}
				String sparql = SparqlToXUtils.generateSelectInstanceDataSubjects(	
						conn, 
						oInfo,
						classList,
						requestBody.getLimitOverride(),
						requestBody.getOffsetOverride(),
						requestBody.getCountOnly());

				// execute
				NodeGroupExecutor ngExecutor = this.getExecutor(null );		
				ngExecutor.dispatchRawSparql(conn, sparql);
				String id = ngExecutor.getJobID();

				retval.setSuccess(true);
				retval.addResult(JOB_ID_RESULT_KEY, id);

			}
			catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
				LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			} 

			return retval.toJson();

		} finally {
			HeadersManager.clearHeaders();
		}

	}

	@ApiOperation(
			value=	"DELETE query nodegroup id",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchDeleteById", method=RequestMethod.POST)
	public JSONObject dispatchDeleteJobById(@RequestBody DispatchByIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchDeleteById";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodeGroupId());
    	try {
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.DELETE);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value=	"DELETE query nodegroup json",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchDeleteFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchDeleteJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody, @RequestHeader HttpHeaders headers ){	
		final String ENDPOINT_NAME="dispatchDeleteFromNodegroup";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.DELETE);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}
	
	@ApiOperation(
			value=	"raw SPARQL SELECT query",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchRawSparql", method=RequestMethod.POST)
	public JSONObject dispatchRawSparql(@RequestBody DispatchRawSparqlRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchRawSparql";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			SimpleResultSet retval = new SimpleResultSet();
			
			try{
				// create a new StoredQueryExecutor
				NodeGroupExecutor ngExecutor = this.getExecutor(null );

				// try to create a sparql connection
				SparqlConnection connection = requestBody.getSparqlConnection();			
	
				// dispatch the job. 
				ngExecutor.dispatchRawSparql(connection, requestBody.getSparql());
				String id = ngExecutor.getJobID();
				
				retval.setSuccess(true);
				retval.addResult(JOB_ID_RESULT_KEY, id);
	
			}
			catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			} 
		
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}
	
	@ApiOperation(
			value=	"raw SPARQL query performing an UPDATE, DELETE, CLEAR, etc.",
			notes=	"result has 'JobId' field"
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchRawSparqlUpdate", method=RequestMethod.POST)
	public JSONObject dispatchRawSparqlUpdate(@RequestBody DispatchRawSparqlRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchRawSparqlUpdate";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			SimpleResultSet retval = new SimpleResultSet();
			
			try{
				// create a new StoredQueryExecutor
				NodeGroupExecutor ngExecutor = this.getExecutor(null );
				// try to create a sparql connection
				SparqlConnection connection = requestBody.getSparqlConnection();			
	
				// dispatch the job. 
				ngExecutor.dispatchRawSparqlUpdate(connection, requestBody.getSparql());
				String id = ngExecutor.getJobID();
				
				retval.setSuccess(true);
				retval.addResult(JOB_ID_RESULT_KEY, id);
	
			}
			catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			} 
		
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}
	
	@ApiOperation(
			value=	"clear graph",
			notes=	"result a single cell with status message<br>that is redundant with job status."
			)
	@CrossOrigin
	@RequestMapping(value="/dispatchClearGraph", method=RequestMethod.POST)
	public JSONObject dispatchClearGraph(@RequestBody SparqlEndpointRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="dispatchClearGraph";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			SimpleResultSet retval = new SimpleResultSet();
			
			try{
				
				// add connection
				SparqlEndpointInterface sei = requestBody.buildSei();
				SparqlEndpointInterface jobSei = servicesgraph_props.buildSei();
				
				// PEC TODO security
				// borrowing auth username password from the services graph
				sei.setUserAndPassword(jobSei.getUserName(), jobSei.getPassword());
				
				ResultsClientConfig  rConf   = new ResultsClientConfig(results_prop.getProtocol(), results_prop.getServer(), results_prop.getPort());
				ResultsClient resClient = new ResultsClient(rConf);
				
				// create a new StoredQueryExecutor
				SparqlExecutor sparqlExec = new SparqlExecutor(
						SparqlToXUtils.generateClearGraphSparql(sei), 
						sei, 
						servicesgraph_props.buildSei(), 
						resClient);
				
				sparqlExec.start();

				retval.addResult(SimpleResultSet.JOB_ID_RESULT_KEY, sparqlExec.getJobId());
				retval.setSuccess(true);
	
			}
			catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
				retval = new SimpleResultSet();
				retval.setSuccess(false);
				retval.addRationaleMessage(SERVICE_NAME, "dispatchRawSparql", e);
			} 
		
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }

	}
	/**
	 * Perform ingestion using a stored nodegroup ID.
	 * PEC:  "NewConnection" in name is inconsistent with most other "ById" endpoints.  Others imply it.
	 */
	@ApiOperation(
			value=	"ingest CSV data given nodegroup id",
			notes=	""
			)
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsNewConnection", method=RequestMethod.POST)
	public JSONObject ingestFromTemplateIdAndCsvStringNewConn(@RequestBody IngestByConnIdCsvStrRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="ingestFromCsvStringsNewConnection";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "chars", String.valueOf(requestBody.getCsvContent().length()));
    	try {
			RecordProcessResults retval = null;
			try{
				NodeGroupExecutor nodeGroupExecutor = this.getExecutor(null);		

				retval = nodeGroupExecutor.ingestFromTemplateIdAndCsvString(requestBody.getSparqlConnection(), requestBody.getTemplateId(), requestBody.getCsvContent(), requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
			}catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
				retval = new RecordProcessResults(false);
				retval.addRationaleMessage(SERVICE_NAME, "ingestFromCsvStringsNewConnection", e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	/**
	 * Perform ingestion by passing in a nodegroup.
	 * PEC:  "NewConnection" in name is inconsistent with most other "ById" endpoints.  Others imply it.
	 */
	@ApiOperation(
			value=	"ingest CSV data given nodegroup json",
			notes=	""
			)
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsAndTemplateNewConnection", method=RequestMethod.POST)
	public JSONObject ingestFromTemplateAndCsvString(@RequestBody IngestByNodegroupCsvStrRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="ingestFromCsvStringsAndTemplateNewConnection";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "chars", String.valueOf(requestBody.getCsvContent().length()));
    	try {
			RecordProcessResults retval = null;
			try{
				NodeGroupExecutor nodeGroupExecutor = this.getExecutor(null);		

				SparqlGraphJson sparqlGraphJson = new SparqlGraphJson(requestBody.getTemplate());
				retval = nodeGroupExecutor.ingestFromTemplateAndCsvString(requestBody.getSparqlConnection(), sparqlGraphJson, requestBody.getCsvContent(), requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
			}catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
				retval = new RecordProcessResults(false);
				retval.addRationaleMessage(SERVICE_NAME, "ingestFromCsvStringsAndTemplateNewConnection", e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * Perform ingestion by passing in a nodegroup.
	 * PEC:  "NewConnection" in name is inconsistent with most other "ById" endpoints.  Others imply it.
	 */
	@ApiOperation(
			value=	"Async ingest CSV data given nodegroup json",
			notes=	"Returns JobId. \n" +
			        "Successful status will have number of records proccessed message at /jobStatusMessage.\n" +
					"Failure will have an error table at /getResultsTable. \n"
			)
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsAndTemplateAsync", method=RequestMethod.POST)
	public JSONObject ingestFromCsvStringsAndTemplateAsync(@RequestBody IngestByNodegroupCsvStrRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="ingestFromCsvStringsAndTemplateAsync";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "chars", String.valueOf(requestBody.getCsvContent().length()));
    	try {
			SimpleResultSet retval = null;
			try{
				NodeGroupExecutor nodeGroupExecutor = this.getExecutor(null);		

				SparqlGraphJson sparqlGraphJson = new SparqlGraphJson(requestBody.getTemplate());
				String jobId = nodeGroupExecutor.ingestFromTemplateAndCsvStringAsync(requestBody.getSparqlConnection(), sparqlGraphJson, requestBody.getCsvContent(), requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
				retval = new SimpleResultSet(true);
				retval.addResult(SimpleResultSet.JOB_ID_RESULT_KEY, jobId);
				
			}catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
				retval = new SimpleResultSet(false);
				retval.addRationaleMessage(SERVICE_NAME, "ingestFromCsvStrings", e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * Perform ingestion using a stored nodegroup ID.
	 */
	@ApiOperation(
			value=	"ingest CSV data given nodegroup id",
			notes=	""
			)
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsById", method=RequestMethod.POST)
	public JSONObject ingestFromCsvStringsById(@RequestBody IngestByIdCsvStrRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="ingestFromCsvStringsById";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getTemplateId(), "chars", String.valueOf(requestBody.getCsvContent().length()));
    	try {
			RecordProcessResults retval = null;
			try{
				NodeGroupExecutor nodeGroupExecutor = this.getExecutor(null);		

				retval = nodeGroupExecutor.ingestFromTemplateIdAndCsvString(requestBody.getSparqlConnection(), requestBody.getTemplateId(), requestBody.getCsvContent(), requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
			}catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
				retval = new RecordProcessResults(false);
				retval.addRationaleMessage(SERVICE_NAME, "ingestFromCsvStrings", e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * Perform ingestion using a stored nodegroup ID.
	 */
	@ApiOperation(
			value=	"Async ingest CSV data given nodegroup id",
			notes=	"Returns JobId. \n" +
			        "Successful status will have number of records proccessed message at /jobStatusMessage.\n" +
					"Failure will have an error table at /getResultsTable. \n"
			)
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsByIdAsync", method=RequestMethod.POST)
	public JSONObject ingestFromCsvStringsByIdAsync(@RequestBody IngestByIdCsvStrRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="ingestFromCsvStringsByIdAsync";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getTemplateId(), "chars", String.valueOf(requestBody.getCsvContent().length()));
    	try {
			SimpleResultSet retval = null;
			try{
				NodeGroupExecutor nodeGroupExecutor = this.getExecutor(null);		

				String jobId = nodeGroupExecutor.ingestFromTemplateIdAndCsvStringAsync(requestBody.getSparqlConnection(), requestBody.getTemplateId(), requestBody.getCsvContent(), requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
				retval = new SimpleResultSet(true);
				retval.addResult(SimpleResultSet.JOB_ID_RESULT_KEY, jobId);
			}catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
				retval = new SimpleResultSet(false);
				retval.addRationaleMessage(SERVICE_NAME, "ingestFromCsvStrings", e);
				LocalLogger.printStackTrace(e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	@ApiOperation(
			value=	"get runtime constraint sparqlIDs given nodegroup id",
			notes=	"returns table of 'valueId', 'itemType', 'valueType'"
			)
	@CrossOrigin
	@RequestMapping(value="/getRuntimeConstraintsByNodeGroupID", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraints(@RequestBody ConstraintsFromIdRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="getRuntimeConstraintsByNodeGroupID";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME, "nodegroupId", requestBody.getNodegroupId());
    	try {
			TableResultSet retval = null;
			
			try {
				NodeGroupStoreConfig ngcConf = new NodeGroupStoreConfig(ngstore_prop.getProtocol(), ngstore_prop.getServer(), ngstore_prop.getPort());
				NodeGroupStoreRestClient nodegroupstoreclient = new NodeGroupStoreRestClient(ngcConf);
				retval = nodegroupstoreclient.executeGetNodeGroupRuntimeConstraints(requestBody.getNodegroupId()) ;
			}
			catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
				LocalLogger.printStackTrace(e);
				retval = new TableResultSet(false);
				retval.addRationaleMessage(SERVICE_NAME, "getRuntimeConstraintsByNodeGroupID", e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value=	"get runtime constraint sparqlIDs given nodegroup json",
			notes=	"returns table of 'valueId', 'itemType', 'valueType'"
			)
	@CrossOrigin
	@RequestMapping(value="/getRuntimeConstraintsByNodeGroup", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraintsFromNodegroup(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		final String ENDPOINT_NAME="getRuntimeConstraintsByNodeGroup";
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME);
    	try {
			TableResultSet retval = null;
			
			try {
				NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
				RuntimeConstraintManager rtci = new RuntimeConstraintManager(ng);
				
				retval = new TableResultSet(true);
				retval.addResults( rtci.getConstrainedItemsDescription() );
			
			}
			catch(Exception e){
				LoggerRestClient.easyLog(logger, SERVICE_NAME, ENDPOINT_NAME + " exception", "message", e.toString());
			    retval = new TableResultSet(false);
				retval.addRationaleMessage(SERVICE_NAME, "getRuntimeConstraintsByNodeGroup", e);
			} 
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@ApiOperation(
			value="Get columns required by import spec",
			notes="Returns \"columnNames\" array"
			)
	@CrossOrigin
	@RequestMapping(value="/getIngestionColumnsById", method=RequestMethod.POST)
	public JSONObject  getIngestionColumnsById(@RequestBody IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			
			SparqlGraphJson sgJson = this.getNodegroupById(requestBody.getId());
			ImportSpecHandler handler = sgJson.getImportSpecHandler();
			String colNames[] = handler.getColNamesUsed();
			
			retval.addResult("columnNames", colNames);
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "getIngestionColumnsById", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}

	
	@ApiOperation(
			value="Get ingestion validation rules",
			notes="Returns columnNames array and dataValidator json array"
			)
	@CrossOrigin
	@RequestMapping(value="/getIngestionColumnInfoById", method=RequestMethod.POST)
	public JSONObject  getIngestionValidations(@RequestBody IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			SparqlGraphJson sgJson = this.getNodegroupById(requestBody.getId());
			ImportSpecHandler handler = sgJson.getImportSpecHandler();
			retval.addResult("columnNames", handler.getColNamesUsed());
			retval.addResult("dataValidator", handler.getDataValidator().toJsonArray());
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "getIngestionColumnInfo", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@ApiOperation(
			value=	"Clear a graph with optional trackFlag."
			)
	@CrossOrigin
	@RequestMapping(value="/clearGraph", method= RequestMethod.POST)
	public JSONObject clearGraph(@RequestBody SparqlEndpointTrackRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		// pass-through to ingestion service
		HeadersManager.setHeaders(headers);
		try {
			IngestorRestClient iclient = new IngestorRestClient(new IngestorClientConfig(ingest_prop.getProtocol(), ingest_prop.getServer(), ingest_prop.getPort()));
			return iclient.execClearGraph(requestBody.buildSei(), requestBody.getTrackFlag()).toJson();
		}
		catch (Exception e) {
			SimpleResultSet err = new SimpleResultSet(false);
			err.addRationaleMessage(SERVICE_NAME, "clearGraph", e);
			LocalLogger.printStackTrace(e);
			return err.toJson();
		}
	}
	
	@ApiOperation(
			value=	"Run a query of tracked events."
			)
	@CrossOrigin
	@RequestMapping(value="/runTrackingQuery", method= RequestMethod.POST)
	public JSONObject runTrackingQuery(@RequestBody TrackQueryRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			IngestorRestClient iclient = new IngestorRestClient(new IngestorClientConfig(ingest_prop.getProtocol(), ingest_prop.getServer(), ingest_prop.getPort()));
			return iclient.execRunTrackingQuery(requestBody.buildSei(), requestBody.getKey(), requestBody.getUser(), requestBody.getStartEpoch(), requestBody.getEndEpoch()).toJson();
		}
		catch (Exception e) {
			SimpleResultSet err = new SimpleResultSet(false);
			err.addRationaleMessage(SERVICE_NAME, "runTrackingQuery", e);
			LocalLogger.printStackTrace(e);
			return err.toJson();
		}
	}
	
	@ApiOperation(
			value=	"Delete tracked events."
			)
	@CrossOrigin
	@RequestMapping(value="/deleteTrackingEvents", method= RequestMethod.POST)
	public JSONObject deleteTrackingEvents(@RequestBody TrackQueryRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			IngestorRestClient iclient = new IngestorRestClient(new IngestorClientConfig(ingest_prop.getProtocol(), ingest_prop.getServer(), ingest_prop.getPort()));
			return iclient.execDeleteTrackingEvents(requestBody.buildSei(), requestBody.getKey(), requestBody.getUser(), requestBody.getStartEpoch(), requestBody.getEndEpoch()).toJson();
		}
		catch (Exception e) {
			SimpleResultSet err = new SimpleResultSet(false);
			err.addRationaleMessage(SERVICE_NAME, "deleteTrackingEvents", e);
			LocalLogger.printStackTrace(e);
			return err.toJson();
		}
	}
			
	@ApiOperation(
			value=	"Get contents of file key from /runTrackingQuery, returns 'contents' field in simple results"
			)
	@CrossOrigin
	@RequestMapping(value="/getTrackedIngestFile", method= RequestMethod.POST)
	public JSONObject getTrackedIngestFile(@RequestBody IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			IngestorRestClient iclient = new IngestorRestClient(new IngestorClientConfig(ingest_prop.getProtocol(), ingest_prop.getServer(), ingest_prop.getPort()));
			return iclient.execGetTrackedIngestFile(requestBody.getId()).toJson();
		}
		catch (Exception e) {
			SimpleResultSet err = new SimpleResultSet(false);
			err.addRationaleMessage(SERVICE_NAME, "getTrackedIngestFile", e);
			LocalLogger.printStackTrace(e);
			return err.toJson();
		}
	}
	
	@ApiOperation(
			value=	"Delete data from a tracked load"
			)
	@CrossOrigin
	@RequestMapping(value="/undoLoad", method= RequestMethod.POST)
	public JSONObject undoLoad(@RequestBody IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			IngestorRestClient iclient = new IngestorRestClient(new IngestorClientConfig(ingest_prop.getProtocol(), ingest_prop.getServer(), ingest_prop.getPort()));
			return iclient.execUndoLoad(requestBody.getId()).toJson();
		}
		catch (Exception e) {
			SimpleResultSet err = new SimpleResultSet(false);
			err.addRationaleMessage(SERVICE_NAME, "getTrackedIngestFile", e);
			LocalLogger.printStackTrace(e);
			return err.toJson();
		}
	}
			
	// get the runtime constraints, if any.
	private JSONArray getRuntimeConstraintsAsJsonArray(String potentialConstraints) throws Exception{
		JSONArray retval = null;
		
		try{
			if(potentialConstraints != null && potentialConstraints.length() > 0 && !potentialConstraints.isEmpty()){
				// we have something of meaning in the constraints. 
				JSONParser jParse = new JSONParser();
				retval = (JSONArray) jParse.parse(potentialConstraints);
			}
		}
		catch(Exception ez){
			throw new Exception("getRuntimeConstraintsAsJsonArray :: Unable to deserialize runtime constraints. error recieved was: " + ez.getMessage());
		}
		// TODO: add a method for consistency checking the JSON once some constraints are made from the string.
		
		return retval;
	}

	private SparqlGraphJson getNodegroupById(String id) throws Exception {
		NodeGroupStoreConfig ngcConf = new NodeGroupStoreConfig(ngstore_prop.getProtocol(), ngstore_prop.getServer(), ngstore_prop.getPort());
		NodeGroupStoreRestClient nodegroupstoreclient = new NodeGroupStoreRestClient(ngcConf);
		return nodegroupstoreclient.executeGetNodeGroupByIdToSGJson(id);
	}
	
	// create the required StoredQueryExecutor
	private NodeGroupExecutor getExecutor(String jobID) throws Exception{
		NodeGroupStoreConfig ngcConf = new NodeGroupStoreConfig(ngstore_prop.getProtocol(), ngstore_prop.getServer(), ngstore_prop.getPort());
		DispatchClientConfig dsConf  = new DispatchClientConfig(dispatch_prop.getProtocol(), dispatch_prop.getServer(), dispatch_prop.getPort());
		ResultsClientConfig  rConf   = new ResultsClientConfig(results_prop.getProtocol(), results_prop.getServer(), results_prop.getPort());
		StatusClientConfig   sConf   = new StatusClientConfig(status_prop.getProtocol(), status_prop.getServer(), status_prop.getPort(), jobID);
		IngestorClientConfig iConf   = new IngestorClientConfig(ingest_prop.getProtocol(), ingest_prop.getServer(), ingest_prop.getPort());
		
		// create the other components we need. 
		NodeGroupStoreRestClient nodegroupstoreclient = new NodeGroupStoreRestClient(ngcConf);
		DispatchRestClient dispatchclient = new DispatchRestClient(dsConf);
		ResultsClient resultsclient = new ResultsClient(rConf);
		IngestorRestClient ingestClient = new IngestorRestClient(iConf);
		
		// create the actual executor
		NodeGroupExecutor retval = new NodeGroupExecutor(nodegroupstoreclient, dispatchclient, resultsclient, servicesgraph_props.buildSei(), ingestClient);
		if(jobID != null){ retval.setJobID(jobID); }
		return retval;
	}
	
	// helper method to figure out if we are looking at a nodegroup alone or a sparqlgraphJSON
	// and return a nodegroup from it.
	private NodeGroup getNodeGroupFromJson(JSONObject jobj) throws Exception{
		NodeGroup retval = null;;
				
		if(SparqlGraphJson.isSparqlGraphJson(jobj)){
			// this was a sparqlGraphJson. unwrap before using.
			SparqlGraphJson sgJson = new SparqlGraphJson(jobj);
			retval = sgJson.getNodeGroup();
		}
		
		else if(NodeGroup.isNodeGroup(jobj)){
			// this was just a node group
			retval = new NodeGroup();
			retval.addJson(NodeGroup.extractNodeList(jobj));
		}
		
		else{
			// something insane was passed. fail with some dignity.
			throw new Exception("Request object does not seem to contain a valid nodegroup serialization");
		}
		
		return retval;
	}		
	
	/**
	 * Retrieve oInfo.
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	private OntologyInfo retrieveOInfo(SparqlConnection conn) throws Exception {
		OntologyInfoClient oClient = new OntologyInfoClient(new OntologyInfoClientConfig(oinfo_props.getProtocol(), oinfo_props.getServer(), oinfo_props.getPort()));
		return oClient.getOntologyInfo(conn);
		
	}
	
	private JobTracker getJobTracker() throws Exception{
		return new JobTracker(servicesgraph_props.buildSei());
	}
	
	
}


