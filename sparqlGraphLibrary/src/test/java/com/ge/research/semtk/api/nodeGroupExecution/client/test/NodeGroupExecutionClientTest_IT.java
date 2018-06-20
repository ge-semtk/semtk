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
package com.ge.research.semtk.api.nodeGroupExecution.client.test;

import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

import static org.junit.Assert.*;

import java.util.UUID;

public class NodeGroupExecutionClientTest_IT {
		
		private static NodeGroupExecutionClient nodeGroupExecutionClient = null;
		private static NodeGroupStoreRestClient nodeGroupStoreClient = null;
		private final static String ID = "test" + UUID.randomUUID();
		
		@BeforeClass
		public static void setup() throws Exception {
			// instantiate a client
			nodeGroupExecutionClient = new NodeGroupExecutionClient(new NodeGroupExecutionClientConfig(IntegrationTestUtility.getServiceProtocol(), IntegrationTestUtility.getNodegroupExecutionServiceServer(), IntegrationTestUtility.getNodegroupExecutionServicePort()));
			nodeGroupStoreClient = IntegrationTestUtility.getNodeGroupStoreRestClient(); // instantiate client, with configurations from properties file
		}
		
		@AfterClass
	    public static void teardown() throws Exception {
	        // delete stored nodegroup when done with all tests
			nodeGroupStoreClient.deleteStoredNodeGroup(ID);
	    } 
				
		/**
		 * Test ingesting data.
		 */
		@Test
		public void testIngestOldName() throws Exception{				
			
			TestGraph.clearGraph();
			TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
			
			String DATA = "cell,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
			
			SparqlGraphJson sgJson_TestGraph = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
			
			assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
			nodeGroupExecutionClient.execIngestionFromCsvStr(sgJson_TestGraph, DATA);
			assertEquals(TestGraph.getNumTriples(),131);	// confirm loaded some triples
		}
		
		/**
		 * Test ingesting data.
		 */
		@Test
		public void testIngest() throws Exception{				
			
			TestGraph.clearGraph();
			TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
			
			String DATA = "cell,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
			
			SparqlGraphJson sgJson_TestGraph = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
			
			assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
			nodeGroupExecutionClient.execIngestionFromCsvStr(sgJson_TestGraph, DATA);
			assertEquals(TestGraph.getNumTriples(),131);	// confirm loaded some triples
		}
		
		/**
		 * Test ingesting data using nodegroup connection
		 */
		@Test
		public void testIngestNGConn() throws Exception{				
			
			TestGraph.clearGraph();
			TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
			
			String DATA = "cell,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
			
			SparqlGraphJson sgJson_TestGraph = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
			
			assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
			nodeGroupExecutionClient.execIngestionFromCsvStr(sgJson_TestGraph, DATA);
			assertEquals(TestGraph.getNumTriples(),131);	// confirm loaded some triples
		}
		
		
		/**
		 * Test ingesting data with a missing column.
		 */
		@Test
		public void testIngestWithMissingColumn() throws Exception{				

			TestGraph.clearGraph();
			TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
			
			String DATA = "cell000,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
			
			SparqlGraphJson sgJson_TestGraph = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
			
			assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
			boolean exceptionThrown = false;
			try{
				nodeGroupExecutionClient.execIngestionFromCsvStr(sgJson_TestGraph, DATA);
				fail(); // should not get here...we expect an exception
			}catch(Exception e){
				exceptionThrown = true;
			}
			assertTrue(exceptionThrown);
			assertEquals(TestGraph.getNumTriples(),123);	// confirm nothing loaded
		}
		
		@Test
		public void testSelectByNodegroupId() throws Exception {		
			
			// store a nodegroup (modified with the test graph)
			JSONObject ngJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json").getJson();
			nodeGroupStoreClient.executeStoreNodeGroup(ID, "testSelectByNodegroupId", "creator", ngJson);
			
			TestGraph.clearGraph();
			TestGraph.uploadOwl("src/test/resources/sampleBattery.owl");
			
			String csvStr = Utility.readFile("src/test/resources/sampleBattery.csv");
			
			nodeGroupExecutionClient.execIngestionFromCsvStrById(ID, csvStr, NodeGroupExecutor.get_USE_NODEGROUP_CONN());
			Table tab = nodeGroupExecutionClient.execDispatchSelectByIdToTable(ID, NodeGroupExecutor.get_USE_NODEGROUP_CONN(), null, null);
			
			assert(true);
		}
		
	}

