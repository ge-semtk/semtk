package com.ge.research.semtk.api.nodeGroupExecution;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.resultSet.ResultType;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.utility.LocalLogger;

public class SparqlExecutor extends Thread {
	private String sparql = null;
	private SparqlEndpointInterface sei = null;
	private SparqlEndpointInterface servicesSei = null;
	private ResultsClient resClient = null;
	private String jobId = null;
			
	public SparqlExecutor(String sparql, SparqlEndpointInterface sei, SparqlEndpointInterface servicesSei, ResultsClient resClient) {
		this.sparql = sparql;
		this.sei = sei;
		this.servicesSei = servicesSei;
		this.resClient = resClient;
		this.jobId = JobTracker.generateJobId();
	}
	
	public void run() {
		JobTracker tracker = null;
		
		// We're dead in the water if we can't track a job.  print stacktrace and return
		try {
			tracker = new JobTracker(servicesSei);
			tracker.createJob(jobId);
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			return;
		}
		
		// try the actual job
		try {
			
			tracker.setJobPercentComplete(jobId, 5);
			SimpleResultSet res = (SimpleResultSet) sei.executeQueryAndBuildResultSet(this.sparql, SparqlResultTypes.CONFIRM);
			
			tracker.setJobPercentComplete(jobId, 80);
			
			// put a table in the results for legacy SPARQLgraph
			Table t = new Table(new String [] {"message"}, new String [] {"string"});
			t.addRow(new String [] {res.getResult("@message")});
			resClient.execStoreTableResults(jobId, t);
			
			tracker.setJobSuccess(jobId);
			
		} catch (Exception e) {
			try {
				tracker.setJobFailure(jobId, e.toString());
			} catch (Exception ee) {
				LocalLogger.printStackTrace(e);
			}
		}
	}
	
	public String getJobId() {
		return this.jobId;
	}
}
