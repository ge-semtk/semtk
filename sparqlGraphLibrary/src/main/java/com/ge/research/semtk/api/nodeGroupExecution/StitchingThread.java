package com.ge.research.semtk.api.nodeGroupExecution;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeDeletionTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Process a table full of requests to combine entities by lookup
 * @author 200001934
 *
 */
public class StitchingThread extends Thread {
	private HeaderTable headerTable = null;
	
	private StitchingStep steps[];
	private SparqlConnection conn = null;
	private NodeGroupStoreRestClient storeClient = null;
	private DispatchRestClient dispatchClient = null;
	private ResultsClient resultsClient = null;
	private SparqlEndpointInterface servicesSei = null;
	
	private JobTracker tracker = null;
	private String jobId = null;
	
	private Table stitched = null;
	/**
	 * @throws Exception 

	 */
	public StitchingThread(
			StitchingStep [] steps, SparqlConnection conn, 
			NodeGroupStoreRestClient storeClient, DispatchRestClient dispatchClient, ResultsClient resultsClient, 
			SparqlEndpointInterface servicesSei, String jobId) throws Exception {
	
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
		this.steps = steps;
		this.conn = conn;
		this.storeClient = storeClient;
		this.dispatchClient = dispatchClient;
		this.resultsClient = resultsClient;
		this.servicesSei = servicesSei;
		this.tracker = new JobTracker(servicesSei);
		
		this.jobId = jobId;
		this.tracker.createJob(this.jobId);
		
	}
	
	
	public void run() {
		ThreadAuthenticator.authenticateThisThread(this.headerTable);
		
		try {
			
			
			for (int i=0; i < this.steps.length; i++) {
				StitchingStep step = this.steps[i];
				int progress = i * 100 / (this.steps.length);
				this.tracker.setJobPercentComplete(this.jobId, progress , "Running nodegroup: " + step.getNodegroupId());

				NodeGroupExecutor executor = new NodeGroupExecutor(this.storeClient, this.dispatchClient, this.resultsClient, this.servicesSei, null);
				Table tab = executor.selectToTableById(conn, step.getNodegroupId());
				this.stitch(tab, step);
			}
			
			this.resultsClient.execStoreTableResults(this.jobId, this.stitched);
			this.tracker.setJobSuccess(jobId);

		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			try {
				// Exceptions are reported via the async job status mechanism
				this.tracker.setJobFailure(this.jobId, "Error during stitching. \n" + e.getMessage());

			} catch (Exception ee) {
				// Bail from total mess with log messages and a stranded job
				LocalLogger.logToStdErr("CombineEntitiesTableThread stranded a job due to another exception during setJobFailure()");
				LocalLogger.printStackTrace(ee);
			}
		}
	}

	private void stitch(Table tab, StitchingStep step) throws Exception {
		
		if (this.stitched == null) {
			this.stitched = tab;
			
		} else {
			String [] keyColumns = step.getKeyColumns();
			
			// error check
			for (String keyCol : keyColumns) {
				if (this.stitched.getColumnIndex(keyCol) < 0) {
					throw new Exception("Stitching " + step.getNodegroupId() + "failed: stitched table is missing key column: " + keyCol);
				}
				if (tab.getColumnIndex(keyCol) < 0) {
					throw new Exception("Stitching " + step.getNodegroupId() + "failed: new table is missing key column: " + keyCol);
				}
			}
			
			// add column names to stitched
			String tabColNames[] = tab.getColumnNames();
			String tabColTypes[] = tab.getColumnTypes();
			for (int i=0; i < tabColNames.length; i++) {
				if (this.stitched.getColumnIndex(tabColNames[i]) < 0) {
					this.stitched.appendColumn(tabColNames[i], tabColTypes[i]);
				}
			}
			
			// build performance-enhancing colMap of tab col's positions in newly expanded this.stitched
			Hashtable<String, Integer> colMap = new Hashtable<String,Integer>();
			for (String col : tab.getColumnNames()) {
				colMap.put(col, this.stitched.getColumnIndex(col));
			}
			
			ArrayList<ArrayList<String>> newRows = new ArrayList<ArrayList<String>>();
			
			// for each stitched row : look for matches in tab
			for (int stitchRow=0; stitchRow < this.stitched.getNumRows(); stitchRow++) {
				// make a mini-table of matching rows from tab
				Table match = tab.getSubsetWhereMatches(keyColumns[0], this.stitched.getCell(stitchRow,  keyColumns[0]));
				for (int i=1; i < keyColumns.length; i++) {
					match = match.getSubsetWhereMatches(keyColumns[i], this.stitched.getCell(stitchRow,  keyColumns[i]));
				}
				
				if (match.getNumRows() > 0) {
					// sub in values from first matching row
					for (int matchCol=0; matchCol < match.getNumColumns(); matchCol++) {
						this.stitched.setCell(stitchRow, colMap.get(match.getColumnNames()[matchCol]), match.getCell(0,matchCol));
					}
					
					// make new rows for rest of matching rows
					for (int matchRow=1; matchRow < match.getNumRows(); matchRow++) {
						ArrayList<String> newRow = new ArrayList<String>();
						// copy target row in stitched
						newRow.addAll(this.stitched.getRow(stitchRow));
						// substitute in all values from matched row
						for (int matchCol=0; matchCol < match.getNumColumns(); matchCol++) {
							newRow.set(colMap.get(match.getColumnNames()[matchCol]), match.getCell(matchRow, matchCol));
						}
						newRows.add(newRow);
					}
				}
			}
			
			// for each  tab row  :  append it to stitched if it hasn't matched anything
			for (int tabRow=0; tabRow < tab.getNumRows(); tabRow++) {
				// make a mini-table of matching rows from stitched
				Table match = this.stitched.getSubsetWhereMatches(keyColumns[0], tab.getCell(tabRow, keyColumns[0]));
				for (int i=1; i < keyColumns.length; i++) {
					match = match.getSubsetWhereMatches(keyColumns[i], tab.getCell(tabRow,  keyColumns[i]));
				}
				// if tab didn't match anything in stitched, copy it over
				if (match.getNumRows() == 0) {
					ArrayList<String> newRow = new ArrayList<String>();
					for (int i=0; i < this.stitched.getNumColumns(); i++) {
						newRow.add("");
					}
					// substitute in all values unmatched row in tab
					for (int tabCol=0; tabCol < tab.getNumColumns(); tabCol++) {
						newRow.set(colMap.get(tab.getColumnNames()[tabCol]), tab.getCell(tabRow, tabCol));
					}
					newRows.add(newRow);
				}
			}
			
			// add all the new rows to stitched
			for (ArrayList<String> newRow : newRows) {
				this.stitched.addRow(newRow);
			}
		}
	}
}