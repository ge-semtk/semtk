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
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class NodeGroupTest {

	@Test
	public void nodeGroupFromJson() throws Exception {		

        SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/sampleBattery.json"));
		NodeGroup ng = sgJson.getNodeGroup();
		
		assertEquals(3, ng.getNodeList().size());
		assertEquals("http://kdl.ge.com/batterydemo#Color",   ng.getNodeList().get(0).getFullUriName());
		assertEquals("http://kdl.ge.com/batterydemo#Cell",    ng.getNodeList().get(1).getFullUriName());
		assertEquals("http://kdl.ge.com/batterydemo#Battery", ng.getNodeList().get(2).getFullUriName());	
		
		// Make sure old fashioned "isOptional: false" on a node translates to new version
		Node color = ng.getNodeBySparqlID("?Color");
		assertEquals(ng.getNodeList().get(1).getNodeItemList().get(0).getOptionalMinus(color), NodeItem.OPTIONAL_FALSE);
	}
	
	@Test
	public void nodeGroupFromJsonOrderBy() throws Exception {		

        SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/sampleBattery.json"));
		NodeGroup ng = sgJson.getNodeGroup();
		ng.clearOrderBy();
		ng.appendOrderBy("?CellId", "DESC");
		ng.appendOrderBy("?Name");
		
		ng.setLimit(100);
		ng.setOffset(20);
		
		JSONObject jObj = ng.toJson();
		String jStr = jObj.toJSONString();
		
		NodeGroup ng2 = NodeGroup.getInstanceFromJson(jObj);
		JSONObject jObj2 = ng2.toJson();
		String jStr2 = jObj2.toJSONString();
		
		assertEquals(jStr, jStr2);
	}
	
	@Test
	public void nodeGroupFromJsonBadDeleteMode() throws Exception {		

        SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/sampleBattery_ErrDeleteMode.json"));
		
        try {
        	NodeGroup ng = sgJson.getNodeGroup();
        	fail("Nodegroup with invalid delete mode loaded");
        	
        } catch (Exception e) {
        	System.out.println("Exception as expected:" + e.getMessage());
        }
		
		
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

		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath(jsonPath));
		NodeGroup nodegroup = sgJson.getNodeGroup();
		
		Node battery = nodegroup.getNodeBySparqlID("?Battery");
		Node battery_0 = nodegroup.getNodeBySparqlID("?Battery_0");
		Node cell = nodegroup.getNodeBySparqlID("?Cell");
		Node cell_0 = nodegroup.getNodeBySparqlID("?Cell_0");
		
		// loaded correctly
		assertEquals(NodeItem.OPTIONAL_FALSE,    battery.getNodeItemList().get(0)  .getOptionalMinus(cell_0));
		assertEquals(NodeItem.OPTIONAL_FALSE,    battery.getNodeItemList().get(0)  .getOptionalMinus(cell));
		assertEquals(NodeItem.OPTIONAL_FALSE,    battery_0.getNodeItemList().get(0).getOptionalMinus(cell));
		
		nodegroup.expandOptionalSubgraphs();
		
		// expanded optionals correctly
		assertEquals(NodeItem.OPTIONAL_TRUE,    battery.getNodeItemList().get(0) .getOptionalMinus(cell_0));
		assertEquals(NodeItem.OPTIONAL_TRUE,     battery.getNodeItemList().get(0) .getOptionalMinus(cell));
		assertEquals(NodeItem.OPTIONAL_FALSE,    battery_0.getNodeItemList().get(0).getOptionalMinus(cell));
		
		// check the query
		String sparql = nodegroup.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, false, 100, null).toLowerCase();

		assertEquals("Did not find enough optionals in the sparql", 4, IntegrationTestUtility.countSubstring(sparql, "optional"));
		
		ArrayList<String> ids = nodegroup.getReturnedSparqlIDs();
		assertTrue("Expand optional didn't return ?name", ids.contains("?name"));
		assertTrue("Expand optional didn't return ?name_0", ids.contains("?name_0"));
		assertTrue("Expand optional didn't return ?cellId", ids.contains("?cellId"));

		assertTrue("Expand optional sparql doesn't contain FROM", sparql.contains("from"));
		
	}
	
	@Test
	public void testExpandOptional2() throws Exception {
		// testing three-way split with only one in a long chain.  Optionals are nodeItems to enums.
		// make sure the chain expands
		// drag the json into SparqlGraph to see
		String jsonPath = "src/test/resources/sampleBattery Triple Optional.json";

        SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath(jsonPath));
		NodeGroup nodegroup = sgJson.getNodeGroup();
		
		Node battery = nodegroup.getNodeBySparqlID("?Battery");
		Node battery_0 = nodegroup.getNodeBySparqlID("?Battery_0");
		Node cell = nodegroup.getNodeBySparqlID("?Cell");
		Node cell_0 = nodegroup.getNodeBySparqlID("?Cell_0");
		Node color = nodegroup.getNodeBySparqlID("?Color");
		Node color_0 = nodegroup.getNodeBySparqlID("?Color_0");
		
		// loaded correctly
		assertEquals(NodeItem.OPTIONAL_REVERSE, battery.getNodeItemList().get(0)  .getOptionalMinus(cell));
		assertEquals(NodeItem.OPTIONAL_TRUE,    cell.getNodeItemList().get(0)     .getOptionalMinus(color));
		assertEquals(NodeItem.OPTIONAL_FALSE,   battery_0.getNodeItemList().get(0).getOptionalMinus(cell));
		assertEquals(NodeItem.OPTIONAL_FALSE,   battery_0.getNodeItemList().get(0).getOptionalMinus(cell_0));
		assertEquals(NodeItem.OPTIONAL_TRUE,    cell_0.getNodeItemList().get(0)   .getOptionalMinus(color_0));

		nodegroup.expandOptionalSubgraphs();
		
		// expanded optionals correctly
		assertEquals(NodeItem.OPTIONAL_REVERSE, battery.getNodeItemList().get(0)  .getOptionalMinus(cell));
		assertEquals(NodeItem.OPTIONAL_TRUE,    cell.getNodeItemList().get(0)     .getOptionalMinus(color));
		assertEquals(NodeItem.OPTIONAL_TRUE, battery_0.getNodeItemList().get(0).getOptionalMinus(cell));
		assertEquals(NodeItem.OPTIONAL_TRUE,   battery_0.getNodeItemList().get(0).getOptionalMinus(cell_0));
		assertEquals(NodeItem.OPTIONAL_FALSE,   cell_0.getNodeItemList().get(0)   .getOptionalMinus(color_0));
	}
	
	@Test
	public void testUnOptionalizeConstrained_do_nothing() throws Exception {

		// load nodegroup
		String jsonPath = "src/test/resources/sampleBatteryThreeCells.json";
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath(jsonPath));
		NodeGroup nodegroup = sgJson.getNodeGroup();
		
		// get pointers to parts
		Node color0 = nodegroup.getNodeBySparqlID("?Color_0");
		Node color1 = nodegroup.getNodeBySparqlID("?Color_1");
		Node color2 = nodegroup.getNodeBySparqlID("?Color_2");
		Node cell0 = nodegroup.getNodeBySparqlID("?Cell_0");
		Node cell1 = nodegroup.getNodeBySparqlID("?Cell_1");
		Node cell2 = nodegroup.getNodeBySparqlID("?Cell_2");
		Node battery = nodegroup.getNodeBySparqlID("?Battery");
		PropertyItem name = battery.getPropertyItem(0);
		PropertyItem cellId0 = cell0.getPropertyItem(0);
		PropertyItem cellId1 = cell1.getPropertyItem(0);
		PropertyItem cellId2 = cell2.getPropertyItem(0);
		NodeItem cellEdge0 = battery.getConnectingNodeItems(cell0).get(0);
		NodeItem cellEdge1 = battery.getConnectingNodeItems(cell1).get(0);
		NodeItem cellEdge2 = battery.getConnectingNodeItems(cell2).get(0);
		NodeItem colorEdge0 = cell0.getConnectingNodeItems(color0).get(0);
		NodeItem colorEdge1 = cell1.getConnectingNodeItems(color1).get(0);
		NodeItem colorEdge2 = cell2.getConnectingNodeItems(color2).get(0);
		
		// set some optionals
		colorEdge0.setOptionalMinus(color0, NodeItem.OPTIONAL_TRUE);
		colorEdge1.setOptionalMinus(color1, NodeItem.OPTIONAL_TRUE);
		colorEdge2.setOptionalMinus(color2, NodeItem.OPTIONAL_TRUE);
		cellId0.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);

		
		// but no constraints


		// go: shouldn't do anything since there are no constraints
		nodegroup.unOptionalizeConstrained();
		
		// check results.  No optionals
		assertEquals(NodeItem.OPTIONAL_TRUE, colorEdge0.getOptionalMinus(color0));
		assertEquals(NodeItem.OPTIONAL_TRUE, colorEdge1.getOptionalMinus(color1));
		assertEquals(NodeItem.OPTIONAL_TRUE, colorEdge2.getOptionalMinus(color2));
		assertEquals(true, cellId0.getIsOptional());
		
	}
	
	@Test
	public void testUnOptionalizeConstrained_normal() throws Exception {

		// load nodegroup
		String jsonPath = "src/test/resources/sampleBatteryThreeCells.json";
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath(jsonPath));
		NodeGroup nodegroup = sgJson.getNodeGroup();
		
		// get pointers to parts
		Node color0 = nodegroup.getNodeBySparqlID("?Color_0");
		Node color1 = nodegroup.getNodeBySparqlID("?Color_1");
		Node color2 = nodegroup.getNodeBySparqlID("?Color_2");
		Node cell0 = nodegroup.getNodeBySparqlID("?Cell_0");
		Node cell1 = nodegroup.getNodeBySparqlID("?Cell_1");
		Node cell2 = nodegroup.getNodeBySparqlID("?Cell_2");
		Node battery = nodegroup.getNodeBySparqlID("?Battery");
		PropertyItem name = battery.getPropertyItem(0);
		PropertyItem cellId0 = cell0.getPropertyItem(0);
		PropertyItem cellId1 = cell1.getPropertyItem(0);
		PropertyItem cellId2 = cell2.getPropertyItem(0);
		NodeItem cellEdge0 = battery.getConnectingNodeItems(cell0).get(0);
		NodeItem cellEdge1 = battery.getConnectingNodeItems(cell1).get(0);
		NodeItem cellEdge2 = battery.getConnectingNodeItems(cell2).get(0);
		NodeItem colorEdge0 = cell0.getConnectingNodeItems(color0).get(0);
		NodeItem colorEdge1 = cell1.getConnectingNodeItems(color1).get(0);
		NodeItem colorEdge2 = cell2.getConnectingNodeItems(color2).get(0);
		
		// set every edge, every return optional
		colorEdge0.setOptionalMinus(color0, NodeItem.OPTIONAL_TRUE);
		colorEdge1.setOptionalMinus(color1, NodeItem.OPTIONAL_TRUE);
		colorEdge2.setOptionalMinus(color2, NodeItem.OPTIONAL_TRUE);
		cellEdge1.setOptionalMinus(cell0, NodeItem.OPTIONAL_TRUE);
		cellEdge1.setOptionalMinus(cell1, NodeItem.OPTIONAL_TRUE);
		cellEdge1.setOptionalMinus(cell2, NodeItem.OPTIONAL_TRUE);
		cellId0.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
		cellId1.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
		cellId2.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
		name.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);

		// set constraint on color0 and color1, cellId1
		color0.setValueConstraint(new ValueConstraint("anything"));
		color1.setValueConstraint(new ValueConstraint("anything"));
		cellId1.setValueConstraint(new ValueConstraint("anything"));
		
		// go: 
		nodegroup.unOptionalizeConstrained();
		
		// check results.  Turned off both colors (leafs) and one intermediate
		assertEquals(NodeItem.OPTIONAL_FALSE, colorEdge0.getOptionalMinus(color0));
		assertEquals(NodeItem.OPTIONAL_FALSE, colorEdge1.getOptionalMinus(color1));
		assertEquals(NodeItem.OPTIONAL_TRUE, colorEdge2.getOptionalMinus(color2));
		assertEquals(NodeItem.OPTIONAL_FALSE, cellEdge0.getOptionalMinus(cell0));
		assertEquals(NodeItem.OPTIONAL_FALSE, cellEdge1.getOptionalMinus(cell1));
		assertEquals(NodeItem.OPTIONAL_TRUE, cellEdge2.getOptionalMinus(cell2));
		assertEquals(true, cellId0.getIsOptional());
		assertEquals(false, cellId1.getIsOptional());  // turned this off too
		assertEquals(true, cellId2.getIsOptional());
		assertEquals(true, name.getIsOptional());
	}
	
	@Test
	public void testUnoptionalizeConstrained_1of3leafs() throws Exception {
		// un-optionalize a leaf by constraining it
		testUnOptionalizeConstrained_one_of_three_leafs("constrain");
		
		// un-optionalize a leaf by declaring it the targetObj in filter query
		testUnOptionalizeConstrained_one_of_three_leafs("filter");
		
		// un-optionalize a leaf by declaring it the targetObj in filter query
		// preserve lack of return and value constraint
		testUnOptionalizeConstrained_one_of_three_leafs("filter1");
	}
	
	public void testUnOptionalizeConstrained_one_of_three_leafs(String mode) throws Exception {

		// load nodegroup
		String jsonPath = "src/test/resources/sampleBatteryThreeCells.json";
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath(jsonPath));
		NodeGroup nodegroup = sgJson.getNodeGroup();
		
		// get pointers to parts
		Node color0 = nodegroup.getNodeBySparqlID("?Color_0");
		Node color1 = nodegroup.getNodeBySparqlID("?Color_1");
		Node color2 = nodegroup.getNodeBySparqlID("?Color_2");
		Node cell0 = nodegroup.getNodeBySparqlID("?Cell_0");
		Node cell1 = nodegroup.getNodeBySparqlID("?Cell_1");
		Node cell2 = nodegroup.getNodeBySparqlID("?Cell_2");
		Node battery = nodegroup.getNodeBySparqlID("?Battery");
		PropertyItem name = battery.getPropertyItem(0);
		PropertyItem cellId0 = cell0.getPropertyItem(0);
		PropertyItem cellId1 = cell1.getPropertyItem(0);
		PropertyItem cellId2 = cell2.getPropertyItem(0);
		NodeItem cellEdge0 = battery.getConnectingNodeItems(cell0).get(0);
		NodeItem cellEdge1 = battery.getConnectingNodeItems(cell1).get(0);
		NodeItem cellEdge2 = battery.getConnectingNodeItems(cell2).get(0);
		NodeItem colorEdge0 = cell0.getConnectingNodeItems(color0).get(0);
		NodeItem colorEdge1 = cell1.getConnectingNodeItems(color1).get(0);
		NodeItem colorEdge2 = cell2.getConnectingNodeItems(color2).get(0);
		
		// set every edge optional
		colorEdge0.setOptionalMinus(color0, NodeItem.OPTIONAL_TRUE);
		colorEdge1.setOptionalMinus(color1, NodeItem.OPTIONAL_TRUE);
		colorEdge2.setOptionalMinus(color2, NodeItem.OPTIONAL_TRUE);
		cellEdge1.setOptionalMinus(cell0, NodeItem.OPTIONAL_TRUE);
		cellEdge1.setOptionalMinus(cell1, NodeItem.OPTIONAL_TRUE);
		cellEdge1.setOptionalMinus(cell2, NodeItem.OPTIONAL_TRUE);
		
		// remove property returns so we have three leaf nodes each: EmptyMain->Empty->Leaf
		// where "Empty" means no returns or constraints
		// so only Leafs have a return.
		cellId0.setIsReturned(false);
		cellId1.setIsReturned(false);
		cellId2.setIsReturned(false);
		name.setIsReturned(false);
		
		if (mode.equals("constrain")) {
			// set constraint on color0 
			color0.setValueConstraint(new ValueConstraint("anything"));
			nodegroup.unOptionalizeConstrained();
			
		} else if (mode.equals("filter")) {
			nodegroup.unOptionalizeConstrained(color0);
			// make sure the process didn't mess with constraint or return
			assertEquals(null, color0.getValueConstraint());
			assertEquals(true, color0.getIsReturned());
			
		} else if (mode.equals("filter1")) {
			color0.setIsReturned(false);
			color0.setValueConstraint(new ValueConstraint("FILTER(?test)"));
			
			nodegroup.unOptionalizeConstrained(color0);
			// make sure the process didn't mess with constraint or return
			assertEquals("FILTER(?test)", color0.getValueConstraint().toString());
			assertEquals(false, color0.getIsReturned());
		}
		
		// check results: unOptionalize only the entire color0 subgraph
		assertEquals(NodeItem.OPTIONAL_FALSE, colorEdge0.getOptionalMinus(color0));
		assertEquals(NodeItem.OPTIONAL_TRUE, colorEdge1.getOptionalMinus(color1));
		assertEquals(NodeItem.OPTIONAL_TRUE, colorEdge2.getOptionalMinus(color2));
		assertEquals(NodeItem.OPTIONAL_FALSE, cellEdge0.getOptionalMinus(cell0));
		assertEquals(NodeItem.OPTIONAL_TRUE, cellEdge1.getOptionalMinus(cell1));
		assertEquals(NodeItem.OPTIONAL_TRUE, cellEdge2.getOptionalMinus(cell2));
		
	}
	@Test
	public void testOptAllUnconstrained_circularity_error() throws Exception {

		// load nodegroup
		String jsonPath = "src/test/resources/sampleBatteryThreeCells.json";
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath(jsonPath));
		NodeGroup nodegroup = sgJson.getNodeGroup();
		
		// get pointers to parts
		Node color1 = nodegroup.getNodeBySparqlID("?Color_1");
		Node color2 = nodegroup.getNodeBySparqlID("?Color_2");
		Node cell2 = nodegroup.getNodeBySparqlID("?Cell_2");
		NodeItem colorEdge2 = cell2.getConnectingNodeItems(color2).get(0);
		
		colorEdge2.pushNode(color1);  // create circularity in nodegroup

		try {
			nodegroup.unOptionalizeConstrained();
			assertTrue("Did not throw circularity exception", false);
		} catch (Exception e) {
			;
		}
		
		
	}
	
}
