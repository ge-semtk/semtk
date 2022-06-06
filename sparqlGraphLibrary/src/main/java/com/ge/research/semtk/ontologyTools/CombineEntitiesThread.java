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
	private String targetUri;
	private String duplicateUri;
	private String duplicateClassUri;
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
			String targetUri, String duplicateUri, 
			ArrayList<String> deletePredicatesFromTarget, ArrayList<String> deletePredicatesFromDuplicate) throws Exception {
		this.tracker = tracker;
		this.jobId = jobId;
		this.oInfo = oInfo;
		this.conn = conn;
		this.targetUri = targetUri;
		this.duplicateUri = duplicateUri;
		this.deletePredicatesFromTarget = deletePredicatesFromTarget != null ? deletePredicatesFromTarget : new ArrayList<String>();
		this.deletePredicatesFromDuplicate = deletePredicatesFromDuplicate != null ? deletePredicatesFromDuplicate : new ArrayList<String>();
		
		
		// get the job started
		this.tracker.createJob(this.jobId);
	}
	
	public void run() {
		try {
			this.tracker.setJobPercentComplete(this.jobId, 5, "Confirming instances and their classes");
			this.confirmInstances();
			
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
	 * Check existance of and instances, and that duplicate is a superclass* of target
	 * @param instanceUri
	 * @param classUri
	 * @throws Exception - not found
	 */
	private void confirmInstances() throws Exception {
		NodeGroup ng = new NodeGroup();
		ng.noInflateNorValidate(this.oInfo);
		SparqlConnection connect = SparqlConnection.deepCopy(this.conn);
		
		// check only the data connection
		connect.clearModelInterfaces();
		connect.addModelInterface(connect.getDataInterface(0));
		ng.setSparqlConnection(connect);
		
		String sparql = SparqlToXLibUtil.generateGetInstanceClass(connect, this.oInfo, this.targetUri);
		Table targetTab = connect.getDefaultQueryInterface().executeQueryToTable(sparql);
		
		sparql = SparqlToXLibUtil.generateGetInstanceClass(connect, this.oInfo, this.duplicateUri);
		Table duplicateTab = connect.getDefaultQueryInterface().executeQueryToTable(sparql);
		
		// Make sure target exists with class
		if (targetTab.getNumRows() == 0) {
			sparql = SparqlToXLibUtil.generateAskInstanceExists(connect, this.oInfo, this.targetUri);
			Table tExists = connect.getDefaultQueryInterface().executeQueryToTable(sparql);
			if (tExists.getCellAsBoolean(0, 0)) 
				throw new Exception(String.format("Target uri <%s> has no class in the triplestore.", this.targetUri));
			else
				throw new Exception(String.format("Target uri <%s> does not exist in the triplestore.", this.targetUri));
		}
		
		// Make sure duplicate exists with class
		if (duplicateTab.getNumRows() == 0) {
			sparql = SparqlToXLibUtil.generateAskInstanceExists(connect, this.oInfo, this.duplicateUri);
			Table tExists = connect.getDefaultQueryInterface().executeQueryToTable(sparql);
			if (tExists.getCellAsBoolean(0, 0)) 
				throw new Exception(String.format("Duplicate uri <%s> has no class in the triplestore.", this.duplicateUri));
			else
				throw new Exception(String.format("Duplicate uri <%s> does not exist in the triplestore.", this.duplicateUri));
		}
		
		// Make sure duplicate's classes are each superclass* of one of target's class
		for (String dupClassName : duplicateTab.getRow(0)) {
			this.duplicateClassUri = dupClassName;
			OntologyClass duplicateClass = this.oInfo.getClass(dupClassName);
			if (duplicateClass == null) {
				throw new Exception(String.format("Duplicate uri <%s> is a %s in the triplestore.  Class isn't found in the ontology.", this.duplicateUri, dupClassName));
			}
			boolean okFlag = false;
			for (String targetClassName : targetTab.getRow(0)) {
				OntologyClass targetClass = this.oInfo.getClass(targetClassName);
				if (targetClass == null) {
					throw new Exception(String.format("Target uri <%s> is a %s in the triplestore.  Class isn't found in the ontology.", this.targetUri, targetClassName));
				}
				if (this.oInfo.classIsA(targetClass, duplicateClass)) {
					okFlag = true;
					break;
				}
			}
			if (!okFlag) {
				throw new Exception(String.format("Duplicate Uri's class %s is not a superClass* of target uri's class: %s", dupClassName, String.join(",", targetTab.getRow(0))));
			}
		}
		
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
		Node n = ng.addNode(this.duplicateClassUri, oInfo);
		n.addValueConstraint(ValueConstraint.buildFilterConstraint(n, "=", this.duplicateUri));
		n.setDeletionMode(NodeDeletionTypes.FULL_DELETE);
		
		// run the query synchronously
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(ng.generateSparqlDelete());
	}
}
