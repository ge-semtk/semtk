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

package com.ge.research.semtk.sparqlX.asynchronousQuery;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.connutil.EndpointNotFoundException;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Base class for dispatchers.
 */
public abstract class AsynchronousNodeGroupBasedQueryDispatcher {

	private String pruneToColumn = null;
	
	protected NodeGroup queryNodeGroup;
	protected OntologyInfoClient oInfoClient;
	protected NodeGroupStoreRestClient ngStoreClient;
	protected SparqlEndpointInterface jobTrackerSei;
	protected JobTracker jobTracker;
	protected ResultsClientConfig resConfig;
	protected SparqlEndpointInterface querySei;
	protected String jobID;
	protected OntologyInfo oInfo;
	protected String domain;
	
	public final static String FLAG_DISPATCH_RETURN_QUERIES = "DISPATCH_RETURN_QUERIES";
	
	public AsynchronousNodeGroupBasedQueryDispatcher(String jobId, SparqlGraphJson sgJson, SparqlEndpointInterface jobTrackerSei, ResultsClientConfig resConfig, SparqlEndpointInterface extConfigSei, boolean unusedFlag, OntologyInfoClient oInfoClient, NodeGroupStoreRestClient ngStoreClient) throws Exception{
		
		this.jobID = jobId;
		this.oInfoClient = oInfoClient;
		this.ngStoreClient = ngStoreClient;
		this.jobTrackerSei = jobTrackerSei;
		this.jobTracker = new JobTracker(jobTrackerSei);
		this.resConfig = resConfig;
		
		// get nodegroup and sei from json
		this.queryNodeGroup = sgJson.getNodeGroup();
		this.querySei = sgJson.getSparqlConn().getDefaultQueryInterface();
		
		// This old hack can't be correct. 
		// Wrapped it with isEmpty() at least...
		if (querySei.getUserName().isEmpty()) {
			this.querySei.setUserAndPassword(extConfigSei.getUserName(), extConfigSei.getPassword());
		}
		
		SparqlConnection nodegroupConn = sgJson.getSparqlConn();
		this.domain = nodegroupConn.getDomain();
		this.oInfo = oInfoClient.getOntologyInfo(nodegroupConn);
		this.queryNodeGroup.validateAgainstModel(oInfo);
		
		this.jobTracker.setJobPercentComplete(this.jobID, 1);
	}
	
	/**
	 * Get the job id
	 */
	public String getJobId(){
		return this.jobID;
	}
	
	public abstract void execute(Object executionSpecificObject1, Object executionSpecificObject2, AutoGeneratedQueryTypes qt, SparqlResultTypes rt, String targetSparqlID);
	
	/**
	 * Send the results to the results service
	 */
	protected void sendResultsToService(Table resTable) throws ConnectException, EndpointNotFoundException, Exception{
				
		HashMap<String, Integer> colInstCounter = new HashMap<String, Integer>();
			
		try{
			
			// repair column headers in the event that a duplicate header is encountered. by convention (established and existing only here), the first instance of a column name 
			// will remain unchanged, all future instances will be postfixed with "[X]" where X is the count encountered so far. this count will start at 1. 
			
			String[] unModColnames = resTable.getColumnNames(); 	// pre-modification column names
			String[] modColnames = new String[unModColnames.length];
			
			int posCount = 0;
			for(String uCol : unModColnames){
				if(colInstCounter.containsKey( uCol.toLowerCase() )){
					// seen this one already. update the counter and add it to the new header list.
					int update = colInstCounter.get( uCol.toLowerCase() ) + 1;
					colInstCounter.put( uCol.toLowerCase() , update);
					
					modColnames[posCount] = uCol + "[" + update + "]";
				}
				else{
					// never seen this column.
					modColnames[posCount] = uCol;
					// add to the hash
					colInstCounter.put( uCol.toLowerCase(), 0 );
				}
				
				posCount+=1;
			}
			resTable.replaceColumnNames(modColnames);
			
			// special rare feature prune To uniquified column
			if (this.pruneToColumn != null) {
				if (resTable.getColumnIndex(this.pruneToColumn) == -1) {
					throw new Exception ("PruneToColumn can not find column named: " + this.pruneToColumn);
				}
				for (String c : resTable.getColumnNames()) {
					if (!c.equals(this.pruneToColumn)) {
						resTable.removeColumn(c);
					}
				}
				resTable.uniquify(new String [] {this.pruneToColumn});
			} 
		} catch(Exception e){
			this.jobTracker.setJobFailure(this.jobID, "Failure preparing results table: " + e.getMessage());
			LocalLogger.printStackTrace(e);
			throw new Exception(e);
		}
			
		try {	
			(new ResultsClient(this.resConfig)).execStoreTableResults(this.jobID, resTable);
		} catch (Exception e) {
			this.jobTracker.setJobFailure(this.jobID, "Failed to write results: " + e.getMessage());
			LocalLogger.printStackTrace(e);
			throw new Exception("Unable to write results", e);
		}
	}

	private void sendResultsToService(JSONObject resJSON)  throws ConnectException, EndpointNotFoundException, Exception{
		try{
			(new ResultsClient(this.resConfig)).execStoreGraphResults(this.jobID, resJSON);
		} catch (Exception e) {
			this.jobTracker.setJobFailure(this.jobID, "Failed to write results: " + e.getMessage());
			LocalLogger.printStackTrace(e);
			throw new Exception("Unable to write results", e);
		}
	}

	/**
	 * Update status
	 */
	protected void updateStatus(int statusPercentNumber) throws UnableToSetStatusException {
		try {
			if (statusPercentNumber >= 100) {
				this.jobTracker.setJobSuccess(this.jobID);
			} else {
				this.jobTracker.setJobPercentComplete(this.jobID, statusPercentNumber);
			}
		} catch (Exception e) {
			throw new UnableToSetStatusException(e.getMessage());
		}
	}
	
	/**
	 * Increment status 
	 */
	protected void incrementStatus(int increment, int max) throws UnableToSetStatusException{
		try {
			this.jobTracker.incrementPercentComplete(this.jobID, increment, max);
		} catch (Exception e) {
			throw new UnableToSetStatusException(e.getMessage());
		}
	}
	
	
	/**
	 * Update job tracker for failure
	 * @param rationale
	 */
	protected void updateStatusToFailed(String rationale) {
		try {
			this.jobTracker.setJobFailure(this.jobID, rationale != null ? rationale : "Exception with e.getMessage()==null");
		} catch (Exception eee) {
			LocalLogger.logToStdErr("Error updating job tracker for failure");
		}
	}
	
	public abstract String getConstraintType() throws Exception;
	
	public abstract String[] getConstraintVariableNames() throws Exception;
	
	/**
	 * Execute a SPARQL query
	 */
	public void executePlainSparqlQuery(String sparqlQuery, SparqlResultTypes resType) throws Exception{
		this.jobTracker.incrementPercentComplete(this.jobID, 1, 10);
		try{
			LocalLogger.logToStdErr("Job " + this.jobID + ": AsynchronousNodeGroupExecutor.executePlainSparqlQuery() start");
			LocalLogger.logToStdErr("Execute SPARQL on " + this.querySei.getServerAndPort() + "\n" + sparqlQuery);
			
			GeneralResultSet genResult = null;

			// execute the query
			if(	resType == SparqlResultTypes.GRAPH_JSONLD ||
					resType == SparqlResultTypes.N_TRIPLES ||
					resType == SparqlResultTypes.CONFIRM ||
					resType == SparqlResultTypes.TABLE ||
					resType == SparqlResultTypes.RDF) {
				genResult = this.querySei.executeQueryAndBuildResultSet(sparqlQuery, resType);
			} else {
				throw new Exception("Unsupported result type: " + resType.toString());
			}
			
			// process the results
			if (genResult.getSuccess()) {
				
				// uncache SEI if delete query
				if (resType == SparqlResultTypes.CONFIRM) {
					SparqlConnection conn = new SparqlConnection();
					conn.addDataInterface(this.querySei);
					this.oInfoClient.uncacheChangedConn(conn);
				}
				
				LocalLogger.logToStdErr("Write results for " + this.jobID);
				if (resType == SparqlResultTypes.GRAPH_JSONLD) {
					LocalLogger.logToStdErr("Query returned JSON-LD");
					this.sendResultsToService(genResult.getResultsJSON());

				} else if(resType == SparqlResultTypes.RDF || resType == SparqlResultTypes.N_TRIPLES) {
					LocalLogger.logToStdErr("Query returned RDF or N_TRIPLES " );
					// RDF results store as json blobs with { "RDF": "<OWL>...</OWL>" }
					this.sendResultsToService(genResult.getResultsJSON());
					
				} else if(resType == SparqlResultTypes.TABLE) {
					Table tab = ((TableResultSet)genResult).getTable();
					LocalLogger.logToStdErr("Query returned table with " + tab.getNumRows() + " results");
					this.sendResultsToService(tab);

				} else if(resType == SparqlResultTypes.CONFIRM ){
					// Confirm:  change results into a table with a column for each field
					ArrayList<String> colnames = ((SimpleResultSet)genResult).getResultsKeys();
					ArrayList<String> coltypes = new ArrayList<String>();
					ArrayList<String> row0 = new ArrayList<String>();
					for (String c : colnames) {
						coltypes.add("string");
						row0.add(((SimpleResultSet)genResult).getResult(c));
					}
					ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
					rows.add(row0);
					Table tab = new Table(colnames, coltypes, rows);
					LocalLogger.logToStdErr("Query returned " + tab.getNumRows() + " results.");
					this.sendResultsToService(tab);
				} else {
					throw new Exception("Unknown query type: no results written: " + resType);
				}
				this.updateStatus(100);		// work's done

			} else {
				this.updateStatusToFailed("Error executing SPARQL query:\n" + genResult.getRationaleAsString("\n"));
			}
			
			LocalLogger.logToStdErr("Job " + this.jobID + ": AsynchronousNodeGroupExecutor.executePlainSparqlQuery() end");

		} catch (Exception e) {
			this.updateStatusToFailed(e.getMessage());
			LocalLogger.printStackTrace(e);
			throw new Exception("Query failed: " + e.getMessage());
		}
	}

	
	protected String getSparqlQuery(AutoGeneratedQueryTypes qt, String targetSparqlID) throws Exception{
		String retval = null;
		if(qt.equals(AutoGeneratedQueryTypes.SELECT_DISTINCT)){
			retval = this.queryNodeGroup.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, false, null, null);
		}
		else if(qt.equals(AutoGeneratedQueryTypes.COUNT)){
			retval = this.queryNodeGroup.generateSparql(AutoGeneratedQueryTypes.COUNT, false, null, null);
		}
		else if(qt.equals(AutoGeneratedQueryTypes.FILTER_CONSTRAINT)){
			
			// find our returnable and pass it on.
			Returnable rt = null;
			rt = this.queryNodeGroup.getNodeBySparqlID(targetSparqlID);
			if(rt == null){
				rt = this.queryNodeGroup.getPropertyItemBySparqlID(targetSparqlID);
			}
			if (rt == null) {
				throw new Exception("Can't find target of filter constraint query in the nodegroup: " + targetSparqlID);
			}
			retval = this.queryNodeGroup.generateSparql(AutoGeneratedQueryTypes.FILTER_CONSTRAINT, false, null, rt);
		}
		else if(qt.equals(AutoGeneratedQueryTypes.CONSTRUCT)){
			retval = this.queryNodeGroup.generateSparqlConstruct();
		}
		else if(qt.equals(AutoGeneratedQueryTypes.DELETE)) {
			retval = this.queryNodeGroup.generateSparqlDelete();
		}
		else if(qt.equals(AutoGeneratedQueryTypes.ASK)){
			retval = this.queryNodeGroup.generateSparql(AutoGeneratedQueryTypes.ASK, false, null, null);
		}
		else{
			throw new Exception("Dispatcher passed and unrecognized query type. it does not know how to build a " + qt.name() +  " query");
		}
		return retval;
	}
	
	public JobTracker getJobTracker() {
		return this.jobTracker;
	}

	/**
	 * Add column name for prune feature (prune table to single unique column)
	 * @param pruneToColumn - column name or null
	 */
	public void addPruneToColumn(String pruneToColumn) {
		this.pruneToColumn = pruneToColumn;
		
	}

}
