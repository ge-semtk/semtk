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


package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class SparqlQueryClientTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	public void testUploadOwl() throws Exception {
		try {
			TestGraph.clearGraph();
			File owlFile = Utility.getResourceAsTempFile(this, "Pet.owl");	
			SparqlQueryClient client = IntegrationTestUtility.getSparqlQueryAuthClient("/sparqlQueryService/uploadOwl", TestGraph.getSparqlServer(), TestGraph.getDataset());
			SimpleResultSet result = client.uploadOwl(owlFile);
			assertTrue(result.getRationaleAsString("\n"), result.getSuccess());
			assertEquals("Number of triples loaded via OWL file", 53, TestGraph.getNumTriples());
		}finally {
			TestGraph.clearGraph();
		}
	}
	
	@Test
	public void testUploadTurtle() throws Exception {
		try {
			TestGraph.clearGraph();
			File ttlFile = Utility.getResourceAsTempFile(this, "musicTestDataset_2017.q2.ttl");	
			SparqlQueryClient client = IntegrationTestUtility.getSparqlQueryAuthClient("/sparqlQueryService/uploadTurtle", TestGraph.getSparqlServer(), TestGraph.getDataset());
			SimpleResultSet result = client.uploadTurtle(ttlFile);
			assertTrue(result.getRationaleAsString("\n"), result.getSuccess());
			assertEquals("Number of triples loaded via TTL file", 215, TestGraph.getNumTriples());
		}finally{
			TestGraph.clearGraph();
		}
	}

	@Test
	public void testDropGraph() throws Exception{
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(getClass(), "Pet.owl");
		assertEquals(53, TestGraph.getNumTriples());
		SparqlQueryClient client = IntegrationTestUtility.getSparqlQueryClient("/sparqlQueryService/dropGraph", TestGraph.getSparqlServer(), TestGraph.getDataset());
		client.dropGraph();
		assertEquals(0, TestGraph.getNumTriples());
	}

}
