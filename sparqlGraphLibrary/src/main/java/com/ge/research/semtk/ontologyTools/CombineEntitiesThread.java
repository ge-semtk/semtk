package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeDeletionTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Thread for combining two entities, a target and a duplicate.
 * All properties are combined unless specified.
 * @author 200001934
 *
 */
public class CombineEntitiesThread extends Thread {
	private JobTracker tracker;
	private String jobId;
	private CombineEntitiesWorker worker;
	
	/**
	 * 
	 * @param tracker
	 * @param jobId
	 * @param oInfo
	 * @param conn
	 * @param classUri
	 * @param primaryUri
	 * @param secondaryUri
	 * @param deletePredicatesFromPrimary
	 * @param deletePredicatesFromSecondary
	 * @throws Exception
	 */
	public CombineEntitiesThread(JobTracker tracker, String jobId, OntologyInfo oInfo, SparqlConnection conn,
			String primaryUri, String secondaryUri, 
			ArrayList<String> deletePredicatesFromPrimary, ArrayList<String> deletePredicatesFromSecondary) throws Exception {
		this.tracker = tracker;
		this.jobId = jobId;
		this.worker = new CombineEntitiesWorker(oInfo, conn, primaryUri, secondaryUri, deletePredicatesFromPrimary, deletePredicatesFromSecondary);
		
		// get the job started
		this.tracker.createJob(this.jobId);
	}
	
	public void run() {
		try {
			this.tracker.setJobPercentComplete(this.jobId, 5, "Confirming instances and their classes");
			this.worker.preCheck();
			
			this.tracker.setJobPercentComplete(this.jobId, 25, "Deleting skipped properties");
			this.worker.deleteSkippedProperties();
			
			this.tracker.setJobPercentComplete(this.jobId, 50, "Combining entities");
			this.worker.combineEntities();
			
			this.tracker.setJobPercentComplete(this.jobId, 75, "Deleting duplicate");
			this.worker.deleteSecondary();
			
			this.tracker.setJobSuccess(this.jobId);
			
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			try {
				// Exceptions are reported via the async job status mechanism
				this.tracker.setJobFailure(this.jobId, e.getMessage());
			} catch (Exception ee) {
				// Bail from total mess with log messages and a stranded job
				LocalLogger.logToStdErr("CombineEntitiesThread stranded a job due to another exception during setJobFailure()");
				LocalLogger.printStackTrace(ee);
			}
		}
	}
}
