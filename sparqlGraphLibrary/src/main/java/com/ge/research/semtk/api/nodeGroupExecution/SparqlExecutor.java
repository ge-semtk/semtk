package com.ge.research.semtk.api.nodeGroupExecution;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.utility.LocalLogger;

public class SparqlExecutor extends Thread {
	private String sparql = null;
	private SparqlEndpointInterface sei = null;
	private SparqlEndpointInterface servicesSei = null;
	private ResultsClient resClient = null;
	private String jobId = null;
	private HeaderTable headerTable = null;
	private JobTracker tracker = null;
	
	public static String CLEAR_GRAPH = "%CLEAR%";
			
	public SparqlExecutor(String sparql, SparqlEndpointInterface sei, SparqlEndpointInterface servicesSei, ResultsClient resClient) throws Exception {
		this.sparql = sparql;
		this.sei = sei;
		this.servicesSei = servicesSei;
		this.resClient = resClient;
		this.jobId = JobTracker.generateJobId();
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
		
		// We're dead in the water if we can't track a job.  print stacktrace and return
		
		this.tracker = new JobTracker(this.servicesSei);
		this.tracker.createJob(this.jobId);
				
	}
	
	public void run() {
		ThreadAuthenticator.authenticateThisThread(this.headerTable);
		
		try {
			
			this.tracker.setJobPercentComplete(this.jobId, 5);
			SimpleResultSet res;
			
			if (this.sparql.equals(CLEAR_GRAPH)) {
				// clear graph is special because some triplestores fail if graph is missing and some don't.
				// Individual sei's catch and handle the error.
				// semTK consistently returns success.
				sei.clearGraph();
				res = new SimpleResultSet(true);
				res.setMessage("Cleared " + sei.getGraph());
				
			} else {
				
				res = (SimpleResultSet) sei.executeQueryAndBuildResultSet(this.sparql, SparqlResultTypes.CONFIRM);
			}
			
			this.tracker.setJobPercentComplete(this.jobId, 80);
			
			// put a table in the results for legacy SPARQLgraph
			Table t = new Table(new String [] {"message"}, new String [] {"string"});
			t.addRow(new String [] {res.getResult("@message")});
			this.resClient.execStoreTableResults(this.jobId, t);
			
			this.tracker.setJobSuccess(this.jobId);
			
		} catch (Exception e) {
			try {
				this.tracker.setJobFailure(this.jobId, e.toString());
			} catch (Exception ee) {
				LocalLogger.printStackTrace(e);
			}
		}
	}
	
	public String getJobId() {
		return this.jobId;
	}
}
