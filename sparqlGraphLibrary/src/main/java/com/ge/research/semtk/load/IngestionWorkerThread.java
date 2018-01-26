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
	int startingRowNum = 0;
	Exception e = null;
	
	public IngestionWorkerThread(SparqlEndpointInterface endpoint, DataLoadBatchHandler batchHandler, ArrayList<ArrayList<String>> dataSetRecords, int startingRowNum, OntologyInfo oInfo, Boolean skipChecks) throws Exception{
		
		this.endpoint = endpoint.copy();    // endpoint is not thread-safe as it contains query results
		this.batchHandler = batchHandler;
		this.dataSetRecords = dataSetRecords;
		this.startingRowNum = startingRowNum;
		this.skipChecks = skipChecks;
		this.oInfo = oInfo;
	}
	
	public void run(){
		try {
			ArrayList<NodeGroup> nodeGroupList = this.batchHandler.convertToNodeGroups(this.dataSetRecords, this.startingRowNum, this.skipChecks);
			
			// Ingest everything that succeeded.
			// If nodeGroupList.size() < dataSetRecords.size(), then there was a failure during actual ingestion which is very bad.
			// But that will have generated an exception in this.e or row in batchHandler.failuresEncountered
			// so it is safe to silently ignore it here.
			if (nodeGroupList.size() > 0) {
				String query = NodeGroup.generateCombinedSparqlInsert(nodeGroupList, oInfo);
				this.endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
			}
			
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			this.e = e;
		}
	}
	
	/**
	 * Return the exception thrown during running of this thread, or null
	 * @return
	 */
	public Exception getException() {
		return this.e;
	}
	
}
