package com.ge.research.semtk.load;

import java.util.ArrayList;
import java.util.HashMap;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.DataToModelTransformer;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.utility.LocalLogger;

public class IngestionWorkerThread extends Thread {

	OntologyInfo oInfo = null;
	SparqlEndpointInterface endpoint = null;
	DataToModelTransformer dtmtf = null;
	ArrayList<ArrayList<String>> dataToLoad = null;
	Boolean skipChecks = false;
	
	public IngestionWorkerThread(SparqlEndpointInterface endpoint, DataToModelTransformer dtmtf, ArrayList<ArrayList<String>> dataSetRecords, OntologyInfo oInfo, Boolean skipChecks){
		
		this.dtmtf = dtmtf;
		this.dataToLoad = dataSetRecords;
		this.skipChecks = skipChecks;
		this.oInfo = oInfo;
		this.endpoint = endpoint;
	}
	
	public void run(){
		try {
			ArrayList<NodeGroup> subGraphsToLoad = this.dtmtf.convertToNodeGroups(dataToLoad, skipChecks);
			String query = NodeGroup.generateCombinedSparqlInsert(subGraphsToLoad, oInfo);
			this.endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
		}
	}
	
	
}
