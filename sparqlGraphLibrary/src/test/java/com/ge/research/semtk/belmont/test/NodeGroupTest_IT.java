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

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.test.TestGraph;

public class NodeGroupTest_IT {

	@Test
	public void testValidateNodeGroup() throws Exception {
		String jsonPath = "src/test/resources/sampleBattery.json";
		String owlPath = "src/test/resources/sampleBattery.owl";
		String domain = "http";

		NodeGroup nodegroup = TestGraph.getNodeGroup(jsonPath); 
		TestGraph.clearGraph();
		TestGraph.uploadOwl(owlPath);
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), domain);
			
		nodegroup.validateAgainstModel(oInfo);
	}
	
	
	@Test
	public void testValidateNodeGroupFail() throws Exception {
		String jsonPath = "src/test/resources/sampleBattery.json";
		String owlPath = "src/test/resources/sampleBattery.owl";
		String domain = "http";

		// model isn't loaded
		NodeGroup nodegroup = TestGraph.getNodeGroup(jsonPath); 
		OntologyInfo oInfo = new OntologyInfo();
		TestGraph.clearGraph();
		
		try {
			oInfo.load(TestGraph.getSei(), domain);
			nodegroup.validateAgainstModel(oInfo);
			fail("Model should have been empty but no exception was thrown");
		} catch (Exception e) {
			System.out.println("Exception as expected:" + e.getMessage());
		} 
		
		// now load it: successful
		TestGraph.uploadOwl(owlPath);
		oInfo.load(TestGraph.getSei(), domain);
		nodegroup.validateAgainstModel(oInfo);
		
		// node with invalid URI
		Node n = new Node("?Invalid", null, null, "http://bad/URI", nodegroup);
		nodegroup.addOneNode(n, null, null, null);
		
		try {
			nodegroup.validateAgainstModel(oInfo);
			fail("Node URI should be illegal but no exception was thrown");
		} catch (Exception e) {
			System.out.println("Exception as expected:" + e.getMessage());
		} 
		
		// bad property URI
		String[] suffixes = {"_BadURI", "_BadPropURI", "_BadPropRange", "_BadNodeItemURI", "_BadNodeItemRange", "_BadConnectionSubtype"};
		for (String suffix : suffixes ) {
			nodegroup = TestGraph.getNodeGroup(jsonPath.replace(".json", suffix+".json")); 
			try {
				nodegroup.validateAgainstModel(oInfo);
				fail("Node URI should be illegal but no exception was thrown");
			} catch (Exception e) {
				System.out.println("Exception as expected:" + e.getMessage());
			} 
		}
	}	
}
