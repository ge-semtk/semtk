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


package com.ge.research.semtk.load.client.test;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.simple.*;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.VirtuosoSparqlEndpointInterface;
import com.ge.research.semtk.resultSet.TableResultSet;


public class IngestorRestClientTest_IT {
	
	private static IngestorRestClient irc = null;
	private static final String DATA = "cell,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
	
	private static SparqlGraphJson sgJson_TestGraph;
	private static String sgJsonString_TestGraph;
	
	@BeforeClass
	public static void setup() throws Exception {
		String serviceProtocol = IntegrationTestUtility.getServiceProtocol();
		String ingestionServiceServer = IntegrationTestUtility.getIngestionServiceServer();
		int ingestionServicePort = IntegrationTestUtility.getIngestionServicePort();
		irc   = new IngestorRestClient(new IngestorClientConfig(serviceProtocol, ingestionServiceServer, ingestionServicePort));

		sgJson_TestGraph = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
		sgJsonString_TestGraph = sgJson_TestGraph.getJson().toJSONString();   // template as a string
	}
	
	
	/**
	 * Test ingesting data.
	 */
	@Test
	public void testIngest() throws Exception{				
		
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		
		assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
		irc.execIngestionFromCsv(sgJsonString_TestGraph, DATA);	// load data
		assertEquals(TestGraph.getNumTriples(),131);	// confirm loaded some triples
	}
	
	/**
	 * Test ingesting data with overriding the SPARQL connection.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testIngestWithConnectionOverride() throws Exception{
		
		// clear the test graph (will confirm later that data isn't loaded here)
		TestGraph.clearGraph();

		// get an override SPARQL connection, by getting the TestGraph dataset and appending "OTHER"
		// TODO could not find a clean way to add this functionality to TestGraph for reusability - maybe in the future
		JSONObject sparqlConnJson = sgJson_TestGraph.getSparqlConn().toJson();  // original TestGraph sparql conn 
				
		SparqlConnection sparqlConnectionOverride = new SparqlConnection(sparqlConnJson.toJSONString()); // get the connection object
		String otherDataset = sparqlConnectionOverride.getDefaultQueryInterface().getGraph() + "OTHER";
		sparqlConnectionOverride.getDataInterface(0).setGraph(otherDataset);
		sparqlConnectionOverride.getModelInterface(0).setGraph(otherDataset);
		
		// clear the override graph and upload OWL to it (else the test load will fail)
		SparqlEndpointInterface seiOverride = new VirtuosoSparqlEndpointInterface(TestGraph.getSparqlServer(), otherDataset, TestGraph.getUsername(), TestGraph.getPassword());
		seiOverride.executeQueryAndBuildResultSet("clear graph <" + otherDataset + ">", SparqlResultTypes.CONFIRM);
		seiOverride.executeAuthUploadOwl(Files.readAllBytes(Paths.get("src/test/resources/testTransforms.owl")));
		
		// get count of triples in override graph (after OWL load, but before data load)
		JSONObject resultJson = seiOverride.executeQuery(Utility.SPARQL_QUERY_TRIPLE_COUNT, SparqlResultTypes.TABLE);			
		Table table = Table.fromJson((JSONObject)resultJson.get(TableResultSet.TABLE_JSONKEY));		
		assertEquals(table.getCell(0,0), "123");	// confirm that data was loaded to the override graph
		
		// load the data
		irc.execIngestionFromCsv(sgJsonString_TestGraph, DATA, sparqlConnectionOverride.toString());
		
		// confirm 0 triples loaded to test graph
		assertEquals(TestGraph.getNumTriples(),0);	
		
		// confirm triples loaded to override graph
		resultJson = seiOverride.executeQuery(Utility.SPARQL_QUERY_TRIPLE_COUNT, SparqlResultTypes.TABLE);			
		table = Table.fromJson((JSONObject)resultJson.get(TableResultSet.TABLE_JSONKEY));		
		assertEquals(table.getCell(0,0), "131");	// confirm that data was loaded to the override graph
	}
	
}
