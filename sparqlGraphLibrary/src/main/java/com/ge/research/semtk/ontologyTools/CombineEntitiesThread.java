package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeDeletionTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;

public class CombineEntitiesThread extends Thread {
	private JobTracker tracker;
	private String jobId;
	private OntologyInfo oInfo;
	private SparqlConnection conn;
	private String classUri;
	private String targetUri;
	private String duplicateUri;
	private ArrayList<String> duplicatePredicatesToSkip;
	
	public CombineEntitiesThread(JobTracker tracker, String jobId, OntologyInfo oInfo, SparqlConnection conn,
			String classUri, String targetUri, String duplicateUri, ArrayList<String> duplicatePredicatesToSkip) throws Exception {
		this.tracker = tracker;
		this.jobId = jobId;
		this.oInfo = oInfo;
		this.conn = conn;
		this.classUri = classUri;
		this.targetUri = targetUri;
		this.duplicateUri = duplicateUri;
		this.duplicatePredicatesToSkip = duplicatePredicatesToSkip;
		
		OntologyClass oClass = oInfo.getClass(classUri);
		if (oClass == null) throw new Exception("Class was not found in ontology: " + classUri);
		
		for (String propUri : duplicatePredicatesToSkip) {
			OntologyProperty p = oInfo.getInheritedPropertyByUri(oClass, propUri);
			if (p == null) throw new Exception(String.format("Class %s does not have property %s", classUri, propUri));
		}
		this.tracker.createJob(this.jobId);
	}
	
	public void run() {
		try {
			this.tracker.setJobPercentComplete(this.jobId, 5, "Deleting skipped properties");
			this.deleteSkippedProperties();
			
			this.tracker.setJobPercentComplete(this.jobId, 35, "Combining entities");
			this.combineEntities();
			
			this.tracker.setJobPercentComplete(this.jobId, 65, "Deleting duplicate");
			this.deleteDuplicate();
			
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

	/**
	 * Build nodegroup to delete duplicatePredicatesToSkip
	 * @throws Exception
	 */
	private void deleteSkippedProperties() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri
		String sparql = SparqlToXLibUtil.generateDeleteExactProps(this.conn, this.targetUri, this.duplicatePredicatesToSkip);
		
		// run the query synchronously
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
	}
	
	/**
	 * Build nodegroup to delete the duplicate
	 * @throws Exception
	 */
	private void combineEntities() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri
		String sparql = SparqlToXLibUtil.generateCombineEntitiesInsert(this.conn, this.targetUri, this.duplicateUri);
		
		// run the query synchronously
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
	}
	
	/**
	 * Build nodegroup to delete the duplicate
	 * @throws Exception
	 */
	private void deleteDuplicate() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri
		NodeGroup ng = new NodeGroup();
		ng.setSparqlConnection(conn);
		Node n = ng.addNode(this.classUri, oInfo);
		n.addValueConstraint(ValueConstraint.buildFilterConstraint(n, "=", this.duplicateUri));
		n.setDeletionMode(NodeDeletionTypes.FULL_DELETE);
		
		// run the query synchronously
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(ng.generateSparqlDelete());
	}
}
