/**
 ** Copyright 2023 General Electric Company
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
package com.ge.research.semtk.utility.test;

import static org.junit.Assert.*; 
import static org.junit.Assume.*;

import java.io.BufferedReader;
import java.io.File;

import org.apache.jena.shacl.validation.Severity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.config.test.ManifestConfigTest_IT;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.services.client.UtilityClient;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.validate.ShaclExecutor;
import com.ge.research.semtk.validate.ShaclBasedGarbageCollector;
import com.ge.research.semtk.validate.test.ShaclExecutorTest_IT;

public class UtilityClientTest_IT {

	// not extending YamlConfigTest because need to match ingestion package footprint
	SparqlEndpointInterface modelFallbackSei = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/model"));
	SparqlEndpointInterface dataFallbackSei = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/data"));
	SparqlEndpointInterface dataSeiFromYaml = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/data"));  // for this package, same as footprint/fallback
	SparqlEndpointInterface defaultGraphSei = TestGraph.getSei(SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);

	public UtilityClientTest_IT() throws Exception {
		super();
	}

	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	private static UtilityClient client = null;

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		SERVICE_PROTOCOL = IntegrationTestUtility.get("protocol");
		SERVICE_SERVER = IntegrationTestUtility.get("utilityservice.server");
		SERVICE_PORT = IntegrationTestUtility.getInt("utilityservice.port");
		client = new UtilityClient(new RestClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, "fake"));
	}

	@Test
	public void testLoadIngestionPackage() throws Exception {

		try {

			// this ingestion package copies to the default graph - only allow to run on Fuseki
			assumeTrue("Skipping test: only use default graph on Fuseki triplestore", TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.FUSEKI_SERVER));

			reset();

			boolean clear = true;
			BufferedReader reader = client.execLoadIngestionPackage(TestGraph.getZipAndUniquifyJunitGraphs(this, "/config/IngestionPackage.zip"), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), clear, modelFallbackSei.getGraph(), dataFallbackSei.getGraph());
			String response = Utility.readToString(reader);

			// check the response stream
			assertTrue("Bad response:\n" + response, response.contains("INFO: Loading manifest for 'Entity Resolution'..."));
			assertFalse("Bad response:\n" + response, response.contains("ERROR:"));
			assertTrue(response.contains("INFO: Loading manifest for 'RACK ontology'..."));
			assertTrue(response.matches("(.*)INFO: Clear graph http://junit/(.*)/rack001/model(.*)"));
			assertTrue(response.matches("(.*)INFO: Clear graph http://junit/(.*)/rack001/data(.*)"));
			assertTrue(response.matches("(.*)INFO: Load OWL OwlModels(.*)AGENTS.owl(.*)"));
			assertTrue(response.contains("INFO: Store nodegroups"));
			assertTrue(response.contains("INFO: Stored: JUNIT query Files of a Given Format"));
			assertTrue(response.matches("(.*)INFO: Load CSV Package-1(.*)PROV_S_ACTIVITY1.csv as class http://arcos.rack/PROV-S#ACTIVITY(.*)"));
			assertTrue(response.contains("WARNING: Input is missing these columns:"));
			assertTrue(response.matches("(.*)INFO: Copy graph http://junit/(.*)/auto/rack001/data to uri://DefaultGraph(.*)"));
			assertTrue(response.contains("INFO: Perform entity resolution"));
			assertTrue(response.contains("INFO: Load complete"));

			// check the counts
			assertEquals("Number of triples loaded to model graph", ManifestConfigTest_IT.NUM_EXPECTED_TRIPLES_MODEL, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", ManifestConfigTest_IT.NUM_EXPECTED_TRIPLES_DATA, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", ManifestConfigTest_IT.NUM_EXPECTED_TRIPLES_MODEL + ManifestConfigTest_IT.NUM_EXPECTED_TRIPLES_DATA + ManifestConfigTest_IT.NUM_NET_CHANGE_ENTITY_RESOLUTION_TRIPLES, defaultGraphSei.getNumTriples());
			assertEquals("Number of nodegroups", ManifestConfigTest_IT.NUM_EXPECTED_NODEGROUPS, IntegrationTestUtility.countItemsInStoreByCreator("junit"));

		}catch(Exception e){
			throw e;
		}finally {
			reset();
		}
	}

	// Test error conditions
	@Test
	public void testLoadIngestionPackage_errorConditions() throws Exception {
		String response;

		// not a zip file
		response = Utility.readToString(client.execLoadIngestionPackage(Utility.getResourceAsTempFile(this,"/animalQuery.json"), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, modelFallbackSei.getGraph(), dataFallbackSei.getGraph()));
		assertTrue(response.contains("ERROR: This endpoint only accepts ingestion packages in zip file format"));

		// contains no top-level manifest.yaml
		response = Utility.readToString(client.execLoadIngestionPackage(Utility.getResourceAsTempFile(this,"/config/IngestionPackage-NoManifest.zip"), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, modelFallbackSei.getGraph(), dataFallbackSei.getGraph()));
		assertTrue(response.contains("ERROR: Cannot find a top-level manifest"));
	}

	@Test
	public void testGetShaclResults() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "DeliveryBasketExample.owl");

		File ttlFile = Utility.getResourceAsTempFile(this, "DeliveryBasketExample-shacl.ttl");
		String jobId;
		JSONObject resultsJson;
		JSONObject expectedJson = Utility.getResourceAsJson(this, "DeliveryBasketExample-shacl-results.json");
		
		// info level
		jobId = client.execGetShaclResults(ttlFile, TestGraph.getSparqlConn(), Severity.Info);
		IntegrationTestUtility.getStatusClient(jobId).waitForCompletion(jobId);
		resultsJson = IntegrationTestUtility.getResultsClient().execGetBlobResult(jobId);
		assertEquals(((JSONArray)resultsJson.get(ShaclExecutor.JSON_KEY_ENTRIES)).size(), ((JSONArray)expectedJson.get(ShaclExecutor.JSON_KEY_ENTRIES)).size());
		Utility.equals(resultsJson, expectedJson);	
		
		// violation level
		jobId = client.execGetShaclResults(ttlFile, TestGraph.getSparqlConn(), Severity.Violation);
		IntegrationTestUtility.getStatusClient(jobId).waitForCompletion(jobId);
		resultsJson = IntegrationTestUtility.getResultsClient().execGetBlobResult(jobId);
		expectedJson = ShaclExecutorTest_IT.removeEntriesBelowSeverityLevel(expectedJson, Severity.Violation);
		assertEquals(((JSONArray)resultsJson.get(ShaclExecutor.JSON_KEY_ENTRIES)).size(), ((JSONArray)expectedJson.get(ShaclExecutor.JSON_KEY_ENTRIES)).size());
		Utility.equals(resultsJson, expectedJson);	
		
		try {
			client.execGetShaclResults(new File("src/nonexistent.ttl"), TestGraph.getSparqlConn(), Severity.Info);
			fail(); // should not get here
		}catch(Exception e) {
			assert(e.getMessage().contains("File does not exist"));
		}
		TestGraph.clearGraph();
	}

	@Test
	public void testShaclBasedGarbageCollection() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "DeliveryBasketExample.owl");
		File ttlFile = Utility.getResourceAsTempFile(this, "DeliveryBasketExample-GarbageCollection-shacl.ttl");
		String jobId = client.execPerformShaclBasedGarbageCollection(ttlFile, TestGraph.getSparqlConn());
		IntegrationTestUtility.getStatusClient(jobId).waitForCompletionSuccess();
		CSVDataset deletedUrisDataset = IntegrationTestUtility.getResultsClient().getTableResultsCSV(jobId, 99999);
		assertEquals(deletedUrisDataset.getColumnIndex(ShaclBasedGarbageCollector.HEADER_DELETED_INSTANCES), 0);
		assertEquals(deletedUrisDataset.getNumRows(), 10);  // 10 URIs garbage collected
	}
	
	// clear graphs and nodegroup store
	private void reset() throws Exception {
		IntegrationTestUtility.clearGraph(modelFallbackSei);
		IntegrationTestUtility.clearGraph(dataFallbackSei);
		IntegrationTestUtility.clearGraph(dataSeiFromYaml);
		IntegrationTestUtility.clearGraph(defaultGraphSei);
		IntegrationTestUtility.cleanupNodegroupStore("junit");
	}

}
