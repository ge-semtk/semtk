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
package com.ge.research.semtk.api.nodeGroupExecution.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class NodeGroupExecutorTest_IT {
	
	private static NodeGroupExecutor nodeGroupExecutor = null;
	private static NodeGroupStoreRestClient nodeGroupStoreRestClient = null;	
	private static String ngID = "test" + UUID.randomUUID();
	private static final String CREATOR = "JUnit NodeGroupExecutorTest_IT";
	private final String DATA =     "cell,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();	
		nodeGroupStoreRestClient = IntegrationTestUtility.getNodeGroupStoreRestClient();
		nodeGroupExecutor = IntegrationTestUtility.getNodegroupExecutor();
		
		IntegrationTestUtility.cleanupNodegroupStore(nodeGroupStoreRestClient, CREATOR);

	}
	
	// utility method to store a nodegroup for use in the test
	private void insertNodeGroupToStore(String ngJsonString) throws Exception{
		JSONObject ngJson = Utility.getJsonObjectFromString(ngJsonString);
		
		nodeGroupStoreRestClient.deleteStoredNodeGroupIfExists(ngID); // delete any existing stored nodegroup with the same ID
		SimpleResultSet sim = nodeGroupStoreRestClient.executeStoreNodeGroup(ngID, "integration test node group", CREATOR, ngJson); // store nodegroup
		assertTrue("failed to store node group: " + sim.getRationaleAsString("||"), sim.getSuccess());		// check success
	}
	
	@AfterClass
    public static void teardown() throws Exception {
        // delete stored nodegroup when done with all tests
		nodeGroupStoreRestClient.deleteStoredNodeGroup(ngID);
    } 
	
	/**
	 * Test ingesting data by nodegroup 
	 */
	@Test
	public void testIngestByNodegroup() throws Exception{				
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "/testTransforms.owl");
		
		// get a nodegroup to execute
		SparqlGraphJson sparqlGraphJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
		
		// check number of triples before insert
		assertEquals(123, TestGraph.getNumTriples());	// get count before loading
		
		// do the insert (using full nodegroup)
		RecordProcessResults res = nodeGroupExecutor.ingestFromTemplateAndCsvString(sparqlGraphJson.getSparqlConn(), sparqlGraphJson, DATA); 
		assertTrue("Ingest failed:" + res.getRationaleAsString(" "), res.getSuccess());
		assertEquals("Ingest has failures", 0, res.getFailuresEncountered());
		
		// check number of triples after insert
		assertEquals(131, TestGraph.getNumTriples());	// confirm loaded some triples
	}
	
	/**
	 * Test ingesting data by nodegroup ID
	 */
	@Test
	public void testIngestByNodegroupID() throws Exception{				
		
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "/testTransforms.owl");
		
		// store a nodegroup to execute
		SparqlGraphJson sparqlGraphJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
		insertNodeGroupToStore(sparqlGraphJson.getJson().toJSONString());
		
		// check number of triples before insert
		assertEquals(123, TestGraph.getNumTriples());	// get count before loading
		
		// do the insert (using nodegroup ID)
		RecordProcessResults res = nodeGroupExecutor.ingestFromTemplateIdAndCsvString(sparqlGraphJson.getSparqlConn(), ngID, DATA);
		
		assertTrue("Ingest failed", res.getSuccess());
		assertEquals("Ingest has failures", 0, res.getFailuresEncountered());
		// check number of triples after insert
		assertEquals(131, TestGraph.getNumTriples());	// confirm loaded some triples
	}
	
	@Test
	public void testIsUseNodegroupConn() throws Exception {
		boolean b = NodeGroupExecutor.isUseNodegroupConn(NodeGroupExecutor.get_USE_NODEGROUP_CONN());
		assertTrue(b);
	}
	
	/**
	 * Test ingesting data by nodegroup ID, use nodegroup's conn
	 */
	@Test
	public void testIngestByNodegroupIDConn() throws Exception{				
		
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "/testTransforms.owl");
		
		// store a nodegroup to execute
		SparqlGraphJson sparqlGraphJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
		insertNodeGroupToStore(sparqlGraphJson.getJson().toJSONString());
		
		// check number of triples before insert
		assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
		
		// do the insert (using nodegroup ID)
		RecordProcessResults res = nodeGroupExecutor.ingestFromTemplateIdAndCsvString(NodeGroupExecutor.get_USE_NODEGROUP_CONN(), ngID, DATA);
		assertTrue(res.getSuccess());
		// check number of triples after insert
		assertEquals(TestGraph.getNumTriples(),131);	// confirm loaded some triples
	}
	
	/**
	 * Test ingesting data by nodegroup ID
	 */
	@Test
	public void testIngestByNodegroupIDError() throws Exception{		
		
		String data = Utility.readFile("src/test/resources/sampleBatteryBadColor.csv");
		
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
		
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
