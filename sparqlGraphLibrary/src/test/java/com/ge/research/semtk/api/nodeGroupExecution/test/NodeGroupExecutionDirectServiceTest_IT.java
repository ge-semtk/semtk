/**
 ** Copyright 2018 General Electric Company
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

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.test.IntegrationTestUtility;

import static org.junit.Assert.*;

/**
 * Test connecting to a service using a generic RestClient instead of the service's custom client.
 * 
 * This should only be used to test bad/missing request parameters.  To test requests with correct parameters, test with the service's custom client.
 */
public class NodeGroupExecutionDirectServiceTest_IT {
	
		private static String nodeGroupExecutionServiceMappingPrefix = "/nodeGroupExecution";
		private static String dispatchSelectByIdEndpoint = "/dispatchSelectById";
	
		private static NodeGroupExecutionClientConfig nodeGroupExecutionClientConfig;
		
		@BeforeClass
		public static void setup() throws Exception {
			IntegrationTestUtility.authenticateJunit();			
			// get client config for nodegroup execution service
			nodeGroupExecutionClientConfig = new NodeGroupExecutionClientConfig(IntegrationTestUtility.getServiceProtocol(), IntegrationTestUtility.getNodegroupExecutionServiceServer(), IntegrationTestUtility.getNodegroupExecutionServicePort());
			nodeGroupExecutionClientConfig.setServiceEndpoint(nodeGroupExecutionServiceMappingPrefix + dispatchSelectByIdEndpoint);
		}
				
		
		/**
		 * Test that we get a good error if we fail to send a nodegroup ID.
		 */
		@Test
		public void test_requestWithMissingNodegroupKey() throws Exception{				
			
			// a generic client with bad parameter(s)
			RestClient genericClient = new RestClient(nodeGroupExecutionClientConfig) {
				
				@Override
				public void buildParametersJSON() throws Exception {		
					//  not sending nodegroup id
					this.parametersJSON.put("sparqlConnection", "WSOIFWE");												
				}
			};			

			// execute
			SimpleResultSet result = genericClient.executeWithSimpleResultReturn();

			// confirm good error message
			assertFalse(result.getSuccess());
			assertTrue(result.getRationaleAsString("").indexOf("Request is missing 'nodeGroupId'") != -1);
		}
		
		/**
		 * Test that we get a good error if we mistakenly send "sparqlConn" instead of "sparqlConnection"
		 */
		@Test
		public void test_requestWithMissingSparqlConnection() throws Exception{				
			
			// a generic client with bad parameter(s)
			RestClient genericClient = new RestClient(nodeGroupExecutionClientConfig) {

				
				@Override
				public void buildParametersJSON() throws Exception {								
					this.parametersJSON.put("nodeGroupId", "DFAWEFWF"); // this key is correct
					this.parametersJSON.put("sparqlConn", "WSOIFWE");	// this key is wrong (service is expecting "sparqlConnection")											
				}
			};			

			// execute
			SimpleResultSet result = genericClient.executeWithSimpleResultReturn();

			// confirm good error message
			assertFalse(result.getSuccess());
			assertTrue(result.getRationaleAsString("").indexOf("Request is missing 'sparqlConnection'") != -1);
		}
	
	}

