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

public class CombineEntitiesThread extends Thread {
	private JobTracker tracker;
	private String jobId;
	private OntologyInfo oInfo;
	private SparqlConnection conn;
	private String classUri;
	private String targetUri;
	private String duplicateUri;
	private ArrayList<String> deletePredicatesFromTarget;
	private ArrayList<String> deletePredicatesFromDuplicate;
	
	/**
	 * 
	 * @param tracker
	 * @param jobId
	 * @param oInfo
	 * @param conn
	 * @param classUri
	 * @param targetUri
	 * @param duplicateUri
	 * @param deletePredicatesFromTarget
	 * @param deletePredicatesFromDuplicate
	 * @throws Exception
	 */
	public CombineEntitiesThread(JobTracker tracker, String jobId, OntologyInfo oInfo, SparqlConnection conn,
			String classUri, String targetUri, String duplicateUri, 
			ArrayList<String> deletePredicatesFromTarget, ArrayList<String> deletePredicatesFromDuplicate) throws Exception {
		this.tracker = tracker;
		this.jobId = jobId;
		this.oInfo = oInfo;
		this.conn = conn;
		this.classUri = classUri;
		this.targetUri = targetUri;
		this.duplicateUri = duplicateUri;
		this.deletePredicatesFromTarget = deletePredicatesFromTarget != null ? deletePredicatesFromTarget : new ArrayList<String>();
		this.deletePredicatesFromDuplicate = deletePredicatesFromDuplicate != null ? deletePredicatesFromDuplicate : new ArrayList<String>();
		
		// check ontology for class
		OntologyClass oClass = oInfo.getClass(classUri);
		if (oClass == null) throw new Exception("Class was not found in ontology: " + classUri);
		
		// check ontology for properties
		for (String propUri : this.deletePredicatesFromTarget) {
			OntologyProperty p = oInfo.getInheritedPropertyByUri(oClass, propUri);
			if (p == null) throw new Exception(String.format("Class %s does not have property %s", classUri, propUri));
		}
		
		for (String propUri : this.deletePredicatesFromDuplicate) {
			OntologyProperty p = oInfo.getInheritedPropertyByUri(oClass, propUri);
			if (p == null) throw new Exception(String.format("Class %s does not have property %s", classUri, propUri));
		}
		
		// get the job started
		this.tracker.createJob(this.jobId);
	}
	
	public void run() {
		try {
			this.tracker.setJobPercentComplete(this.jobId, 5, "Confirming instances exist");
			this.confirmExists(this.targetUri, this.classUri);
			this.confirmExists(this.duplicateUri, this.classUri);
			
			this.tracker.setJobPercentComplete(this.jobId, 25, "Deleting skipped properties");
			this.deleteSkippedProperties();
			
			this.tracker.setJobPercentComplete(this.jobId, 50, "Combining entities");
			this.combineEntities();
			
			this.tracker.setJobPercentComplete(this.jobId, 75, "Deleting duplicate");
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
	 * Check existance of and instance of a class via exception
	 * @param instanceUri
	 * @param classUri
	 * @throws Exception - not found
	 */
	private void confirmExists(String instanceUri, String classUri) throws Exception {
		NodeGroup ng = new NodeGroup();
		ng.noInflateNorValidate(this.oInfo);
		ng.setSparqlConnection(this.conn);
		ng.addNodeInstance(classUri, oInfo, instanceUri);
		Table table = this.conn.getDefaultQueryInterface().executeQueryToTable(ng.generateSparqlAsk());
		if (! table.getCellAsBoolean(0,0) ) 
			throw new Exception(String.format("Could not find instance of %s with URI %s", classUri, instanceUri));
	}
	
	/**
	 * Build nodegroup to delete duplicatePredicatesToSkip
	 * @throws Exception
	 */
	private void deleteSkippedProperties() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri

		if (deletePredicatesFromTarget.size() > 0) {
			String sparql = SparqlToXLibUtil.generateDeleteExactProps(this.conn, this.targetUri, this.deletePredicatesFromTarget);
			this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
		}
		
		if (this.deletePredicatesFromDuplicate.size() > 0) {
			String sparql = SparqlToXLibUtil.generateDeleteExactProps(this.conn, this.duplicateUri, this.deletePredicatesFromDuplicate);
			this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
		}
	}
	
	/**
	 * Build nodegroup to delete the duplicate
	 * @throws Exception
	 */
	private void combineEntities() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri
		String sparql = SparqlToXLibUtil.generateCombineEntitiesInsertOutgoing(this.conn, this.targetUri, this.duplicateUri);
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
		
		sparql = SparqlToXLibUtil.generateCombineEntitiesInsertIncoming(this.conn, this.targetUri, this.duplicateUri);
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
