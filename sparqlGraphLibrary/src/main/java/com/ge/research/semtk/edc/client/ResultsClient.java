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

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.client.RestClient;

public class ResultsClient extends RestClient implements Runnable {
	
	private int ROWS_TO_PROCESS = 1000;  // the default row allocation to send. this can be tuned if things may fail.

	public ResultsClient (ResultsClientConfig config) {
		this.conf = config;
	}
	
	@Override
	public void buildParametersJSON() throws Exception {
		// TODO: what do you think of this
		((ResultsClientConfig) this.conf).addParameters(this.parametersJSON);

	}

	@Override
	public void handleEmptyResponse() throws Exception {
		// TODO:  why is this re-implemented for all subclasses
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
	 * Store file contents.  sample is shorter csv
	 * @param contents
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execStoreCsvResults(String jobId, String contents) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/storeCsvResults");
		this.parametersJSON.put("contents", contents);
		this.parametersJSON.put("jobId", jobId);

		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("contents");
			this.parametersJSON.remove("jobId");
		}
	}
	

	/**
	 * Store Table.  fullResult is csv.  sample is json.
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
		
		long startTime=0, endTime=0;
		double prepSec = 0.0;
		double sendSec = 0.0;
		boolean timerFlag = false;
		
		Thread thread = null;

		if(totalRows == 0){
			// just create and send the header row.
			StringBuilder resultsSoFar = new StringBuilder();

			for(int i1 = 0; i1 < table.getNumColumns(); i1 += 1){
				resultsSoFar.append((table.getColumnNames())[i1]);
				if(i1 < table.getNumColumns() - 1){ resultsSoFar.append(","); }
			}

			resultsSoFar.append("\n");


			conf.setServiceEndpoint("results/storeIncrementalCsvResults");
			this.parametersJSON.put("contents", resultsSoFar.toString());
			this.parametersJSON.put("jobId", jobId);
			this.parametersJSON.put("segmentNumber", segment);

			thread = new Thread(this);
			thread.run();
		}

		else{    // write out all the results, y'know?
			while(tableRowsDone < totalRows){
				if (timerFlag) { startTime = System.nanoTime();}
				int tableRowsAtStart = tableRowsDone;
				// get the next few rows.
				StringBuilder resultsSoFar = new StringBuilder();
				//String lastResults  = "";

				// get the next allocation of rows. 
				for(int i = 0; i < this.ROWS_TO_PROCESS; i += 1){
					try{

						// Make sure we include a header row.
						if(tableRowsDone == 0){ // first record...
							for(int i1 = 0; i1 < table.getNumColumns(); i1 += 1){
								resultsSoFar.append((table.getColumnNames())[i1]);
								if(i1 < table.getNumColumns() - 1){ resultsSoFar.append(","); }
							}
						}

						// can we get the next results and add to what we want.
						StringBuilder curr = new StringBuilder(table.getRow(tableRowsDone).toString());
						tableRowsDone += 1;
						// remove the leading and trailing "[" "]" from the row.
						curr.setLength(curr.length()-1);
						curr.deleteCharAt(0);

						// add to the existing results we want to send.
						//lastResults = resultsSoFar.toString(); // PEC changed  
						resultsSoFar.append("\n");
						resultsSoFar.append(curr); // TODO when this was using +=, it would have triggered the batch-too-big behavior, but now that it's a StringBuilder, not sure

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

				// send the current one:

				conf.setServiceEndpoint("results/storeIncrementalCsvResults");
				this.parametersJSON.put("contents", resultsSoFar.toString());
				this.parametersJSON.put("jobId", jobId);
				this.parametersJSON.put("segmentNumber", segment);

				thread = new Thread(this);
				thread.run();

				if (timerFlag) { 
					endTime = System.nanoTime();
					sendSec += ((endTime - startTime) / 1000000000.0);
					System.err.println(String.format("tot send=%.2f sec", sendSec));
					startTime = endTime;
				}
			} // end of while loop.

		}

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
	 * Store Table.  fullResult is csv.  sample is json.
	 * @param contents
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void execStoreSingleFileResults(String jobId, String contents, String extension) throws ConnectException, EndpointNotFoundException, Exception {
		conf.setServiceEndpoint("results/storeSingleFileResults");
		this.parametersJSON.put("contents", contents);
		this.parametersJSON.put("extension", extension);
		this.parametersJSON.put("jobId", jobId);

		try {
			SimpleResultSet res = this.execute();
			res.throwExceptionIfUnsuccessful();
			return;
			
		} finally {
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("contents");
			this.parametersJSON.remove("e");
			this.parametersJSON.remove("jobId");
		}
	}
	
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
