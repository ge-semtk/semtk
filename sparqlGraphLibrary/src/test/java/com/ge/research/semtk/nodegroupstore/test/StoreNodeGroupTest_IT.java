package com.ge.research.semtk.nodegroupstore.test;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.nodegroupStore.SparqlQueries;
import com.ge.research.semtk.services.nodegroupStore.StoreNodeGroup;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
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
	
	/****
	 *   Utility code for making a copy of the nodegroup store and adding a nodegroup to it
	 * 
	@Test
	public void storeNgTEMPdemoNodegroup() throws Exception {
		
		// make a copy of node group store graph in virtuoso
		// upload to store: a demoNodegroup
	
		// get a nodegroup to ingest
		String ngJsonStr = Utility.readFile("C:\\Users\\200001934\\Desktop\\Temp\\demoNodegroup  ISWC\\sparql_graph.json");
		SparqlGraphJson sgJson = new SparqlGraphJson(ngJsonStr);
		
		// build the data
		String creationDateString = Utility.getSPARQLCurrentDateString(); 
		String data = SparqlQueries.getHeaderRow() + StoreNodeGroup.getInsertRow(sgJson.getJson(), sgJson.getSparqlConnJson(), "demoNodegroup", "ISWC demo", "pecuddihy@gmail.com", creationDateString);
		
		// get the nodegroup ingestion template
		JSONObject templateJson = Utility.getResourceAsJson(this, "/nodegroups/store.json");
		SparqlGraphJson templateSgJson = new SparqlGraphJson(templateJson);
		
		// swap the connection
		SparqlConnection conn = new SparqlConnection();
		conn.addDataInterface("virtuoso", "http://vesuvius-test.crd.ge.com:2420", "http://research.ge.com/knowledge/prefab/data2", "dba", "dba");
		conn.addModelInterface("virtuoso", "http://vesuvius-test.crd.ge.com:2420", "http://research.ge.com/knowledge/prefab/model2", "dba", "dba");
		conn.setDomain("http://");
		conn.setName("TestPrefab");
		templateSgJson.setSparqlConn(conn);
		
		// clear model and data graphs
		GeneralResultSet resultSet = conn.getModelInterface(0).executeQueryAndBuildResultSet("clear all", SparqlResultTypes.CONFIRM);
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
		resultSet = conn.getDataInterface(0).executeQueryAndBuildResultSet("clear all", SparqlResultTypes.CONFIRM);
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
		
		// upload owl to model connection
		SparqlEndpointInterface sei = conn.getModelInterface(0);
		Path path = Paths.get("../sparqlGraphLibrary/src/main/Semantics/OwlModels/prefabNodeGroup.owl");
		byte[] owl = Files.readAllBytes(path);
		resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
		
		// ingest nodegroup to data connection
		Dataset ds = new CSVDataset(data, true);
		DataLoader dl = new DataLoader(templateSgJson, 1, ds, "dba", "dba");
		int recordsProcessed = dl.importData(true); 
		
		assertEquals(1, recordsProcessed);
	}
	***/
}
