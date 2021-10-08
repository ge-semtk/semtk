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

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeGroupItemStr;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.load.utility.ImportSpec;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class NodeGroupTest_IT {
	private static OntologyInfo oInfo = null;
	private static String colorURI = "http://kdl.ge.com/batterydemo#Color";
	private static String colorItemURI = "http://kdl.ge.com/batterydemo#color";
	private static String cellNodeURI = "http://kdl.ge.com/batterydemo#Cell";
	private static String batteryURI = "http://kdl.ge.com/batterydemo#Battery";
	private static String cellItemURI = "http://kdl.ge.com/batterydemo#cell";
	private static String nameItemURI = "http://kdl.ge.com/batterydemo#name";
	private static String badURI = "http://kdl.ge.com/batterydemo#bad";
	private static String badLocal = "bad";

	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		String owlPath = "/sampleBattery.owl";
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(NodeGroupTest_IT.class, owlPath);
		TestGraph.ingest(NodeGroupTest_IT.class, "/sampleBattery.json", "/sampleBattery.csv");
		
		oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false);
	}
	
	@Test
	public void testValidateNodeGroup() throws Exception {
		
		String jsonPath = "/sampleBattery.json";
		NodeGroup nodegroup = TestGraph.getNodeGroupFromResource(this, jsonPath); 
		
		nodegroup.validateAgainstModel(oInfo);
	}
	
	
	@Test
	/**
	 * Test legacy "Exeption on first failure" form of Validate
	 * @throws Exception
	 */
	public void testValidateNodeGroupFail() throws Exception {
		String jsonPath = "/sampleBattery.json";

		// model isn't loaded
		NodeGroup nodegroup = TestGraph.getNodeGroupFromResource(this, jsonPath); 
				
		try {
			OntologyInfo oInfoEmpty = new OntologyInfo();
			nodegroup.validateAgainstModel(oInfoEmpty);
			fail("Model should have been empty but no exception was thrown");
		} catch (Exception e) {
			System.out.println("Exception as expected:" + e.getMessage());
		} 
		
		// now load it: successful
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
			nodegroup = TestGraph.getNodeGroupFromResource(this, jsonPath.replace(".json", suffix+".json")); 
			try {
				nodegroup.validateAgainstModel(oInfo);
				fail("Node URI should be illegal but no exception was thrown");
			} catch (Exception e) {
				System.out.println("Exception as expected:" + e.getMessage());
			} 
		}
	}
	
	/*
	 * Helper to run inflate and Validate and check expected results
	 */
	private void inflateAndValidate(NodeGroup ng, ImportSpec importSpec, String [] expectErrors, NodeGroupItemStr [] expectItems, String [] expectWarnings) throws Exception {
		ArrayList<String> modelErrMsgs = new ArrayList<String>();
		ArrayList<NodeGroupItemStr> invalidItems = new  ArrayList<NodeGroupItemStr>();
		ArrayList<String> warnings = new ArrayList<String>();

		ng.inflateAndValidate(oInfo, importSpec, modelErrMsgs, invalidItems, warnings);
		
		assertEquals("wrong number of error messages\n" + String.join(",", modelErrMsgs) + "\n", expectErrors.length, modelErrMsgs.size());
		assertEquals("wrong number of invalid items", expectItems.length, invalidItems.size());
		assertEquals("wrong number of warnings\n" + String.join(",", warnings)+ "\n", expectWarnings.length, warnings.size());
		
		for (String expectErr : expectErrors) {
			boolean found = false;
			for (String modelErr : modelErrMsgs) {
				if (modelErr.contains(expectErr)) {
					found = true;
				}
			}
			assertTrue("Expected error was not generated: " + expectErr, found);
		}
		
		for (NodeGroupItemStr expectItem : expectItems) {
			boolean found = false;
			for (NodeGroupItemStr modelItem : invalidItems) {
				if (modelItem.getStr().equals(expectItem.getStr())) {
					found = true;
				}
			}
			assertTrue("Expected error item was not generated: " + expectItem.getStr(), found);
		}
		
		for (String expectWarn : expectWarnings) {
			boolean found = false;
			for (String w : warnings) {
				if (w.contains(expectWarn)) {
					found = true;
				}
			}
			assertTrue("Expected warning was not generated: " + expectWarn, found);
		}
		
	}
	
	@Test
	/**
	 * inflateAndValidate: 
	 * 		Node with bad URI and no properties or nodes
	 *    		1a. error, added
	 */
	public void testValidateBadNodeURI() throws Exception {
		String jsonPath = "/sampleBattery.json";

		NodeGroup nodegroup = TestGraph.getNodeGroupFromResource(this, jsonPath); 
		
		String badURI = "http://bad/URI";
		Node n = new Node("?Invalid", null, null, badURI, nodegroup);
		nodegroup.addOneNode(n, null, null, null);
		inflateAndValidate(nodegroup, 
				null,
				new String [] {badURI} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(n)}, 
				new String [] {});
		assertTrue("Invalid node was not inflated into nodegroup", nodegroup.getNodeBySparqlID("?Invalid") != null);

	}
	
	@Test
	/**
	 * inflateAndValidate: 
	 * 		Property item unknown (domain)
	 * 			2a. if used:  error, added
	 *  		2b. not used: warning, deleted
	 */
	public void testValidateBadPropItemDomain() throws Exception {
		String jsonPath = "/sampleBattery.json";

		NodeGroup nodegroup = TestGraph.getNodeGroupFromResource(this, jsonPath); 
		
		String badURI = "http://bad_URI";
		String badKeyname = "bad_URI";
		Node n = nodegroup.getNodeBySparqlID("?Battery");
		PropertyItem pi = new PropertyItem(badKeyname, XSDSupportedType.STRING.getSimpleName(), XSDSupportedType.STRING.getPrefixedName(), badURI);
	
		n.getPropertyItems().add(pi);
		nodegroup.changeSparqlID(pi, "?random");
		pi.setIsReturned(true);
		
		// used: error
		inflateAndValidate(nodegroup, 
				null,
				new String [] {badURI} , 
				new NodeGroupItemStr [] { new NodeGroupItemStr(n, pi)}, 
				new String [] {});
		assertTrue("Invalid property item was not inflated into nodegroup", n.getPropertyItemBySparqlID("?random") != null);
		
		// unused: warning and delete
		pi.setIsReturned(false);
		inflateAndValidate(nodegroup, 
				null,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {badKeyname});
		assertTrue("Invalid unused property item was not removed from nodegroup", n.getPropertyItemBySparqlID("?random") == null);

	}
	
	@Test
	/**
	 * inflateAndValidate: 
	 * 		Property item unknown (range)
	 * 			3a. if used:  error, added
	 *  		3b. not used: warning, corrected
	 */
	public void testValidateBadPropItemRange() throws Exception {
		String jsonPath = "/sampleBattery.json";

		NodeGroup nodegroup = TestGraph.getNodeGroupFromResource(this, jsonPath); 
		
		String badKeyname = "birthday";
		Node n = nodegroup.getNodeBySparqlID("?Battery");
		
		// find "birthday" propItem, and change range
		PropertyItem pi = null;
		for (PropertyItem p : n.getPropertyItems()) {
			if (p.getKeyName().equals(badKeyname)) {
				p.changeValueType(XSDSupportedType.DOUBLE);
				pi = p;
				break;
			}
		}
		pi.setValueConstraint(new ValueConstraint("filter(whatever)"));
		
		// used: error
		inflateAndValidate(nodegroup, 
				null,
				new String [] {badKeyname} , 
				new NodeGroupItemStr [] { new NodeGroupItemStr(n, pi)}, 
				new String [] {});
		assertTrue("Invalid property item range was not inflated into nodegroup", n.getPropertyByKeyname(badKeyname) != null);
		
		// unused: warning 
		pi.setValueConstraint(null);
		inflateAndValidate(nodegroup, 
				null,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {badKeyname});
		
		// unused: still there, and fixed range
		pi = n.getPropertyByKeyname(badKeyname);
		assertTrue("Invalid unused property item range was not inflated into nodegroup", pi != null);
		assertEquals("Invid unused property item range was not corrected", XSDSupportedType.DATETIME, pi.getValueType());

	}
	
	@Test
	/**
	 * inflateAndValidate: 
	 * 		Node item unknown (domain)
	 * 			4a. if used:  error, added,  items all targets
	 *  		4b. not used: warning, deleted
	 */
	public void testValidateBadNodeItemDomain() throws Exception {

		// build a nodegroup
		NodeGroup nodegroup = new NodeGroup();
		
		// add Battery
		// change cell nodeitem to bad
		nodegroup.addNode(batteryURI, oInfo);
		Node batteryNode = nodegroup.getNode(0);
		NodeItem cellItem = batteryNode.getNodeItem(cellItemURI);
		cellItem.changeUriConnect(badURI);
		
		// 4b unused: warning, delete
		inflateAndValidate(nodegroup, 
				null,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {badURI});
	
		assertEquals("Invalid unused node item domain was not removed", null, batteryNode.getNodeItem(badURI));
		cellItem = batteryNode.getNodeItem(cellItemURI);
		assertTrue("Missing Node item was not inflated", cellItem != null);


		// 4a used: error 
		Node cell1 = nodegroup.addClassFirstPath(cellNodeURI, oInfo);
		cellItem.changeUriConnect(badURI);
		
		inflateAndValidate(nodegroup, 
				null,
				new String [] {badURI} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(batteryNode, cellItem, cell1), new NodeGroupItemStr(batteryNode, cellItem, null)}, 
				new String [] {});
		
		// used: still there
		cellItem = batteryNode.getNodeItem(badURI);
		assertTrue("Invalid unused property item range was not inflated into nodegroup", cellItem != null);

	}
	

	@Test
	/**
	 * inflateAndValidate: 
	 * 		Node item bad range
	 * 			5a. if connected to model-correct targets: warn, fix
	 *          5b. if connected to nothing, warn, fix
	 *          5c. if connected to model-good & model-bad:  error, added, items all targets
	 */
	public void testValidateBadNodeItemRange() throws Exception {

		// build a nodegroup
		NodeGroup nodegroup = new NodeGroup();
		
		Node batteryNode = nodegroup.addNode(batteryURI, oInfo);

		// 5a. targets are model-correct, range wrong: warn and fix 
		Node cell1 = nodegroup.addClassFirstPath(cellNodeURI, oInfo);
		Node cell2 = nodegroup.addClassFirstPath(cellNodeURI, oInfo);
		NodeItem cellItem = batteryNode.getNodeItem(cellItemURI);
		cellItem.changeUriValueType(badURI);
		
		inflateAndValidate(nodegroup, 
				null,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {badLocal});
		assertTrue("Invalid node item range connected to model-valid nodes did not get its range corrected", batteryNode.getNodeItem(cellItemURI) != null);

		// 5b. Delete the target connections and results should be the same
		nodegroup.deleteNode(cell1, false);
		nodegroup.deleteNode(cell2, false);
		cellItem.changeUriValueType(badURI);
		
		inflateAndValidate(nodegroup, 
				null,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {badLocal});
		assertTrue("Invalid node item range un-connected did not get its range corrected", batteryNode.getNodeItem(cellItemURI) != null);
		
		// 5c. connect to a bad node colorNode, and a good node cell1, with bad range
		Node colorNode = nodegroup.addNode(colorURI, oInfo);
		batteryNode.setConnection(colorNode, cellItemURI);  // bad target
		cell1 = nodegroup.addNode(cellNodeURI, oInfo);
		batteryNode.setConnection(cell1, cellItemURI); // good target
		cellItem.changeUriValueType(badURI);  // bad range too
		
		inflateAndValidate(nodegroup, 
				null,
				new String [] {badLocal} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(batteryNode, cellItem, null), new NodeGroupItemStr(batteryNode, cellItem, colorNode), new NodeGroupItemStr(batteryNode, cellItem, cell1)}, 
				new String [] {});
		
		// used: still there with bad range
		cellItem = batteryNode.getNodeItem(cellItemURI);
		assertTrue("Invalid ranged node item with one good, one bad connection was removed from nodegroup", cellItem != null);
		assertEquals("After fixing nodeItem domain, node has wrong number of nodeItems", 1, batteryNode.getNodeItemList().size());

	}
	
	@Test
	/**
	 * inflateAndValidate: 
	 * 		Node item ok range
	 * 			6a. connected to targets which don't match range:  Error, added, items
	 */
	public void testValidateBadNodeItemTargets() throws Exception {

		// build a nodegroup
		NodeGroup nodegroup = new NodeGroup();
		
		
		
		Node batteryNode = nodegroup.addNode(batteryURI, oInfo);

		
		// connect battery through cell to a color (bad) and a cell (ok)
		Node colorNode = nodegroup.addNode(colorURI, oInfo);
		batteryNode.setConnection(colorNode, cellItemURI);  // bad target
		Node cell1 = nodegroup.addNode(cellNodeURI, oInfo);
		batteryNode.setConnection(cell1, cellItemURI); // good target
		NodeItem ni = batteryNode.getNodeItem(cellItemURI);
		
		inflateAndValidate(nodegroup, 
				null,
				new String [] {colorNode.getSparqlID()} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(batteryNode, ni, colorNode)}, 
				new String [] {});
		
		// used: still there with bad URI
		ni = batteryNode.getNodeItem(cellItemURI);
		assertTrue("Correctly ranged node item with one good, one bad connection was removed from nodegroup", ni != null);

	}
	
	@Test
	public void testRepair1a_BadNodeURI() throws Exception {		
		// bad ?Battery URI
		// battery name is unused except importSpec
		
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this, "sampleBattery_invalid_1a.json");
		NodeGroup nodegroup = sgjson.getNodeGroup();
		ImportSpec importSpec = sgjson.getImportSpec();
		Node batteryNode = nodegroup.getNodeBySparqlID("?Battery");
		Node cellNode = nodegroup.getNodeBySparqlID("?Cell");
		PropertyItem name = batteryNode.getPropertyByKeyname("name");
		NodeItem cell = batteryNode.getNodeItem(cellItemURI);
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {"class", "property", "edge"} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(batteryNode), new NodeGroupItemStr(batteryNode, name), new NodeGroupItemStr(batteryNode, cell, cellNode), new NodeGroupItemStr(batteryNode, cell, null)}, 
				new String [] {});
		
		// make sure propItem name was not deflated
		assertTrue("Property 'name' which is only used in import was not inflated into invalid Battery node", batteryNode.getPropertyByKeyname("name") != null);
		
		// fix
		nodegroup.changeItemDomain(batteryNode, batteryURI);
		
		// no validation errors remain
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {});
	}
	
	
	
	@Test
	public void testRepair2a_PropBadDomain() throws Exception {		
		// bad ?Battery->name_BAD
		// Property is part of union that when working, should return three rows
		// Property is also in the importSpec
		
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this, "sampleBattery_invalid_2a.json");
		NodeGroup nodegroup = sgjson.getNodeGroup();
		ImportSpec importSpec = sgjson.getImportSpec();
		Node batteryNode = nodegroup.getNodeBySparqlID("?Battery");
		PropertyItem nameProp = batteryNode.getPropertyByKeyname("name_BAD");
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {"_BAD"} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(batteryNode, nameProp)}, 
				new String [] {});
		
		// fix
		nodegroup.changeItemDomain(batteryNode, nameProp, nameItemURI);
		importSpec.changePropertyDomain(batteryNode.getSparqlID(), "name_BAD", nameItemURI);
		
		// no validation errors remain
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {});
		
		// union query still works
		sgjson.setNodeGroup(nodegroup);
		Table resTab = TestGraph.execTableSelect(sgjson);
		assertEquals("Corrected nodegroup did not return correct rows of data", 3, resTab.getNumRows());
	}
	
	@Test
	public void testRepair3a_PropBadRange() throws Exception {		
		// bad ?Battery->name range set to int
		// Property is part of union that when working, should return three rows
		// Property is also in the importSpec
		
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this, "sampleBattery_invalid_3a.json");
		NodeGroup nodegroup = sgjson.getNodeGroup();
		ImportSpec importSpec = sgjson.getImportSpec();
		Node batteryNode = nodegroup.getNodeBySparqlID("?Battery");
		PropertyItem nameProp = batteryNode.getPropertyByKeyname("name");
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {"name"} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(batteryNode, nameProp)}, 
				new String [] {});
		
		// fix
		nodegroup.changeItemRange(nameProp, "http://www.w3.org/2001/XMLSchema#string");
		
		// no validation errors remain
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {});
		
		// union query still works
		sgjson.setNodeGroup(nodegroup);
		Table resTab = TestGraph.execTableSelect(sgjson);
		assertEquals("Corrected nodegroup did not return correct rows of data", 3, resTab.getNumRows());
	}
	
	@Test
	public void testRepair4a_NItemBadDomain() throws Exception {		
		// bad ?Cell->color_BAD
		// NodeItem is part of union that when working, should return three rows
		
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this, "sampleBattery_invalid_4a.json");
		NodeGroup nodegroup = sgjson.getNodeGroup();
		ImportSpec importSpec = sgjson.getImportSpec();
		Node cellNode = nodegroup.getNodeBySparqlID("?Cell");
		NodeItem colorItem = cellNode.getNodeItem(colorItemURI + "_BAD");
		Node colorNode = nodegroup.getNodeBySparqlID("?Color");
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {"_BAD"} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(cellNode, colorItem, null), new NodeGroupItemStr(cellNode, colorItem, colorNode)}, 
				new String [] {});
		
		// fix
		nodegroup.changeItemDomain(cellNode, colorItem, null, colorItemURI);
		
		// no validation errors remain
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {});
		
		// union query still works
		sgjson.setNodeGroup(nodegroup);
		Table resTab = TestGraph.execTableSelect(sgjson);
		assertEquals("Corrected nodegroup did not return correct rows of data", 3, resTab.getNumRows());
	}
	
	@Test
	public void testRepair5a_NItemBadRange() throws Exception {		
		// color nodeItem range set to invalid
		// connected color node URI set to BATTERY so the nodeItem can't be auto-fixed
		
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this, "sampleBattery_invalid_5a.json");
		NodeGroup nodegroup = sgjson.getNodeGroup();
		ImportSpec importSpec = sgjson.getImportSpec();
		Node cellNode = nodegroup.getNodeBySparqlID("?Cell");
		NodeItem colorItem = cellNode.getNodeItem(colorItemURI);
		Node colorNode = nodegroup.getNodeBySparqlID("?Color");
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {"_BAD"} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(cellNode, colorItem, null), new NodeGroupItemStr(cellNode, colorItem, colorNode)}, 
				new String [] {});
		
		// fix
		nodegroup.changeItemRange(colorItem, colorURI);
		
		// no validation errors remain
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {"does not allow connection"} , 
				new NodeGroupItemStr [] {new NodeGroupItemStr(cellNode, colorItem, colorNode)}, 
				new String [] {});
		
		nodegroup.changeItemDomain(colorNode, colorURI);
		importSpec.changeNodeDomain(colorNode.getSparqlID(), colorURI);
		
		// no validation errors remain
		inflateAndValidate(nodegroup, 
				importSpec,
				new String [] {} , 
				new NodeGroupItemStr [] {}, 
				new String [] {"#cell", "#name", "#birthday"});   // deleted unused properties
				
		// union query still works
		sgjson.setNodeGroup(nodegroup);
		Table resTab = TestGraph.execTableSelect(sgjson);
		assertEquals("Corrected nodegroup did not return correct rows of data", 3, resTab.getNumRows());
	}
	
}
