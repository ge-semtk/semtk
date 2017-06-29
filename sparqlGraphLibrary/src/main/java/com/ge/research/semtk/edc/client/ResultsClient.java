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
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.client.RestClient;

public class ResultsClient extends RestClient implements Runnable {
	
	private int ROWS_TO_PROCESS = 1000;  // the default row allocation to send. this can be tuned if things may fail.

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
	 * Not meant to be used.
	 * @return
	 * @throws Exception
	 */
	public SimpleResultSet execute() throws ConnectException, EndpointNotFoundException, Exception{
		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute StatusClient with no enpoint specified.");
		}
		JSONObject resultJSON = (JSONObject)super.execute();	
		
		SimpleResultSet ret = (SimpleResultSet) SimpleResultSet.fromJson(resultJSON);  
		return ret;
	}
	
	/**
	 * Store a table (as json)
	 * @param contents
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execStoreTableResults(String jobId, Table table) throws ConnectException, EndpointNotFoundException, Exception {
		// chunk up the table by size and then send all the chunks. 
		// hopefully, this will avoid sending anything too large to the results service
		
		int tableRowsDone = 0;
		int totalRows     = table.getNumRows();
		int segment       = 0;
		int finalSegmentNumber = (int) (Math.ceil(totalRows/ROWS_TO_PROCESS) - 1);
		
		long startTime=0, endTime=0;
		double prepSec = 0.0;
		double sendSec = 0.0;
		boolean timerFlag = false;
		
		Thread thread = null;

		// write the start of the JSON
		conf.setServiceEndpoint("results/initializeTableResultsJson"); 
		this.parametersJSON.put("jobId", jobId);
		this.parametersJSON.put("columnNames", table.getColumnNames());
		this.parametersJSON.put("columnTypes", table.getColumnTypes());
		thread = new Thread(this);
		thread.run();
		
		// write the data rows to JSON, in batches
		while(tableRowsDone < totalRows){
			if (timerFlag) { startTime = System.nanoTime();}
			int tableRowsAtStart = tableRowsDone;
			// get the next few rows.
			StringBuilder resultsSoFar = new StringBuilder();

			// get the next allocation of rows. 
			for(int i = 0; i < this.ROWS_TO_PROCESS; i += 1){
				try{

					// get the next row into a comma separated string.
					String curr = new StringBuilder(table.getRow(tableRowsDone).toString()).toString(); // ArrayList.toString() is fast
					// but if any element contained commas, then can't use ArrayList.toString()
					if(StringUtils.countMatches(curr, ",") != (table.getNumColumns() - 1)){
						// at least one comma exists within an element
						// the following approach is relatively slow, so only use when needed
						// escape double quotes (using "" for csv files), then enclose each element in double quotes 
						curr = table.getRow(tableRowsDone).stream()
								.map(s -> (new StringBuilder()).append("\"").append(s.replace("\"","\"\"")).append("\"").toString())
								.collect(Collectors.joining(","));
					}else{
						// there are no commas within elements
						// do a simple fast replace to remove spaces after commas (added by ArrayList.toString()) and enclose each element in quotes
						curr = StringUtils.substring(curr, 1, curr.length() - 1);		// remove the brackets added by ArrayList.toString()
						curr = "\"" + StringUtils.replace(curr, ", ", "\",\"") + "\""; 	// replace comma-space with quotes-comma-quotes
					}
					curr = "[" + curr + "]";	// add enclosing brackets

					// the row now has: 1) quoted elements 2) no spaces after delimiter commas 3) enclosing brackets

					tableRowsDone += 1;

					// add to the existing results we want to send.
					//lastResults = resultsSoFar.toString(); // PEC changed  
					resultsSoFar.append(curr); // TODO when this was using +=, it would have triggered the batch-too-big behavior, but now that it's a StringBuilder, not sure
					if(segment != finalSegmentNumber){
						resultsSoFar.append(",");
					}

				}
				catch(IndexOutOfBoundsException eek){
					// we have run out of rows. the remaining rows were fewer than the block size. just note this and move on.
					i = this.ROWS_TO_PROCESS;
				}

				// TODO review with Justin.  Removing the "revert to slightly smaller batch size" for now because saving the lastBatch after every row
				// was slowing the performance.  We can reintroduce it in a better way later.  For now, let any exceptions flow up
				//				catch(Exception eee){
				//					// the send size would have been too large.
				//					tableRowsDone = tableRowsDone - 1;
				//					
				//					System.out.println("*** caught an exception trying to process a result: " +  tableRowsDone);
				//					System.out.println(eee.getMessage());
				//			
				//					i = this.ROWS_TO_PROCESS; // remove the one that broke things. this way, we reprocess it
				//					//resultsSoFar = new StringBuilder(lastResults); // reset the values.  
				//				}
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

			// take care of last run
			if (thread != null) {
				thread.join();
				((SimpleResultSet) this.getRunRes()).throwExceptionIfUnsuccessful();
				if (this.getRunException() != null) {
					throw this.getRunException();
				}
				segment += 1;
				conf.setServiceEndpoint(null);
				this.parametersJSON.remove("contents");
				this.parametersJSON.remove("jobId");
			}

			// send the current batch  
			conf.setServiceEndpoint("results/storeTableResultsJsonIncremental"); 
			this.parametersJSON.put("contents", resultsSoFar.toString());
			this.parametersJSON.put("jobId", jobId);
			thread = new Thread(this);
			thread.run();

			if (timerFlag) { 
				endTime = System.nanoTime();
				sendSec += ((endTime - startTime) / 1000000000.0);
				System.err.println(String.format("tot send=%.2f sec", sendSec));
				startTime = endTime;
			}
		} // end of while loop.
		
		// write the end of the JSON
		conf.setServiceEndpoint("results/finalizeTableResultsJson"); 
		this.parametersJSON.put("jobId", jobId);
		this.parametersJSON.put("rowCount", table.getNumRows());
		thread = new Thread(this);
		thread.run();

		// cleanup
		// take care of last run
		if (thread != null) {
			thread.join();
			((SimpleResultSet) this.getRunRes()).throwExceptionIfUnsuccessful();
			if (this.getRunException() != null) {
				throw this.getRunException();
			}
		}

		if (timerFlag) { System.err.println(String.format("prep=%.2f sec   send=%.2f sec", prepSec, sendSec)); }
		return;
	}
	
	/**
	 * This is for backwards compatibility only.
	 */
	@SuppressWarnings("unchecked")
	public URL [] execGetResults(String jobId) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/getResults");
		this.parametersJSON.put("jobId", jobId);

		try {
			SimpleResultSet res = this.execute();
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
			this.parametersJSON.remove("contents");
			this.parametersJSON.remove("jobId");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void execDeleteStorage(String jobId) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/deleteStorage");
		this.parametersJSON.put("jobId", jobId);
		
		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jobId");
		}
	}
	
}
