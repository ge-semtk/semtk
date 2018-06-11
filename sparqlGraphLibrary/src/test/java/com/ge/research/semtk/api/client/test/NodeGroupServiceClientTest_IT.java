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
package com.ge.research.semtk.api.client.test;

import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupService.SparqlIdReturnedTuple;
import com.ge.research.semtk.nodeGroupService.SparqlIdTuple;
import com.ge.research.semtk.nodeGroupService.client.NodeGroupServiceConfig;
import com.ge.research.semtk.nodeGroupService.client.NodeGroupServiceRestClient;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class NodeGroupServiceClientTest_IT {
		
		private static NodeGroupServiceRestClient ngServiceClient = null;
		
		@BeforeClass
		public static void setup() throws Exception {
			// instantiate a client
			ngServiceClient = new NodeGroupServiceRestClient(
					new NodeGroupServiceConfig(
							IntegrationTestUtility.getServiceProtocol(), 
							IntegrationTestUtility.getNodegroupServiceServer(), 
							IntegrationTestUtility.getNodegroupServicePort()));
		}
		
		@AfterClass
	    public static void teardown() throws Exception {
	  
	    } 
				
		/**
		 * Test SAMPLE data.
		 */
		@Test
		public void changeSparqlIds() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			
			// build some changes
			ArrayList<SparqlIdTuple> tuples = new ArrayList<SparqlIdTuple>();
			tuples.add(new SparqlIdTuple("Color", "RenamedColor"));
			tuples.add(new SparqlIdTuple("?Cell", "?RenamedCell"));
			
			// run successful change
			SparqlGraphJson sgJson2 = ngServiceClient.execChangeSparqlIds(sgJson, tuples);
			
			NodeGroup ng2 = sgJson2.getNodeGroup();
			assertTrue(ng2.getItemBySparqlID("RenamedColor") != null);
			assertTrue(ng2.getItemBySparqlID("Color") == null);
			
			
		}
	
		@Test
		public void changeSparqlIdsIllegalId() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			
			
			// run illegal sparqlid
			ArrayList<SparqlIdTuple> tuples = new ArrayList<SparqlIdTuple>();
			tuples.add(new SparqlIdTuple("Color", "2Rename!dColor"));
			try {
				ngServiceClient.execChangeSparqlIds(sgJson, tuples);
				fail("Missing expected exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("unsupported character"));
			}
			
		}
		@Test
		public void changeSparqlIdsMissing() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
				
			
			// run missing sparqlid
			ArrayList<SparqlIdTuple> tuples = new ArrayList<SparqlIdTuple>();
			tuples.add(new SparqlIdTuple("ColorMissing", "RenamedColor"));
			
			try {
				ngServiceClient.execChangeSparqlIds(sgJson, tuples);
				fail("Missing expected exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Id was not found"));
			}
			
		}
		
		@Test
		public void changeSparqlIdsDuplicate() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			
			// run duplicate sparqlid
			ArrayList<SparqlIdTuple> tuples = new ArrayList<SparqlIdTuple>();
			tuples.add(new SparqlIdTuple("Color", "Duplicate"));
			tuples.add(new SparqlIdTuple("?Cell", "Duplicate"));
			
			try {
				ngServiceClient.execChangeSparqlIds(sgJson, tuples);
				fail("Missing expected exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("already in use"));
			}
			
		}
		
		@Test
		public void changeSparqlIdsNodeGroup() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson0 = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			
			// simulate building json with only the NodeGroup
			SparqlGraphJson sgJson = new SparqlGraphJson(sgJson0.getSNodeGroupJson());

			
			// build some changes
			ArrayList<SparqlIdTuple> tuples = new ArrayList<SparqlIdTuple>();
			tuples.add(new SparqlIdTuple("Color", "RenamedColor"));
			tuples.add(new SparqlIdTuple("?Cell", "?RenamedCell"));
			
			// run successful change
			SparqlGraphJson sgJson2 = ngServiceClient.execChangeSparqlIds(sgJson, tuples);
			
			NodeGroup ng2 = sgJson2.getNodeGroup();
			assertTrue(ng2.getItemBySparqlID("RenamedColor") != null);
			assertTrue(ng2.getItemBySparqlID("Color") == null);
			
		}
		
		/**
		 * Test SAMPLE data.
		 */
		@Test
		public void getReturnedSparqlIds() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			
			// run successful change
			String [] res = ngServiceClient.execGetReturnedSparqlIds(sgJson);
			assertTrue(Arrays.asList(res).indexOf("?Name") > -1);
			assertTrue(Arrays.asList(res).indexOf("?CellId") > -1);

		}
		
		/**
		 * Test SAMPLE data.
		 */
		@Test
		public void getReturnedRuntimeConstraints() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/EIOMultiGraph.json");
			
			// run successful change
			Table res = ngServiceClient.execGetRuntimeConstraints(sgJson);
			String csvString = res.toCSVString();
			
			// NOTE: returning a table is awful
			assertTrue(csvString.contains("\"?hdid\",PROPERTYITEM,INTEGER"));
		}
		
		/**
		 * Test SAMPLE data.
		 */
		@Test
		public void setIsReturned() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			
			// build some changes
			ArrayList<SparqlIdReturnedTuple> tuples = new ArrayList<SparqlIdReturnedTuple>();
			tuples.add(new SparqlIdReturnedTuple("Name", false));
			tuples.add(new SparqlIdReturnedTuple("?CellId", true));
			
			// run successful change
			SparqlGraphJson sgJson2 = ngServiceClient.execSetIsReturned(sgJson, tuples);
			
			NodeGroup ng2 = sgJson2.getNodeGroup();
			// note that I'm dorking with the "?" prefixes too
			assertFalse(ng2.getItemBySparqlID("?Name").getIsReturned());
			assertTrue(ng2.getItemBySparqlID("CellId").getIsReturned());

			
		}
		
		/**
		 * Test SAMPLE data.
		 */
		@Test
		public void setIsReturnedInvalid() throws Exception{				
			
			// get a nodegroup
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
			
			// build some changes
			ArrayList<SparqlIdReturnedTuple> tuples = new ArrayList<SparqlIdReturnedTuple>();
			tuples.add(new SparqlIdReturnedTuple("Name", false));
			tuples.add(new SparqlIdReturnedTuple("?DoesnExist", true));
			
			// run successful change
			try {
				SparqlGraphJson sgJson2 = ngServiceClient.execSetIsReturned(sgJson, tuples);
				fail("Missing expected exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Id was not found"));
				assertTrue(e.getMessage().contains("DoesnExist"));

			}
		}
		
	}

