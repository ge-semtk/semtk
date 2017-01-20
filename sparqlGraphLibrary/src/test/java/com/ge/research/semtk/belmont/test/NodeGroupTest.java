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

import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.load.utility.Utility;

public class NodeGroupTest {

	@Test
	public void nodeGroupFromJson() throws Exception {		

        SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/sampleBattery.json"));
		NodeGroup ng = NodeGroup.getInstanceFromJson(sgJson.getSNodeGroupJson());
		
		assertEquals(3, ng.getNodeList().size());
		assertEquals(ng.getNodeList().get(0).getFullUriName(),"http://kdl.ge.com/batterydemo#Color");
		assertEquals(ng.getNodeList().get(1).getFullUriName(),"http://kdl.ge.com/batterydemo#Cell");
		assertEquals(ng.getNodeList().get(2).getFullUriName(),"http://kdl.ge.com/batterydemo#Battery");	
	}
	
	@Test
	public void checkDuplicateSparqlIdInLoad() throws Exception {
		
		NodeGroup ng = new NodeGroup();
		Node cellTest = new Node("Cell", null, null, "fakeUri", ng);
		Node cellTest2 = new Node("Cell", null, null, "fakeUri", ng);
		ng.addOneNode(cellTest, null, null, null);
		ng.addOneNode(cellTest2, null, null, null);
			
        SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/sampleBattery.json"));
        JSONObject nodeGroupJson = sgJson.getSNodeGroupJson();
        
		ng.addJsonEncodedNodeGroup(nodeGroupJson);
        
		assertEquals(5, ng.getNodeList().size());
		assertTrue(ng.getNodeBySparqlID("?Cell") != null);
		assertTrue(ng.getNodeBySparqlID("?Cell_0")!= null);
		assertTrue(ng.getNodeBySparqlID("?Cell_1")!= null);		
	}
	
}
