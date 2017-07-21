/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.edc.client;

import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.utility.Utility;

public class ResultsClient extends RestClient implements Runnable {
	
	// batch sizes for sending and retrieving results.  These can be tuned.
	private int BATCH_SIZE_SEND = 5000;
	private int BATCH_SIZE_RETRIEVE = 50000;
	
	public ResultsClient (ResultsClientConfig config) {
		this.conf = config;
	}
	
	@Override
	public void buildParametersJSON() throws Exception {
		((ResultsClientConfig) this.conf).addParameters(this.parametersJSON);
	}

	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response");
	}	
	
	/**
	 * Store a table (as json).  
	 * Uses 3 endpoints to initialize, incrementally add data, and finalize the result.
	 * @param contents
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execStoreTableResults(String jobId, Table table) throws ConnectException, EndpointNotFoundException, Exception {
		// chunk up the table by size and then send all the chunks. 
		// hopefully, this will avoid sending anything too large to the results service
		
		int tableRowsDone = 0;
		int totalRows     = table.getNumRows();
		
		long startTime=0, endTime=0;
		double prepSec = 0.0;
		double sendSec = 0.0;
		boolean timerFlag = false;
		
		Thread thread = null;

		// write the start of the JSON
		conf.setServiceEndpoint("results/storeTableResultsJsonInitialize"); 
		this.parametersJSON.put("jobId", jobId);
		this.parametersJSON.put("jsonRenderedHeader", createNewHeaderMap(table).toJSONString());
		thread = new Thread(this);
		thread.start();
		
		// do formatting tasks in parallel
		formatTableElements(table);		// escapes double quotes
		
		// write the data rows to JSON, in batches
		while(tableRowsDone < totalRows){
			if (timerFlag) { startTime = System.nanoTime();}
			int tableRowsAtStart = tableRowsDone;
			// get the next few rows.
			StringBuilder resultsSoFar = new StringBuilder();

			// get the next allocation of rows. 
			for(int i = 0; i < this.BATCH_SIZE_SEND; i++){
				
				if(!(tableRowsDone < table.getNumRows())){
					break;	// processed all rows - we're done
				}
				try{

					resultsSoFar.append("[");
					ArrayList<String> row = table.getRow(tableRowsDone);
					for(int j = 0; j < row.size(); j++){
						resultsSoFar.append("\"").append(row.get(j)).append("\"");	// enclose in quotes (tried putting in thread, but resulted in worse performance)
						if(j < row.size() - 1){
							resultsSoFar.append(",");								// don't append comma to the last element of the row
						}
					}
					resultsSoFar.append("]");
					
					// each row has: 1) internal double quotes escaped 2) elements surrounded by double quotes 3) enclosing brackets

					tableRowsDone += 1;
					
					if(i < BATCH_SIZE_SEND - 1){
						resultsSoFar.append("\n");
					}
				}
				catch(IndexOutOfBoundsException eek){
					// we have run out of rows. the remaining rows were fewer than the block size. just note this and move on.
					i = this.BATCH_SIZE_SEND;
				}

			}

			// fail if tableRowsDone has not changed. this implies that even the first result was too large.
			if((tableRowsDone == tableRowsAtStart) && (tableRowsDone < totalRows)){
				throw new Exception("unable to write results. there is a row size which is too large. row number was " + tableRowsDone + " of a total " + totalRows + ".");
			}

			if (timerFlag) { 
				endTime = System.nanoTime();
				prepSec += ((endTime - startTime) / 1000000000.0);
				System.err.println(String.format("tot prep=%.2f sec", prepSec));
				startTime = endTime;
			}

			// wait for previous batch to finish
			waitForThreadToFinish(thread);
			conf.setServiceEndpoint(null);			
			
			// send the current batch  
			conf.setServiceEndpoint("results/storeTableResultsJsonAddIncremental"); 
			this.parametersJSON.put("contents", Utility.compress(resultsSoFar.toString())); 
			this.parametersJSON.put("jobId", jobId);
			thread = new Thread(this);
			thread.start();

			if (timerFlag) { 
				endTime = System.nanoTime();
				sendSec += ((endTime - startTime) / 1000000000.0);
				System.err.println(String.format("tot send=%.2f sec", sendSec));
				startTime = endTime;
			}
		} // end of while loop.
		
		// wait for the final batch to finish
		waitForThreadToFinish(thread);
		
		// write the end of the JSON
		conf.setServiceEndpoint("results/storeTableResultsJsonFinalize"); 
		this.parametersJSON.put("jobId", jobId);
		thread = new Thread(this);
		thread.start();
		// wait for the finalize run to finish
		waitForThreadToFinish(thread);

		if (timerFlag) { System.err.println(String.format("prep=%.2f sec   send=%.2f sec", prepSec, sendSec)); }
		
		return;
	}
		
	private JSONObject createNewHeaderMap(Table table) throws Exception {
		return table.getHeaderJson();
	}
	
	
	/**
	 * Get results (possibly truncated) in JSON format for a job
	 * @return a TableResultSet object
	 */
	@SuppressWarnings("unchecked")
	public TableResultSet execTableResultsJson(String jobId, Integer maxRows) throws ConnectException, EndpointNotFoundException, Exception {

		ArrayList<Thread> threads = new ArrayList<Thread>();	
		ArrayList<ResultsClient> clients = new ArrayList<ResultsClient>();
		try {
			int numRowsToRetrieve = getNumRows(jobId);
			if(maxRows != null && maxRows < numRowsToRetrieve){
				numRowsToRetrieve = maxRows.intValue();
			}
			int numBatches = (int)Math.ceil((double)numRowsToRetrieve / (double)BATCH_SIZE_RETRIEVE);  
			ArrayList<TableResultSet> resultSets = new ArrayList<TableResultSet>();
			
			// kick off all threads
			for(int i = 0; i < numBatches; i++){
				
				// set request parameters
				conf.setServiceEndpoint("results/getTableResultsJson");
				this.parametersJSON.put("jobId", jobId);
				this.parametersJSON.put("startRow", i * BATCH_SIZE_RETRIEVE);
				if(i < numBatches - 1){
					this.parametersJSON.put("maxRows", BATCH_SIZE_RETRIEVE); 
				}else{
					this.parametersJSON.put("maxRows", numRowsToRetrieve - (i * BATCH_SIZE_RETRIEVE)); // last batch
				}		
				
				// kick off a thread using a "copy" of the client (necessary to avoid overwrites)  TODO find a cleaner way
				ResultsClient client = new ResultsClient((ResultsClientConfig) conf); 
				client.parametersJSON = (JSONObject) this.parametersJSON.clone(); 
				Thread thread = new Thread(client);
				clients.add(client);
				threads.add(thread);
				thread.start();						// execute
				this.parametersJSON.clear();  		// clear parameters for next time	
			}
			
			// wait for threads to finish
			for(int i = 0; i < threads.size(); i++){
				threads.get(i).join();
				TableResultSet resultSet = clients.get(i).getRunResAsTableResultSet();
				resultSet.throwExceptionIfUnsuccessful();
				resultSets.add(resultSet);
			}
			
			// merge batch results into a single TableResultSets
			TableResultSet ret = TableResultSet.merge(resultSets);		
			
			// clean up
			for(TableResultSet trs : resultSets){
				trs = null;
			}
			for(ResultsClient client : clients){
				client = null;
			}
			for(Thread thread : threads){
				thread = null;
			}
			
			return ret;			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.clear();
		}
	}		

	/**
	 * Get results (possibly truncated) in CSV format for a job
	 * @return a CSVDataset object
	 */
	@SuppressWarnings("unchecked")
	public CSVDataset execTableResultsCsv(String jobId, Integer maxRows) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/getTableResultsCsv");
		this.parametersJSON.put("jobId", jobId);
		if(maxRows != null){
			this.parametersJSON.put("maxRows", maxRows.intValue());
		}
		this.parametersJSON.put("appendDownloadHeaders", false);

		try {
			String s = (String) super.execute(true);  // true to return raw response (not parseable into JSON)
			return new CSVDataset(s, true);
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.clear();
		}
	}	
	
	/**
	 * This is for backwards compatibility only.
	 */
	@SuppressWarnings("unchecked")
	public URL [] execGetResults(String jobId) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/getResults");
		this.parametersJSON.put("jobId", jobId);

		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			String sampleUrlStr = res.getResult("sampleURL");
			String fullUrlStr = res.getResult("fullURL");
			URL sampleUrl = (!sampleUrlStr.equals("")) ? new URL(sampleUrlStr) : null;
			URL fullUrl = (!fullUrlStr.equals("")) ? new URL(fullUrlStr) : null;

			URL [] ret = { sampleUrl, fullUrl };
			return ret;			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.clear();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void execDeleteStorage(String jobId) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/deleteStorage");
		this.parametersJSON.put("jobId", jobId);
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.clear();
		}
	}
	
	/**
	 * Iterate through each table element, and format: 1) escape internal quotes
	 */
	private void formatTableElements(Table table) throws InterruptedException{
		
		int THREAD_BATCH_SIZE = 100;	// number of rows for each thread to handle
		ArrayList<TableFormatter> threads = new ArrayList<TableFormatter>();
		
		for(int i = 0; i < table.getNumRows(); i += THREAD_BATCH_SIZE){
			int startIndex = i;
			int endIndex = i + THREAD_BATCH_SIZE;
			if(endIndex > table.getNumRows()){
				endIndex = table.getNumRows();
			}
			TableFormatter thread = new TableFormatter(table.getRows(), startIndex, endIndex);
			threads.add(thread);
			thread.start();
		}
		
		// wait for all threads to finish
		for(TableFormatter t : threads){
			t.join();
		}
	}

	/**
	 * Waits for the thread to finish, and throws an exception if not successful
	 */
	private void waitForThreadToFinish(Thread thread) throws Exception{
		if (thread != null) {
			thread.join();
			(this.getRunResAsSimpleResultSet()).throwExceptionIfUnsuccessful();
			if (this.getRunException() != null) {
				throw this.getRunException();
			}
		}
		this.parametersJSON.clear();  // clear parameters for next time
	}
	
	/**
	 * Execute a service call to get number of result rows for a given job.
	 * @throws Exception 
	 */
	private int getNumRows(String jobId) throws Exception {
		conf.setServiceEndpoint("results/getTableResultsRowCount");
		this.parametersJSON.put("jobId", jobId);
		this.run();
		SimpleResultSet res = this.getRunResAsSimpleResultSet();
		res.throwExceptionIfUnsuccessful();
		if (this.getRunException() != null) {
			throw this.getRunException();
		}
		this.parametersJSON.clear();  // clear parameters for next time
		return res.getResultInt("rowCount");
	}	
}


/**
 * Thread to format a subset of a table
 * Note: tried enclosing each element in double quotes here as well, but resulted in overall worse performance
 */
class TableFormatter extends Thread{  

	private ArrayList<ArrayList<String>> rows;
	private int startIndex, endIndex;

	public TableFormatter(ArrayList<ArrayList<String>> rows, int startIndex, int endIndex){
		this.rows = rows;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public void run(){  
		for(int i = startIndex; i < endIndex; i++){
			for(int j = 0; j < rows.get(i).size(); j++){				
				if(rows.get(i).get(j).indexOf('\"') > -1){
					
					rows.get(i).set(j, 
							
						StringUtils.replace(	
							(StringUtils.replace(
									(StringUtils.replace(rows.get(i).get(j), "\"", "\\\"")), // core
									"\t", "    ")), // tabs
							"\n", "\\n")  // newlines
								
							); // escape internal double quotes
				}
			}
		}
	} 
}

