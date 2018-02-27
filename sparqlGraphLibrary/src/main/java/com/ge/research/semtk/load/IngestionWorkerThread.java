/**
 ** Copyright 2017-2018 General Electric Company
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
import java.util.HashMap;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.DataLoadBatchHandler;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.utility.LocalLogger;

public class IngestionWorkerThread extends Thread {

	OntologyInfo oInfo = null;
	SparqlEndpointInterface endpoint = null;
	DataLoadBatchHandler batchHandler = null;
	ArrayList<ArrayList<String>> dataSetRecords = null;
	Boolean skipChecks = false;
	Boolean skipIngest = false;
	int startingRowNum = 0;
	Exception e = null;
	
	int recommendedBatchSize = -1;
	int maxQueryChars = 100000;    // Arbitrarily limit query size.  Virtuoso seems to lock up in the millions.  Only 10's of thousands are needed ususally.
									// TODO: make this configurable instead of batch size
    
	int optimalQueryChars = 3500;   // Virtuoso is far more efficient around this size during large loads
									// TODO: make this configurable instead of batch size 3500
    
	public IngestionWorkerThread(SparqlEndpointInterface endpoint, DataLoadBatchHandler batchHandler, ArrayList<ArrayList<String>> dataSetRecords, int startingRowNum, OntologyInfo oInfo, Boolean skipChecks, Boolean skipIngest) throws Exception{
		
		this.endpoint = endpoint.copy();    // endpoint is not thread-safe as it contains query results
		this.batchHandler = batchHandler;
		this.dataSetRecords = dataSetRecords;
		this.startingRowNum = startingRowNum;
		this.skipChecks = skipChecks;
		this.skipIngest = skipIngest;
		this.oInfo = oInfo;
		this.recommendedBatchSize = dataSetRecords.size();   // at first, presume datasetRecords is fine
	}
	
	/**
	 * Runs a thread.
	 * If dataSetRecords is so big it needs splitting,
	 * this.recommendedBatchSize will be set to a size that worked.
	 * It is more efficient to use this value when sizing future threads.
	 */
	public void run(){
		try {
			ArrayList<NodeGroup> nodeGroupList = this.batchHandler.convertToNodeGroups(this.dataSetRecords, this.startingRowNum, this.skipChecks);
			
			if (nodeGroupList.size() > 0 && ! this.skipIngest) {
				
				// try to run one efficient query
				String query = NodeGroup.generateCombinedSparqlInsert(nodeGroupList, oInfo);
				if (query.length() <= this.optimalQueryChars) {
					//System.out.println("Query length DEBUG-1: " + query.length());   
					this.endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
					
				} else {
					
					ArrayList<String> queryList = this.splitIntoQueries(nodeGroupList);
					
					// run queryList
					for (String q : queryList) {
						//System.out.println("Query length DEBUG-2: " + q.length());   
						this.endpoint.executeQuery(q, SparqlResultTypes.CONFIRM);
					}
				}
			}
			
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			this.e = e;
		}
	}
	
	/**
	 * Use nodeGroupList to generate 1 or more queries that fit in this.maxQueryChars
	 * And set this.recommendedBatchSize
	 * @param nodeGroupList
	 * @return
	 * @throws Exception - including if one nodegroup still can't make a small enough query
	 */
	private ArrayList<String> splitIntoQueries(ArrayList<NodeGroup> nodeGroupList) throws Exception {
		ArrayList<String> queryList = null;
		int longestQueryLen = 0;
		
		// increment recommendedNumRecords so first iteration of loop puts it back
		this.recommendedBatchSize += 1;
		
		// loop through starting at recommendedNumRecords decreasing each time
		// until all queries are small enough
		do {
			this.recommendedBatchSize = this.recommendedBatchSize - 1;
			
			// wipe out queryList
			queryList = new ArrayList<String>();
			int index0 = 0;
			int indexN = Math.min(nodeGroupList.size(), this.recommendedBatchSize);
			longestQueryLen = 0;
			
			// build queryList in chunks of recommendedNumRecords
			while (index0 < nodeGroupList.size()) {
				// build sublist of nodegroups
				ArrayList<NodeGroup> ngSubList = new ArrayList<NodeGroup>();
				for (int i=index0; i < indexN; i++) {
					ngSubList.add(nodeGroupList.get(i));
				}
				
				// generate query and check size
				String subQuery = NodeGroup.generateCombinedSparqlInsert(ngSubList, this.oInfo);
				queryList.add(subQuery);
				longestQueryLen = Math.max(longestQueryLen, subQuery.length());
				
				// get ready for next iteration
				index0 += this.recommendedBatchSize;
				indexN = Math.min(nodeGroupList.size(), indexN + this.recommendedBatchSize);
			}
			
		} while (longestQueryLen > this.optimalQueryChars && this.recommendedBatchSize > 1);
		
		// check if we found a solution
		if (this.recommendedBatchSize < 2 && longestQueryLen > this.maxQueryChars) {
			throw new Exception("Query is too long: " + longestQueryLen);
		}
		
		return queryList;
	}
	
	public int getRecommendedBatchSize() {
		return this.recommendedBatchSize;
	}
	
	/**
	 * Return the exception thrown during running of this thread, or null
	 * @return
	 */
	public Exception getException() {
		return this.e;
	}
	
}
