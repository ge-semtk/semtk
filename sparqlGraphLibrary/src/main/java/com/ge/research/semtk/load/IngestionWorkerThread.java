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
	int maxQueryChars = 9900;       // TODO: make this configurable instead of batch size
	int optimalQueryChars = 3500;   // TODO: make this configurable instead of batch size
	
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
					System.out.println(query.length());
					this.endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
					
				} else {
					
					ArrayList<String> queryList = this.splitIntoQueries(nodeGroupList);
					
					// run queryList
					for (String q : queryList) {
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
		int maxQueryLen = 0;
		
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
			maxQueryLen = 0;
			
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
				maxQueryLen = Math.max(maxQueryLen, subQuery.length());
				
				// get ready for next iteration
				index0 += this.recommendedBatchSize;
				indexN = Math.min(nodeGroupList.size(), indexN + this.recommendedBatchSize);
			}
			
		} while (maxQueryLen > this.optimalQueryChars && this.recommendedBatchSize > 1);
		
		// check if we found a solution
		if (this.recommendedBatchSize < 2 && maxQueryLen > this.maxQueryChars) {
			throw new Exception("Query is too long");
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
