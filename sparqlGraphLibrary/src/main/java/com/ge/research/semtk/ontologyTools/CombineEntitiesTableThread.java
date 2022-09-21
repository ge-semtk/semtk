package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Process a table full of requests to combine entities by lookup
 * @author 200001934
 *
 */
public class CombineEntitiesTableThread extends Thread {
	private JobTracker tracker;
	private ResultsClient resultsClient;
	private String jobId;
	private CombineEntitiesInputTable table;
	private OntologyInfo oInfo;
	private SparqlConnection conn;
	private ArrayList<String> deletePredicatesFromPrimary;
	private ArrayList<String> deletePredicatesFromSecondary;
	private Table errorTab;
	

	/**
	 * Create a thread for combining entities based on lookup properties
	 * @param tracker
	 * @param resultsClient
	 * @param jobId
	 * @param oInfo
	 * @param conn
	 * @param deletePredicatesFromPrimary - list of predicates to delete from primary before merging
	 * @param deletePredicatesFromSecondary - list of predicates to delete from secondary 
	 * @param table - CombineEntitiesInputTable - explains what work is to be done 
	 * @throws Exception
	 */
	public CombineEntitiesTableThread(JobTracker tracker, ResultsClient resultsClient, String jobId, 
			OntologyInfo oInfo, SparqlConnection conn,
			ArrayList<String> deletePredicatesFromPrimary,
			ArrayList<String> deletePredicatesFromSecondary,
			CombineEntitiesInputTable table) throws Exception {
		this.tracker = tracker;
		this.resultsClient = resultsClient;
		this.jobId = jobId;
		this.tracker.createJob(this.jobId);
		this.oInfo = oInfo;
		this.conn = conn;
		this.deletePredicatesFromPrimary = deletePredicatesFromPrimary;
		this.deletePredicatesFromSecondary = deletePredicatesFromSecondary;
		this.table = table;
		
		this.errorTab = new Table(new String [] { "row", "error" });
		
		// Check the properties in the table to make sure they're legal
		for (String p : this.table.getPrimaryPropNames()) {
			if (this.oInfo.getProperty(p) == null && !p.equals(SparqlToXLibUtil.TYPE_PROP)) {
				throw new Exception("Unknown primary property: " + p);
			}
		}
		
		for (String p : this.table.getSecondaryPropNames()) {
			if (this.oInfo.getProperty(p) == null && !p.equals(SparqlToXLibUtil.TYPE_PROP)) {
				throw new Exception("Unknown secondary property: " + p);
			}
		}

	}
	
	
	public void run() {
		ArrayList<CombineEntitiesWorker> workerList = new ArrayList<CombineEntitiesWorker>();
		
		try {
			
			this.tracker.createJob(this.jobId);
			
			// create all the workers and precheck in the process
			for (int i=0; i < this.table.getNumRows(); i++) {
				Hashtable<String, String> primaryHash = this.table.getPrimaryPropValHash(i);
				Hashtable<String, String> secondaryHash = this.table.getSecondaryPropValHash(i);
				
				// do the work
				try {
					CombineEntitiesWorker w =  new CombineEntitiesWorker(this.oInfo, this.conn, primaryHash, secondaryHash, this.deletePredicatesFromPrimary, this.deletePredicatesFromSecondary);
					w.preCheck();
					workerList.add(w);
					
				} catch (Exception e) {
					// build error table
					LocalLogger.printStackTrace(e);
					this.errorTab.addRow(new String [] { String.valueOf(i), e.getMessage()});
				}
			}
			
			// stop now if any errors have occurred
			if (this.errorTab.getNumRows() > 0) {
				this.resultsClient.execStoreTableResults(this.jobId, this.errorTab);
				this.tracker.setJobFailure(this.jobId, "Entity merge table contains errors.  No merging was attempted.");
				return;
			}

			
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			try {
			
				// store possibly empty error table
				this.resultsClient.execStoreTableResults(this.jobId, this.errorTab);
				this.tracker.setJobFailure(this.jobId, "Error during precheck.  No merging was attempted. \n" + e.getMessage());
			} catch (Exception ee) {
				
				// Bail from total mess with log messages and a stranded job
				LocalLogger.logToStdErr("CombineEntitiesTableThread stranded a job due to another exception during setJobFailure()");
				LocalLogger.printStackTrace(ee);
			}
			return;
		}
		
		
		try {
			
			// do the actual combining
			for (CombineEntitiesWorker w : workerList) {
				w.combine();
			}
			
			this.tracker.setJobSuccess(this.jobId);
			
		} catch (Exception e) {
			// TODO fix this
			LocalLogger.printStackTrace(e);
			try {
				// Exceptions are reported via the async job status mechanism
				this.tracker.setJobFailure(this.jobId, "Error during merge.  Incomplete merge occurred. \n" + e.getMessage());

			} catch (Exception ee) {
				// Bail from total mess with log messages and a stranded job
				LocalLogger.logToStdErr("CombineEntitiesTableThread stranded a job due to another exception during setJobFailure()");
				LocalLogger.printStackTrace(ee);
			}
		}
	}

}
