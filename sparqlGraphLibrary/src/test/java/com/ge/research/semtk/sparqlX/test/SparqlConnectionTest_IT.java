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

import org.apache.jena.graph.Graph;
import org.junit.Test;

import com.ge.research.semtk.test.TestGraph;

public class SparqlConnectionTest_IT {
	
	@Test
	public void testGetJenaGraph() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadTurtleResource(this, "musicTestDataset_2017.q2.ttl");
		Graph g = TestGraph.getSparqlConn().getJenaGraph();
		assertEquals(g.size(), 215);
	}
	
}
