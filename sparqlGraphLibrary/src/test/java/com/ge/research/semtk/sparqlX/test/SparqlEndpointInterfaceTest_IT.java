/**
 ** Copyright 2016-2020 General Electric Company
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

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

/**
 * Tests the TestGraph SparqlEndpointInterface (integrationtest.properties)
 * Could be Virtuoso, Neptune, Fuseki... etc.
 * @author 200001934
 */
public class SparqlEndpointInterfaceTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();		
	}
	
	
	@Test 
	public void testQuery() throws Exception{
		TestGraph.clearGraph();
		TestGraph.initGraphWithData(this.getClass(), "sampleBattery");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromResource(this, "/sampleBattery.json");
		TestGraph.queryAndCheckResults(sgJson, this, "/sampleBatteryResults.csv");
	}
	
	
	@Test
	public void testRoundTrip() throws Exception {
		TestGraph.clearGraph();
		String s = Utility.getResourceAsString(this, "/Pet.owl");
		TestGraph.getSei().executeUpload(s.getBytes());
		
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false );
		assertTrue("Can't find the 'Dog' class", oInfo.getClassNames().contains("http://research.ge.com/kdl/pet#Dog"));
	}

}
