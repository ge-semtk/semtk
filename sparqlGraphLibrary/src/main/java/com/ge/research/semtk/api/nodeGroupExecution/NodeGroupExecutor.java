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
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
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
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;
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
	public static final String USE_NODEGROUP_CONN_STR_SHORT = "NODEGROUP_DEFAULT";
		
	// all the internal instances needed to manage external communications
	private NodeGroupStoreRestClient storeClient = null;
	private DispatchRestClient dispatchClient = null;
	private ResultsClient resultsClient = null;
	private StatusClient statusClient = null;
	private IngestorRestClient ingestClient = null;
	// internal data.
	private String currentJobId = null;
	
	// the Stored Query Executor will be the heart of the stored Query Executor Service.
	// all of the most important actions will occur in this class
	
	public NodeGroupExecutor(NodeGroupStoreRestClient nodegroupstoreclient, DispatchRestClient dispatchclient,
			ResultsClient resultsclient, StatusClient statusclient, IngestorRestClient ingestorClient){
		
		this.storeClient = nodegroupstoreclient;
		this.dispatchClient   = dispatchclient;
		this.resultsClient    = resultsclient;
		this.statusClient    = statusclient;
		this.ingestClient   = ingestorClient;
	}
	
	public NodeGroupExecutor(String jobID, NodeGroupStoreRestClient nodegroupstoreclient, DispatchRestClient dispatchclient,
			ResultsClient resultsclient, StatusClient statusclient, IngestorRestClient ingestorClient){

		this(nodegroupstoreclient, dispatchclient, resultsclient, statusclient, ingestorClient); // call the other constructor
		// assume the given Job ID is actually a real one. if it is not, we will find out later... there is no good (read: "cheap") way to validate at this point. 
		// the only truly meaningful way to check the existence of the a JobID is to check against the status service. if this seems error-prone or otherwise problematic,
		// we can put that explicit check in later.

		this.currentJobId = jobID;
		this.statusClient.setJobId(jobID);
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
		this.statusClient.setJobId(jobID);
	}
	
	// Status Information
	public int getJobPercentCompletion() throws Exception{
		int retval = 0;
		
		// get the reported completion percentage of the Job referenced by the currentJobId 
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getJobPercentCompletion -- the current job ID is null. unable to get completion on nonexistent job.");
		}
		else{
			retval = this.statusClient.execGetPercentComplete();
		}
		return retval;
	}
	
	// Status Information
	public void incrementPercentComplete(int increment, int max) throws Exception{
		int retval = 0;
		
		// get the reported completion percentage of the Job referenced by the currentJobId 
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getJobPercentCompletion -- the current job ID is null. unable to get completion on nonexistent job.");
		}
		else{
			this.statusClient.execIncrementPercentComplete(increment, max);
		}
		return;
	}
	
	public String getJobStatus() throws Exception{
		String retval = null;
		
		// get the status of the Job referenced by the currentJobId 
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getJobStatus -- the current job ID is null. unable to get status on nonexistent job.");
		}
		else{
			retval = this.statusClient.execGetStatus();
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
			retval = this.statusClient.execGetStatusMessage();
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
			int percent = this.statusClient.execGetPercentComplete();
			if(percent != 100){ 	// not 100%? incomplete
				retval = false;
			}
			else{					// must be done. 
				retval = true;
			}
		}
		return retval;
	}
	
	/**
	 * WARNING: never returns if job somehow fails and doesn't tell status service
	 * @throws Exception
	 */
	public void waitOnJobCompletion() throws Exception{
		
		this.waitOnJobCompletion(10000, 0);
	}
	
	/**
	 * Wait for job to complete in an efficient manner
	 * @param sleepMsec - how often to query.  Note job will return if it finishes sooner.
	 * @param maxMinutes - ignored if < 1
	 * @throws Exception - failure or job does not complete in maxMinutes
	 */
	public void waitOnJobCompletion(int sleepMsec, int maxMinutes) throws Exception{
		
		if(sleepMsec < 10){ sleepMsec = 10; }
		
		int percentComplete = 0;
		int elapsedMsec = 0;
		int maxMsec = maxMinutes * 60 * 1000;
		
		while(percentComplete < 100) {
			if (maxMsec > 0 && elapsedMsec > maxMsec) {
				throw new Exception("Job did not complete within " + maxMinutes + " minutes");
			}
			percentComplete = this.statusClient.execWaitForPercentOrMsec(100, sleepMsec);
			elapsedMsec += sleepMsec;
		}
		
		return;
	}
	
	// Results Information
	public URL[] getResultsLocation() throws Exception{
		LocalLogger.logToStdErr("Using DEPRECATED getResultsLocation in NodeGroupExcutor.java.  Depending on your setup, security may prevent retrieval of URL's");
		URL[] retval = null;
		
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getResultsLocation -- the current job ID is null. unable to get info on nonexistent job.");
		}
		else{
			retval = this.resultsClient.execGetResults(currentJobId);
		}
		return retval;
	}
	
	public Table getTableResults() throws Exception{
		Table retval = null;
		
		if(this.currentJobId == null){
			throw new Exception("StoredQueryExecutor::getTableResults -- the current job ID is null. unable to get info on nonexistent job.");
		}
		else{
			retval = this.resultsClient.getTableResultsJson(this.currentJobId, null);
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
			retval = this.resultsClient.execGetGraphResult(this.currentJobId);
		}
		return retval;
	}
	
	// Dispatch actions
	public void dispatchRawSparql(SparqlConnection sc, String sparqlQuery) throws Exception {
	
		SimpleResultSet simpleRes = null;
		
		simpleRes = this.dispatchClient.executeRawSparqlQuery(sc, sparqlQuery);
		
		// set up the Job ID
		this.setJobID(simpleRes.getResult("requestID"));
	}
	
	// Dispatch actions
	public void dispatchRawSparqlUpdate(SparqlConnection sc, String sparqlQuery) throws Exception {
	
		SimpleResultSet simpleRes = null;
		
		simpleRes = this.dispatchClient.executeRawSparqlUpdateQuery(sc, sparqlQuery);
		
		// set up the Job ID
		this.setJobID(simpleRes.getResult("requestID"));
	}
	
	public void dispatchJob(DispatcherSupportedQueryTypes qt, SparqlConnection sc, NodeGroup ng, JSONObject externalConstraints, JSONArray runtimeConstraints, String targetObjectSparqlID) throws Exception{
		this.dispatchJob(qt, sc, ng, externalConstraints, null, runtimeConstraints, -1, -1, targetObjectSparqlID);
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
	@SuppressWarnings("unchecked")
	public void dispatchJob(DispatcherSupportedQueryTypes qt, SparqlConnection sc, NodeGroup ng, JSONObject externalConstraints, QueryFlags flags, JSONArray runtimeConstraints, int limitOverride, int offsetOverride, String targetObjectSparqlID) throws Exception{
		// externalConstraints as used by executeQueryFromNodeGroup

		LocalLogger.logToStdOut("Sending a " + qt + " query to the dispatcher...");
		
		if(externalConstraints != null){
			LocalLogger.logToStdOut("Setting external constraints: " + externalConstraints.toJSONString());
		}
		
		// apply the runtimeConstraints
		if(runtimeConstraints != null){
			LocalLogger.logToStdOut("Setting runtime constraints: " + runtimeConstraints.toJSONString());
			RuntimeConstraintManager rtci = new RuntimeConstraintManager(ng);
			rtci.applyConstraintJson(runtimeConstraints);
		}
		
		if (flags != null && flags.isSet(QueryFlags.FLAG_UNOPTIONALIZE_CONSTRAINED)) {
			if (qt.equals(DispatcherSupportedQueryTypes.FILTERCONSTRAINT)){	
				Returnable targetObj = ng.getItemBySparqlID(targetObjectSparqlID);
				ng.unOptionalizeConstrained(targetObj);
			} else {
				ng.unOptionalizeConstrained();
			}
		}
		
		if (limitOverride > 0) {
			LocalLogger.logToStdOut("Setting limitOverride: " + limitOverride);
			ng.setLimit(limitOverride);
		}
		if (offsetOverride > -1) {
			LocalLogger.logToStdOut("Setting offsetOverride: " + offsetOverride);
			ng.setOffset(offsetOverride);
		}		
		
		// assemble the nodeGroup and the connection into a nodegroup as the services want them.
		JSONObject sendable = new JSONObject();		
		sendable.put("sparqlConn", sc.toJson());  	// serialized connection
		sendable.put("sNodeGroup", ng.toJson());	// serialized nodegroup
		
		SimpleResultSet simpleRes = null;
		
		// select the appropriate type and try to dispatch the job.
		if(qt.equals(DispatcherSupportedQueryTypes.SELECT_DISTINCT)){
			simpleRes = this.dispatchClient.executeSelectQueryFromNodeGroup(sendable, externalConstraints, flags);
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.COUNT)){
			simpleRes = this.dispatchClient.executeCountQueryFromNodeGroup(sendable, externalConstraints);			
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.FILTERCONSTRAINT)){			
			LocalLogger.logToStdOut("Setting targetObjectSparqlID: " + targetObjectSparqlID);
			sendable.put("targetObjectSparqlID", targetObjectSparqlID);			
			simpleRes = this.dispatchClient.executeFilterQueryFromNodeGroup(sendable, targetObjectSparqlID, externalConstraints);			
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.DELETE)){
			simpleRes = this.dispatchClient.executeDeleteQueryFromNodeGroup(sendable, externalConstraints);			
		}	
		else if(qt.equals(DispatcherSupportedQueryTypes.CONSTRUCT)){
			simpleRes = this.dispatchClient.executeConstructQueryFromNodeGroup(sendable, externalConstraints);
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION)){
			simpleRes = this.dispatchClient.executeConstructQueryForInstanceManipulationFromNodeGroup(sendable, externalConstraints);
		}
		else{
			throw new Exception("NodeGroupExecutor:dispatchJob :: DispatcherSupportedQueryTypes type " + qt.name() + " is not currently supported.");
		}
		
		// set up the Job ID
		this.setJobID(simpleRes.getResult("requestID"));
	}
	
	public void dispatchJob(DispatcherSupportedQueryTypes qt, SparqlConnection sc, String storedNodeGroupId, JSONObject externalConstraints, JSONArray runtimeConstraints, String targetObjectSparqlID) throws Exception{
		this.dispatchJob(qt, sc, storedNodeGroupId, externalConstraints, null, runtimeConstraints, -1, -1, targetObjectSparqlID);
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
	public void dispatchJob(DispatcherSupportedQueryTypes qt, SparqlConnection sc, String storedNodeGroupId, JSONObject externalConstraints, QueryFlags flags, JSONArray runtimeConstraints, int limitOverride, int offsetOverride, String targetObjectSparqlID) throws Exception{
		
		// get the node group from the remote store
		TableResultSet trs = this.storeClient.executeGetNodeGroupById(storedNodeGroupId);
		Table t = trs.getResults();
		
		if(t.getNumRows() < 1){ 
			throw new Exception("Could not find nodegroup with id: " + storedNodeGroupId);
		}
		if(t.getNumRows() > 1){
			throw new Exception("Nodegroup lookup returned multiple results.  id: " + storedNodeGroupId);
		}
		
		String serializedNodeGroup = t.getRow(0).get( t.getColumnIndex("NodeGroup") );
		
		JSONParser jParse = new JSONParser();
		JSONObject encodedNodeGroup = (JSONObject) jParse.parse(serializedNodeGroup);
		NodeGroup ng = new NodeGroup();	
		SparqlConnection conn = sc;
		
		// what is encodedNodeGroup
		if (SparqlGraphJson.isSparqlGraphJson(encodedNodeGroup)) {
			
			// SparqlGraphJson
			SparqlGraphJson sgJson = new SparqlGraphJson(encodedNodeGroup);			
			ng.addJsonEncodedNodeGroup(sgJson.getSNodeGroupJson());			
			if (NodeGroupExecutor.isUseNodegroupConn(sc)) {
				conn = sgJson.getSparqlConn();
			}		
		
		} else if (NodeGroup.isNodeGroup(encodedNodeGroup)) {
			
			// plain NodeGroup
			ng.addJsonEncodedNodeGroup(encodedNodeGroup);
			
			if (NodeGroupExecutor.isUseNodegroupConn(sc)) {
				throw new Exception("Caller requested use of nodegroup's connection but none exists.");
			}
			
		// Error
		} else{
			throw new Exception("Value given for encoded node group is neither SparqlGraphJson nor NodeGroup");
		}
		
		// dispatch the job itself
		this.dispatchJob(qt, conn, ng, externalConstraints, flags, runtimeConstraints, limitOverride, offsetOverride, targetObjectSparqlID);
	}
	
	public URL[] dispatchJobSynchronous(DispatcherSupportedQueryTypes qt, SparqlConnection sc, String storedNodeGroupId, JSONObject externalConstraints, JSONArray runtimeConstraints, String targetObjectSparqlID) throws Exception {
		return dispatchJobSynchronous(qt, sc, storedNodeGroupId, externalConstraints, null, runtimeConstraints, -1, -1, targetObjectSparqlID);
	}
		
	public URL[] dispatchJobSynchronous(DispatcherSupportedQueryTypes qt, SparqlConnection sc, String storedNodeGroupId, JSONObject externalConstraints, QueryFlags flags, JSONArray runtimeConstraints, int limitOverride, int offsetOverride, String targetObjectSparqlID) throws Exception {
		
		// dispatch the job
		this.dispatchJob(qt, sc, storedNodeGroupId, externalConstraints, flags, runtimeConstraints, limitOverride, offsetOverride, targetObjectSparqlID) ;
		
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
		return this.dispatchJobSynchronous(qt, sc, ng, externalConstraints, null, runtimeConstraints, -1, -1, targetObjectSparqlID);
	}
	
	public URL[] dispatchJobSynchronous(DispatcherSupportedQueryTypes qt, SparqlConnection sc, NodeGroup ng, JSONObject externalConstraints, QueryFlags flags, JSONArray runtimeConstraints, int limitOverride, int offsetOverride, String targetObjectSparqlID) throws Exception {
		
		// dispatch the job
		this.dispatchJob(qt, sc, ng, externalConstraints, flags, runtimeConstraints, limitOverride, offsetOverride, targetObjectSparqlID) ;
		
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
		
		SparqlGraphJson sparqlGraphJson = this.getSparqlGraphJson(storedNodeGroupId);
			
		return ingestFromTemplateIdAndCsvString(conn, sparqlGraphJson, csvContents);
	}
	
	
	/**
	 * Ingest CSV, given a nodegroup ID
	 * @param conn - override, may be null
	 * @param storedNodeGroupId
	 * @param csvContents
	 * @return
	 * @throws Exception
	 */
	public String ingestFromTemplateIdAndCsvStringAsync(SparqlConnection conn, String storedNodeGroupId, String csvContents) throws Exception{
		
		SparqlGraphJson sparqlGraphJson = this.getSparqlGraphJson(storedNodeGroupId);
			
		return ingestFromTemplateAndCsvStringAsync(conn, sparqlGraphJson, csvContents);
	}
	
	private SparqlGraphJson getSparqlGraphJson(String storedNodeGroupId) throws Exception {
		// get the node group from the remote store
		TableResultSet trs = this.storeClient.executeGetNodeGroupById(storedNodeGroupId);
		Table t = trs.getResults();

		if(t.getNumRows() < 1){ 
			throw new Exception("Could not find stored nodegroup: " + storedNodeGroupId);
		}
		if(t.getNumRows() > 1){
			throw new Exception("Multiple results found when retrieving stored nodegroup: " + storedNodeGroupId);
		}

		String serializedNodeGroup = t.getRow(0).get( t.getColumnIndex("NodeGroup") );		
		JSONObject parsedStoredNg = (JSONObject) (new JSONParser()).parse(serializedNodeGroup);
		return new SparqlGraphJson(parsedStoredNg);
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
		
		// check to make sure there is an importspec attached. how to do this?
		if(sparqlGraphJson.getJson().get("importSpec") == null){
			// there was no importspec. this is not valid for the requested operation.
			throw new Exception("ingestFromTemplateIdAndCsvString -- the stored nodeGroup did not contain an import spec and is not elligible to use to ingest data.");
		}
		
		String sgjStr = sparqlGraphJson.getJson().toJSONString();
		String connStr = this.getOverrideConnJson(conn, sparqlGraphJson).toJSONString();
		this.ingestClient.execIngestionFromCsv(sgjStr, csvContents, connStr);
		retval = this.ingestClient.getLastResult();

		LocalLogger.logToStdOut("Ingestion results: " + retval.toJson().toJSONString());
		
		return retval;
	}
	
	/**
	 * Ingest CSV, given a nodegroup object
	 * @param conn - override, may be null
	 * @param sparqlGraphJson
	 * @param csvContents
	 * @return jobId
	 * @throws Exception
	 */
	public String ingestFromTemplateAndCsvStringAsync(SparqlConnection conn, SparqlGraphJson sparqlGraphJson, String csvContents) throws Exception{
				
		// check to make sure there is an importspec attached. how to do this?
		if(sparqlGraphJson.getJson().get("importSpec") == null){
			// there was no importspec. this is not valid for the requested operation.
			throw new Exception("ingestFromTemplateIdAndCsvString -- the stored nodeGroup did not contain an import spec and is not elligible to use to ingest data.");
		}
		
		String sgjStr = sparqlGraphJson.getJson().toJSONString();
		String connStr = this.getOverrideConnJson(conn, sparqlGraphJson).toJSONString();
		
		return this.ingestClient.execIngestionFromCsvAsync(sgjStr, csvContents, connStr);
	}
	
	/**
	 * Look at conn and sgJson and determine proper override connection
	 * @param conn
	 * @param sgJson
	 * @return
	 * @throws Exception
	 */
	private JSONObject getOverrideConnJson(SparqlConnection conn, SparqlGraphJson sgJson) throws Exception {
		if (NodeGroupExecutor.isUseNodegroupConn(conn)) {
			return sgJson.getSparqlConnJson();
		} else {
			return conn.toJson();
		}
	}
	
	public static boolean isUseNodegroupConn(SparqlConnection conn) throws Exception {
		return conn.equals(NodeGroupExecutor.get_USE_NODEGROUP_CONN(), false);
	}
	
}






































