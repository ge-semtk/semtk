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


package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class BelmontConstructQueryTest_IT {
	
	private static SparqlGraphJson sgJson = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		sgJson = TestGraph.initGraphWithData("sampleBattery");
	}
	
	@Test
	public void testConstructQuery1() throws Exception{
			
		NodeGroup ng = sgJson.getNodeGroup();
		SparqlEndpointInterface sei = sgJson.getSparqlConn().getDefaultQueryInterface();

		String query = ng.generateSparqlConstruct(false);
		JSONObject responseJson = sei.executeQuery(query, SparqlResultTypes.GRAPH_JSONLD);
			
		// pass if there's a @graph and the first element contains anything about a (b)attery
		assertTrue(((JSONArray)responseJson.get("@graph")).get(0).toString().contains("attery"));
		
		NodeGroup responseNodeGroup = NodeGroup.fromConstructJSON(responseJson);
		assertEquals(responseNodeGroup.getNodeCount(),9);
	}

	
	@Test 
	public void testConstructQuery2() throws Exception {		
		
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/sampleBattery_PlusConstraints.json");		
		SparqlEndpointInterface sei = sgJson.getSparqlConn().getDefaultQueryInterface();
		
		String query = ng.generateSparqlConstruct(false);
		
		JSONObject responseJson = sei.executeQuery(query, SparqlResultTypes.GRAPH_JSONLD);
		NodeGroup responseNodeGroup = NodeGroup.fromConstructJSON(responseJson);
		assertEquals(responseNodeGroup.getNodeCount(),3);
		// TODO test the PlusConstraints part
	}
}
