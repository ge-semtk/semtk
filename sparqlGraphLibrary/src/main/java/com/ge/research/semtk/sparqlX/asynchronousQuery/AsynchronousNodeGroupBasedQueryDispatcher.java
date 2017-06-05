package com.ge.research.semtk.sparqlX.asynchronousQuery;

import java.net.ConnectException;
import java.util.Calendar;
import java.util.HashMap;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;

public abstract class AsynchronousNodeGroupBasedQueryDispatcher {

	protected NodeGroup queryNodeGroup;
	protected SparqlQueryClient retrievalClient; // used to get info on EDC service end points: the ones used to service EDC calls.
	protected ResultsClient resultsClient;
	protected StatusClient statusClient;
	
	protected SparqlEndpointInterface sei;

	protected String jobID;
	protected OntologyInfo oInfo;
	protected String domain;
	
	public AsynchronousNodeGroupBasedQueryDispatcher(String jobId, JSONObject encodedNodeGroupWithConnection, ResultsClient rClient, StatusClient sClient, SparqlQueryClient edcQueryClient) throws Exception{
		this.jobID = jobId;
		
		this.resultsClient = rClient;
		this.statusClient = sClient;
		
		// get nodegroup and sei from json
		SparqlGraphJson sgJson = new SparqlGraphJson(encodedNodeGroupWithConnection);
		System.err.println("processing incoming nodegroup - in base class");

		System.err.println("about to get the nodegroup");
		this.queryNodeGroup = sgJson.getNodeGroup();

		System.err.println("about to get the default qry interface");
		this.sei = sgJson.getSparqlConn().getDefaultQueryInterface();
		
		SparqlConnection nodegroupConn = sgJson.getSparqlConn();
		this.domain = nodegroupConn.getDomain();

		// load oInfo via the edcQueryClient.  Note the SparqlEndpointInterface is overwritten by the nodegroupConn,
		// so we're just using the SparqlQueryClient, not any of the connections
		this.oInfo = new OntologyInfo(edcQueryClient.getConfig(), nodegroupConn);
		
		SparqlQueryClientConfig config = new SparqlQueryClientConfig(	
				edcQueryClient.getConfig().getServiceProtocol(),
				edcQueryClient.getConfig().getServiceServer(), 
				edcQueryClient.getConfig().getServicePort(), 
				edcQueryClient.getConfig().getServiceEndpoint(),
				sei.getServerAndPort(),
				sei.getServerType(),
				sei.getDataset());
		
		this.retrievalClient = new SparqlQueryClient(config);
		
		this.updateStatus(0);
		
	}
	
	/**
	 * return the JobID. the clients will need this.
	 * @return
	 */
	public String getJobId(){
		return this.jobID;
	}
	
	public abstract TableResultSet execute(Object ExecutionSpecificConfigObject) throws Exception;
	
	protected int updateRunStatus(float increment, int last, int ceiling) throws UnableToSetStatusException{
		int retval = ceiling;
		if(( last + increment) >= ceiling ){ retval =  ceiling;}
		else{
			this.updateStatus((int)(last + increment));
			retval = (int)(last + increment);
		}
		return retval;
	}
	
	/**
	 * send the collected results to the results service. 
	 * this will just return a true/false about whether the results were likely sent.
	 * @throws Exception 
	 * @throws EndpointNotFoundException 
	 * @throws ConnectException 
	 */
	protected void sendResultsToService(TableResultSet currResults) throws ConnectException, EndpointNotFoundException, Exception{
				
		HashMap<String, Integer> colInstCounter = new HashMap<String, Integer>();
			
		try{
			
			// repair column headers in the event that a duplicate header is encountered. by convention (established and existing only here), the first instance of a column name 
			// will remain unchanged, all future instances will be postfixed with "[X]" where X is the count encountered so far. this count will start at 1. 
			Table resTable = currResults.getTable();
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
			
			this.resultsClient.execStoreTableResults(this.jobID, resTable);
		}
		catch(Exception eee){
			this.statusClient.execSetFailure("Failed to write results: " + eee.getMessage());
			eee.printStackTrace();
			throw new Exception("Unable to write results");
		}
	}
	/**
	 * send updates to the status service. 
	 * @param statusPercentNumber
	 * @throws UnableToSetStatusException
	 */
	protected void updateStatus(int statusPercentNumber) throws UnableToSetStatusException{
		
		try {
		// if statusPercentNumber >= 100, instead, set the success or failure.
			if(statusPercentNumber >= 100){
				this.statusClient.execSetSuccess();	
			}
			// else, try to set a value.
			else{
				this.statusClient.execSetPercentComplete(statusPercentNumber);
			}
			
		} catch (Exception e) {
			throw new UnableToSetStatusException(e.getMessage());
		}
	}
	
	
	/**
	 * the work failed. let the callers know via the status service. 
	 * @param rationale
	 * @throws UnableToSetStatusException
	 */
	protected void updateStatusToFailed(String rationale) throws UnableToSetStatusException{
		try{
			this.statusClient.execSetFailure(rationale != null ? rationale : "Exception with e.getMessage()==null");
			System.err.println("attempted to write failure message to status service");
		}
		catch(Exception eee){
			System.err.println("failed to write failure message to status service");
			throw new UnableToSetStatusException(eee.getMessage());
		}
	}
	
	/**
	 * used for testing the service/ probably not useful in practice
	 * @return
	 */
	public StatusClient getStatusClient(){ return this.statusClient;}
	/**
	 * used for testing the service/ probably not useful in practice
	 * @return
	 */
	public ResultsClient getResultsClient(){ return this.resultsClient;}
	
	public abstract String getConstraintType() throws Exception;
	
	public abstract String[] getConstraintVariableNames() throws Exception;
}
