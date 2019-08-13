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

package com.ge.research.semtk.api.nodeGroupExecution.client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

public class NodeGroupExecutionClient extends RestClient {
	
	// json keys
	// TODO should probably move these elsewhere and/or consolidate with other classes
	private static final String JSON_KEY_JOB_ID = "jobID";
	private static final String JSON_KEY_NODEGROUP_ID = "nodeGroupId";
	private static final String JSON_KEY_LIMIT_OVERRIDE = "limitOverride";
	private static final String JSON_KEY_OFFSET_OVERRIDE = "offsetOverride";
	private static final String JSON_KEY_MAX_WAIT_MSEC = "maxWaitMsec";
	private static final String JSON_KEY_NODEGROUP  = "jsonRenderedNodeGroup";
	private static final String JSON_KEY_PERCENT_COMPLETE  = "percentComplete";
	private static final String JSON_KEY_SPARQL_CONNECTION = "sparqlConnection";
	private static final String JSON_KEY_RUNTIME_CONSTRAINTS = "runtimeConstraints";
	private static final String JSON_KEY_EDC_CONSTRAINTS = "externalDataConnectionConstraints";
	private static final String JSON_KEY_FLAGS = "flags";
	
	// service mapping
	private static final String mappingPrefix = "/nodeGroupExecution";
	
	// endpoints
	private static final String jobStatusEndpoint = "/jobStatus";
	private static final String jobStatusMessageEndpoint = "/jobStatusMessage";
	private static final String jobCompletionCheckEndpoint = "/getJobCompletionCheck";
	private static final String jobCompletionPercentEndpoint = "/getJobCompletionPercentage";
	private static final String waitForPercentOrMsecEndpoint = "/waitForPercentOrMsec";

	private static final String resultsLocationEndpoint = "/getResultsLocation";
	private static final String dispatchByIdEndpoint = "/dispatchById";
	private static final String dispatchFromNodegroupEndpoint = "/dispatchFromNodegroup";
	private static final String ingestFromCsvStringsNewConnectionEndpoint = "/ingestFromCsvStringsNewConnection";
	private static final String ingestFromCsvStringsByIdEndpoint = "/ingestFromCsvStringsById";
	private static final String ingestFromCsvStringsByIdAsyncEndpoint = "/ingestFromCsvStringsByIdAsync";
	private static final String ingestFromCsvStringsAndTemplateNewConnectionEndpoint = "/ingestFromCsvStringsAndTemplateNewConnection";
	private static final String ingestFromCsvStringsAndTemplateAsync = "/ingestFromCsvStringsAndTemplateAsync";
	private static final String getResultsTableEndpoint = "/getResultsTable";
	private static final String getResultsJsonLdEndpoint = "/getResultsJsonLd";
	private static final String dispatchSelectByIdEndpoint = "/dispatchSelectById";
	private static final String dispatchSelectByIdSyncEndpoint = "/dispatchSelectByIdSync";
	private static final String dispatchSelectFromNodegroupEndpoint = "/dispatchSelectFromNodegroup";
	private static final String dispatchCountByIdEndpoint = "/dispatchCountById";
	private static final String dispatchCountFromNodegroupEndpoint = "/dispatchCountFromNodegroup";
	private static final String dispatchFilterByIdEndpoint = "/dispatchFilterById";
	private static final String dispatchFilterFromNodegroupEndpoint ="/dispatchFilterFromNodegroup";
	private static final String dispatchDeleteByIdEndpoint = "/dispatchDeleteById";
	private static final String dispatchDeleteFromNodegroupEndpoint = "/dispatchDeleteFromNodegroup";
	private static final String dispatchRawSparqlEndpoint = "/dispatchRawSparql";
	private static final String dispatchConstructByIdEndpoint = "/dispatchConstructById";
	private static final String dispatchConstructFromNodegroupEndpoint = "/dispatchConstructFromNodegroup";
	private static final String dispatchConstructByIdEndpointForInstanceManipulationEndpoint = "/dispatchConstructForInstanceManipulationById";
	private static final String dispatchConstructFromNodegroupEndpointForInstanceManipulationEndpoint = "/dispatchConstructForInstanceManipulationFromNodegroup";

	@Override
	public void buildParametersJSON() throws Exception {
	}

	@Override
	public void handleEmptyResponse() throws Exception {
	}
	
	public NodeGroupExecutionClient (NodeGroupExecutionClientConfig necc){
		this.conf = necc;
	}
	
	public String getJobStatus(String jobId) throws Exception{
		SimpleResultSet ret = this.execGetJobStatus(jobId);
		return ret.getResult("status");
	}
	
	public boolean getJobSuccess(String jobId) throws Exception {
		return this.getJobStatus(jobId).equalsIgnoreCase("success");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execGetJobStatus(String jobId) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + jobStatusEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		
		return retval;
	}
	
	public Boolean isJobComplete(String jobId) throws Exception{
		SimpleResultSet ret = this.execGetJobCompletionCheck(jobId);
		
		String val = ret.getResult("completed");
		if(val.equalsIgnoreCase("true")) { return true; }
		else{ return false; }
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execGetJobCompletionCheck(String jobId) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + jobCompletionCheckEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		} finally{
			this.reset();
		}		
		return retval;		
	}
	
	//execGetStatusMessage()
	public String getJobStatusMessage(String jobId) throws Exception{
		SimpleResultSet ret = execGetJobStatusMessage(jobId);
		return ret.getResult("message");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execGetJobStatusMessage(String jobId) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + jobStatusMessageEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		} finally{
			this.reset();
		}
		return retval;		
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execGetJobCompletionPercentage(String jobId) throws Exception {
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + jobCompletionPercentEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}finally{
			this.reset();
		}
		return retval;
	}
	
	/**
	 * Get results table throwing exceptions if anything goes wrong
	 * @param jobId
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	
	public Table getResultsTable(String jobId) throws Exception {
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint(mappingPrefix + getResultsTableEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		
		try{
			retval = this.executeWithTableResultReturn();
		} finally{
			this.reset();
		}
		
		if (! retval.getSuccess()) {
			throw new Exception(String.format("Job failed.  JobId='%s' Message='%s'", jobId, retval.getRationaleAsString("\n")));
		}
		
		return retval.getTable();
	}
	
	
	@SuppressWarnings("unchecked")
	public JSONObject execGetResultsJsonLd(String jobId) throws Exception {
		JSONObject retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + getResultsJsonLdEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		
		try{
			retval = (JSONObject) this.execute();
		} finally{
			this.reset();
		}
		
		return retval;
	}
	
	/**
	 * get results URLs",
	 * DEPRECATED: URLS may not work in secure deployment of SemTK
	 * Results service /getTableResultsJsonForWebClient and /getTableResultsCsvForWebClient are safer
	 * @param jobId
	 * @return
	 * @throws Exception
	 */
	public Table getResultsLocation(String jobId) throws Exception{
		TableResultSet ret = this.execGetResultsLocation(jobId);
		return ret.getTable();
	}
	
	/**
	 * get results URLs",
	 * DEPRECATED: URLS may not work in secure deployment of SemTK
	 * Results service /getTableResultsJsonForWebClient and /getTableResultsCsvForWebClient are safer
	 * @param jobId
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public TableResultSet execGetResultsLocation(String jobId) throws Exception{
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint(mappingPrefix + resultsLocationEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		
		return retval;
	}

	public SimpleResultSet execWaitForPercentOrMsec(String jobId, int maxWaitMsec, int percentComplete) throws Exception {
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + waitForPercentOrMsecEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		this.parametersJSON.put(JSON_KEY_MAX_WAIT_MSEC, maxWaitMsec);
		this.parametersJSON.put(JSON_KEY_PERCENT_COMPLETE, percentComplete);
		try {
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		} finally{
			this.reset();
		}
		return retval;
	}
	
	public int waitForPercentOrMsec(String jobId, int maxWaitMsec, int percentComplete) throws Exception {
		SimpleResultSet ret = this.execWaitForPercentOrMsec(jobId, maxWaitMsec, percentComplete);
		return ret.getResultInt("percentComplete");
	}
	
// action-specific endpoints for ID-based executions
	
	/**
	 * 	
	 * @param nodegroupID    -- string ID for the nodegroup to be executed. this assumes that the node group resides in a nodegroup store that was config'd on the far end (service)
	 * @param overrideConn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */

//  Functions removed:
//  If you have a RuntimeConstraintManager then you must have the nodegroup
//  If you have the NodeGroup, use dispatchSelectFromNodeGroupToJobId
//  (leaving these creates ambiguity when user calls with null runtime constraints)
//		
//	public String dispatchSelectByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
//		return this.dispatchSelectByIdToJobId(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints, -1, -1, null);
//	}
//	public String dispatchSelectByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride, QueryFlags flags) throws Exception{
//		SimpleResultSet ret =  this.execDispatchSelectById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints, limitOverride, offsetOverride, flags);
//		return ret.getResult("JobId");
//	}
	
	public String dispatchSelectByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.dispatchSelectByIdToJobId(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraintsJson, -1, -1, null);
	}
	public String dispatchSelectByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride, QueryFlags flags) throws Exception{
		SimpleResultSet ret =  this.execDispatchSelectById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraintsJson, limitOverride, offsetOverride, flags);
		return ret.getResult("JobId");
	}

	public String dispatchConstructByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret =  this.execDispatchConstructById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}

	public String dispatchConstructForInstanceManipulationByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret =  this.execDispatchConstructForInstanceManipulationById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	
//  Functions removed:
//  If you have a RuntimeConstraintManager then you must have the nodegroup
//  If you have the NodeGroup, use dispatchSelectFromNodeGroup
//  (leaving these creates ambiguity when user calls with null runtime constraints)
//	
//	public Table execDispatchSelectByIdToTable(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception {
//		return this.execDispatchSelectByIdToTable(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints, -1, -1, null);
//	}
//
//	public Table execDispatchSelectByIdToTable(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride, QueryFlags flags) throws Exception {
//		
//		return this.execDispatchSelectByIdToTable(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints.toJson(), limitOverride, offsetOverride, flags);
//	}
	
	/**
	 * Main entry points.
	 * Note that since we're 
	 * @param nodegroupID
	 * @param overrideConn
	 * @param edcConstraintsJson
	 * @param runtimeConstraintsJson - generate from RuntimeConstraintManager.buildRuntimeConstraintJson()
	 * @return
	 * @throws Exception
	 */
	public Table execDispatchSelectByIdToTable(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception {
		return this.execDispatchSelectByIdToTable(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraintsJson, -1, -1, null);
	}

	public Table execDispatchSelectByIdToTable(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride, QueryFlags flags) throws Exception {
		
		// dispatch the job
		String jobId = this.dispatchSelectByIdToJobId(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraintsJson, limitOverride, offsetOverride, flags);
		
		try {
			return this.waitForJobAndGetTable(jobId);
		} catch (Exception e) {
			// Add nodegroupID and "SELECT" to the error message
			throw new Exception(String.format("Error executing SELECT on nodegroup id='%s'", nodegroupID), e);
		}
	}
	
	public JSONObject execDispatchConstructByIdToJsonLd(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception {
		
		// dispatch the job
		String jobId = this.dispatchConstructByIdToJobId(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		
		try {
			return this.waitForJobAndGetJsonLd(jobId);			
		} catch (Exception e) {
			// Add nodegroupID and "SELECT" to the error message
			throw new Exception(String.format("Error executing Construct on nodegroup id='%s'", nodegroupID), e);
		}		
	}
	
	public JSONObject execDispatchConstructForInstanceManipulationByIdToJsonLd(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception {
		
		// dispatch the job
		String jobId = this.dispatchConstructForInstanceManipulationByIdToJobId(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		
		try {
			return this.waitForJobAndGetJsonLd(jobId);			
		} catch (Exception e) {
			// Add nodegroupID and "SELECT" to the error message
			throw new Exception(String.format("Error executing Construct on nodegroup id='%s'", nodegroupID), e);
		}		
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
			percent = this.waitForPercentOrMsec(jobId, freqMsec, 100);
			if (percent == 100) {
				return;
			}
		}
		throw new Exception("Job " + jobId + " is only " + String.valueOf(percent) + "% complete after " + String.valueOf(maxTries) + " tries.");
	}
	// try for 6 minutes
	private void waitForCompletion(String jobId) throws Exception {
		this.waitForCompletion(jobId, 9000, 40);
	}
	
	/**
	 * Given jobId, check til job is done, check for success, get table
	 * @param jobId
	 * @return
	 * @throws Exception - if anything other than a valid table is returned
	 */
	public Table waitForJobAndGetTable(String jobId) throws Exception {
		// wait for completion
		this.waitForCompletion(jobId);
		
		// check for success
		if (this.getJobSuccess(jobId)) {
			return this.getResultsTable(jobId);
		} else {
			String msg = this.getJobStatusMessage(jobId);
			throw new Exception(String.format("Job %s failed with message='%s'", jobId, msg));
		}
	}
	
	/**
	 * Wait for (an ingestion) job that gives a message on success and table string on error
	 * @param jobId
	 * @return
	 * @throws Exception
	 */
	public String waitForIngestionJob(String jobId) throws Exception {
		// wait for completion
		this.waitForCompletion(jobId);
		
		// check for success
		if (this.getJobSuccess(jobId)) {
			return this.getJobStatusMessage(jobId);
		} else {
			String msg = this.getResultsTable(jobId).toCSVString();
			throw new Exception(String.format("Job %s failed with error table:\n%s", jobId, msg));
		}
	}

	private JSONObject waitForJobAndGetJsonLd(String jobId) throws Exception {
		
		waitForCompletion(jobId);
		
		// check for success
		if (this.getJobSuccess(jobId)) {
			return this.execGetResultsJsonLd(jobId);
		} else {
			String msg = this.getJobStatusMessage(jobId);
			throw new Exception(String.format("Job %s failed with message='%s'", jobId, msg));
		}
	}
	
	
//  Functions removed:
//  If you have a RuntimeConstraintManager then you must have the nodegroup
//  If you have the NodeGroup, use execDispatchSelectFromNodeGroup
//  (leaving these creates ambiguity when user calls with null runtime constraints)
//	//	public SimpleResultSet execDispatchSelectById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, QueryFlags flags) throws Exception{
//		return this.execDispatchSelectById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints, -1, -1, null);
//	}
//	
//	public SimpleResultSet execDispatchSelectById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride, QueryFlags flags) throws Exception{
//		return this.execDispatchSelectById(
//				nodegroupID, overrideConn, edcConstraintsJson, 
//				runtimeConstraints == null ? null : runtimeConstraints.toJson(), 
//				limitOverride, offsetOverride, flags) ;
//	}
	
	public SimpleResultSet execDispatchSelectById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, QueryFlags flags) throws Exception{
		return this.execDispatchSelectById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraintsJson, -1, -1, flags) ;
	}
	
	/**
	 * Most common way to use runtime constraints.  Since we're executing by ID we probably don't have a runtomConstraintsManager
	 * but we could have generated some with RuntimeConstraintManager.buildRuntimeConstraintJson()
	 * 
	 * @param nodegroupID
	 * @param overrideConn
	 * @param edcConstraintsJson
	 * @param runtimeConstraintsJson
	 * @param limitOverride
	 * @param offsetOverride
	 * @param flags
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchSelectById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride, QueryFlags flags) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchSelectByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS, runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_FLAGS, flags == null ? null : flags.toJSONString());		

		
		try{
			LocalLogger.logToStdErr("sending executeDispatchSelectById request");
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful(String.format("Error running SELECT on nodegroup id='%s'", nodegroupID));
		}
		finally{
			this.reset();
		}
		LocalLogger.logToStdErr("executeDispatchSelectById request finished without exception");
		return retval;
	}
	
	public TableResultSet execDispatchSelectByIdSync(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, QueryFlags flags) throws Exception{
		return this.execDispatchSelectByIdSync(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints, -1, -1, null);
	}
	
	@SuppressWarnings("unchecked")
	public TableResultSet execDispatchSelectByIdSync(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride, QueryFlags flags) throws Exception{
		TableResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchSelectByIdSyncEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS, runtimeConstraints == null ? null : runtimeConstraints.toJSONString());	
		this.parametersJSON.put(JSON_KEY_FLAGS, flags == null ? null : flags.toJSONString());		

		
		try{
			LocalLogger.logToStdErr("sending executeDispatchSelectByIdSync request");
			retval = new TableResultSet((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful(String.format("Error running SELECT on nodegroup id='%s'", nodegroupID));
		}
		finally{
			this.reset();
		}
		LocalLogger.logToStdErr("executeDispatchSelectByIdSync request finished without exception");
		return retval;
	}
	
	public SimpleResultSet execDispatchConstructById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		return this.execDispatchById(nodegroupID, overrideConn, edcConstraintsJson, null, runtimeConstraints, -1, -1);
	}
		
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchConstructById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchConstructByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		try{
			LocalLogger.logToStdErr("sending executeDispatchSelectById request");
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful(String.format("Error running SELECT on nodegroup id='%s'", nodegroupID));
		}
		finally{
			this.reset();
		}
		LocalLogger.logToStdErr("executeDispatchSelectById request finished without exception");
		return retval;
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchConstructForInstanceManipulationById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchConstructByIdEndpointForInstanceManipulationEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		try{
			LocalLogger.logToStdErr("sending executeDispatchSelectById request");
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful(String.format("Error running SELECT on nodegroup id='%s'", nodegroupID));
		}
		finally{
			this.reset();
		}
		LocalLogger.logToStdErr("executeDispatchSelectById request finished without exception");
		return retval;
	}
	/**
		 * 	
		* @param nodegroupID    -- string ID for the nodegroup to be executed. this assumes that the node group resides in a nodegroup store that was config'd on the far end (service)
		 * @param overrideConn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
		 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
		 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
		 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
		 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
		 * @return {String}              jobId
		 */
	public String dispatchCountByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret =  this.execDispatchCountById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	public String dispatchCountByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet ret =  this.execDispatchCountById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints, limitOverride, offsetOverride);
		return ret.getResult("JobId");
	}
	
	public SimpleResultSet execDispatchCountById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		return this.execDispatchCountById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints, -1, -1);
	}

	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchCountById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchCountByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		
		try{
			LocalLogger.logToStdErr("sending executeDispatchCountById request");
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		LocalLogger.logToStdErr("executeDispatchCountById request finished without exception");
		return retval;
	}
	
	/**
	 * Get the count as a long, or throw an exception
	 * @param nodegroupID
	 * @param overrideConn
	 * @param edcConstraintsJson
	 * @param runtimeConstraints
	 * @return
	 * @throws Exception
	 */
	public Long dispatchCountById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret =  this.execDispatchCountById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		
		Table tab = this.waitForJobAndGetTable(ret.getResult("JobId"));
		return tab.getCellAsLong(0, 0);
	}
	
	public Long dispatchCountByNodegroup(NodeGroup nodegroup, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret =  this.execDispatchCountFromNodeGroup(nodegroup, overrideConn, edcConstraintsJson, runtimeConstraints);
		
		Table tab = this.waitForJobAndGetTable(ret.getResult("JobId"));
		return tab.getCellAsLong(0, 0);
	}
	
	/**
	 * 	
	 * @param nodegroupID    -- string ID for the nodegroup to be executed. this assumes that the node group resides in a nodegroup store that was config'd on the far end (service)
	 * @param overrideConn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @param targetObjectSparqlId -- the ID of the object to filter for valid values of. these are the sparql IDs used in the nodegroup.
	 * @return
	 */
	public String dispatchFilterByIdToJobId(String nodegroupID, String targetObjectSparqlId, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret =  this.execDispatchFilterById(nodegroupID, targetObjectSparqlId, overrideConn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	public String dispatchFilterByIdToJobId(String nodegroupID, String targetObjectSparqlId, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet ret =  this.execDispatchFilterById(nodegroupID, targetObjectSparqlId, overrideConn, edcConstraintsJson, runtimeConstraints, limitOverride, offsetOverride);
		return ret.getResult("JobId");
	}

	public SimpleResultSet execDispatchFilterById(String nodegroupID, String targetObjectSparqlId, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		return this.execDispatchFilterById(nodegroupID, targetObjectSparqlId, overrideConn, edcConstraintsJson, runtimeConstraints, -1, -1);
	}

	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchFilterById(String nodegroupID, String targetObjectSparqlId, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride) throws Exception{
			SimpleResultSet retval = null;
			
			conf.setServiceEndpoint(mappingPrefix + dispatchFilterByIdEndpoint);
			this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
			this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
			this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

			this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
			this.parametersJSON.put("targetObjectSparqlId", targetObjectSparqlId);
			this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
			this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
			
			
			try{
				LocalLogger.logToStdErr("sending executeDispatchFilterById request");
				retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
				retval.throwExceptionIfUnsuccessful();
			}
			finally{
				this.reset();
			}
			LocalLogger.logToStdErr("executeDispatchFilterById request finished without exception");
			return retval;
		}
		
	/** 
	 * @param nodegroupID    -- string ID for the nodegroup to be executed. this assumes that the node group resides in a nodegroup store that was config'd on the far end (service)
	 * @param overrideConn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	public String dispatchDeleteByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret =  this.execDispatchDeleteById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
		
	public String dispatchDeleteByIdToSuccessMsg(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		String jobId = this.dispatchDeleteByIdToJobId(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		this.waitForCompletion(jobId);
		if (this.getJobSuccess(jobId)) {
			return this.getResultsTable(jobId).getCell(0, 0);
		} else {
			throw new Exception(this.getJobStatusMessage(jobId));
		}
	}

	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchDeleteById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
			SimpleResultSet retval = null;
			
			conf.setServiceEndpoint(mappingPrefix + dispatchDeleteByIdEndpoint);
			this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
			this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
			this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
			this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
			
			try{
				LocalLogger.logToStdErr("sending executeDispatchDeleteById request");
				retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
				retval.throwExceptionIfUnsuccessful();
			}
			finally{
				this.reset();
			}
			LocalLogger.logToStdErr("executeDispatchDeleteById request finished without exception");
			return retval;
		}

// action-specific endpoints for nodegroup-based executions

	/**
	 * 	
	 * @param ng   -- the nodegroup to execute a selection query from
	 * @param overrideConn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	public String dispatchSelectFromNodeGroupToJobId(NodeGroup ng, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		return this.dispatchSelectFromNodeGroupToJobId(ng, overrideConn, edcConstraintsJson, runtimeConstraints, null);
	}
	
	public String dispatchSelectFromNodeGroupToJobId(NodeGroup ng, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, QueryFlags flags) throws Exception{
		SimpleResultSet ret = this.execDispatchSelectFromNodeGroup(ng, overrideConn, edcConstraintsJson, runtimeConstraints, flags);
		return ret.getResult("JobId");
	}
	
	public SimpleResultSet execDispatchSelectFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception {
		return this.execDispatchSelectFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints, null);
	}
	
	public SimpleResultSet execDispatchSelectFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, QueryFlags flags) throws Exception{
	
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchSelectFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, conn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		this.parametersJSON.put(JSON_KEY_FLAGS,            flags == null ? null : flags.toJSONString());		
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful("Error at " + mappingPrefix + dispatchSelectFromNodegroupEndpoint);
		}
		finally{
			this.reset();
		}
		
		return retval;
	}
	
	public String dispatchConstructFromNodeGroupToJobId(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret = this.execDispatchConstructFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	public String dispatchConstructForInstanceManipulationFromNodeGroupToJobId(NodeGroup ng, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret = this.execDispatchConstructForInstanceManipulationFromNodeGroup(ng, overrideConn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchConstructFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchConstructFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, conn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful("Error at " + mappingPrefix + dispatchSelectFromNodegroupEndpoint);
		}
		finally{
			this.reset();
		}
		
		return retval;
	}	
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchConstructForInstanceManipulationFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchConstructFromNodegroupEndpointForInstanceManipulationEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, conn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful("Error at " + mappingPrefix + dispatchSelectFromNodegroupEndpoint);
		}
		finally{
			this.reset();
		}
		
		return retval;
	}
	
	public JSONObject dispatchConstructFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		
		SimpleResultSet ret = this.execDispatchConstructFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints);
		
		return this.waitForJobAndGetJsonLd(ret.getResult("JobId"));
	}
	
	public JSONObject dispatchConstructForInstanceManipulationFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		
		SimpleResultSet ret = this.execDispatchConstructForInstanceManipulationFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints);
		
		return this.waitForJobAndGetJsonLd(ret.getResult("JobId"));
	}
	public Table dispatchDeleteFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		
		SimpleResultSet ret = this.execDispatchDeleteFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints);
		return this.waitForJobAndGetTable(ret.getResult("JobId"));
	}
	public Table dispatchSelectFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		
		SimpleResultSet ret = this.execDispatchSelectFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints, null);
		return this.waitForJobAndGetTable(ret.getResult("JobId"));
	}
	
	public Table dispatchSelectFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, QueryFlags flags) throws Exception{
		
		SimpleResultSet ret = this.execDispatchSelectFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints, flags);
		return this.waitForJobAndGetTable(ret.getResult("JobId"));
	}
	
	public Table dispatchSelectFromNodeGroup(SparqlGraphJson sgjson, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		return this.dispatchSelectFromNodeGroup(sgjson.getNodeGroup(), sgjson.getSparqlConn(), edcConstraintsJson, runtimeConstraints, null);
	}
	
	public Table dispatchSelectFromNodeGroup(SparqlGraphJson sgjson, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints, QueryFlags flags) throws Exception{
		return this.dispatchSelectFromNodeGroup(sgjson.getNodeGroup(), sgjson.getSparqlConn(), edcConstraintsJson, runtimeConstraints, flags);
	}
	
	/**
	 * 	
	 * @param ng   -- the nodegroup to execute a selection query from
	 * @param conn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	
	public String dispatchCountFromNodeGroupToJobId(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret = this.execDispatchCountFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchCountFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchCountFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, conn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		
		return retval;
	}
	
	/**
	 * 	
	 * @param ng   -- the nodegroup to execute a selection query from
	 * @param conn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	
	public String dispatchDeleteFromNodeGroupToJobId(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret = this.execDispatchDeleteFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchDeleteFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchDeleteFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, conn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		
		return retval;
	}	
	
	/**
	 * 	
	 * @param ng   -- the nodegroup to execute a selection query from
	 * @param conn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @param targetObjectSparqlId -- the ID of the object to filter for valid values of. these are the sparql IDs used in the nodegroup.
	 * @return
	 */
	
	public String dispatchFilterFromNodeGroupToJobId(NodeGroup ng, String targetObjectSparqlId, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret = this.execDispatchFilterFromNodeGroup(ng, targetObjectSparqlId, conn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchFilterFromNodeGroup(NodeGroup ng, String targetObjectSparqlId, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchFilterByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, conn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		this.parametersJSON.put("targetObjectSparqlId", targetObjectSparqlId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		
		return retval;
	}	

// Execute Dispatch maintained for backward compatibility -- they are largely replaced by the "Select" variants...
/**
 * 	
 * @param nodegroupID    -- string ID for the nodegroup to be executed. this assumes that the node group resides in a nodegroup store that was config'd on the far end (service)
 * @param overrideConn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
 * @param flagsJson -- an array of flag strings rendered as a JSON array (e.g. ["RDB_QUERYGEN_OMIT_ALIASES"])
 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
 * @return
 */

	public String dispatchByIdWithToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, JSONArray flagsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet ret =  this.execDispatchById(nodegroupID, overrideConn, edcConstraintsJson, flagsJson, runtimeConstraints, limitOverride, offsetOverride);
		return ret.getResult("JobId");
	}
	
	public String dispatchByIdToJobId(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret =  this.execDispatchById(nodegroupID, overrideConn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	public SimpleResultSet execDispatchById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		return this.execDispatchById(nodegroupID, overrideConn, edcConstraintsJson, null, runtimeConstraints, -1, -1);
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchById(String nodegroupID, SparqlConnection overrideConn, JSONObject edcConstraintsJson, JSONArray flagsJson, RuntimeConstraintManager runtimeConstraints, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_FLAGS, flagsJson == null ? null : flagsJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		try{
			LocalLogger.logToStdErr("sending executeDispatchById request");
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		LocalLogger.logToStdErr("executeDispatchById request finished without exception");
		return retval;
	}
	
	
	
	/**
	 * 	
	 * @param ng   -- the nodegroup to execute a selection query from
	 * @param overrideConn -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraints -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	
	public String dispatchFromNodeGroupToJobId(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet ret = this.execDispatchFromNodeGroup(ng, conn, edcConstraintsJson, runtimeConstraints);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet execDispatchFromNodeGroup(NodeGroup ng, SparqlConnection conn, JSONObject edcConstraintsJson, RuntimeConstraintManager runtimeConstraints) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, conn.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraints == null ? null : runtimeConstraints.toJSONString());		
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		
		return retval;
	}

	/**
	 * Ingest CSV using a nodegroup ID.
	 * MIS-NAMED FUNCTION retained for BACKWARDS COMPATIBILITY
	 */
	public RecordProcessResults execIngestionFromCsvStr(String nodegroupAndTemplateId, String csvContentStr, SparqlConnection overrideConn) throws Exception {
		return this.execIngestionFromCsvStrNewConnection(nodegroupAndTemplateId, csvContentStr, overrideConn);
	}
	
	/**
	 * Ingest CSV using a nodegroup ID.
	 */
	@SuppressWarnings("unchecked")
	public RecordProcessResults execIngestionFromCsvStrNewConnection(String nodegroupAndTemplateId, String csvContentStr, SparqlConnection overrideConn) throws Exception {
		RecordProcessResults retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + ingestFromCsvStringsNewConnectionEndpoint);
		this.parametersJSON.put("templateId", nodegroupAndTemplateId);
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
		this.parametersJSON.put("csvContent", csvContentStr);
	
		try{
			JSONObject jobj = (JSONObject) this.execute();
			retval = new RecordProcessResults(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		return retval;
	}
	
	/**
	 * Ingest CSV using a nodegroup ID.
	 */
	public RecordProcessResults execIngestionFromCsvStrById(String nodegroupAndTemplateId, String csvContentStr, SparqlConnection overrideConn) throws Exception {
		return execIngestionFromCsvStrNewConnection(nodegroupAndTemplateId, csvContentStr, overrideConn);
	}
	
	
	/**
	 * execIngestFromCsvStringsByIdAsync
	 * @param nodegroupAndTemplateId
	 * @param csvContentStr
	 * @param overrideConn
	 * @return jobId string
	 * @throws Exception if call is unsuccessful
	 */
	@SuppressWarnings("unchecked")
	public String execIngestFromCsvStringsByIdAsync(String nodegroupAndTemplateId, String csvContentStr, SparqlConnection overrideConn) throws Exception {
		
		conf.setServiceEndpoint(mappingPrefix + ingestFromCsvStringsByIdAsyncEndpoint);
		this.parametersJSON.put("templateId", nodegroupAndTemplateId);
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, overrideConn.toJson().toJSONString());
		this.parametersJSON.put("csvContent", csvContentStr);
	
		try{
			JSONObject jobj = (JSONObject) this.execute();
			SimpleResultSet retval = SimpleResultSet.fromJson(jobj);
			retval.throwExceptionIfUnsuccessful();
			return retval.getResult(SimpleResultSet.JOB_ID_RESULT_KEY);
		}
		finally{
			this.reset();
		}
	}
	
	/**
	 * Ingest from a template
	 * @param sgjsonWithOverride
	 * @param csvContentStr
	 * @return
	 * @throws Exception
	 */
	public String execIngestFromCsvStringsAndTemplateAsync(SparqlGraphJson sgjsonWithOverride, String csvContentStr) throws Exception {
		conf.setServiceEndpoint(mappingPrefix + ingestFromCsvStringsAndTemplateAsync);
		this.parametersJSON.put("template", sgjsonWithOverride.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sgjsonWithOverride.getSparqlConn().toJson().toJSONString());
		this.parametersJSON.put("csvContent", csvContentStr);
	
		try{
			JSONObject jobj = (JSONObject) this.execute();
			SimpleResultSet retval = SimpleResultSet.fromJson(jobj);
			retval.throwExceptionIfUnsuccessful();
			return retval.getResult(SimpleResultSet.JOB_ID_RESULT_KEY);
		}
		finally{
			this.reset();
		}
	}

	/**
	 * Ingest a csv table asynchronously
	 * @param nodegroupAndTemplateId
	 * @param csvContentStr
	 * @param overrideConn =
	 * @return
	 * @throws Exception
	 */
	public String dispatchIngestFromCsvStringsByIdAsync(String nodegroupAndTemplateId, String csvContentStr, SparqlConnection overrideConn) throws Exception {
		String jobId = this.execIngestFromCsvStringsByIdAsync(nodegroupAndTemplateId, csvContentStr, overrideConn);
		this.waitForCompletion(jobId);
		if (this.getJobSuccess(jobId)) {
			return this.getJobStatusMessage(jobId);
		} else {
			throw new Exception("Ingestion failed:\n" + this.getResultsTable(jobId).toCSVString());
		}
	}
	
	public String dispatchIngestFromCsvStringsByIdAsync(String nodegroupAndTemplateId, String csvContentStr) throws Exception {
		return this.dispatchIngestFromCsvStringsByIdAsync(nodegroupAndTemplateId, csvContentStr, NodeGroupExecutor.get_USE_NODEGROUP_CONN());
	}
	
	/**
	 * Ingest CSV using a nodegroup.
	 */
	@SuppressWarnings("unchecked")
	public RecordProcessResults execIngestionFromCsvStr(SparqlGraphJson sparqlGraphJson, String csvContentStr) throws Exception {
		RecordProcessResults retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + ingestFromCsvStringsAndTemplateNewConnectionEndpoint);
		this.parametersJSON.put("template", sparqlGraphJson.getJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlGraphJson.getSparqlConnJson().toJSONString());
		this.parametersJSON.put("csvContent", csvContentStr);
	
		try{
			JSONObject jobj = (JSONObject) this.execute();
			retval = new RecordProcessResults(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		return retval;
	}

	public SimpleResultSet execDispatchSelectFromNodeGroupResource(String resourcePath, Object jarObj, SparqlConnection conn) throws Exception {
		return this.execDispatchSelectFromNodeGroupResource(resourcePath, jarObj, conn, null);
		
	}
	
	/**
	 * Read sgjson from disk, apply runtime constraints, execute select to a ResultSet
	 * @param resourcePath
	 * @param jarObj
	 * @param conn
	 * @param runtimeConstraintsJson
	 * @return ResultSet
	 * @throws Exception
	 */
	public SimpleResultSet execDispatchSelectFromNodeGroupResource(String resourcePath, Object jarObj, SparqlConnection conn, JSONArray runtimeConstraintsJson) throws Exception {
		
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(jarObj, resourcePath));
		NodeGroup ng = sgjson.getNodeGroup();
		if (runtimeConstraintsJson != null) {
			RuntimeConstraintManager manager = new RuntimeConstraintManager(ng);
			manager.applyConstraintJson(runtimeConstraintsJson);
		}
		return this.execDispatchSelectFromNodeGroup(ng, conn, null, null);
	}
	
	public Table dispatchSelectFromNodeGroupResourceToTable(String resourcePath, Object jarObj, SparqlConnection conn) throws Exception {
		
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(jarObj, resourcePath));
		sgjson.setSparqlConn(conn);
		return this.dispatchSelectFromNodeGroup(sgjson, null, null);
		
	}
	
public SimpleResultSet execDispatchDeleteFromNodeGroupResource(String resourcePath, Object jarObj, SparqlConnection conn, JSONArray runtimeConstraintsJson) throws Exception {
		
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(jarObj, resourcePath));
		NodeGroup ng = sgjson.getNodeGroup();
		if (runtimeConstraintsJson != null) {
			RuntimeConstraintManager manager = new RuntimeConstraintManager(ng);
			manager.applyConstraintJson(runtimeConstraintsJson);
		}
		return this.execDispatchDeleteFromNodeGroup(ng, conn, null, null);
	}
}
