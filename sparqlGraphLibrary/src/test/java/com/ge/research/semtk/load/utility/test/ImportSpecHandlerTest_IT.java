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

package com.ge.research.semtk.load.utility.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class ImportSpecHandlerTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	@Test
	public void test_getMappedPropItems() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/sampleBattery.owl");		
		
		// load in ImportSpecHandler
		String jsonPath = "src/test/resources/sampleBattery_PlusConstraints.json";
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath);
		OntologyInfo oInfo = sgJson.getOntologyInfo();
		NodeGroup nodegroup = sgJson.getNodeGroup();
		ImportSpecHandler handler = new ImportSpecHandler(sgJson.getImportSpecJson(), sgJson.getSNodeGroupJson(), sgJson.getSparqlConn(), oInfo);
		
		// Try it with no headers
		ArrayList<PropertyItem> pItems = handler.getUndeflatablePropItems(nodegroup);
		assertTrue(pItems.size() == 3);
				
		// Test
		handler.setHeaders(new ArrayList<String>(Arrays.asList("Battery", "Cell", "birthday", "color")));
		pItems = handler.getUndeflatablePropItems(nodegroup);
		assertTrue(pItems.size() == 3);
		
		// Try it again in different order and capitalization
		handler.setHeaders(new ArrayList<String>(Arrays.asList("battery", "color", "Cell", "birthday")));
		pItems = handler.getUndeflatablePropItems(nodegroup);
		assertTrue(pItems.size() == 3);
	}

}
