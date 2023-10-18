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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.jena.graph.Graph;
import org.junit.Test;

import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;

public class SparqlConnectionTest_IT {
	
	@Test
	public void testGetJenaGraph() throws Exception {
		
		// test connection with all (1) graphs containing data
		TestGraph.clearGraph();
		TestGraph.uploadTurtleResource(this, "musicTestDataset_2017.q2.ttl");
		SparqlConnection conn = TestGraph.getSparqlConn();
		Graph g = conn.getJenaGraph();
		assertEquals(g.size(), 215);
		
		// test connection with one graph containing data, one graph empty
		SparqlEndpointInterface sei = TestGraph.getSei("nonexistentGraph");
		conn.addDataInterface(sei);
		g = conn.getJenaGraph();
		assertEquals(g.size(), 215);
		
		// test connection with no graphs containing data
		try {
			TestGraph.clearGraph();
			conn = TestGraph.getSparqlConn();
			g = conn.getJenaGraph();
			fail(); // shouldn't get here
		}catch(Exception e) {
			assertTrue(e.getMessage().equals("Cannot get Jena graph for this connection (all graphs may be empty)"));
		}
	}
}
