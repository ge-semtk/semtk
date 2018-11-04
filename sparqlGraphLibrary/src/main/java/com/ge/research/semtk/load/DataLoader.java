/**
 ** Copyright 2018 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */


package com.ge.research.semtk.load;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.DataLoadBatchHandler;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Imports a dataset into a triple store.
 **/
public class DataLoader {
	// actually orchestrates the loading of data from a dataset based on a template.

	NodeGroup master = null;
	ArrayList<NodeGroup> nodeGroupBatch = new ArrayList<NodeGroup>();     // stores the subgraphs to be loaded.  
	SparqlEndpointInterface endpoint = null;
	DataLoadBatchHandler batchHandler = null; 
	int batchSize = 1;	// maximum batch size for insertion to the triple store
	String username = null;
	String password = null;
	OntologyInfo oInfo = null;
	
	int MAX_WORKER_THREADS = 2; // 10;
	
	public final static String FAILURE_CAUSE_COLUMN_NAME = "Failure Cause";
	public final static String FAILURE_RECORD_COLUMN_NAME = "Failure Record Number";

	int totalRecordsProcessed = 0;
	
	public DataLoader(){
		// default and does nothing special 
	}
	
	public DataLoader(SparqlGraphJson sgJson, int bSize) throws Exception {
		// take a json object which encodes the node group, the import spec and the connection in one package. 
		
		this.batchSize = bSize;
		
		this.endpoint = sgJson.getSparqlConn().getInsertInterface();
		
		LocalLogger.logToStdOut("Load to graph " + endpoint.getGraph() + " on " + endpoint.getServerAndPort());
		
		this.oInfo = sgJson.getOntologyInfo();				
		this.master = sgJson.getNodeGroup(this.oInfo);
		this.batchHandler = new DataLoadBatchHandler(sgJson, this.batchSize, endpoint);		
	}
	

	public DataLoader(SparqlGraphJson sgJson, int bSize, Dataset ds, String username, String password) throws Exception{
		this(sgJson, bSize);
		this.setCredentials(username, password);
		this.setDataset(ds);
		this.validateColumns(ds);
		
	}
	
	public DataLoader(JSONObject json, int bSize) throws Exception {
		this(new SparqlGraphJson(json), bSize);
	}
	
	public DataLoader(JSONObject json, int bSize, Dataset ds, String username, String password) throws Exception{
		this(new SparqlGraphJson(json), bSize, ds, username, password);
	}
	
	public String getDatasetGraphName(){
		return this.endpoint.getGraph();
	}
	
	public int getTotalRecordsProcessed(){
		return this.totalRecordsProcessed;
	}
	
	private void validateColumns(Dataset ds) throws Exception {
		// not sure we should care if dataset has some column(s) missing
		// as the actual mapping process will decide if there's enough data
	}
	
	private void validateColumnsOLD(Dataset ds) throws Exception {
		
		// validate that the columns specified in the template are present in the dataset
		String[] colNamesToIngest = batchHandler.getImportColNames();   // col names from JSON		
		ArrayList<String> colNamesInDataset = ds.getColumnNamesinOrder();
		for(String c: colNamesToIngest){
			if(!colNamesInDataset.contains(c)){
				ds.close();  // close the dataset
				
				// log some info on the bad column before throwing the exception.
				LocalLogger.logToStdErr("column " + c + " not found in the data set. the length of the column name was " + c.length() + " . the character codes are as follows:");
				for(int i = 0; i < c.length(); i += 1){
					char curr = c.charAt(i);
					System.err.print((int) curr + " ");
				}
				LocalLogger.logToStdErr("");
				// available columns list 
				LocalLogger.logToStdErr("available columns are: ");
				for(String k : colNamesInDataset ){
					System.err.print(k + " (");
					for(int m = 0; m < k.length(); m += 1){
						char curr = k.charAt(m);
						System.err.print((int) curr + " ");
					}
					LocalLogger.logToStdErr(")" );
				}
				
				throw new Exception("Column '" + c + "' not found in dataset. Available columns are: " + colNamesInDataset.toString());
			}
		}
	}
	public void setCredentials(String user, String pass){
		this.endpoint.setUserAndPassword(user, pass);
	}
	
	public void setDataset(Dataset ds) throws Exception{
		if(this.batchHandler == null){
			throw new Exception("There was no DAta to model TRansform initialized when setting dataset.");
		}
		else{
			this.batchHandler.setDataset(ds);
		}
		
	}
	
	public void setBatchSize(int bSize){
		// set the maximum batch size for insertion to the triple store
		this.batchSize = bSize;
	}	
	
	public void setRetrivalBatchSize(int rBatchSize){
		this.batchHandler.setBatchSize(rBatchSize);
	}
	
	
	/**
	 * Performs one or two pass ingestion.
	 */
	public int importData(Boolean precheck)throws Exception{
		return importData(precheck, false);
	}
	
	/**
	 * Performs one or two pass ingestion.
	 * Note: Check the error report if you don't know the expected number of records ingested
	 *       Or use a flavor of this function that returns the error table.
	 * @param precheck check that the ingest will succeed before starting it
 	 * @param skipIngest skip the actual ingest (e.g. for precheck only)
	 * @return number of records ingested
	 * @throws Exception
	 */
	public int importData(Boolean precheck, Boolean skipIngest) throws Exception{

		// check the nodegroup for consistency before continuing.		
		LocalLogger.logToStdErr("about to validate against model.");		
		this.master.validateAgainstModel(this.oInfo);
		LocalLogger.logToStdErr("validation completed.");
		
		Boolean precheckFailed = false;
		this.totalRecordsProcessed = 0;	// reset the counter.
		this.batchHandler.resetDataSet();
		
		// PASS 1
		if(precheck){
			// perform "pre-check"
			String exceptionHeader = "Error during ingest pre-check.  At least one thread threw exception.  e.g.: ";
			this.totalRecordsProcessed = this.runIngestionThreads(true, false, exceptionHeader);  // skip ingest, don't skip check
			this.batchHandler.generateNotFoundURIs();
			
			// inspect the transformer to determine if the checks succeeded
			if(this.batchHandler.getErrorReport().getRows().size() != 0){
				precheckFailed = true;
			}
			
		} else if (this.batchHandler.containsLookupModeCreate()) {
			
			// perform invisible pass.  Only goal is to identify legally missing URI's and set them to NOT_FOUND.
			String exceptionHeader = "Error during URILookup first pass.  At least one thread threw exception.  e.g.: ";
			this.totalRecordsProcessed = this.runIngestionThreads(true, true, exceptionHeader); // skip ingest, skip check
			this.batchHandler.generateNotFoundURIs();
		}
		
		
		
		// NOTE: when create-URI-if-lookup-fails exists is implemented,
		//       another pass will be needed here.   
		//       Pass 1 would flag URI's that need to be created (possibly duplicated from different threads)
		//       And right here, those flagged URI's would be given UUIDs.
		
		// PASS 2
		if (!skipIngest && !precheckFailed) {
			String exceptionHeader = null;
			if (precheck) 
				exceptionHeader = "Error in ingestion after successful pre-check.\nPartial ingestion may have occurred.  At least one thread threw exception.  e.g.: ";
			else
				exceptionHeader = "Error during one-pass ingestion.\nParial ingestion may have occurred.  At least one thread threw exception.  e.g.:";
			
			this.batchHandler.resetDataSet();
			Boolean skipCheck = precheck;
			this.totalRecordsProcessed = this.runIngestionThreads(false, skipCheck, exceptionHeader); // don't skip ingest
			
		} else {
			this.totalRecordsProcessed = 0;
		}
		
		this.batchHandler.closeDataSet();			// close all connections and clean up
		return this.totalRecordsProcessed;          // report.
	}
	
	/**
	 * Make a pass through all the data with multi-threaded IngestionWorkerThreads
	 * throwing an exception if any happen
	 * and creating the Error Report if any errors occur.
	 * @param skipIngest
	 * @param skipCheck
	 * @param exceptionHeader
	 * @return recordsProcessed
	 * @throws Exception - if any thread throws an exception, one will be thrown as (exceptionHeader + e)
	 */
	private int runIngestionThreads(boolean skipIngest, boolean skipCheck, String exceptionHeader) throws Exception {		
		
		int recordsProcessed = 0;
		int startingRow = 1;
		LocalLogger.logToStdOut("Records processed:" + (skipIngest ? " (no ingest)" : ""));
		long lastMillis = System.currentTimeMillis();  // use this to report # recs loaded every X sec
		
		ArrayList<IngestionWorkerThread> wrkrs = new ArrayList<IngestionWorkerThread>();
		
		ArrayList<ArrayList<String>> nextRecords = null;
		
		int numThreads = 2;    // first pass, run few threads to get recommendBatchSize
		                                             // turns out that this hurt performance, set to MAX_WORKER_THREADS
		while (true) {
			// get the next set of records from the data set.
			
			try{
				nextRecords = this.batchHandler.getNextRecordsFromDataSet();
			}catch(Exception e){ break; } // record set exhausted
			
			if(nextRecords == null || nextRecords.size() == 0 ){ break; }
			
			// spin up a thread to do the work.
			if(wrkrs.size() < numThreads){
				// spin up the thread and do the work. 
				IngestionWorkerThread worker = new IngestionWorkerThread(this.endpoint, this.batchHandler, nextRecords, startingRow, this.oInfo, skipCheck, skipIngest);
				startingRow += nextRecords.size();
				wrkrs.add(worker);
				worker.start();
				recordsProcessed += nextRecords.size();
			}
			
			// thread pool is full.  Wait for all of them to complete.
			// and then we can start over.
			// Over simplistic logic could be improved for performance.
			if(wrkrs.size() == numThreads){
				for(int i = 0; i < wrkrs.size(); i++){
					IngestionWorkerThread thread = wrkrs.get(i);
					this.joinAndThrowIfException(thread, exceptionHeader);
					
					// check recommended batch size
					if (thread.getRecommendedBatchSize() < this.batchHandler.getBatchSize()) {
						LocalLogger.logToStdOut("Changing batch size from " + this.batchHandler.getBatchSize() + " to " + thread.getRecommendedBatchSize()); 
						this.batchHandler.setBatchSize(thread.getRecommendedBatchSize());
					}
				}
				wrkrs.clear();
				numThreads = this.MAX_WORKER_THREADS;   // after first pass, go full speed with MAX_WORKER_THREADS
				
				// log to stdout occasionally
				long nowMillis = System.currentTimeMillis();
				if (nowMillis - lastMillis > 1000) {
					LocalLogger.logToStdOutNoEOL("..." + startingRow);
					lastMillis = nowMillis;
				}
			}
		}
		
		LocalLogger.logToStdOutNoEOL("..." + startingRow + "\n");
		
		// join all remaining threads
		for(int i = 0; i < wrkrs.size(); i++){
			this.joinAndThrowIfException(wrkrs.get(i), exceptionHeader);
		}
		LocalLogger.logToStdOut("(DONE)");
		return recordsProcessed;
	}
	
	/**
	 * Check completed thread for exceptions and failure information
	 * @param worker
	 */
	private void joinAndThrowIfException(IngestionWorkerThread worker, String exceptionHeader) throws Exception {
		worker.join();
		Exception e = worker.getException();
		if (e != null) {
			throw new Exception(exceptionHeader, e);
		}
	}
	
	/**
	 * Returns a table containing the failed data rows, along with failure cause and row number.
	 */
	public Table getLoadingErrorReport(){
		return this.batchHandler.getErrorReport();
	}
	
	/**
	 * Returns an error report giving the row number and failure cause for each failure.
	 */
	public String getLoadingErrorReportBrief(){
		String s = "";
		Table errorReport = this.batchHandler.getErrorReport();
		int failureCauseIndex = errorReport.getColumnIndex(FAILURE_CAUSE_COLUMN_NAME);
		int failureRowIndex = errorReport.getColumnIndex(FAILURE_RECORD_COLUMN_NAME);
		ArrayList<ArrayList<String>> rows = errorReport.getRows();
		for(ArrayList<String> row:rows){
			s += "Error in row " + row.get(failureRowIndex) + ": " + row.get(failureCauseIndex) + "\n";
		}
		return s;
	}
	
	/**
	 * import data and get the error string (or null if successful)
	 * @param precheck
	 * @return error report string or null
	 * @throws Exception
	 */
	public String importDataGetBriefError(boolean precheck) throws Exception {
		this.importData(precheck);
		
		String ret = this.getLoadingErrorReportBrief();
		if (ret.isEmpty()) {
			return null;
		} else {
			return ret;
		}
	}
	
	/**
	 * import data and get the error table (or null if successful)
	 * @param precheck
	 * @return error report string or null
	 * @throws Exception
	 */
	public Table importDataGetErrorTable(boolean precheck) throws Exception {
		this.importData(precheck);
		
		Table ret = this.getLoadingErrorReport();
		if (ret.getNumRows() == 0) {
			return null;
		} else {
			return ret;
		}
	}

}
