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

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;


public class IngestorRestClientTest_IT {
	
	private static IngestorRestClient irc = null;
	private String data = "cell,size in,lot,material,guy,treatment\ncellA,5,lot5,silver,Smith,spray\n";
	
	@BeforeClass
	public static void setup() throws Exception {
		String serviceProtocol = IntegrationTestUtility.getServiceProtocol();
		String ingestionServiceServer = IntegrationTestUtility.getIngestionServiceServer();
		int ingestionServicePort = IntegrationTestUtility.getIngestionServicePort();
		irc   = new IngestorRestClient(new IngestorClientConfig(serviceProtocol, ingestionServiceServer, ingestionServicePort));
	}
	
	
	/**
	 * Test ingesting data.
	 */
	@Test
	public void testIngest() throws Exception{				
		
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		
		SparqlGraphJson sgJson_TestGraph = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");
		String sgJsonString_TestGraph = sgJson_TestGraph.getJson().toJSONString(); 
		
		assertEquals(TestGraph.getNumTriples(),123);	// get count before loading
		irc.execIngestionFromCsv(sgJsonString_TestGraph, data);	// load data
		assertEquals(TestGraph.getNumTriples(),131);	// confirm loaded some triples
	}
	
//	/**
//	 * Test ingesting data with overriding the SPARQL connection.
//	 */
//	@Test
//	public void testIngestWithConnectionOverride() throws Exception{
//		
//		//irc.execIngestionFromCsv(String template, String data, String sparqlConnectionOverride)
//		// TODO FINISH THIS!
//	}
	
}
