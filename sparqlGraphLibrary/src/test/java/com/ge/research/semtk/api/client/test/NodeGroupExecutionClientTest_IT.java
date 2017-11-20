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

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

import static org.junit.Assert.*;

public class NodeGroupExecutionClientTest_IT {
		
		private static NodeGroupExecutionClient nodeGroupExecutionClient = null;
		
		@BeforeClass
		public static void setup() throws Exception {
			// instantiate a client
			nodeGroupExecutionClient = new NodeGroupExecutionClient(new NodeGroupExecutionClientConfig(IntegrationTestUtility.getServiceProtocol(), IntegrationTestUtility.getNodegroupExecutionServiceServer(), IntegrationTestUtility.getNodegroupExecutionServicePort()));
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
			nodeGroupExecutionClient.execIngestionFromCsvStr(sgJson_TestGraph, DATA, sgJson_TestGraph.getSparqlConn().toJson());
			assertEquals(TestGraph.getNumTriples(),1311);	// confirm loaded some triples
		}
		
	}

