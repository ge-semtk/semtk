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

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class NodeGroupTest {

	@Test
	public void nodeGroupFromJson() throws Exception {		

        SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/sampleBattery.json"));
		NodeGroup ng = NodeGroup.getInstanceFromJson(sgJson.getSNodeGroupJson());
		
		assertEquals(3, ng.getNodeList().size());
		assertEquals(ng.getNodeList().get(0).getFullUriName(),"http://kdl.ge.com/batterydemo#Color");
		assertEquals(ng.getNodeList().get(1).getFullUriName(),"http://kdl.ge.com/batterydemo#Cell");
		assertEquals(ng.getNodeList().get(2).getFullUriName(),"http://kdl.ge.com/batterydemo#Battery");	
		
		// Make sure old fashioned "isOptional: false" on a node translates to new version
		Node color = ng.getNodeBySparqlID("?Color");
		assertEquals(ng.getNodeList().get(1).getNodeItemList().get(0).getSNodeOptional(color), NodeItem.OPTIONAL_FALSE);
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
	
	@Test
	public void testDuplicatePrefix() throws Exception {
		NodeGroup ng = new NodeGroup();
		ng.addToPrefixHash("/here/is/a/prefixed#value");
		ng.addToPrefixHash("/here/is/another/prefixed#value");
		String p = ng.generateSparqlPrefix();
		assertTrue(p.contains("prefix prefixed:</here/is/a/prefixed#>"));
		assertTrue(p.contains("prefix prefixed_0:</here/is/another/prefixed#>"));
	}	
	
	@Test
	public void testExpandOptional() throws Exception {
		// testing chain with optional properties at each end and a property in between
		// drag the json into SparqlGraph to see
		String jsonPath = "src/test/resources/sampleBattery Optional Props.json";

		NodeGroup nodegroup = TestGraph.getNodeGroup(jsonPath);
		
		Node battery = nodegroup.getNodeBySparqlID("?Battery");
		Node battery_0 = nodegroup.getNodeBySparqlID("?Battery_0");
		Node cell = nodegroup.getNodeBySparqlID("?Cell");
		Node cell_0 = nodegroup.getNodeBySparqlID("?Cell_0");
		
		// loaded correctly
		assertEquals(NodeItem.OPTIONAL_FALSE,    battery.getNodeItemList().get(0)  .getSNodeOptional(cell_0));
		assertEquals(NodeItem.OPTIONAL_FALSE,    battery.getNodeItemList().get(0)  .getSNodeOptional(cell));
		assertEquals(NodeItem.OPTIONAL_FALSE,    battery_0.getNodeItemList().get(0).getSNodeOptional(cell));
		
		nodegroup.expandOptionalSubgraphs();
		
		// expanded optionals correctly
		assertEquals(NodeItem.OPTIONAL_TRUE,    battery.getNodeItemList().get(0) .getSNodeOptional(cell_0));
		assertEquals(NodeItem.OPTIONAL_TRUE,     battery.getNodeItemList().get(0) .getSNodeOptional(cell));
		assertEquals(NodeItem.OPTIONAL_FALSE,    battery_0.getNodeItemList().get(0).getSNodeOptional(cell));
		
		// check the query
		String sparql = nodegroup.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 100, null).replaceAll("\\s+", "");
		String s = "select distinct ?name ?name_0 ?cellId where {\r\n" + 
				"   ?Battery a batterydemo:Battery.\r\n" + 
				"   ?Battery batterydemo:name ?name_0 .\r\n" + 
				"   optional {\r\n" + 
				"\r\n" + 
				"      ?Battery batterydemo:cell ?Cell.\r\n" + 
				"\r\n" + 
				"         ?Battery_0 batterydemo:cell ?Cell.\r\n" + 
				"         ?Battery_0 a batterydemo:Battery.\r\n" + 
				"         optional {\r\n" + 
				"            ?Battery_0 batterydemo:name ?name .\r\n" + 
				"         }\r\n" + 
				"   }\r\n" + 
				"   optional {\r\n" + 
				"\r\n" + 
				"      ?Battery batterydemo:cell ?Cell_0.\r\n" + 
				"         optional {\r\n" + 
				"            ?Cell_0 batterydemo:cellId ?cellId .\r\n" + 
				"         }\r\n" + 
				"   }\r\n" + 
				"}";
		s = s.replaceAll("\\s+", "");
		System.out.println(sparql);
		System.out.println(s);
		assertTrue(sparql.contains(s));
	}
	
	@Test
	public void testExpandOptional2() throws Exception {
		// testing three-way split with only one in a long chain.  Optionals are nodeItems to enums.
		// make sure the chain expands
		// drag the json into SparqlGraph to see
		String jsonPath = "src/test/resources/sampleBattery Triple Optional.json";

		NodeGroup nodegroup = TestGraph.getNodeGroup(jsonPath);
		
		Node battery = nodegroup.getNodeBySparqlID("?Battery");
		Node battery_0 = nodegroup.getNodeBySparqlID("?Battery_0");
		Node cell = nodegroup.getNodeBySparqlID("?Cell");
		Node cell_0 = nodegroup.getNodeBySparqlID("?Cell_0");
		Node color = nodegroup.getNodeBySparqlID("?Color");
		Node color_0 = nodegroup.getNodeBySparqlID("?Color_0");
		
		// loaded correctly
		assertEquals(NodeItem.OPTIONAL_REVERSE, battery.getNodeItemList().get(0)  .getSNodeOptional(cell));
		assertEquals(NodeItem.OPTIONAL_TRUE,    cell.getNodeItemList().get(0)     .getSNodeOptional(color));
		assertEquals(NodeItem.OPTIONAL_FALSE,   battery_0.getNodeItemList().get(0).getSNodeOptional(cell));
		assertEquals(NodeItem.OPTIONAL_FALSE,   battery_0.getNodeItemList().get(0).getSNodeOptional(cell_0));
		assertEquals(NodeItem.OPTIONAL_TRUE,    cell_0.getNodeItemList().get(0)   .getSNodeOptional(color_0));

		nodegroup.expandOptionalSubgraphs();
		
		// expanded optionals correctly
		assertEquals(NodeItem.OPTIONAL_REVERSE, battery.getNodeItemList().get(0)  .getSNodeOptional(cell));
		assertEquals(NodeItem.OPTIONAL_TRUE,    cell.getNodeItemList().get(0)     .getSNodeOptional(color));
		assertEquals(NodeItem.OPTIONAL_TRUE, battery_0.getNodeItemList().get(0).getSNodeOptional(cell));
		assertEquals(NodeItem.OPTIONAL_TRUE,   battery_0.getNodeItemList().get(0).getSNodeOptional(cell_0));
		assertEquals(NodeItem.OPTIONAL_FALSE,   cell_0.getNodeItemList().get(0)   .getSNodeOptional(color_0));
	}
}
