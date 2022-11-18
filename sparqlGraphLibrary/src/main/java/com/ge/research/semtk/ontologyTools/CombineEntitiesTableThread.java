package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
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
	private ArrayList<String> deletePredicatesFromTarget;
	private ArrayList<String> deletePredicatesFromDuplicate;
	private Table errorTab;
	private HeaderTable headerTable = null;
	private RestrictionChecker checker = null;


	/**
	 * Create a thread for combining entities based on lookup properties
	 * @param tracker
	 * @param resultsClient
	 * @param jobId
	 * @param oInfo
	 * @param conn
	 * @param deletePredicatesFromTarget - list of predicates to delete from target before merging
	 * @param deletePredicatesFromDuplicate - list of predicates to delete from duplicate 
	 * @param table - CombineEntitiesInputTable - explains what work is to be done 
	 * @throws SemtkUserException - data problem for user
	 * @throws Exception - catch all
	 */
	public CombineEntitiesTableThread(JobTracker tracker, ResultsClient resultsClient, String jobId, 
			OntologyInfo oInfo, SparqlConnection conn,
			ArrayList<String> deletePredicatesFromTarget,
			ArrayList<String> deletePredicatesFromDuplicate,
			CombineEntitiesInputTable table) throws SemtkUserException, Exception {
		this.tracker = tracker;
		this.resultsClient = resultsClient;
		this.jobId = jobId;
		this.tracker.createJob(this.jobId);
		this.oInfo = oInfo;
		this.conn = conn;
		this.deletePredicatesFromTarget = deletePredicatesFromTarget;
		this.deletePredicatesFromDuplicate = deletePredicatesFromDuplicate;
		this.table = table;
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();

		this.errorTab = new Table(new String [] { "row", "error" });
		this.checker = new RestrictionChecker(conn, oInfo);
		CombineEntitiesWorker.replacePropertyAbbrev(this.deletePredicatesFromTarget);
		CombineEntitiesWorker.replacePropertyAbbrev(this.deletePredicatesFromDuplicate);
		
		// Check the properties in the table to make sure they're legal
		for (String p : this.table.getTargetPropNames()) {
			if (this.oInfo.getProperty(p) == null && !p.equals(SparqlToXLibUtil.TYPE_PROP)) {
				throw new SemtkUserException("Unknown target property: " + p);
			}
		}
		
		for (String p : this.table.getDuplicatePropNames()) {
			if (this.oInfo.getProperty(p) == null && !p.equals(SparqlToXLibUtil.TYPE_PROP)) {
				throw new SemtkUserException("Unknown duplicate property: " + p);
			}
		}

	}
	
	
	public void run() {
		ThreadAuthenticator.authenticateThisThread(this.headerTable);

		ArrayList<CombineEntitiesWorker> workerList = new ArrayList<CombineEntitiesWorker>();
		
		try {
			
			this.tracker.createJob(this.jobId);
			
			// create all the workers and precheck in the process
			for (int i=0; i < this.table.getNumRows(); i++) {
				Hashtable<String, String> targetHash = this.table.getTargetPropValHash(i);
				Hashtable<String, String> duplicateHash = this.table.getDuplicatePropValHash(i);
				
				// do the work
				try {
					CombineEntitiesWorker w =  new CombineEntitiesWorker(
							this.oInfo, this.conn, 
							targetHash, duplicateHash, 
							this.deletePredicatesFromTarget, this.deletePredicatesFromDuplicate,
							this.checker);
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
		
		this.setJobPercentComplete(50);
		
		try {
			
			// do the actual combining
			for (CombineEntitiesWorker w : workerList) {
				w.combine();
			}
			
			this.tracker.setJobSuccess(this.jobId, String.format("Combined %d rows of entities.", this.table.getNumRows()));
			
		} catch (Exception e) {
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
	
	/**
	 * setJobPercentComplete and eat errors
	 * @param percent
	 */
	private void setJobPercentComplete(int percent) {
		try {
			this.tracker.setJobPercentComplete(this.jobId, percent);
		} catch (Exception e) {}
	}

}
