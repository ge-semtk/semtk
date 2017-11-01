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

// PEC LICENSE NOTE soft process names

package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;


public class LoadNodeGroupFromConstructQueryResultsTest {
	
	@Test 
	public void parseConstructQueryResults() throws Exception {
		
		// get some sample JSON, as would see from the Virtuoso construct query (return as JSON-LD)
		String constructQueryResultJSON = "{ \"@graph\": [ " 
				+ "{ \"@id\": \"http://research.ge.com/soft/data#Cell_EEEE298\",  \"@type\" : [ { \"@id\": \"http://research.ge.com/soft/testconfig#Cell\"} ] , \"http://research.ge.com/soft/testconfig#cellId\" : [ { \"@value\" : \"EEEE298\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] , \"http://research.ge.com/soft/testconfig#TShirtPrinting\" : [ { \"@id\": \"http://research.ge.com/soft/data#SoftShue_EEEE298_Freedom_2015-06-12\"} { \"@id\": \"http://research.ge.com/soft/data#SoftShue_EEEE222\"} ] ,  \"http://research.ge.com/soft/testconfig#sizeInches\" : [ {\"@value\":\"2\",\"@type\":\"http://www.w3.org/2001/XMLSchema#int\"} {\"@value\":\"3\",\"@type\":\"http://www.w3.org/2001/XMLSchema#int\"} ] } , "
				+ "{ \"@id\": \"http://research.ge.com/soft/data#SoftShue_EEEE298_Freedom_2015-06-12\", \"@type\" : [ { \"@id\": \"http://research.ge.com/soft/testconfig#TShirtPrinting\"} ] , \"http://research.ge.com/soft/testconfig#mouseLot\" : [ { \"@value\" : \"50416-1\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] ,  \"http://research.ge.com/soft/testconfig#mouseMaterial\" : [ { \"@value\" : \"Toothmouse\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] , \"http://research.ge.com/soft/testconfig#mouseVendor\" : [ { \"@value\" : \"Target\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] }, "
				+ "{ \"@id\": \"http://research.ge.com/soft/data#SoftShue_EEEE222\", \"@type\" : [ { \"@id\": \"http://research.ge.com/soft/testconfig#TShirtPrinting\"} ] , \"http://research.ge.com/soft/testconfig#mouseLot\" : [ { \"@value\" : \"50416-1\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] ,  \"http://research.ge.com/soft/testconfig#mouseMaterial\" : [ { \"@value\" : \"Toothmouse\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] , \"http://research.ge.com/soft/testconfig#mouseVendor\" : [ { \"@value\" : \"Target\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] }, "		
				+ "] }";		
		JSONObject json = (JSONObject) (new JSONParser()).parse(constructQueryResultJSON);
		
		// now get the NodeGroup
		NodeGroup ng = NodeGroup.fromConstructJSON(json);
		
		assertEquals(ng.getNodeList().size(),3);  // 1 cell, 2 screen printings
		
		Node cellNode = ng.getNodeBySparqlID("?Cell");
		assertEquals(cellNode.getFullUriName(),"http://research.ge.com/soft/testconfig#Cell");  
		assertEquals(cellNode.getInstanceValue(),"http://research.ge.com/soft/data#Cell_EEEE298");		
		assertEquals(cellNode.getPropertyItems().size(), 2);
		assertEquals(cellNode.getPropertyItems().get(0).getInstanceValues().size(),2);			// cell id
		
		PropertyItem pi = cellNode.getPropertyByKeyname("cellId");
		assertTrue(pi.getInstanceValues().get(0).equals("EEEE298"));  // cell id
		
		pi = cellNode.getPropertyByKeyname("sizeInches");
		assertEquals(pi.getInstanceValues().size(),2);		// sizeInches 
		assertEquals(pi.getInstanceValues().get(0),"2");  	// sizeInches 
		assertEquals(pi.getInstanceValues().get(1),"3");  	// sizeInches 
		
		assertEquals(cellNode.getNodeItemList().size(),1);
		assertEquals(cellNode.getNodeItemList().get(0).getUriConnectBy(),"http://research.ge.com/soft/testconfig#TShirtPrinting");
		assertEquals(cellNode.getNodeItemList().get(0).getUriValueType(),"http://research.ge.com/soft/testconfig#TShirtPrinting");
		assertEquals(cellNode.getNodeItemList().get(0).getNodeList().size(),2);
		assertEquals(cellNode.getNodeItemList().get(0).getNodeList().get(0).getInstanceValue(),"http://research.ge.com/soft/data#SoftShue_EEEE298_Freedom_2015-06-12");
		assertEquals(cellNode.getNodeItemList().get(0).getNodeList().get(1).getInstanceValue(),"http://research.ge.com/soft/data#SoftShue_EEEE222");
		assertTrue(cellNode.getNodeItemList().get(0).getConnected());
		assertTrue(cellNode.getIsReturned());
		assertEquals(cellNode.getConnectedNodes().size(), 2);  // cell connected to 2  printing nodes
		
		Node TShirtPrintingNode = ng.getNodeBySparqlID("?TShirtPrinting");
		assertEquals(TShirtPrintingNode.getFullUriName(),"http://research.ge.com/soft/testconfig#TShirtPrinting");
		assertEquals(TShirtPrintingNode.getInstanceValue(),"http://research.ge.com/soft/data#SoftShue_EEEE298_Freedom_2015-06-12");
		assertEquals(TShirtPrintingNode.getPropertyItems().size(),3);
		assertEquals(TShirtPrintingNode.getNodeItemList().size(),0);
		
		pi = TShirtPrintingNode.getPropertyByKeyname("mouseLot");
		assertEquals(pi.getUriRelationship(),"http://research.ge.com/soft/testconfig#mouseLot");
		assertEquals(pi.getInstanceValues().get(0),"50416-1");
		assertEquals(pi.getValueType(),"string");
		assertTrue(pi.getIsReturned());
		assertEquals(TShirtPrintingNode.getConnectedNodes().size(), 0);
		
		Node TShirtPrintingNode1 = ng.getNodeBySparqlID("?TShirtPrinting_0");
		assertEquals(TShirtPrintingNode1.getFullUriName(),"http://research.ge.com/soft/testconfig#TShirtPrinting");
		assertEquals(TShirtPrintingNode1.getInstanceValue(),"http://research.ge.com/soft/data#SoftShue_EEEE222");
	
	}
	
	/**
	 * Confirm throws exception if it's not a @graph JSON object
	 */
	@Test 
	public void testNoGraphKey() throws Exception {
		
		NodeGroup ngTest = null;
		boolean thrown = false;
		try {
			// note that this JSON has "@NOT-GRAPH" instead of "@graph"
			String constructQueryResultJSON = "{ \"@NOT-GRAPH\": [ { \"@id\": \"http://research.ge.com/soft/data#Cell_EEEE298\",  \"@type\" : [ { \"@id\": \"http://research.ge.com/soft/testconfig#Cell\"} ] , \"http://research.ge.com/soft/testconfig#cellId\" : [ { \"@value\" : \"EEEE298\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] , \"http://research.ge.com/soft/testconfig#TShirtPrinting\" : [ { \"@id\": \"http://research.ge.com/soft/data#SoftShue_EEEE298_Freedom_2015-06-12\"} ] ,  \"http://research.ge.com/soft/testconfig#sizeInches\" : [ {\"@value\":\"2\",\"@type\":\"http://www.w3.org/2001/XMLSchema#int\"} ] } , { \"@id\": \"http://research.ge.com/soft/data#SoftShue_EEEE298_Freedom_2015-06-12\", \"@type\" : [ { \"@id\": \"http://research.ge.com/soft/testconfig#TShirtPrinting\"} ] , \"http://research.ge.com/soft/testconfig#mouseLot\" : [ { \"@value\" : \"50416-1\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] ,  \"http://research.ge.com/soft/testconfig#mouseMaterial\" : [ { \"@value\" : \"Toothmouse\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] , \"http://research.ge.com/soft/testconfig#mouseVendor\" : [ { \"@value\" : \"Target\" , \"@type\" : \"http://www.w3.org/2001/XMLSchema#string\" } ] } ] }";
			JSONObject json = (JSONObject) (new JSONParser()).parse(constructQueryResultJSON);
			ngTest = NodeGroup.fromConstructJSON(json);
		} catch (Exception e) {
			thrown = true;
		}
		assertEquals(0, ngTest.getNodeCount());	
	}	
	
}
