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


package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import java.io.File;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.connutil.EndpointNotFoundException;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.OntologyInfoClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.PredicateStats;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class OntologyInfoClientTest_IT {
	
	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	
	private final String JOB_ID1 = "client_test_job_1";
	
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		SERVICE_PROTOCOL = IntegrationTestUtility.get("protocol");
		SERVICE_SERVER = IntegrationTestUtility.get("ontologyinfoservice.server");
		SERVICE_PORT = IntegrationTestUtility.getInt("ontologyinfoservice.port");
	}
	
	private OntologyInfoClient getClient() throws Exception {
		OntologyInfoClientConfig config = new OntologyInfoClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT);
		return new OntologyInfoClient(config);
	}
	
	@Test
	public void testGetOInfoFromConn() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/sampleBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		SparqlConnection conn = sgJson.getSparqlConn();
		
		OntologyInfoClient client = this.getClient();
		client.uncacheChangedModel(conn);
		OntologyInfo oInfo = client.getOntologyInfo(conn);
		
		assertEquals("uploadOwl didn't result in correct number of classes", 3, oInfo.getNumberOfClasses());		
	}
	
	@Test
	public void testUncacheChangedModel() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/sampleBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		SparqlConnection conn = sgJson.getSparqlConn();
		
		OntologyInfoClient client = this.getClient();
		client.uncacheChangedModel(conn);
		OntologyInfo oInfo = client.getOntologyInfo(conn);
		assertEquals("uploadOwl didn't result in correct number of classes", 3, oInfo.getNumberOfClasses());
		
		// clear graph without re-caching should give old results
		TestGraph.getSei().clearGraph();
		oInfo = client.getOntologyInfo(conn);
		assertEquals("clear graph took effect without updating the cache", 3, oInfo.getNumberOfClasses());
		
		// clear cache and the clearGraph should have taken effect
		client.uncacheChangedModel(conn);
		oInfo = client.getOntologyInfo(conn);
		assertEquals("uncacheChangedModel didn't clear the cache", 0, oInfo.getNumberOfClasses());
		
	}
	
	@Test
	public void testUncacheConn() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/sampleBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		SparqlConnection conn = sgJson.getSparqlConn();
		
		OntologyInfoClient client = this.getClient();
		client.uncacheOntology(conn);
		OntologyInfo oInfo = client.getOntologyInfo(conn);
		assertEquals("uploadOwl didn't result in correct number of classes", 3, oInfo.getNumberOfClasses());
		
		// clear graph without re-caching should give old results
		TestGraph.getSei().clearGraph();
		oInfo = client.getOntologyInfo(conn);
		assertEquals("clear graph took effect without updating the cache", 3, oInfo.getNumberOfClasses());
		
		// clear cache and the clearGraph should have taken effect
		client.uncacheOntology(conn);
		oInfo = client.getOntologyInfo(conn);
		assertEquals("uncacheChangedModel didn't clear the cache", 0, oInfo.getNumberOfClasses());
		
	}
	
	@Test
	public void testCacheThroughQueryClient() throws Exception {
		TestGraph.clearGraph();
		
		// get ontology and test conn
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		SparqlConnection conn = sgJson.getSparqlConn();
		
		// create sparql query client
		SparqlQueryAuthClientConfig qConfig = new SparqlQueryAuthClientConfig(
				IntegrationTestUtility.get("protocol"), 
				IntegrationTestUtility.get("sparqlqueryservice.server"), 
				IntegrationTestUtility.getInt("sparqlqueryservice.port"), 
				"sparqlQueryService/uploadOwl", 
				conn.getModelInterface(0).getServerAndPort(), 
				conn.getModelInterface(0).getServerType(), 
				conn.getModelInterface(0).getDataset(),
				IntegrationTestUtility.get("sparqlendpoint.username"),
				IntegrationTestUtility.get("sparqlendpoint.password"));
		SparqlQueryClient qClient = new SparqlQueryClient(qConfig);
		
		// make sure original oInfo is empty
		OntologyInfoClient client = this.getClient();
		client.uncacheChangedModel(conn);
		OntologyInfo oInfo = client.getOntologyInfo(conn);
		assertEquals("original model is not empty", 0, oInfo.getNumberOfClasses());
		
		// load through query client.  should update the oInfo client cache.
		SimpleResultSet res = qClient.uploadOwl(new File("src/test/resources/sampleBattery.owl"));
		res.throwExceptionIfUnsuccessful();
		oInfo = client.getOntologyInfo(conn);
		assertEquals("query client uploadOwl didn't update oInfo cache", 3, oInfo.getNumberOfClasses());	
	}
	
	@Test
	public void testGetPredicateStats() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this.getClass(), "sampleBattery.owl");
		PredicateStats stats = TestGraph.getPredicateStats();		
	}
	
	@Test
	public void testGetCardinalityViolations() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "Cardinality.owl");	// load Cardinal.sadl : the classes, restrictions, and instance data	
		String jobId;
		CSVDataset result;
		
		// no max rows
		jobId = this.getClient().execGetCardinalityViolations(TestGraph.getSparqlConn(), -1, false);
		IntegrationTestUtility.getStatusClient(jobId).waitForCompletionSuccess();
		result = IntegrationTestUtility.getResultsClient().getTableResultsCSV(jobId, 999999);
		assertEquals(result.getNumRows(), 36);
		
		// max rows
		jobId = this.getClient().execGetCardinalityViolations(TestGraph.getSparqlConn(), 10, false);
		IntegrationTestUtility.getStatusClient(jobId).waitForCompletionSuccess();
		result = IntegrationTestUtility.getResultsClient().getTableResultsCSV(jobId, 999999);
		assertEquals(result.getNumRows(), 10);
		
		// max rows and concise format
		jobId = this.getClient().execGetCardinalityViolations(TestGraph.getSparqlConn(), -1, true);
		IntegrationTestUtility.getStatusClient(jobId).waitForCompletionSuccess();
		result = IntegrationTestUtility.getResultsClient().getTableResultsCSV(jobId, 999999);
		assertEquals(result.getNumRows(), 22);
	}
}
