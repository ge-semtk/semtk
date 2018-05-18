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

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.utility.LocalLogger;

public class NodeGroupExecutionClient extends RestClient {
	
	// json keys
	// TODO should probably move these elsewhere and/or consolidate with other classes
	private static final String JSON_KEY_JOB_ID = "jobID";
	private static final String JSON_KEY_NODEGROUP_ID = "nodeGroupId";
	private static final String JSON_KEY_LIMIT_OVERRIDE = "limitOverride";
	private static final String JSON_KEY_OFFSET_OVERRIDE = "offsetOverride";
	private static final String JSON_KEY_NODEGROUP  = "jsonRenderedNodeGroup";
	private static final String JSON_KEY_SPARQL_CONNECTION = "sparqlConnection";
	private static final String JSON_KEY_RUNTIME_CONSTRAINTS = "runtimeConstraints";
	private static final String JSON_KEY_EDC_CONSTRAINTS = "externalDataConnectionConstraints";
	
	// service mapping
	private static final String mappingPrefix = "/nodeGroupExecution";
	
	// endpoints
	private static final String jobStatusEndpoint = "/jobStatus";
	private static final String jobStatusMessageEndpoint = "/jobStatusMessage";
	private static final String jobCompletionCheckEndpoint = "/getJobCompletionCheck";
	private static final String jobCompletionPercentEndpoint = "/getJobCompletionPercentage";
	private static final String resultsLocationEndpoint = "/getResultsLocation";
	private static final String dispatchByIdEndpoint = "/dispatchById";
	private static final String dispatchFromNodegroupEndpoint = "/dispatchFromNodegroup";
	private static final String ingestFromCsvStringsNewConnectionEndpoint = "/ingestFromCsvStringsNewConnection";
	private static final String ingestFromCsvStringsByIdEndpoint = "/ingestFromCsvStringsById";
	private static final String ingestFromCsvStringsAndTemplateNewConnectionEndpoint = "/ingestFromCsvStringsAndTemplateNewConnection";
	private static final String getResultsTableEndpoint = "/getResultsTable";
	private static final String getResultsJsonLdEndpoint = "/getResultsJsonLd";
	private static final String dispatchSelectByIdEndpoint = "/dispatchSelectById";
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
	
	public String executeGetJobStatusWithSimpleReturn(String jobId) throws Exception{
		SimpleResultSet ret = this.executeGetJobStatus(jobId);
		return ret.getResult("status");
	}
	
	public boolean executeGetJobStatusIsSuccess(String jobId) throws Exception {
		return this.executeGetJobStatusWithSimpleReturn(jobId).equalsIgnoreCase("success");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeGetJobStatus(String jobId) throws Exception{
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
	
	public Boolean executeGetJobCompletionCheckWithSimpleReturn(String jobId) throws Exception{
		SimpleResultSet ret = this.executeGetJobCompletionCheck(jobId);
		
		String val = ret.getResult("completed");
		if(val.equalsIgnoreCase("true")) { return true; }
		else{ return false; }
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeGetJobCompletionCheck(String jobId) throws Exception{
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
	public String executeGetJobStatusMessageWithSimpleReturn(String jobId) throws Exception{
		SimpleResultSet ret = executeGetJobStatusMessage(jobId);
		return ret.getResult("message");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeGetJobStatusMessage(String jobId) throws Exception{
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
	public SimpleResultSet executeGetJobCompletionPercentage(String jobId) throws Exception {
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
	public Table executeGetResultsTable(String jobId) throws Exception {
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
	public JSONObject executeGetResultsJsonLd(String jobId) throws Exception {
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
	
	public Table executeGetResultsLocationWithSimpleReturn(String jobId) throws Exception{
		TableResultSet ret = this.executeGetResultsLocation(jobId);
		return ret.getTable();
	}
	
	@SuppressWarnings("unchecked")
	public TableResultSet executeGetResultsLocation(String jobId) throws Exception{
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint(mappingPrefix + resultsLocationEndpoint);
		this.parametersJSON.put(JSON_KEY_JOB_ID, jobId);
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			JSONObject tblWrapper = (JSONObject)jobj.get("table");
			
			Table tbl = Table.fromJson((JSONObject)tblWrapper.get("@table"));
			retval.addResults(tbl);
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			this.reset();
		}
		
		return retval;
	}

// action-specific endpoints for ID-based executions
	
	/**
	 * 	
	 * @param nodegroupID    -- string ID for the nodegroup to be executed. this assumes that the node group resides in a nodegroup store that was config'd on the far end (service)
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */

	public String executeDispatchSelectByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchSelectById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}

	public String executeDispatchConstructByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchConstructById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}

	public String executeDispatchConstructForInstanceManipulationByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchConstructForInstanceManipulationById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	/**
	 * Execute SELECT on nodegroup id, returning a jobId
	 * @param nodegroupID
	 * @param sparqlConnectionJson
	 * @param edcConstraintsJson
	 * @param runtimeConstraintsJson
	 * @return {String} jobId
	 * @throws Exception
	 */
	public String executeDispatchSelectByIdToJobId(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchSelectByIdWithSimpleReturn(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
	}
	
	public String executeDispatchConstructByIdToJobId(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchConstructByIdWithSimpleReturn(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
	}
	
	public String executeDispatchConstructForInstanceManipulationByIdToJobId(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchConstructByIdWithSimpleReturn(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
	}

	
	
	/**
	 * Execute SELECT all the way through to table
	 * @param nodegroupID
	 * @param sparqlConnectionJson
	 * @param edcConstraintsJson
	 * @param runtimeConstraintsJson
	 * @return
	 * @throws Exception
	 */
	public Table executeDispatchSelectByIdToTable(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception {
		
		// dispatch the job
		String jobId = this.executeDispatchSelectByIdToJobId(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		try {
			return this.waitForJobAndGetTable(jobId);
		} catch (Exception e) {
			// Add nodegroupID and "SELECT" to the error message
			throw new Exception(String.format("Error executing SELECT on nodegroup id='%s'", nodegroupID), e);
		}
	}
	
	public JSONObject executeDispatchConstructByIdToJsonLd(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception {
		
		// dispatch the job
		String jobId = this.executeDispatchConstructByIdToJobId(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		try {
			return this.waitForJobAndGetJsonLd(jobId);			
		} catch (Exception e) {
			// Add nodegroupID and "SELECT" to the error message
			throw new Exception(String.format("Error executing Construct on nodegroup id='%s'", nodegroupID), e);
		}		
	}
	
	public JSONObject executeDispatchConstructForInstanceManipulationByIdToJsonLd(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception {
		
		// dispatch the job
		String jobId = this.executeDispatchConstructForInstanceManipulationByIdToJobId(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		try {
			return this.waitForJobAndGetJsonLd(jobId);			
		} catch (Exception e) {
			// Add nodegroupID and "SELECT" to the error message
			throw new Exception(String.format("Error executing Construct on nodegroup id='%s'", nodegroupID), e);
		}		
	}
	
	/**
	 * Given jobId, check til job is done, check for success, get table
	 * @param jobId
	 * @return
	 * @throws Exception - if anything other than a valid table is returned
	 */
	private Table waitForJobAndGetTable(String jobId) throws Exception {
		// wait for completion
		while(! this.executeGetJobCompletionCheckWithSimpleReturn(jobId)) {
			// wait a while
			Thread.sleep(100);
		}
		
		// check for success
		if (this.executeGetJobStatusIsSuccess(jobId)) {
			return this.executeGetResultsTable(jobId);
		} else {
			String msg = this.executeGetJobStatusMessageWithSimpleReturn(jobId);
			throw new Exception(String.format("Job %s failed with message='%s'", jobId, msg));
		}
	}

	private JSONObject waitForJobAndGetJsonLd(String jobId) throws Exception {
		// wait for completion
		while(! this.executeGetJobCompletionCheckWithSimpleReturn(jobId)) {
			// wait a while
			Thread.sleep(100);
		}
		
		// check for success
		if (this.executeGetJobStatusIsSuccess(jobId)) {
			return this.executeGetResultsJsonLd(jobId);
		} else {
			String msg = this.executeGetJobStatusMessageWithSimpleReturn(jobId);
			throw new Exception(String.format("Job %s failed with message='%s'", jobId, msg));
		}
	}
	
	public SimpleResultSet executeDispatchSelectById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchSelectById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson, -1, -1);
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchSelectById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchSelectByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
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
	
	public SimpleResultSet executeDispatchConstructById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson, -1, -1);
	}
		
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchConstructById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchConstructByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
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
	public SimpleResultSet executeDispatchConstructForInstanceManipulationById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchConstructByIdEndpointForInstanceManipulationEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
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
		 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
		 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
		 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
		 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
		 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
		 * @return {String}              jobId
		 */
	public String executeDispatchCountByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchCountById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	public String executeDispatchCountByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet ret =  this.executeDispatchCountById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson, limitOverride, offsetOverride);
		return ret.getResult("JobId");
	}
	
	public SimpleResultSet executeDispatchCountById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchCountById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson, -1, -1);
	}

	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchCountById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchCountByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
		
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
	 * @param sparqlConnectionJson
	 * @param edcConstraintsJson
	 * @param runtimeConstraintsJson
	 * @return
	 * @throws Exception
	 */
	public Long executeDispatchCountByIdToLong(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchCountById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		Table tab = this.waitForJobAndGetTable(ret.getResult("JobId"));
		return tab.getCellAsLong(0, 0);
	}
	
	public Long executeDispatchCountByNodegroupToLong(NodeGroup nodegroup, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchCountFromNodeGroup(nodegroup, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		Table tab = this.waitForJobAndGetTable(ret.getResult("JobId"));
		return tab.getCellAsLong(0, 0);
	}
	
	/**
	 * 	
	 * @param nodegroupID    -- string ID for the nodegroup to be executed. this assumes that the node group resides in a nodegroup store that was config'd on the far end (service)
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @param targetObjectSparqlId -- the ID of the object to filter for valid values of. these are the sparql IDs used in the nodegroup.
	 * @return
	 */
	public String executeDispatchFilterByIdWithSimpleReturn(String nodegroupID, String targetObjectSparqlId, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchFilterById(nodegroupID, targetObjectSparqlId, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	public String executeDispatchFilterByIdWithSimpleReturn(String nodegroupID, String targetObjectSparqlId, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet ret =  this.executeDispatchFilterById(nodegroupID, targetObjectSparqlId, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson, limitOverride, offsetOverride);
		return ret.getResult("JobId");
	}

	public SimpleResultSet executeDispatchFilterById(String nodegroupID, String targetObjectSparqlId, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchFilterById(nodegroupID, targetObjectSparqlId, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson, -1, -1);
	}

	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchFilterById(String nodegroupID, String targetObjectSparqlId, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride) throws Exception{
			SimpleResultSet retval = null;
			
			conf.setServiceEndpoint(mappingPrefix + dispatchFilterByIdEndpoint);
			this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
			this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
			this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

			this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
			this.parametersJSON.put("targetObjectSparqlId", targetObjectSparqlId);
			this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
			this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
			
			
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
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	public String executeDispatchDeleteByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
			SimpleResultSet ret =  this.executeDispatchCountById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
			return ret.getResult("JobId");
		}
		
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchDeleteById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
			SimpleResultSet retval = null;
			
			conf.setServiceEndpoint(mappingPrefix + dispatchDeleteByIdEndpoint);
			this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
			this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
			this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
			this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
			
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
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	
	public String executeDispatchSelectFromNodeGroupWithSimpleReturn(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret = this.executeDispatchSelectFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	
	public SimpleResultSet executeDispatchSelectFromNodeGroup(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchSelectFromNodeGroup(ng.toJson(), sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchSelectFromNodeGroup(JSONObject ngJson, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchSelectFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ngJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful("Error at " + mappingPrefix + dispatchSelectFromNodegroupEndpoint);
		}
		finally{
			this.reset();
		}
		
		return retval;
	}
	
	public String executeDispatchConstructFromNodeGroupWithSimpleReturn(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret = this.executeDispatchConstructFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	public String executeDispatchConstructForInstanceManipulationFromNodeGroupWithSimpleReturn(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret = this.executeDispatchConstructForInstanceManipulationFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchConstructFromNodeGroup(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchConstructFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
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
	public SimpleResultSet executeDispatchConstructForInstanceManipulationFromNodeGroup(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchConstructFromNodegroupEndpointForInstanceManipulationEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful("Error at " + mappingPrefix + dispatchSelectFromNodegroupEndpoint);
		}
		finally{
			this.reset();
		}
		
		return retval;
	}
	
	public JSONObject executeDispatchConstructFromNodeGroupToTable(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		
		SimpleResultSet ret = this.executeDispatchConstructFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		return this.waitForJobAndGetJsonLd(ret.getResult("JobId"));
	}
	
	public JSONObject executeDispatchConstructForInstanceManipulationFromNodeGroupToTable(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		
		SimpleResultSet ret = this.executeDispatchConstructForInstanceManipulationFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		return this.waitForJobAndGetJsonLd(ret.getResult("JobId"));
	}
	
	public Table executeDispatchSelectFromNodeGroupToTable(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		
		SimpleResultSet ret = this.executeDispatchSelectFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		return this.waitForJobAndGetTable(ret.getResult("JobId"));
	}
	
	public Table executeDispatchSelectFromNodeGroupToTable(JSONObject ngJson, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		
		SimpleResultSet ret = this.executeDispatchSelectFromNodeGroup(ngJson, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		
		return this.waitForJobAndGetTable(ret.getResult("JobId"));
	}
	
	/**
	 * 	
	 * @param ng   -- the nodegroup to execute a selection query from
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	
	public String executeDispatchCountFromNodeGroupWithSimpleReturn(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret = this.executeDispatchCountFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchCountFromNodeGroup(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchCountFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
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
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	
	public String executeDispatchDeleteFromNodeGroupWithSimpleReturn(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret = this.executeDispatchDeleteFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchDeleteFromNodeGroup(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchDeleteFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
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
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @param targetObjectSparqlId -- the ID of the object to filter for valid values of. these are the sparql IDs used in the nodegroup.
	 * @return
	 */
	
	public String executeDispatchFilterFromNodeGroupWithSimpleReturn(NodeGroup ng, String targetObjectSparqlId, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret = this.executeDispatchFilterFromNodeGroup(ng, targetObjectSparqlId, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchFilterFromNodeGroup(NodeGroup ng, String targetObjectSparqlId, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchFilterByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
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
 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
 * @return
 */

	public String executeDispatchByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet ret =  this.executeDispatchById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson, limitOverride, offsetOverride);
		return ret.getResult("JobId");
	}
	
	public String executeDispatchByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	public SimpleResultSet executeDispatchById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		return this.executeDispatchById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson, -1, -1);
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson, int limitOverride, int offsetOverride) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchByIdEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP_ID, nodegroupID);
		this.parametersJSON.put(JSON_KEY_LIMIT_OVERRIDE, limitOverride);
		this.parametersJSON.put(JSON_KEY_OFFSET_OVERRIDE, offsetOverride);

		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
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
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	
	public String executeDispatchFromNodeGroupWithSimpleReturn(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONObject runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret = this.executeDispatchFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	@SuppressWarnings("unchecked")
	public SimpleResultSet executeDispatchFromNodeGroup(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONObject runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + dispatchFromNodegroupEndpoint);
		this.parametersJSON.put(JSON_KEY_NODEGROUP, ng.toJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionJson.toJSONString());
		this.parametersJSON.put(JSON_KEY_EDC_CONSTRAINTS, edcConstraintsJson == null ? null : edcConstraintsJson.toJSONString());	
		this.parametersJSON.put(JSON_KEY_RUNTIME_CONSTRAINTS,            runtimeConstraintsJson == null ? null : runtimeConstraintsJson.toJSONString());		
		
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
	public RecordProcessResults execIngestionFromCsvStr(String nodegroupAndTemplateId, String csvContentStr, JSONObject sparqlConnectionAsJsonObject) throws Exception {
		return this.execIngestionFromCsvStrNewConnection(nodegroupAndTemplateId, csvContentStr, sparqlConnectionAsJsonObject);
	}
	
	/**
	 * Ingest CSV using a nodegroup ID.
	 */
	@SuppressWarnings("unchecked")
	public RecordProcessResults execIngestionFromCsvStrNewConnection(String nodegroupAndTemplateId, String csvContentStr, JSONObject sparqlConnectionAsJsonObject) throws Exception {
		RecordProcessResults retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + ingestFromCsvStringsNewConnectionEndpoint);
		this.parametersJSON.put("templateId", nodegroupAndTemplateId);
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionAsJsonObject.toJSONString());
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
	public RecordProcessResults execIngestionFromCsvStrById(String nodegroupAndTemplateId, String csvContentStr, JSONObject sparqlConnectionAsJsonObject) throws Exception {
		return execIngestionFromCsvStrNewConnection(nodegroupAndTemplateId, csvContentStr, sparqlConnectionAsJsonObject);
	}
	
	/**
	 * MIS-NAMED FUNCTION retained for BACKWARDS COMPATIBILITY
	 */
	public RecordProcessResults execIngestionFromCsvStr(SparqlGraphJson sparqlGraphJson, String csvContentStr, JSONObject sparqlConnectionAsJsonObject) throws Exception {
		return 	this.execIngestionFromCsvStrNewConnection(sparqlGraphJson, csvContentStr, sparqlConnectionAsJsonObject);
	}

	/**
	 * Ingest CSV using a nodegroup.
	 */
	@SuppressWarnings("unchecked")
	public RecordProcessResults execIngestionFromCsvStrNewConnection(SparqlGraphJson sparqlGraphJson, String csvContentStr, JSONObject sparqlConnectionAsJsonObject) throws Exception {
		RecordProcessResults retval = null;
		
		conf.setServiceEndpoint(mappingPrefix + ingestFromCsvStringsAndTemplateNewConnectionEndpoint);
		this.parametersJSON.put("template", sparqlGraphJson.getJson().toJSONString());
		this.parametersJSON.put(JSON_KEY_SPARQL_CONNECTION, sparqlConnectionAsJsonObject.toJSONString());
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

}
