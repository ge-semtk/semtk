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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.plotting.PlotSpecHandler;
import com.ge.research.semtk.plotting.PlotlyPlotSpecHandler;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class SparqlGraphJsonTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	public void test_deflate_inflate() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "sampleBattery.owl");		
		
		String jsonPath = "src/test/resources/sampleBatteryTestDeflate.json";
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath);
		OntologyInfo oInfo = sgJson.getOntologyInfo();
		NodeGroup nodegroupDeflated = sgJson.getNodeGroup();
		NodeGroup nodegroupInflated = sgJson.getNodeGroup(oInfo);
		
		// ?Cell_1->cellId was missing from input but re-inflated properly
		assertTrue(nodegroupDeflated.getNodeBySparqlID("?Cell_1").getPropertyItems().size() == 0);
		assertTrue(nodegroupInflated.getNodeBySparqlID("?Cell_1").getPropertyItems().get(0).getKeyName().equals("cellId"));
		
		// ?Cell_1->color was missing from input but re-inflated properly
		assertTrue(nodegroupDeflated.getNodeBySparqlID("?Cell_1").getNodeItemList().size() == 0);
		assertTrue(nodegroupInflated.getNodeBySparqlID("?Cell_1").getNodeItemList().get(0).getKeyName().equals("color"));
		assertTrue(nodegroupInflated.getNodeBySparqlID("?Cell_1").getNodeItemList().get(0).getValueType().equals("Color"));
		
		// sampleBatteryTestDeflate.json is verified correctly deflated
		// so deflate the re-inflation and check
		NodeGroup nodegroupDeflated2 = new NodeGroup();
		ArrayList<PropertyItem> mappedProps = sgJson.getImportSpecHandler().getUndeflatablePropItems(nodegroupInflated);
		mappedProps.size();
		JSONArray generatedJSON = (JSONArray) (nodegroupInflated.toJson(mappedProps).get("sNodeList")) ;
		nodegroupDeflated2.addJson( generatedJSON );
		
		// re-deflated should have same number of nodes
		assertTrue(nodegroupDeflated.getNodeCount() == nodegroupDeflated2.getNodeCount());
		
		// loop through re-deflated nodes
		for (int i=0; i < nodegroupDeflated2.getNodeCount(); i++) {
			Node node2 = nodegroupDeflated2.getNodeList().get(i);
			Node node = nodegroupDeflated.getNodeBySparqlID(node2.getSparqlID());
			
			// check size of prop and node lists
			assertTrue(node2.getPropertyItems().size() == node.getPropertyItems().size());
			assertTrue(node2.getNodeItemList().size() == node.getNodeItemList().size());
		}
	}

	@Test
	public void test_badInflates() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "sampleBattery.owl");		
		
		String files[] = {	
				"src/test/resources/sampleBatteryInflateBadPropURI.json",
				"src/test/resources/sampleBatteryInflateBadPropRange.json",
				"src/test/resources/sampleBatteryInflateBadNodeURI.json"
		};
		String msg[] = {	
				"#cellIdBad",
				"#stringBad",
				"#ColorBad"
		};
		
		for (int i=0; i < files.length; i++) {
			// load in ImportSpecHandler
			SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(files[i]);
			OntologyInfo oInfo = sgJson.getOntologyInfo();
			try {
				sgJson.getNodeGroup(oInfo);
				assertTrue("Did not throw exception inflating " + files[i], false);
			} catch (Exception e) {
				assertTrue(e.getMessage().contains(msg[i]));
			}
		}
	}
	
}


// PEC HERE:  test the four smapleBattery Inflate/Deflate files