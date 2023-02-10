/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.sparqlX.dispatch.QueryGroup;

import java.io.IOException;
import java.util.HashMap;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;

public class QueryGroupFusionWorkerThread extends Thread {

	private int MAX_SIMULTANEOUS_DEEPWORKERS = 10;
	private double MAX_ROWS_PER_DEEP_WORKER = 1000;
	
	private Table myPartialResult;
	private int   myStartOffset;
	public Object[] globalResultSet;
	private String[] myColumnsInOrder;
	private HashMap<String, String> mySemanticColumnValues;
	
	public QueryGroupFusionWorkerThread(Table partialResult, int offset, Object[] globalResult,
			String[] columnsInOrder, HashMap<String, String> semanticColumnValues){
		// create a worker that can enter the values for its subset.
		this.myPartialResult = partialResult;
		this.myStartOffset   = offset;
		this.globalResultSet = globalResult;
		this.myColumnsInOrder = columnsInOrder;
		this.mySemanticColumnValues = semanticColumnValues;
		
		
		//LocalLogger.logToStdOut("group fusion thread initialized with a start offset of: " + this.myStartOffset);
	}
	
	
	public void run(){
		
		if(this.myPartialResult == null){ // nothing to do
			return;
		}

		else{
			// create the row control structure. 
			Integer[] rowControl = new Integer[this.myColumnsInOrder.length];
			String[] semanticValues = new String[this.mySemanticColumnValues.keySet().size() + 1]; // the first value here is intentionally left blank/null. 
			
			// pre-calc anything we can to add
			int semCounter = 0;
			int colCounter = 0;
			for(String currCol : myColumnsInOrder){
				if(this.mySemanticColumnValues.keySet().contains(currCol) && (this.myPartialResult.getColumnIndex(currCol) != -1 )){
					// the value exists in both datasets.
					
					// check the value of the col colunter to make sure that we have not processed this value on the semantic side already. 
					// since semantic values are always first in the sorted list, this is a safe operation. 
					
					if(colCounter < (this.mySemanticColumnValues.keySet().size())){ // we have not processed this result yet...
						// get the data from the semantic values.
						semCounter += 1;  // only update when we hit a new semantic value.
						semanticValues[semCounter] = (this.mySemanticColumnValues.get(currCol));
						
						rowControl[colCounter] = -1 * semCounter; // sentinel index for using the semantic column number.
																  // negative index is for going to semantic look up. 						
					}

					// handle the external part. -- this part can be done only if the semantic one has been done once?
					else{
						try{
							int idx = this.myPartialResult.getColumnIndex(currCol); // here is the row number.
							if(idx >= 0){ rowControl[colCounter] = idx; }
							else { rowControl[colCounter] = null; }
							
						}
						catch(Exception e){
							rowControl[colCounter] = null; // no such value. use the global sentinel eventually. for now, make it null to make lookups faster. 
						} 
					}
								
				}
				else if(this.mySemanticColumnValues.keySet().contains(currCol)){
					// get the data from the semantic values.
					semCounter += 1;  // only update when we hit a new semantic value.
					semanticValues[semCounter] = (this.mySemanticColumnValues.get(currCol));
					
					rowControl[colCounter] = -1 * semCounter; // sentinel index for using the semantic column number.
															  // negative index is for going to semantic look up. 
				}
				else{
						try{
							int idx = this.myPartialResult.getColumnIndex(currCol); // here is the row number.
							if(idx >= 0){ rowControl[colCounter] = idx; }
							else { rowControl[colCounter] = null; }
							
						}
						catch(Exception e){
							rowControl[colCounter] = null; // no such value. use the global sentinel eventually. for now, make it null to make lookups faster. 
						} 
										
				}
				colCounter += 1;	// always update this value. 
			}	
			
			
			// thread the actual work
			
			// get the number of subqueries to max out at.
			QueryGroupFusionWorkerDeepThread[] spunup = new QueryGroupFusionWorkerDeepThread[(int) Math.ceil(myPartialResult.getNumRows() / this.MAX_ROWS_PER_DEEP_WORKER)];
			int attemptSentinel = (int) Math.ceil(myPartialResult.getNumRows() / this.MAX_ROWS_PER_DEEP_WORKER);
			
			try{
				int threadLimit = this.MAX_SIMULTANEOUS_DEEPWORKERS;
				int finished = 0;
				int iteration = 0;
				int running = 0;
				int currentReq = 0;
				int startPosition = 0;
				int outputOffset = this.myStartOffset;
				
				while(finished < attemptSentinel){
					if(running == attemptSentinel){
						// nothing left to do... just wait for them to finish. 
						break;
					}
					
					if(running >= threadLimit || currentReq >= attemptSentinel){
						for(int joined = 0; joined < threadLimit; joined += 1){
							try {
								spunup[joined + (threadLimit * iteration)].join();
								finished += 1;
								running = running - 1;
								if(finished >= attemptSentinel){break;}
							} catch (Exception e) {
								LocalLogger.printStackTrace(e);
								throw new IOException("(Join failure in fusion threading : joined value was "+ joined + ":: " + e.getClass().toString() + " : "+ e.toString() + ")");
							}
						}
						if(currentReq%threadLimit == 0){ iteration += 1;}
					}
					else{
						if (currentReq < attemptSentinel) {
							try {
								spunup[currentReq] = new QueryGroupFusionWorkerDeepThread(this.myPartialResult.getRows(), startPosition, (int)this.MAX_ROWS_PER_DEEP_WORKER, outputOffset, rowControl, semanticValues, this.globalResultSet);
								spunup[currentReq].start();
								
								// update start position
								startPosition += (int)this.MAX_ROWS_PER_DEEP_WORKER;
								outputOffset += (int)this.MAX_ROWS_PER_DEEP_WORKER;
								
							} catch (Exception EEE) {
								throw new Exception("spin up of fusion thread failed. reported:" + EEE.toString());
							}
							// update the counters
							running += 1;
							currentReq += 1;	
						}	
					}
				}
						
				// make sure it all really closed.
				for(int joined = 0; joined < attemptSentinel; joined += 1){
					try {
						spunup[joined ].join();
						continue;
					} catch (Exception e) {
						LocalLogger.printStackTrace(e);
						throw new Exception("(Join failure in fusion threading : joined value was "+ joined + ":: " + e.getClass().toString() + " : "+ e.toString() + ")");
					}
				}

			
			}
			catch(Exception e){
					LocalLogger.logToStdOut("Fusion threading failure: " + e.toString());
			}
		}
	}
	

	
}
