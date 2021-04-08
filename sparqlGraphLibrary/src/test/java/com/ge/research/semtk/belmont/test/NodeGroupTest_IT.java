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

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeGroupItemStr;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class NodeGroupTest_IT {
	private static OntologyInfo oInfo = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		String owlPath = "/sampleBattery.owl";
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(NodeGroupTest_IT.class, owlPath);
		
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
	private void inflateAndValidate(NodeGroup ng, String [] expectErrors, Object [] expectItems, String [] expectWarnings) throws Exception {
		ArrayList<String> modelErrMsgs = new ArrayList<String>();
		ArrayList<NodeGroupItemStr> invalidItems = new  ArrayList<NodeGroupItemStr>();
		ArrayList<String> warnings = new ArrayList<String>();

		ng.inflateAndValidate(oInfo, modelErrMsgs, invalidItems, warnings);
		
		assertEquals("wrong number of error messages", expectErrors.length, modelErrMsgs.size());
		assertEquals("wrong number of invalid items", expectItems.length, invalidItems.size());
		assertEquals("wrong number of error warnings", expectWarnings.length, warnings.size());
		
		for (String err : expectErrors) {
			boolean found = false;
			for (String modelErr : modelErrMsgs) {
				if (modelErr.contains(err)) {
					found = true;
				}
			}
			assertTrue("Expected error was not generated: " + err, found);
		}
		
		for (Object item : expectItems) {
			boolean found = false;
			for (NodeGroupItemStr itemStr : invalidItems) {
				if (item instanceof Node && itemStr.getSnode() == item) {
					found = true;
				} else if (item instanceof PropertyItem && itemStr.getpItem() == item) {
					found = true;
				} else if (item instanceof NodeItem && itemStr.getnItem() == item) {
					found = true;
				}
			}
			String name = "";
			if (item instanceof Node) {
				name = ((Node)item).getSparqlID();
			} else if (item instanceof PropertyItem) {
				name = ((PropertyItem)item).getKeyName();
			} else if (item instanceof NodeItem) {
				name = ((NodeItem)item).getUriConnectBy();
			}
			
			assertTrue("Expected invalid item was not generated: " + name, found);
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
	 * inflateAndValidate: Node with bad URI and no properties or nodes
	 *    	error, added
	 */
	public void testValidateBadNodeURI() throws Exception {
		String jsonPath = "/sampleBattery.json";

		NodeGroup nodegroup = TestGraph.getNodeGroupFromResource(this, jsonPath); 
		
		String badURI = "http://bad/URI";
		Node n = new Node("?Invalid", null, null, badURI, nodegroup);
		nodegroup.addOneNode(n, null, null, null);
		inflateAndValidate(nodegroup, 
				new String [] {badURI} , 
				new Object [] {n}, 
				new String [] {});
		assertTrue("Invalid node was not inflated into nodegroup", nodegroup.getNodeBySparqlID("?Invalid") != null);

	}
	
	@Test
	/**
	 * inflateAndValidate: Property item unknown (domain)
	 * 		error if used, added
	 *  	warning if not used, deleted
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
				new String [] {badURI} , 
				new Object [] {pi}, 
				new String [] {});
		assertTrue("Invalid property item was not inflated into nodegroup", n.getPropertyItemBySparqlID("?random") != null);
		
		// unused: warning and delete
		pi.setIsReturned(false);
		inflateAndValidate(nodegroup, 
				new String [] {} , 
				new Object [] {}, 
				new String [] {badKeyname});
		assertTrue("Invalid unused property item was not removed from nodegroup", n.getPropertyItemBySparqlID("?random") == null);

	}
	

}
