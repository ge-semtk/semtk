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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstrainedItems;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.NodeGroupResultSet;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.asynchronousQuery.DispatcherSupportedQueryTypes;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * service to run stored nodegroups. 
 * @author 200018594
 *
 */
@RestController
@RequestMapping("/nodeGroupExecution")
public class NodeGroupExecutionRestController {
 	static final String SERVICE_NAME = "nodeGroupExecutionService";
 	
	@Autowired
	NodegroupExecutionProperties prop;
	
	@CrossOrigin
	@RequestMapping(value="/jobStatus", method=RequestMethod.POST)
	public JSONObject getJobStatus(@RequestBody StatusRequestBody requestBody){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			String results = sqe.getJobStatus();
			retval.setSuccess(true);
			retval.addResult("status", results);
		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "jobStatus", e);
		}
	
		return retval.toJson();
	} 
	
	@CrossOrigin
	@RequestMapping(value="/jobStatusMessage", method=RequestMethod.POST)
	public JSONObject getJobStatusMessage(@RequestBody StatusRequestBody requestBody){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			String results = sqe.getJobStatusMessage();
			retval.setSuccess(true);
			retval.addResult("message", results);
		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "jobStatusMessage", e);
		}
	
		return retval.toJson();
	} 
	
	@CrossOrigin
	@RequestMapping(value="/getJobCompletionCheck", method=RequestMethod.POST)
	public JSONObject getJobCompletion(@RequestBody StatusRequestBody requestBody ){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			Boolean results = sqe.getJobCompletion();
			retval.setSuccess(true);
			if(results){
				retval.addResult("completed", "true");
			}
			else{
				retval.addResult("completed", "false");
			}
		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "getJobCompletionCheck", e);
		}
	
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getJobCompletionPercentage", method=RequestMethod.POST)
	public JSONObject getJobCompletionPercent(@RequestBody StatusRequestBody requestBody ){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			int results = sqe.getJobPercentCompletion();
			retval.setSuccess(true);
			retval.addResult("percent", results);

		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "getJobCompletionPercentage", e);
		}
	
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getResultsTable", method=RequestMethod.POST)
	public JSONObject getResultsTable(@RequestBody StatusRequestBody requestBody ){
		TableResultSet retval = new TableResultSet();
		
		try{
			NodeGroupExecutor nge = this.getExecutor(prop, requestBody.getJobID());
			Table retTable = nge.getTableResults();
			retval.setSuccess(true);
			retval.addResults(retTable);
		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new TableResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "getResultsTable", e);
		}
		return retval.toJson();
	}
	
	
	@CrossOrigin
	@RequestMapping(value="/getResultsJsonLd", method=RequestMethod.POST)
	public JSONObject getResultsJsonLd(@RequestBody StatusRequestBody requestBody ){
		NodeGroupResultSet retval = new NodeGroupResultSet();
		
		try{
			NodeGroupExecutor nge = this.getExecutor(prop, requestBody.getJobID());
			JSONObject retLd = nge.getJsonLdResults();
			retval.setSuccess(true);
			retval.addResultsJSON(retLd);
		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new NodeGroupResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "getResultsJsonLd", e);
		}
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getResultsLocation", method=RequestMethod.POST)
	public JSONObject getResultsLocation(@RequestBody StatusRequestBody requestBody ){
		TableResultSet retval = new TableResultSet();
		
		// note: make sure the response is sane when results do not yet exist. the failure should be as graceful as we can make them.
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			URL[] results = sqe.getResultsLocation();
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
			LocalLogger.printStackTrace(e);
			retval = new TableResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "getResultsLocation", e);
		}
	
		return retval.toJson();
	}
	
	// base methods which others use
	
	public JSONObject dispatchAnyJobById(@RequestBody DispatchByIdRequestBody requestBody, DispatcherSupportedQueryTypes qt){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor sqe = this.getExecutor(prop, null );
			// try to create a sparql connection

			SparqlConnection connection = requestBody.getSparqlConnection();			
			// create a json object from the external data constraints. 
			
			JSONObject edcConstraints = null;
			if(requestBody.getExternalDataConnectionConstraints() != null && !requestBody.getExternalDataConnectionConstraints().equals("")){
				// attempt to process the constraints to json.
				JSONParser jParse = new JSONParser();
				edcConstraints = (JSONObject)jParse.parse(requestBody.getExternalDataConnectionConstraints());
			}

			// try to get the runtime constraints
			JSONArray runtimeConstraints = this.getRuntimeConstraintsAsJsonArray(requestBody.getRuntimeConstraints());
			
			// check if this is actually for a filter query
			String targetId = null;
			if(requestBody instanceof FilterDispatchByIdRequestBody){
				// set the target ID
				targetId = ((FilterDispatchByIdRequestBody)requestBody).getTargetObjectSparqlId();
			}
			
			// dispatch the job. 
			sqe.dispatchJob(qt, connection, requestBody.getNodeGroupId(), edcConstraints, runtimeConstraints, targetId);
			String id = sqe.getJobID();
			
			retval.setSuccess(true);
			retval.addResult("JobId", id); 

		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage("service: " + SERVICE_NAME + " method: dispatchAnyJobById()", e);
		}
	
		return retval.toJson();

	}

	public JSONObject dispatchAnyJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody, DispatcherSupportedQueryTypes qt){

		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor sqe = this.getExecutor(prop, null );
			// try to create a sparql connection
			SparqlConnection connection = requestBody.getSparqlConnection();			
			// create a json object from the external data constraints. 
			
			JSONObject edcConstraints = null;
			if(requestBody.getExternalDataConnectionConstraints() != null && !requestBody.getExternalDataConnectionConstraints().equals("")){
				// attempt to process the constraints to json.
				JSONParser jParse = new JSONParser();
				edcConstraints = (JSONObject)jParse.parse(requestBody.getExternalDataConnectionConstraints());
			}
			
			// get the nodegroup. we are assuming that the user should send a node group complete with original connection info, since we 
			// store them that way. we'll perform a quick check to find out though
			JSONObject encodedNodeGroup = requestBody.getJsonNodeGroup();
			NodeGroup ng = new NodeGroup();
			
			// check that sNodeGroup is a key in the json. if so, this has a connection and the rest.			
			if (SparqlGraphJson.isSparqlGraphJson(encodedNodeGroup)) {
				SparqlGraphJson sgJson = new SparqlGraphJson(encodedNodeGroup);
				ng.addJsonEncodedNodeGroup(sgJson.getSNodeGroupJson());
				
				if (connection == null) {
					connection = sgJson.getSparqlConn();
				}
			}
			
			// otherwise, check for a truncated one that is only the nodegroup proper.
			else if(NodeGroup.isNodeGroup(encodedNodeGroup)) {
				ng.addJsonEncodedNodeGroup(encodedNodeGroup);
				
				if (connection == null) {
						throw new Exception("No sparql connection is specified");
				}
			}
			else{
				// no idea what this is...
				throw new Exception("Value given for encoded node group is neither SparqlGraphJson nor NodeGroup");
			}
		
			// retrieve the connection from the nodegroup if needed
			
			
			// try to get the runtime constraints
			JSONArray runtimeConstraints = this.getRuntimeConstraintsAsJsonArray(requestBody.getRuntimeConstraints());
			
			
			String targetId = null;
			if(requestBody instanceof FilterDispatchFromNodeGroupRequestBody){
				// set the target ID
				targetId = ((FilterDispatchFromNodeGroupRequestBody)requestBody).getTargetObjectSparqlId();
			}
			
			// dispatch the job. 
			sqe.dispatchJob(qt, connection, ng, edcConstraints, runtimeConstraints, targetId);
			String id = sqe.getJobID();
			
			retval.setSuccess(true);
			retval.addResult("JobId", id);

		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "../dispatchAnyJobById()", e);
		}
	
		return retval.toJson();

	}

	// end base methods
	
	@CrossOrigin
	@RequestMapping(value="/dispatchById", method=RequestMethod.POST)
	public JSONObject dispatchJobById(@RequestBody DispatchByIdRequestBody requestBody){
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody ){	
		return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);

	}

	@CrossOrigin
	@RequestMapping(value="/dispatchSelectById", method=RequestMethod.POST)
	public JSONObject dispatchSelectJobById(@RequestBody DispatchByIdRequestBody requestBody){
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchSelectFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchSelectJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody ){	
		return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT);

	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchConstructById", method=RequestMethod.POST)
	public JSONObject dispatchConstructJobById(@RequestBody DispatchByIdRequestBody requestBody){
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT);
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchConstructFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchConstructJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody ){	
		return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT);

	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchConstructForInstanceManipulationById", method=RequestMethod.POST)
	public JSONObject dispatchConstructInstanceJobById(@RequestBody DispatchByIdRequestBody requestBody){
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION);
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchConstructForInstanceManipulationFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchConstructInstanceJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody ){	
		return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION);

	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchCountById", method=RequestMethod.POST)
	public JSONObject dispatchCountJobById(@RequestBody DispatchByIdRequestBody requestBody){
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.COUNT);
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchCountFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchCountJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody ){	
		return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.COUNT);

	}

	@CrossOrigin
	@RequestMapping(value="/dispatchFilterById", method=RequestMethod.POST)
	public JSONObject dispatchFilterJobById(@RequestBody FilterDispatchByIdRequestBody requestBody){
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.FILTERCONSTRAINT);
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchFilterFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchFilterJobFromNodegroup(@RequestBody FilterDispatchFromNodeGroupRequestBody requestBody ){	
		return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.FILTERCONSTRAINT);

	}

	@CrossOrigin
	@RequestMapping(value="/dispatchDeleteById", method=RequestMethod.POST)
	public JSONObject dispatchDeleteJobById(@RequestBody DispatchByIdRequestBody requestBody){
			return dispatchAnyJobById(requestBody, DispatcherSupportedQueryTypes.DELETE);
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchDeleteFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchDeleteJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody ){	
		return dispatchAnyJobFromNodegroup(requestBody, DispatcherSupportedQueryTypes.DELETE);

	}
	
	// direct Sparql Execution
	@CrossOrigin
	@RequestMapping(value="/dispatchRawSparql", method=RequestMethod.POST)
	public JSONObject dispatchRawSparql(@RequestBody DispatchRawSparqlRequestBody requestBody){

		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			NodeGroupExecutor sqe = this.getExecutor(prop, null );
			// try to create a sparql connection
			SparqlConnection connection = requestBody.getSparqlConnection();			

			// dispatch the job. 
			sqe.dispatchRawSparql(connection, requestBody.getSparql());
			String id = sqe.getJobID();
			
			retval.setSuccess(true);
			retval.addResult("JobId", id);

		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "dispatchRawSparql", e);
		}
	
		return retval.toJson();

	}
	/**
	 * Perform ingestion using a stored nodegroup ID.
	 * PEC:  "NewConnection" in name is inconsistent with most other "ById" endpoints.  Others imply it.
	 */
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsNewConnection", method=RequestMethod.POST)
	public JSONObject ingestFromTemplateIdAndCsvStringNewConn(@RequestBody IngestByConnIdCsvStrRequestBody requestBody) throws Exception{
		RecordProcessResults retval = null;
		try{
			NodeGroupExecutor nodeGroupExecutor = this.getExecutor(prop, null);		
			retval = nodeGroupExecutor.ingestFromTemplateIdAndCsvString(requestBody.getSparqlConnection(), requestBody.getTemplateId(), requestBody.getCsvContent());
		}catch(Exception e){
			retval = new RecordProcessResults(false);
			retval.addRationaleMessage(SERVICE_NAME, "ingestFromCsvStringsNewConnection", e);
		}
		return retval.toJson();
	}

	/**
	 * Perform ingestion by passing in a nodegroup.
	 * PEC:  "NewConnection" in name is inconsistent with most other "ById" endpoints.  Others imply it.
	 */
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsAndTemplateNewConnection", method=RequestMethod.POST)
	public JSONObject ingestFromTemplateAndCsvString(@RequestBody IngestByNodegroupCsvStrRequestBody requestBody) throws Exception{
		RecordProcessResults retval = null;
		try{
			NodeGroupExecutor nodeGroupExecutor = this.getExecutor(prop, null);		
			SparqlGraphJson sparqlGraphJson = new SparqlGraphJson(requestBody.getTemplate());
			retval = nodeGroupExecutor.ingestFromTemplateIdAndCsvString(requestBody.getSparqlConnection(), sparqlGraphJson, requestBody.getCsvContent());
		}catch(Exception e){
			retval = new RecordProcessResults(false);
			retval.addRationaleMessage(SERVICE_NAME, "ingestFromCsvStringsAndTemplateNewConnection", e);
		}
		return retval.toJson();
	}
	
	/**
	 * Perform ingestion using a stored nodegroup ID.
	 */
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsById", method=RequestMethod.POST)
	public JSONObject ingestFromCsvStringsById(@RequestBody IngestByIdCsvStrRequestBody requestBody) throws Exception{
		RecordProcessResults retval = null;
		try{
			NodeGroupExecutor nodeGroupExecutor = this.getExecutor(prop, null);		
			retval = nodeGroupExecutor.ingestFromTemplateIdAndCsvString(requestBody.getSparqlConnection(), requestBody.getTemplateId(), requestBody.getCsvContent());
		}catch(Exception e){
			retval = new RecordProcessResults(false);
			retval.addRationaleMessage(SERVICE_NAME, "ingestFromCsvStrings", e);
		}
		return retval.toJson();
	}

	
	@CrossOrigin
	@RequestMapping(value="/getRuntimeConstraintsByNodeGroupID", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraints(@RequestBody ConstraintsFromIdRequestBody requestBody){
		TableResultSet retval = null;
		
		try {
			NodeGroupStoreConfig ngcConf = new NodeGroupStoreConfig(prop.getNgStoreProtocol(), prop.getNgStoreServer(), prop.getNgStorePort());
			NodeGroupStoreRestClient nodegroupstoreclient = new NodeGroupStoreRestClient(ngcConf);
			retval = nodegroupstoreclient.executeGetNodeGroupRuntimeConstraints(requestBody.getNodegroupId()) ;
		}
		catch(Exception e){
			retval = new TableResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getRuntimeConstraintsByNodeGroupID", e);
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getRuntimeConstraintsByNodeGroup", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraintsFromNodegroup(@RequestBody NodegroupRequest requestBody){
		TableResultSet retval = null;
		
		try {
			NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
			RuntimeConstrainedItems rtci = new RuntimeConstrainedItems(ng);
			
			retval = new TableResultSet(true);
			retval.addResults( rtci.getConstrainedItemsDescription() );
		
		}
		catch(Exception e){
			retval = new TableResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getRuntimeConstraintsByNodeGroup", e);
		}
		
		return retval.toJson();
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

	// create the required StoredQueryExecutor
	private NodeGroupExecutor getExecutor(NodegroupExecutionProperties prop, String jobID) throws Exception{
		NodeGroupStoreConfig ngcConf = new NodeGroupStoreConfig(prop.getNgStoreProtocol(), prop.getNgStoreServer(), prop.getNgStorePort());
		DispatchClientConfig dsConf  = new DispatchClientConfig(prop.getDispatchProtocol(), prop.getDispatchServer(), prop.getDispatchPort());
		ResultsClientConfig  rConf   = new ResultsClientConfig(prop.getResultsProtocol(), prop.getResultsServer(), prop.getResultsPort());
		StatusClientConfig   sConf   = new StatusClientConfig(prop.getStatusProtocol(), prop.getStatusServer(), prop.getStatusPort(), jobID);
		IngestorClientConfig iConf   = new IngestorClientConfig(prop.getIngestProtocol(), prop.getIngestServer(), prop.getIngestPort());
		
		// create the other components we need. 
		NodeGroupStoreRestClient nodegroupstoreclient = new NodeGroupStoreRestClient(ngcConf);
		DispatchRestClient dispatchclient = new DispatchRestClient(dsConf);
		ResultsClient resultsclient = new ResultsClient(rConf);
		StatusClient statusclient = new StatusClient(sConf);
		IngestorRestClient ingestClient = new IngestorRestClient(iConf);
		
		// create the actual executor
		NodeGroupExecutor retval = new NodeGroupExecutor(nodegroupstoreclient, dispatchclient, resultsclient, statusclient, ingestClient);
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
	
}


