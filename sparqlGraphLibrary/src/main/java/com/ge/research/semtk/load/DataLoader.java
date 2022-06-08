/**
 ** Copyright 2019 General Electric Company
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.DataLoadBatchHandler;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.InMemoryInterface;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Imports a dataset into a triple store.
 **/
public class DataLoader implements Runnable {
	// actually orchestrates the loading of data from a dataset based on a template.

	public final static String FAILURE_CAUSE_COLUMN_NAME = "Failure Cause";
	public final static String FAILURE_RECORD_COLUMN_NAME = "Failure Record Number";
	public final static int DEFAULT_BATCH_SIZE_INGEST = 8;
	public final static int DEFAULT_BATCH_SIZE_PRECHECK = 64;
	

	NodeGroup master = null;
	ArrayList<NodeGroup> nodeGroupBatch = new ArrayList<NodeGroup>();     // stores the subgraphs to be loaded.  
	SparqlEndpointInterface endpoint = null;
	DataLoadBatchHandler batchHandler = null; 
	int batchSize = DEFAULT_BATCH_SIZE_INGEST;
	String username = null;
	String password = null;
	OntologyInfo oInfo = null;
	
	int maxThreads = 3; 
	int insertQueryIdealSizeOverride = 0;
	
	int totalRecordsProcessed = 0;
	
	private int maxCacheLength = (int) (20000000);  // 20 meg-ish
	
	// async information
	Boolean asyncSkipIngest = null;
	Boolean asyncPrecheck = null;
	ResultsClient rClient = null;
	StatusClient sClient = null;
	HeaderTable headerTable = null;
	int datasetNumRows = 0;
	int percentStart = 0;
	int percentEnd = 0;

	InMemoryInterface cacheSei = null;
	boolean doNotCache = false;
	InMemoryInterfaceUploadThread uploadThread = null;
	AtomicInteger lastUploadSize = new AtomicInteger(0);
	
	public DataLoader(){
		// default and does nothing special 
	}
	
	/* 
	 * BATCH_SIZE is now automatic.  Suggesting one has minimal impact (and how would you know what to suggest?)
	 * Constructors including batch size are now deprecated.
	 */
	public DataLoader(SparqlGraphJson sgJson) throws Exception {
		// take a json object which encodes the node group, the import spec and the connection in one package. 
		
		this.batchSize = DEFAULT_BATCH_SIZE_INGEST;
		
		this.endpoint = sgJson.getSparqlConn().getInsertInterface();
		
		LocalLogger.logToStdOut("Load to graph " + endpoint.getGraph() + " on " + endpoint.getServerAndPort());
		
		this.oInfo = sgJson.getOntologyInfo();				
		this.master = sgJson.getNodeGroup(this.oInfo);
		this.batchHandler = new DataLoadBatchHandler(sgJson, this.batchSize, endpoint);		
	}
	
	@Deprecated
	public DataLoader(SparqlGraphJson sgJson, int bSize) throws Exception {
		this(sgJson);
		this.batchSize = Math.min(100, bSize);
		this.batchHandler.setBatchSize(this.batchSize);
	}
	
	public DataLoader(SparqlGraphJson sgJson, Dataset ds, String username, String password) throws Exception{
		this(sgJson);
		this.setCredentials(username, password);
		this.setDataset(ds);
		this.validateColumns(ds);
	}

	@Deprecated
	// use vesrion without bSize
	public DataLoader(SparqlGraphJson sgJson, int bSize, Dataset ds, String username, String password) throws Exception{
		this(sgJson, ds, username, password);
		this.batchSize = Math.min(100, bSize);
		this.batchHandler.setBatchSize(this.batchSize);
		
	}
	
	public DataLoader(JSONObject json) throws Exception {
		this(new SparqlGraphJson(json));
	}
	
	public DataLoader(JSONObject json, Dataset ds, String username, String password) throws Exception{
		this(new SparqlGraphJson(json), ds, username, password);
	}
	
	public ArrayList<String> getWarnings() {
		return this.batchHandler.getWarnings();
	}
	
	/**
	 * Get a JSONArray of warnings
	 * @return
	 */
	public JSONArray getWarningsJson() {
		JSONArray ret = new JSONArray();
		ArrayList<String> warnings = this.batchHandler.getWarnings();
		if (warnings != null && warnings.size() > 0) {
			ret.addAll(warnings);
		} 
		return ret;
	}
	
	public void setLogPerformance(boolean logFlag) {
		this.endpoint.setLogPerformance(logFlag);
	}
	
	// for testing
	public void doNotCache() {
		this.doNotCache = true;
	}
	/**
	 * Override the ideal insert query size provided by the SparqlEndpointInterface
	 * @param override
	 */
	public void overrideInsertQueryIdealSize(int override) {
		this.insertQueryIdealSizeOverride = override;
	}
	
	public String getDatasetGraphName(){
		return this.endpoint.getGraph();
	}
	
	public int getTotalRecordsProcessed(){
		return this.totalRecordsProcessed;
	}
	
	private void validateColumns(Dataset ds) throws Exception {
		this.batchHandler.validateData();
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
		
		// set up information for percent complete
		this.datasetNumRows = batchHandler.getDsRows();
		long cellCount = this.datasetNumRows * batchHandler.getImportColNames().length;

		// default: no cache
		this.cacheSei = null;

		if (skipIngest || this.doNotCache) {
			;
		// virtuoso 
		} else if (this.endpoint.getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER) ) {
			if (this.datasetNumRows > 50) {
				this.cacheSei = new InMemoryInterface("http://cache");
				this.maxCacheLength = 5000000;   // 1/100 of MAX_INT
			}
			
		// neptune never
		} else if (this.endpoint.getServerType().equals(SparqlEndpointInterface.NEPTUNE_SERVER)) { 
			;
			
		// fuseki and blazegraph and ???   Just repeat > 50 logic.
		} else if (this.datasetNumRows > 50){
			this.cacheSei = new InMemoryInterface("http://cache");
		}
		
		// check the nodegroup for consistency before continuing.			
		this.master.validateAgainstModel(this.oInfo);
		
		Boolean precheckFailed = false;
		this.totalRecordsProcessed = 0;	// reset the counter.
		this.batchHandler.resetDataSet();
		
		// for percent complete, what portion of the total is this pass
		if (skipIngest) {
			this.percentStart = 0;
			this.percentEnd = 100;
		} else {
			this.percentStart = 0;
			this.percentEnd = 20;
		}
		
		// PASS 1
		if(precheck){
			int validationErrorCount = this.batchHandler.validateData();
			
			if (validationErrorCount == 0) {
				// perform "pre-check"
				String exceptionHeader = "Error during ingest pre-check.  At least one thread threw exception.  e.g.: ";
				this.totalRecordsProcessed = this.runIngestionThreads(true, false, exceptionHeader);  // skip ingest, don't skip check
				this.batchHandler.generateNotFoundURIs();
			}
			
			// inspect the transformer to determine if the checks succeeded
			if(this.batchHandler.getErrorReport().getRows().size() != 0){
				precheckFailed = true;
			}
			
		} else if (this.batchHandler.containsLookupWithCreate()) {
			
			// perform invisible pass.  Only goal is to identify legally missing URI's and set them to NOT_FOUND.
			String exceptionHeader = "Error during URILookup first pass.  At least one thread threw exception.  e.g.: ";
			this.totalRecordsProcessed = this.runIngestionThreads(true, true, exceptionHeader); // skip ingest, skip check
			this.batchHandler.generateNotFoundURIs();
		}
		
		
		// next pass, if any, is all the remaining weight
		this.percentStart = this.percentEnd;
		this.percentEnd = 99;
		
		
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
			this.totalRecordsProcessed = this.runIngestionThreads(false, true, exceptionHeader); // ingest, skip precheck (already done)
			
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
		int dumpCacheRowInterval = 0;
		int dumpCacheNext = 0;
		long startEpoch = Instant.now().getEpochSecond();   // for reporting only
		int threadsUsed = 0;                                // for reporting only
		
		String mode = "";
		if (skipIngest && skipCheck) {
			mode = "URILookup only";  
		} else if (skipIngest && !skipCheck) {
			mode = "precheck";
		} else if (!skipIngest && skipCheck) {
			mode = "ingest";
		} else if (!skipIngest && !skipCheck) {
			mode = "precheck & ingest";
		} 
		
		LocalLogger.logToStdOut("Records processed (" + mode + "): ", true, false);
		long lastMillis = System.currentTimeMillis();  // use this to report # recs loaded every X sec
		
		ArrayList<IngestionWorkerThread> wrkrs = new ArrayList<IngestionWorkerThread>();
		
		ArrayList<ArrayList<String>> nextRecords = null;
		
		int numThreads = 2;    // first pass, run few threads to get recommendBatchSize
		
		while (true) {
			// get the next set of records from the data set.
			
			try{
				nextRecords = this.batchHandler.getNextRecordsFromDataSet();
			}catch(Exception e){ break; } // record set exhausted
			
			if(nextRecords == null || nextRecords.size() == 0 ){ break; }
			
			// spin up the thread and do the work. 
			SparqlEndpointInterface ingestSei = (!skipIngest && this.cacheSei != null) ? this.cacheSei : this.endpoint;
			IngestionWorkerThread worker = new IngestionWorkerThread(ingestSei, this.batchHandler, nextRecords, startingRow, this.oInfo, skipCheck, skipIngest);
			if (this.insertQueryIdealSizeOverride > 0) {
				worker.setOptimalQueryChars(this.insertQueryIdealSizeOverride);
			}
			startingRow += nextRecords.size();
			wrkrs.add(worker);
			worker.start();
			recordsProcessed += nextRecords.size();
			
			// thread pool is full.  Wait for all of them to complete.
			// and then we can start over.
			// Over simplistic logic could be improved for performance.
			if(wrkrs.size() >= numThreads){
				threadsUsed = numThreads;
				int retries = 0;
				for(int i = 0; i < wrkrs.size(); i++){
					IngestionWorkerThread thread = wrkrs.get(i);
					this.joinAndThrowIfException(thread, exceptionHeader);
					
					// check recommended batch size
					if (skipIngest) {
						if (this.batchHandler.getBatchSize() < DEFAULT_BATCH_SIZE_PRECHECK) {
							this.batchHandler.setBatchSize(Math.min(DEFAULT_BATCH_SIZE_PRECHECK, this.batchHandler.getBatchSize() * 2));
						}
					
					} else if (thread.getRecommendedBatchSize() != this.batchHandler.getBatchSize()) {
						//LocalLogger.logToStdOut("Changing batch size from " + this.batchHandler.getBatchSize() + " to " + thread.getRecommendedBatchSize()); 
						this.batchHandler.setBatchSize(thread.getRecommendedBatchSize());
					}
					retries += thread.endpoint.getRetries();
				}
				wrkrs.clear();
				
				if (retries != 0 && this.maxThreads != 1) {
					this.maxThreads -= 1;
					LocalLogger.logToStdErr("Reducing max threads to " + Integer.toString(this.maxThreads));
				}
				numThreads = this.maxThreads;   // after first pass, go full speed with maxThreads
				
				// log to stdout occasionally
				long nowMillis = System.currentTimeMillis();
				if (nowMillis - lastMillis > 1000) {
					
					// calculate percent complete
					double fraction = (double)startingRow / Math.max(1, this.datasetNumRows);
					int percent = this.percentStart + (int) Math.floor((this.percentEnd - this.percentStart) * fraction); 
					percent = Math.min(99, percent);  // don't let it hit 100 due to rounding.  100 will fail in status service.
					LocalLogger.logToStdOut("..." + recordsProcessed, false, false);
					lastMillis = nowMillis;
					
					// tell status client if there is one set up
					if (this.sClient != null) {
						this.sClient.execSetPercentComplete(percent);
					}
				}
				
				// if there is a cacheSei, make sure it isn't getting too large
				if (!skipIngest && this.cacheSei != null) {
					// after 100 rows, guess a good place to upload
					if (dumpCacheRowInterval == 0 && startingRow > 100) {
						int testSize = this.cacheSei.dumpToTurtle().length();
						
						// estimate rows between dumping cache based on first 100+ rows of ingestion
						// use at least 75,000 characters in the cache for the estimate
						dumpCacheRowInterval = (int) (this.maxCacheLength) / Math.max(50000, testSize) * startingRow;
						dumpCacheNext = dumpCacheRowInterval;
						LocalLogger.logToStdErr("Cache interval rows init: " + dumpCacheRowInterval);
					}
					// do upload when needed
					if (dumpCacheNext > 0 && startingRow > dumpCacheNext) {
						// upload and clear
						launchUploadCache();
						this.cacheSei = new InMemoryInterface("http://cache");
						
						int lastUploadSize = this.lastUploadSize.get();
						
						if (lastUploadSize > 0) {
							// check size and adjust dumpCacheRowInterval with 1/5 weighted average
							int newInterval = (int) (this.maxCacheLength / lastUploadSize) * dumpCacheRowInterval;
							dumpCacheRowInterval = (int) (((dumpCacheRowInterval * 4) + newInterval) / 5);  
							LocalLogger.logToStdErr("Cache interval rows new: " + dumpCacheRowInterval);
						}
						dumpCacheNext += dumpCacheRowInterval;
						
					}
				}
			}
		}
		
		LocalLogger.logToStdOut("..." + recordsProcessed, false, false);
		
		// for reporting only
		if (wrkrs.size() > threadsUsed) {
			threadsUsed = wrkrs.size();
		}
		// join all remaining threads
		for(int i = 0; i < wrkrs.size(); i++){
			this.joinAndThrowIfException(wrkrs.get(i), exceptionHeader);
		}
		
		// If a temporary in-memory graph was used, then dump it to owl and upload it
		if (!skipIngest && this.cacheSei != null) {
			this.launchUploadCache();
			this.waitForUpload();    // wait for last one.
		}
		
		String timingInfo = String.format(" %d threads %sed %d records in %d sec", threadsUsed, mode, recordsProcessed, Instant.now().getEpochSecond() - startEpoch);
		LocalLogger.logToStdOut(" (DONE)" + "\n" + timingInfo, false, true);
		
		// tell status client if there is one set up
		if (this.sClient != null) {
			this.sClient.execSetPercentComplete(Math.min(99,this.percentEnd));
		}
		
		// return to defaults
		this.batchSize = DEFAULT_BATCH_SIZE_INGEST;
		this.batchHandler.setBatchSize(DEFAULT_BATCH_SIZE_INGEST);
		
		return recordsProcessed;
	}
	
	/**
	 * Launch a new upload.  Waiting for previous to complete.
	 * @return length of previous upload, or 0
	 * @throws Exception
	 */
	private void launchUploadCache() throws Exception {
		
		this.uploadThread = new InMemoryInterfaceUploadThread(this.cacheSei , this.endpoint, this.headerTable, this.lastUploadSize);
		this.uploadThread.start();
	}
	
	/**
	 * Wait for previous upload to complete, if any
	 * @throws Exception
	 */
	private void waitForUpload() throws Exception {
		if (this.uploadThread != null) {
			this.uploadThread.join();
			if (this.uploadThread.getException() != null) {
				throw this.uploadThread.getException();
			}
			return;
		}
		else {
			return;
		}
	}
	
	public int getMaxThreads() {
		return maxThreads;
	}

	/**
	 * Override the default max threads
	 * @param maxThreads - number   OR BETTER: zero will set to number of available processors
	 */
	public void overrideMaxThreads(int maxThreads) {
		if (maxThreads==0) {
			int processors = Runtime.getRuntime().availableProcessors();
			if (processors > 8) {
				this.maxThreads = 8;    // haven't seen any improvements over 6, but use 8 if we have them
			} else if (processors > 4) {
				this.maxThreads = processors - 1;
			} else {
				this.maxThreads = processors;
			}
		} else {
			this.maxThreads = maxThreads;
		}
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
		StringBuilder s = new StringBuilder();
		Table errorReport = this.batchHandler.getErrorReport();
		int failureCauseIndex = errorReport.getColumnIndex(FAILURE_CAUSE_COLUMN_NAME);
		int failureRowIndex = errorReport.getColumnIndex(FAILURE_RECORD_COLUMN_NAME);
		ArrayList<ArrayList<String>> rows = errorReport.getRows();
		for(ArrayList<String> row:rows){
			s.append("Error in row " + row.get(failureRowIndex) + ": " + row.get(failureCauseIndex) + "\n");
		}
		return s.toString();
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
	
	/**
	 * Run the data load asynchronously
	 * @param precheck 
	 * @param skipIngest 
	 * @param sClient
	 * @param rClient
	 * @throws Exception
	 */
	public void runAsync(Boolean precheck, Boolean skipIngest, StatusClient sClient, ResultsClient rClient) throws Exception {
		this.asyncPrecheck = precheck;
		this.asyncSkipIngest = skipIngest;
		this.sClient = sClient;
		this.rClient = rClient;
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
		
		this.sClient.execSetPercentComplete(1);
		
		(new Thread(this)).start();
	}
	
	/**
	 * Perform a load asynchronously, after having first called setupAsyncRun()
	 */
	public void run() {
		if (this.asyncPrecheck == null || this.asyncSkipIngest == null || this.sClient == null || this.rClient == null) {
			this.asyncFailure(new Exception("Internal error: ran async data load without first calling setupAsyncRun()"));
		
		// perform the load
		} else {
			ThreadAuthenticator.authenticateThisThread(this.headerTable);
			String jobId = this.sClient.getJobId();  // wonky that status client has this and results client needs it.
			try {
				int recordsProcessed = this.importData(this.asyncPrecheck, this.asyncSkipIngest);
				
				Table errorTable = this.getLoadingErrorReport();
				
				if(this.asyncPrecheck) {
					if (errorTable.getRows().size() == 0) {
						sClient.execSetSuccess("Imported " + String.valueOf(recordsProcessed) + " records.");
					} else {
						rClient.execStoreTableResults(jobId, errorTable);
						sClient.execSetFailure("Failures encountered");
					}
				}
				else {
					if(recordsProcessed > 0) {
						sClient.execSetSuccess("Imported " + String.valueOf(recordsProcessed) + " records.");
					} else {
						rClient.execStoreTableResults(jobId, errorTable);
						sClient.execSetFailure("Failures encountered");
					}	
				}
				
			} catch (Exception e) {
				this.asyncFailure(e);
			}
		}
	}
	
	public void asyncFailure(Exception e) {
		try {
			LocalLogger.printStackTrace(e);
			this.sClient.execSetFailure(e.getMessage());
			
		} catch (Exception ee) {
			// we're out of luck if we can't tell the status service what happened
			LocalLogger.logToStdErr("Exception when trying to report to Status Service:");
			LocalLogger.printStackTrace(ee);
		}
	}
	
	
	/**
	 * Variant with no connection override
	 * Deprecated functions totally ignore batchSize.  It is now automatic.
	 */
	@Deprecated
	public static int loadFromCsv(String loadTemplateFilePath, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, int batchSize) throws Exception{
		return loadFromCsv(loadTemplateFilePath, csvFilePath, sparqlEndpointUser, sparqlEndpointPassword);
	}
	
	@Deprecated
	public static int loadFromCsv(String loadTemplateFilePath, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, int batchSize, SparqlConnection connectionOverride) throws Exception{
		return loadFromCsv(loadTemplateFilePath,csvFilePath,sparqlEndpointUser, sparqlEndpointPassword, connectionOverride);
	}
	
	@Deprecated
	public static int loadFromCsv(JSONObject loadTemplateJson, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, int batchSize, SparqlConnection connectionOverride) throws Exception{
		return loadFromCsv(loadTemplateJson, csvFilePath, sparqlEndpointUser, sparqlEndpointPassword, connectionOverride);
	}
	
	public static int loadFromCsv(String loadTemplateFilePath, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword) throws Exception{
		return loadFromCsv(loadTemplateFilePath, csvFilePath, sparqlEndpointUser, sparqlEndpointPassword, null);
	}
	
	/**
	 * Variant with loading template file path instead of JSON object
	 */
	public static int loadFromCsv(String loadTemplateFilePath, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, SparqlConnection connectionOverride) throws Exception{

		if(!loadTemplateFilePath.endsWith(".json")){
			throw new Exception("Error: Template file " + loadTemplateFilePath + " is not a JSON file");
		}
		
		LocalLogger.logToStdOut("--------- Load data from CSV... ---------------------------------------");
		LocalLogger.logToStdOut("Template:   " + loadTemplateFilePath);
		LocalLogger.logToStdOut("CSV file:   " + csvFilePath);
		LocalLogger.logToStdOut("Batch size: " + DEFAULT_BATCH_SIZE_INGEST);	
		LocalLogger.logToStdOut("Connection override: " + connectionOverride);	// may be null if no override connection provided
				
		return loadFromCsv(Utility.getJSONObjectFromFilePath(loadTemplateFilePath), csvFilePath, sparqlEndpointUser, sparqlEndpointPassword, connectionOverride);
	}
	
	public static int loadFromCsv(String loadTemplateFilePath, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, SparqlConnection connectionOverride, boolean preCheck, boolean skipIngest) throws Exception{

		if(!loadTemplateFilePath.endsWith(".json")){
			throw new Exception("Error: Template file " + loadTemplateFilePath + " is not a JSON file");
		}
		
		LocalLogger.logToStdOut("--------- Load data from CSV... ---------------------------------------");
		LocalLogger.logToStdOut("Template:   " + loadTemplateFilePath);
		LocalLogger.logToStdOut("CSV file:   " + csvFilePath);
		LocalLogger.logToStdOut("Batch size: " + DEFAULT_BATCH_SIZE_INGEST);	
		LocalLogger.logToStdOut("Connection override: " + connectionOverride);	// may be null if no override connection provided
				
		return loadFromCsv(Utility.getJSONObjectFromFilePath(loadTemplateFilePath), csvFilePath, sparqlEndpointUser, sparqlEndpointPassword, connectionOverride, -1, preCheck, skipIngest);
	}
	
	public static int loadFromCsv(JSONObject loadTemplateJson, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, SparqlConnection connectionOverride) throws Exception{
		return loadFromCsv(loadTemplateJson, csvFilePath, sparqlEndpointUser, sparqlEndpointPassword, connectionOverride, -1);
	}
	
	public static int loadFromCsv(JSONObject loadTemplateJson, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, SparqlConnection connectionOverride, int overrideMaxThreads) throws Exception{
		return DataLoader.loadFromCsv(loadTemplateJson, csvFilePath, sparqlEndpointUser, sparqlEndpointPassword, connectionOverride, overrideMaxThreads, true, false);
	}
	
	
	/**
	 * Utility method to load data given a template json, CSV file, and SPARQL connection
	 * @param loadTemplateJson 			the JSON containing the load template
	 * @param csvFilePath 				path to the CSV data file
	 * @param sparqlEndpointUser 		username for SPARQL endpoint
	 * @param sparqlEndpointPassword 	password for SPARQL endpoint
	 * @param connectionOverride 		a SPARQL connection to override the connection in the template (use null to not override)
	 * @param overrideMaxThreads	    set the number of threads max
	 * @return							total CSV records processed
	 */
	public static int loadFromCsv(JSONObject loadTemplateJson, String csvFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, SparqlConnection connectionOverride, int overrideMaxThreads, boolean preCheck, boolean skipIngest) throws Exception{
				
		// validate arguments
		if(!csvFilePath.endsWith(".csv")){
			throw new Exception("Error: Data file " + csvFilePath + " is not a CSV file");
		}		
		
		// create SparqlGraphJson, override connection if needed
		SparqlGraphJson sgJson = new SparqlGraphJson(loadTemplateJson);
		if(connectionOverride != null){	
			sgJson.setSparqlConn(connectionOverride);
		}
					
		// get needed column names from the JSON template
		String[] colNamesToIngest = sgJson.getImportSpecHandler().getColNamesUsed();		
		
		// open the dataset, using only the needed column names
		Dataset dataset = null;
		try{
			dataset = new CSVDataset(csvFilePath, colNamesToIngest);
			LocalLogger.logToStdOut("Ingest " + colNamesToIngest.length + " columns, " + dataset.getNumRows() + " rows");
			if(dataset.getNumRows() == 0){
				return 0;
			}
		}catch(Exception e){
			String s = "Could not instantiate CSV dataset: " + e.getMessage();
			LocalLogger.logToStdErr(s);
			throw new Exception(s);
		}
		
		// load the data
		try{
			DataLoader loader = new DataLoader(sgJson, dataset, sparqlEndpointUser, sparqlEndpointPassword);
			if (overrideMaxThreads > 0) {
				loader.overrideMaxThreads(overrideMaxThreads);
			}
			int recordsAdded = loader.importData(preCheck, skipIngest);
			LocalLogger.logToStdOut("Inserted " + recordsAdded + " records");
			if(loader.getLoadingErrorReport().getNumRows() > 0){
				// e.g. URI lookup errors may appear here
				LocalLogger.logToStdOut("Error report:\n" + loader.getLoadingErrorReportBrief());
				throw new Exception("Could not load data: loading errors: " + loader.getLoadingErrorReportBrief());
			}
			return recordsAdded;
		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			throw new Exception("Could not load data: " + e.getMessage());
		}
	}
	
	public static int loadFromCsvString(JSONObject sgjsonJson, String csvData, String sparqlEndpointUser, String sparqlEndpointPassword, boolean precheck) throws Exception{
		
		Dataset dataset = new CSVDataset(csvData, true);
		
		
		// load the data
		try{
			DataLoader loader = new DataLoader(sgjsonJson, dataset, sparqlEndpointUser, sparqlEndpointPassword);
			
			int recordsAdded = loader.importData(precheck);
			LocalLogger.logToStdOut("Inserted " + recordsAdded + " records");
			if(loader.getLoadingErrorReport().getNumRows() > 0){
				// e.g. URI lookup errors may appear here
				LocalLogger.logToStdOut("Error report:\n" + loader.getLoadingErrorReportBrief());
				throw new Exception("Could not load data: loading errors: " + loader.getLoadingErrorReportBrief());
			}
			return recordsAdded;
		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			throw new Exception("Could not load data: " + e.getMessage());
		}
	}

}
