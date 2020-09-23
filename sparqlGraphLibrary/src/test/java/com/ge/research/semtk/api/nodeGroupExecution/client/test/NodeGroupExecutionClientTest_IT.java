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
import static org.junit.Assume.assumeTrue;


import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.LoadTracker;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

import static org.junit.Assert.*;

import java.util.UUID;

public class NodeGroupExecutionClientTest_IT {
		
		private static NodeGroupExecutionClient nodeGroupExecutionClient = null;
		private static NodeGroupStoreRestClient nodeGroupStoreClient = null;
		private final static String ID = "test" + UUID.randomUUID();
		private final static String CREATOR = "JUnit NodeGroupExecutorTest_IT";

		@BeforeClass
		public static void setup() throws Exception {
			IntegrationTestUtility.authenticateJunit();
			// instantiate a client
			nodeGroupExecutionClient = new NodeGroupExecutionClient(new NodeGroupExecutionClientConfig(IntegrationTestUtility.get("protocol"), IntegrationTestUtility.get("nodegroupexecution.server"), IntegrationTestUtility.getInt("nodegroupexecution.port")));
			nodeGroupStoreClient = IntegrationTestUtility.getNodeGroupStoreRestClient(); // instantiate client, with configurations from properties file
			
			
			IntegrationTestUtility.cleanupNodegroupStore(nodeGroupStoreClient, CREATOR);
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
			TestGraph.uploadOwlResource(this, "/testTransforms.owl");
			
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
			TestGraph.uploadOwlResource(this, "/testTransforms.owl");
			
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
			TestGraph.uploadOwlResource(this, "/testTransforms.owl");
			
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
			TestGraph.uploadOwlResource(this, "/testTransforms.owl");
			
			String DATA = "cell000,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
			
			SparqlGraphJson sgJson_TestGraph = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
			
			assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
			
			nodeGroupExecutionClient.execIngestionFromCsvStr(sgJson_TestGraph, DATA);
			// simply confirming there's no exception
		}
		
		@Test
		public void testSelectByNodegroupId() throws Exception {		
			
			// store a nodegroup (modified with the test graph)
			JSONObject ngJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json").getJson();
			try {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			} catch (Exception e) {
			}
			nodeGroupStoreClient.executeStoreNodeGroup(ID, "testSelectByNodegroupId", CREATOR, ngJson);
			
			TestGraph.clearGraph();
			TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
			
			String csvStr = Utility.readFile("src/test/resources/sampleBattery.csv");
			
			nodeGroupExecutionClient.execIngestionFromCsvStrById(ID, csvStr, NodeGroupExecutor.get_USE_NODEGROUP_CONN());
			
			// test the test
			OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());
			
			Table tab = nodeGroupExecutionClient.execDispatchSelectByIdToTable(ID, NodeGroupExecutor.get_USE_NODEGROUP_CONN(), null, null);
			
			assert(true);
		}
		
		@Test
		public void testIngestByNodegroupIdAsyncAndDelete() throws Exception {	
			// also tests waitForPercentOrMsec
			// also tests getJobStatus
			
			// store a nodegroup (modified with the test graph)
			JSONObject ngJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json").getJson();
			try {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			} catch (Exception e) {
			}
			nodeGroupStoreClient.executeStoreNodeGroup(ID, "testSelectByNodegroupId", CREATOR, ngJson);
			
			try {
				TestGraph.clearGraph();
				TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
				
				String csvStr = Utility.readFile("src/test/resources/sampleBattery.csv");
				
				// perform ingestion
				String jobId = nodeGroupExecutionClient.execIngestFromCsvStringsByIdAsync(ID, csvStr, NodeGroupExecutor.get_USE_NODEGROUP_CONN());
				int percent = nodeGroupExecutionClient.waitForPercentOrMsec(jobId, 60000, 100);
				assertTrue("Ingestion didn't finish in 60 seconds", percent == 100);
				assertTrue("Ingestion failed", nodeGroupExecutionClient.getJobSuccess(jobId));
				
				// select back the data post ingest
				Table tab = nodeGroupExecutionClient.execDispatchSelectByIdToTable(ID, NodeGroupExecutor.get_USE_NODEGROUP_CONN(), null, null);
				assertTrue("Select failed to retrieve ingested data", tab.getNumRows() == 4);
				
				// delete some by nodegroup
				NodeGroup delNg = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery_DeleteSimple.json").getNodeGroup();
				tab = nodeGroupExecutionClient.dispatchDeleteFromNodeGroup(delNg, TestGraph.getSparqlConn(), null, null);
				
				// select back the data post delete
				tab = nodeGroupExecutionClient.execDispatchSelectByIdToTable(ID, NodeGroupExecutor.get_USE_NODEGROUP_CONN(), null, null);
				assertTrue("Data remains after delete", tab.getNumRows() == 0);
				
			} finally {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			}
		}
		
		@Test
		public void testIngestByNodegroupAndDeleteById() throws Exception {	
			// also tests waitForPercentOrMsec
			// also tests getJobStatus
			
			// store a nodegroup (modified with the test graph)
			SparqlGraphJson sgjSelectInsert = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			SparqlGraphJson sgjDelete = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery_DeleteSimple.json");

			try {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			} catch (Exception e) {
			}
			
			nodeGroupStoreClient.executeStoreNodeGroup(ID, "sampleBattery_deleteSimple", CREATOR, sgjDelete.toJson());
			
			try {
				TestGraph.clearGraph();
				TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
				
				String csvStr = Utility.readFile("src/test/resources/sampleBattery.csv");
				
				// perform ingestion
				RecordProcessResults res = nodeGroupExecutionClient.execIngestionFromCsvStr(sgjSelectInsert, csvStr);
				assertTrue("Ingestion failed", res.getSuccess());
				
				// select back the data post ingest
				Table tab = nodeGroupExecutionClient.dispatchSelectFromNodeGroup(sgjSelectInsert, null, null, null);
				assertTrue("Select failed to retrieve ingested data", tab.getNumRows() == 4);
				 
				// delete some by nodegroup ID
				String success = nodeGroupExecutionClient.dispatchDeleteByIdToSuccessMsg(ID, NodeGroupExecutor.get_USE_NODEGROUP_CONN(), null, null);
				
				// select back the data post delete
				tab = nodeGroupExecutionClient.dispatchSelectFromNodeGroup(sgjSelectInsert, null, null, null);
				assertTrue("Data remains after delete", tab.getNumRows() == 0);
				
			} finally {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			}
		}
		
		@Test
		public void testIngestByNodegroupByIdAsync() throws Exception {	
			// tests "dispatch" version of call 
			
			nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			SparqlGraphJson sgjSelectInsert = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			nodeGroupStoreClient.executeStoreNodeGroup(ID, "sampleBattery_deleteSimple", CREATOR, sgjSelectInsert.toJson());
				
			try {
				TestGraph.clearGraph();
				TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
				
				String csvStr = Utility.readFile("src/test/resources/sampleBattery.csv");
				
				// perform ingestion
				nodeGroupExecutionClient.dispatchIngestFromCsvStringsByIdSync(ID, csvStr);
				
				// select back the data post ingest
				Table tab = nodeGroupExecutionClient.dispatchSelectFromNodeGroup(sgjSelectInsert, null, null, null);
				assertTrue("Select failed to retrieve ingested data", tab.getNumRows() == 4);
				
			} finally {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			}
		}
		
		@Test
		public void testIngestByNodegroupByIdAsyncError() throws Exception {	
			// tests "dispatch" version of call 
			
			nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			SparqlGraphJson sgjSelectInsert = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			nodeGroupStoreClient.executeStoreNodeGroup(ID, "sampleBattery_deleteSimple", CREATOR, sgjSelectInsert.toJson());
				
			try {
				TestGraph.clearGraph();
				TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
				
				String csvStr = Utility.readFile("src/test/resources/sampleBattery.csv");
				csvStr += "\ngarbage,garbage,";
				
				// perform ingestion
				try {
					nodeGroupExecutionClient.dispatchIngestFromCsvStringsByIdSync(ID, csvStr);
					fail("Expected ingest exception did not occur");
				} catch (Exception e) {
					
				}
				
				
			} finally {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			}
		}
		
		@Test
		public void testSelectByNodegroupIdSync() throws Exception {		
			
			// store a nodegroup (modified with the test graph)
			JSONObject ngJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json").getJson();
			
			try {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			} catch (Exception e) {
			}
			nodeGroupStoreClient.executeStoreNodeGroup(ID, "testSelectByNodegroupId", CREATOR, ngJson);
			
			TestGraph.clearGraph();
			TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
			
			String csvStr = Utility.readFile("src/test/resources/sampleBattery.csv");
			nodeGroupExecutionClient.execIngestionFromCsvStrById(ID, csvStr, NodeGroupExecutor.get_USE_NODEGROUP_CONN());
			
			TableResultSet res = nodeGroupExecutionClient.execDispatchSelectByIdSync(ID, NodeGroupExecutor.get_USE_NODEGROUP_CONN(), null, null, null);
			Table tab = res.getTable();
			assert(true);
		}
		
		@Test
		public void testLoadTracking() throws Exception {	
			String user = ThreadAuthenticator.getThreadUserName();
			
			// delete all tracking info
			try {
				nodeGroupExecutionClient.deleteTrackingEvents(null, null, user, null, null);
			} catch (Exception e) {
				if (e.getMessage().contains("Tracking is not configured")) {
					assumeTrue("Tracking is not configured", false);
				} else {
					throw e;
				}
			}
			Table tab = nodeGroupExecutionClient.runTrackingQuery(null, null, user, null, null);
			assertEquals("Tracking query was not empty after deleting all", 0, tab.getNumRows());
			
			// clear graph
			nodeGroupExecutionClient.clearGraph(TestGraph.getSei(), true);
			tab = nodeGroupExecutionClient.runTrackingQuery(null, null, user, null, null);
			assertEquals("Tracking query didn't find clearGraph", 1, tab.getNumRows());
			
			Thread.sleep(1000);  // make sure epochs are different
			
			String DATA = "cell,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
			SparqlGraphJson sgJson_TestGraph = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
			TestGraph.uploadOwlResource(this, "/testTransforms.owl");
			
			// ingest one with no tracking
			nodeGroupExecutionClient.execIngestionFromCsvStr(sgJson_TestGraph, DATA);
						
			// ingest one with tracking
			nodeGroupExecutionClient.execIngestionFromCsvStr(sgJson_TestGraph, DATA, true, "$TRACK_KEY");
			
			// make sure one ingestion was tracked
			tab = nodeGroupExecutionClient.runTrackingQuery(null, null, user, null, null);
			assertEquals("Tracking query didn't find ingest", 2, tab.getNumRows());
			
			// retrieve contents
			String fileKey = tab.getCell(1, "fileKey");
			String contents = nodeGroupExecutionClient.getTrackedIngestFile(fileKey);
			assertTrue("tracked file contents didn't match", contents.equals(DATA));
			
			// try an undo
			int triples1 = TestGraph.getNumTriples();
			nodeGroupExecutionClient.undoLoad(fileKey);
			int triples2 = TestGraph.getNumTriples();
			assertTrue("Nothing was deleted by undo", triples2 < triples1);
			TestGraph.getSei().clearPrefix(LoadTracker.buildBaseURI(fileKey));
			int triples3 = TestGraph.getNumTriples();
			assertTrue("undo left triples behind", triples3 == triples2);

			// make sure undo ingestion was tracked
			tab = nodeGroupExecutionClient.runTrackingQuery(null, null, user, null, null);
			assertTrue("Tracking query did not return an 'undo' event", tab.toCSVString().contains("undo"));
			
			// delete tracking
			nodeGroupExecutionClient.deleteTrackingEvents(null, fileKey, null, null, null);

			// retrieve contents should fail
			try {
				nodeGroupExecutionClient.getTrackedIngestFile(fileKey);
				fail("Missing exception when retrieving deleted tracking file");
			} catch (Exception e) {
			}
		}
	}

