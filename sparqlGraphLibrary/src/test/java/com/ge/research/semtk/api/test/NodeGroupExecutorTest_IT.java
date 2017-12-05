/**
 ** Copyright 2017 General Electric Company
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
package com.ge.research.semtk.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class NodeGroupExecutorTest_IT {
	
	private static NodeGroupExecutor nodeGroupExecutor = null;
	private static NodeGroupStoreRestClient ngsrc = null;	
	private static String ngID = "test" + UUID.randomUUID();
	
	private final String DATA =     "cell,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
	private final String DATA_ERR = "cell,size in,lot,material,guy,treatment\ncellA,five,lot5,silver,Smith\n";
	
	@BeforeClass
	public static void setup() throws Exception{
		ngsrc = new NodeGroupStoreRestClient(new NodeGroupStoreConfig(IntegrationTestUtility.getServiceProtocol(), IntegrationTestUtility.getNodegroupStoreServiceServer(),  IntegrationTestUtility.getNodegroupStoreServicePort()));
		DispatchRestClient drc = new DispatchRestClient(new DispatchClientConfig(IntegrationTestUtility.getServiceProtocol(), IntegrationTestUtility.getDispatchServiceServer(), IntegrationTestUtility.getDispatchServicePort()));
		StatusClient stc = new StatusClient(new StatusClientConfig(IntegrationTestUtility.getServiceProtocol(), IntegrationTestUtility.getStatusServiceServer(), IntegrationTestUtility.getStatusServicePort(), "totally fake"));
		ResultsClient rc  = new ResultsClient(new ResultsClientConfig(IntegrationTestUtility.getServiceProtocol(), IntegrationTestUtility.getResultsServiceServer(), IntegrationTestUtility.getResultsServicePort()));
		IngestorRestClient ic = new IngestorRestClient(new IngestorClientConfig(IntegrationTestUtility.getServiceProtocol(), IntegrationTestUtility.getIngestionServiceServer(), IntegrationTestUtility.getIngestionServicePort()));		
		nodeGroupExecutor = new NodeGroupExecutor(ngsrc, drc, rc, stc, ic);
	}
	
	// utility method to store a nodegroup for use in the test
	private void insertNodeGroupToStore(String ngJsonString) throws Exception{
		JSONObject ngJson = Utility.getJsonObjectFromString(ngJsonString);
		ngsrc.deleteStoredNodeGroup(ngID); // delete any existing stored nodegroup with the same ID
		SimpleResultSet sim = ngsrc.executeStoreNodeGroup(ngID, "integration test node group", "Jane Smith", ngJson); // store nodegroup
		assertTrue("failed to store node group: " + sim.getRationaleAsString("||"), sim.getSuccess());		// check success
	}
	
	
	/**
	 * Test ingesting data by nodegroup 
	 */
	@Test
	public void testIngestByNodegroup() throws Exception{				
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		
		// get a nodegroup to execute
		SparqlGraphJson sparqlGraphJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
		
		// check number of triples before insert
		assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
		
		// do the insert (using full nodegroup)
		nodeGroupExecutor.ingestFromTemplateIdAndCsvString(sparqlGraphJson.getSparqlConn(), sparqlGraphJson, DATA);
		
		// check number of triples after insert
		assertEquals(TestGraph.getNumTriples(),131);	// confirm loaded some triples
	}
	
	/**
	 * Test ingesting data by nodegroup ID
	 */
	@Test
	public void testIngestByNodegroupID() throws Exception{				
		
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		
		// store a nodegroup to execute
		SparqlGraphJson sparqlGraphJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
		insertNodeGroupToStore(sparqlGraphJson.getJson().toJSONString());
		
		// check number of triples before insert
		assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
		
		// do the insert (using nodegroup ID)
		nodeGroupExecutor.ingestFromTemplateIdAndCsvString(sparqlGraphJson.getSparqlConn(), ngID, DATA);
		
		// check number of triples after insert
		assertEquals(TestGraph.getNumTriples(),131);	// confirm loaded some triples
	}
	
	/**
	 * Test ingesting data by nodegroup ID
	 */
	@Test
	public void testIngestByNodegroupIDError() throws Exception{		
		
		String data = Utility.readFile("src/test/resources/sampleBatteryBadColor.csv");
		String owlPath = "src/test/resources/sampleBattery.owl";
		
		TestGraph.clearGraph();
		TestGraph.uploadOwl(owlPath);
		
		// store a nodegroup to execute
		SparqlGraphJson sparqlGraphJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		insertNodeGroupToStore(sparqlGraphJson.getJson().toJSONString());
		
		// do the insert (using nodegroup ID)
		RecordProcessResults retval = nodeGroupExecutor.ingestFromTemplateIdAndCsvString(sparqlGraphJson.getSparqlConn(), ngID, data);
		// check number of triples after insert
		assertEquals(retval.getFailuresEncountered(), 1);
		assert(retval.getResultsJSON().containsKey("errorTable"));
	}
	
}
