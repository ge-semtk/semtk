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

package com.ge.research.semtk.api.nodeGroupExecution;

import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstrainedItems;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.asynchronousQuery.DispatcherSupportedQueryTypes;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;
import com.ge.research.semtk.utility.LocalLogger;

public class NodeGroupExecutor {
	// json representing intent to get SparqlConnection from the accompanying nodegroup
	// note that spaces, quotes and capitalization will be ignored
	private static final String USE_NODEGROUP_CONN_STR = 
			"{ \"name\" :   \"%NODEGROUP%\","
			+ "\"domain\" : \"%NODEGROUP%\","
			+ "\"model\" : [], " 
			+ "\"data\" : []        } ";
		
	// all the internal instances needed to manage external communications
	private NodeGroupStoreRestClient ngsrc = null;
	private DispatchRestClient drc = null;
	private ResultsClient rc = null;
	private StatusClient sc = null;
	private IngestorRestClient irc = null;
	// internal data.
	private String currentJobId = null;
	
	// the Stored Query Executor will be the heart of the stored Query Executor Service.
	// all of the most important actions will occur in this class
	
	public NodeGroupExecutor(NodeGroupStoreRestClient nodegroupstoreclient, DispatchRestClient dispatchclient,
			ResultsClient resultsclient, StatusClient statusclient, IngestorRestClient ingestorClient){
		
		this.ngsrc = nodegroupstoreclient;
		this.drc   = dispatchclient;
		this.rc    = resultsclient;
		this.sc    = statusclient;
		this.irc   = ingestorClient;
	}
	
	public NodeGroupExecutor(String jobID, NodeGroupStoreRestClient nodegroupstoreclient, DispatchRestClient dispatchclient,
			ResultsClient resultsclient, StatusClient statusclient, IngestorRestClient ingestorClient){

		this(nodegroupstoreclient, dispatchclient, resultsclient, statusclient, ingestorClient); // call the other constructor
		// assume the given Job ID is actually a real one. if it is not, we will find out later... there is no good (read: "cheap") way to validate at this point. 
		// the only truly meaningful way to check the existence of the a JobID is to check against the status service. if this seems error-prone or otherwise problematic,
		// we can put that explicit check in later.

		this.currentJobId = jobID;
		this.sc.setJobId(jobID);
	}
	
	public static SparqlConnection get_USE_NODEGROUP_CONN() throws Exception {
		return new SparqlConnection(USE_NODEGROUP_CONN_STR);
	}
	
	//Job ID related
	public String getJobID(){
		return this.currentJobId;
	}
	
	public void setJobID(String jobID){
		this.currentJobId = jobID;
		this.sc.setJobId(jobID);
	}
	
	// Status Information
	public int getJobPercentCompletion() throws Exception{
		int retval = 0;
		
		// get the reported completion percentage of the Job referenced by the currentJobId 
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getJobPercentCompletion -- the current job ID is null. unable to get completion on nonexistent job.");
		}
		else{
			retval = this.sc.execGetPercentComplete();
		}
		return retval;
	}
	
	public String getJobStatus() throws Exception{
		String retval = null;
		
		// get the status of the Job referenced by the currentJobId 
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getJobStatus -- the current job ID is null. unable to get status on nonexistent job.");
		}
		else{
			retval = this.sc.execGetStatus();
		}
		return retval;
	}

	public String getJobStatusMessage() throws Exception{
		String retval = null;
		
		// get the status of the Job referenced by the currentJobId 
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getJobStatus -- the current job ID is null. unable to get status on nonexistent job.");
		}
		else{
			retval = this.sc.execGetStatusMessage();
		}
		return retval;
	}
	
	public Boolean getJobCompletion() throws Exception{
		Boolean retval = false;
		
		// get the status of the Job referenced by the currentJobId 
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getJobCompletion -- the current job ID is null. unable to get info on nonexistent job.");
		}
		else{
			int percent = this.sc.execGetPercentComplete();
			if(percent != 100){ 	// not 100%? incomplete
				retval = false;
			}
			else{					// must be done. 
				retval = true;
			}
		}
		return retval;
	}
	
	public void waitOnJobCompletion() throws Exception{
		// keeps polling the status until it reaches 100%
		// default wait interval is one second.
		
		this.waitOnJobCompletion(1000);
	}
	
	public void waitOnJobCompletion(int sleepInterval) throws Exception{
		// this would be used to simulate a synchronous call.
		// warning: this sleeps between invocations to avoid just constantly polling the service and giving it a headache.
		// if the sleep interval is less than a half second, it is reset to a half second.
		if(sleepInterval < 10){ sleepInterval = 10; }
		
		int percentComplete = 0;
		
		while(percentComplete < 100){
			percentComplete = this.getJobPercentCompletion();
			Thread.sleep(sleepInterval);
		}
		
		return;
	}
	
	// Results Information
	public URL[] getResultsLocation() throws Exception{
		URL[] retval = null;
		
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getResultsLocation -- the current job ID is null. unable to get info on nonexistent job.");
		}
		else{
			retval = this.rc.execGetResults(currentJobId);
		}
		return retval;
	}
	
	public Table getTableResults() throws Exception{
		Table retval = null;
		
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getTableResults -- the current job ID is null. unable to get info on nonexistent job.");
		}
		else{
			retval = this.rc.execTableResultsJson(this.currentJobId, null).getTable();
		}
		return retval;
		
	}
	
	// this method only works if the results are a JSON-LD result...
	public JSONObject getJsonLdResults() throws Exception{
		JSONObject retval = null;
		
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getTableResults -- the current job ID is null. unable to get info on nonexistent job.");
		}
		else{
			retval = this.rc.execGetGraphResult(this.currentJobId);
		}
		return retval;
	}
	
	// Dispatch actions
	public void dispatchRawSparql(SparqlConnection sc, String sparqlQuery) throws Exception {
	
		SimpleResultSet simpleRes = null;
		
		simpleRes = this.drc.executeRawSparqlQuery(sc, sparqlQuery);
		
		// set up the Job ID
		this.setJobID(simpleRes.getResult("requestID"));
	}
	
	/**
	 * 
	 * @param qt
	 * @param sc
	 * @param ng
	 * @param externalConstraints
	 * @param runtimeConstraints
	 * @param targetObjectSparqlID
	 * @throws Exception
	 */
	public void dispatchJob(DispatcherSupportedQueryTypes qt, SparqlConnection sc, NodeGroup ng, JSONObject externalConstraints, JSONArray runtimeConstraints, String targetObjectSparqlID) throws Exception{
		// externalConstraints as used by executeQueryFromNodeGroup

		// apply the runtimeConstraints
		if(runtimeConstraints != null){
			RuntimeConstrainedItems rtci = new RuntimeConstrainedItems(ng);
			rtci.applyConstraintJson(runtimeConstraints);
		}
		
		// serialize the nodeGroup and connection info to JSON...
		JSONObject serializedNodeGroup  = ng.toJson();
		JSONObject serializedConnection = sc.toJson();
		
		// assemble the nodeGroup and the connection into a nodegroup as the services want them.
		JSONObject sendable = new JSONObject();
		sendable.put("sparqlConn", serializedConnection);
		sendable.put("sNodeGroup", serializedNodeGroup);
		
		SimpleResultSet simpleRes = null;
		
		// select the appropriate type and try to dispatch the job.
		if(qt.equals(DispatcherSupportedQueryTypes.SELECT_DISTINCT)){
			simpleRes = this.drc.executeSelectQueryFromNodeGroup(sendable, externalConstraints);
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.COUNT)){
			simpleRes = this.drc.executeCountQueryFromNodeGroup(sendable, externalConstraints);			
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.FILTERCONSTRAINT)){
			
			sendable.put("targetObjectSparqlID", targetObjectSparqlID);
			
			simpleRes = this.drc.executeFilterQueryFromNodeGroup(sendable, targetObjectSparqlID, externalConstraints);			
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.DELETE)){
			simpleRes = this.drc.executeDeleteQueryFromNodeGroup(sendable, externalConstraints);			
		}	
		
		else if(qt.equals(DispatcherSupportedQueryTypes.CONSTRUCT)){
			simpleRes = this.drc.executeConstructQueryFromNodeGroup(sendable, externalConstraints);
		}

		else if(qt.equals(DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION)){
			simpleRes = this.drc.executeConstructQueryForInstanceManipulationFromNodeGroup(sendable, externalConstraints);
		
		}
		
		
		else{
			throw new Exception("NodeGroupExecutor:dispatchJob :: DispatcherSupportedQueryTypes type " + qt.name() + " is not currently supported.");
		}
		
		// set up the Job ID
		this.setJobID(simpleRes.getResult("requestID"));
	}
	
	/**
	 * 
	 * @param qt
	 * @param sc - if null, fill from the stored nodegroup
	 * @param storedNodeGroupId
	 * @param externalConstraints
	 * @param runtimeConstraints
	 * @param targetObjectSparqlID
	 * @throws Exception
	 */
	public void dispatchJob(DispatcherSupportedQueryTypes qt, SparqlConnection sc, String storedNodeGroupId, JSONObject externalConstraints, JSONArray runtimeConstraints, String targetObjectSparqlID) throws Exception{
		
		// get the node group from the remote store
		TableResultSet trs = this.ngsrc.executeGetNodeGroupById(storedNodeGroupId);
		Table t = trs.getResults();
		
		if(t.getNumRows() < 1){ 
			throw new Exception("StoredQueryExecutor::dispatchJob -- the ID passed to look up a remote node group (" + storedNodeGroupId + ") did not return any results.");
		}
		if(t.getNumRows() > 1){
			throw new Exception("StoredQueryExecutor::dispatchJob -- the ID passed to look up a remote node group (" + storedNodeGroupId + ") returned more than one result. this is likely not good.");
		}
		
		String serializedNodeGroup = t.getRow(0).get( t.getColumnIndex("NodeGroup") );
		
		JSONParser jParse = new JSONParser();
		JSONObject encodedNodeGroup = (JSONObject) jParse.parse(serializedNodeGroup);
		NodeGroup ng = new NodeGroup();	
		
		// check that sNodeGroup is a key in the json. if so, this has a connection and the rest.
		if(encodedNodeGroup.containsKey("sNodeGroup")){
			LocalLogger.logToStdErr("located key: sNodeGroup");
			ng.addJsonEncodedNodeGroup((JSONObject) encodedNodeGroup.get("sNodeGroup"));
		}
		
		// otherwise, check for a truncated one that is only the nodegroup proper.
		else if(encodedNodeGroup.containsKey("sNodeList")){
			ng.addJsonEncodedNodeGroup(encodedNodeGroup);
		}
		else{
			// no idea what this is...
			throw new Exception("Value given for encoded node group does not seem to be a node group as it has neither sNodeGroup or sNodeList keys");
		}
		
		// retrieve the connection from the nodegroup if needed
		if (this.isUseNodegroupConn(sc)) {
			if(encodedNodeGroup.containsKey("sparqlConn")) {
				sc = new SparqlConnection();
				sc.fromJson((JSONObject) encodedNodeGroup.get("sparqlConn"));
			} else {
				throw new Exception("No sparql connection is specified");
			}
		}
		
		// dispatch the job itself
		this.dispatchJob(qt, sc, ng, externalConstraints, runtimeConstraints, targetObjectSparqlID);
	}
	
	public URL[] dispatchJobSynchronous(DispatcherSupportedQueryTypes qt, SparqlConnection sc, String storedNodeGroupId, JSONObject externalConstraints, JSONArray runtimeConstraints, String targetObjectSparqlID) throws Exception {
		
		// dispatch the job
		this.dispatchJob(qt, sc, storedNodeGroupId, externalConstraints, runtimeConstraints, targetObjectSparqlID) ;
		
		// wait on the job to complete
		this.waitOnJobCompletion();
	
		// check for the failure
		if(!this.getJobStatus().equalsIgnoreCase("success")){
			throw new Exception("StoredQueryExecutor::singleCallDispatch -- the job failed. no results will be returned.");
		}
		
		// send back the results
		return this.getResultsLocation();
	}
	
	public URL[] dispatchJobSynchronous(DispatcherSupportedQueryTypes qt, SparqlConnection sc, NodeGroup ng, JSONObject externalConstraints, JSONArray runtimeConstraints, String targetObjectSparqlID) throws Exception {
		
		// dispatch the job
		this.dispatchJob(qt, sc, ng, externalConstraints, runtimeConstraints, targetObjectSparqlID) ;
		
		// wait on the job to complete
		this.waitOnJobCompletion();
	
		// check for the failure
		if(!this.getJobStatus().equalsIgnoreCase("success")){
			throw new Exception("StoredQueryExecutor::singleCallDispatch -- the job failed. no results will be returned.");
		}
		
		// send back the results
		return this.getResultsLocation();
	}

	
	/**
	 * Ingest CSV, given a nodegroup ID
	 * @param conn - override, may be null
	 * @param storedNodeGroupId
	 * @param csvContents
	 * @return
	 * @throws Exception
	 */
	public RecordProcessResults ingestFromTemplateIdAndCsvString(SparqlConnection conn, String storedNodeGroupId, String csvContents) throws Exception{
		
		// get the node group from the remote store
		TableResultSet trs = this.ngsrc.executeGetNodeGroupById(storedNodeGroupId);
		Table t = trs.getResults();
		
		if(t.getNumRows() < 1){ 
			throw new Exception("StoredQueryExecutor::dispatchJob -- the ID passed to look up a remote node group (" + storedNodeGroupId + ") did not return any results.");
		}
		if(t.getNumRows() > 1){
			throw new Exception("StoredQueryExecutor::dispatchJob -- the ID passed to look up a remote node group (" + storedNodeGroupId + ") returned more than one result. this is likely not good.");
		}
		
		String serializedNodeGroup = t.getRow(0).get( t.getColumnIndex("NodeGroup") );		
		JSONObject parsedStoredNg = (JSONObject) (new JSONParser()).parse(serializedNodeGroup);
		SparqlGraphJson sparqlGraphJson = new SparqlGraphJson(parsedStoredNg);
			
		return ingestFromTemplateIdAndCsvString(conn, sparqlGraphJson, csvContents);
	}
		
	
	/**
	 * Ingest CSV, given a nodegroup object
	 * @param conn - override, may be null
	 * @param sparqlGraphJson
	 * @param csvContents
	 * @return
	 * @throws Exception
	 */
	public RecordProcessResults ingestFromTemplateIdAndCsvString(SparqlConnection conn, SparqlGraphJson sparqlGraphJson, String csvContents) throws Exception{
		
		RecordProcessResults retval = null;

		// replace the connection information in the sparqlGraphJson
		if (this.isUseNodegroupConn(conn)) {
			sparqlGraphJson.setSparqlConn(conn);   
		}
		
		// check to make sure there is an importspec attached. how to do this?
		if(sparqlGraphJson.getJson().get("importSpec") == null){
			// there was no importspec. this is not valid for the requested operation.
			throw new Exception("ingestFromTemplateIdAndCsvString -- the stored nodeGroup did not contain an import spec and is not elligible to use to ingest data.");
		}
		
		this.irc.execIngestionFromCsv(sparqlGraphJson.getJson().toJSONString(), csvContents, sparqlGraphJson.getSparqlConnJson().toJSONString());
		retval = this.irc.getLastResult();

		LocalLogger.logToStdOut("Ingestion results: " + retval.toJson().toJSONString());
		
		return retval;
	}
	
	public static boolean isUseNodegroupConn(SparqlConnection conn) throws Exception {
		return conn.equals(NodeGroupExecutor.get_USE_NODEGROUP_CONN(), false);
	}
	
}






































