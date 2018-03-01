package com.ge.research.semtk.nodegroupstore.test;

import static org.junit.Assert.*;

import java.util.Collections;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.test.UtilityTest;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.services.nodegroupStore.SparqlQueries;
import com.ge.research.semtk.services.nodegroupStore.StoreNodeGroup;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class StoreNodeGroupTest_IT {

	@Test
	public void storeNg() throws Exception {
		// test storing a nodegroup without using any clients.  Use ingestor code directly to the triple-store
		// make the nodegroup artificially huge
	
		// setup TestGraph
		TestGraph.clearGraph();
		TestGraph.uploadOwl("../sparqlGraphLibrary/src/main/Semantics/OwlModels/prefabNodeGroup.owl");
		
		// get a nodegroup to ingest
		String ngJsonStr = Utility.readFile("../sparqlGraphLibrary/src/test/resources/lookupCellDuraBattery.json");
		SparqlGraphJson sgJson = new SparqlGraphJson(ngJsonStr);
		String creationDateString = Utility.getSPARQLCurrentDateString(); 
		
		// artificially make the nodegroup giant
		JSONObject sgJsonJson = sgJson.getJson();
		String x1000s = String.join("", Collections.nCopies(80000, "x"));
		sgJsonJson.put("add1000s", x1000s);
		
		// test SparqlQueries.getHeaderRow() and StoreNodeGroup.getInsertRow()
		// build the data
		String data = SparqlQueries.getHeaderRow() + StoreNodeGroup.getInsertRow(sgJson.getJson(), sgJson.getSparqlConnJson(), "TEST_id", "TEST_comments", "TEST_creator", creationDateString);
		
		// get the nodegroup ingestion template
		JSONObject templateJson = Utility.getResourceAsJson(this, "/nodegroups/store.json");
		SparqlGraphJson templateSgJson = new SparqlGraphJson(templateJson);
		templateSgJson.setSparqlConn(TestGraph.getSparqlConn("http://"));
		
		// ingest
		Dataset ds = new CSVDataset(data, true);
		DataLoader dl = new DataLoader(templateSgJson, 1, ds, IntegrationTestUtility.getSparqlServerUsername(), IntegrationTestUtility.getSparqlServerPassword());
		int recordsProcessed = dl.importData(true); 
		
		assertEquals(1, recordsProcessed);
	}

}
