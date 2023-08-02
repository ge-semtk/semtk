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
import com.ge.research.semtk.load.utility.IngestionNodegroupBuilder;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
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
		TestGraph.uploadOwlResource(this, "sampleBattery.owl");		
		
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

	// Test Import edge cases.
	//
	// Test combinations where node is both looked up by a property and passed in as a URI:
	//    URI lookup -> empty data, missing, success
	//    Node URI -> empty, illegal, success
	//  where "success" in both might be same or might be different values
	//
	//  Enumerated and non-enumerated
	// 
	//  Model:
	//    Person is lookup by name create if missing
	//    Parent is enum, looked by by name error if missing
	//    Child is looked up by name, error if missing
	//    Friend has no lookups.
	@Test
	public void test_UriLookupAndNodeUri() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "Family.owl");		
				
		String csv;
		Table tab;
		int rows;
	
		// empty uri and lookup-by-name, no lookup on the friend
		// no-op.  no errors.
		csv = "name, Friend, friend_name\n" +
			  "Dad,,\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		assertEquals("Wrong number of rows returned: \n" + tab.toCSVString(), 6, tab.getNumRows());
		
		// empty uri and lookup-by-name, is lookup on child
		// no-op. no errors.
		csv = "name, Child, child_name\n" +
			  "Dad,,\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		assertEquals("Wrong number of rows returned: \n" + tab.toCSVString(), 6, tab.getNumRows());
		
		// empty uri. lookup-by-name of Greg, is lookup on child
		// insert relationship
		csv = "name, Child, child_name\n" +
			  "Dad,,Greg\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert child: \n" + tab.toCSVString(), "Greg", tab.getCell(1, 2));
		
		// uri: Anthony. lookup-by-name: none on Friend
		// insert relationship
		csv = "name, Friend, friend_name\n" +
			  "Dad,http://Family#Anthony,\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert friend: \n" + tab.toCSVString(), "Anthony", tab.getCell(1, 4));
		
		// uri: Greg. lookup-by-name: Empty on child
		// insert relationship
		csv = "name, Child, child_name\n" +
			  "Mom,http://Family#Greg,\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert child: \n" + tab.toCSVString(), "Greg", tab.getCell(4, 2));
		
		
		// uri: Greg. lookup-by-name: Greg
		// insert relationship
		csv = "name, Child, child_name\n" +
			  "Anthony,http://Family#Greg,Greg\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert child: \n" + tab.toCSVString(), "Greg", tab.getCell(0, 2));
		
		// uri: Greg. lookup-by-name: Jeff   
		// ignore the Node URI since lookup succeeded
		csv = "name, Child, child_name\n" +
			  "Greg,http://Family#Jeff,Anthony\n";
		
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert lookup child: \n" + tab.toCSVString(), "Anthony", tab.getCell(2, 2));

		// uri: Bad. lookup-by-name: none on Friend 
		// ERROR
		csv = "name, Friend, friend_name\n" +
			  "Dad,http://Family#Anthony^,\n";
		try {
			TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
			fail("Missing expected exception on bad URI format");
		} catch (Exception e) {
			
			assertTrue("Exception is missing 'Anthony^'", e.getMessage().contains("Anthony^"));
		}
		
		// uri: Bad. lookup-by-name: empty on Child 
		// ERROR
		csv = "name, Child, child_name\n" +
			  "Dad,http://Family#Anthony^,\n";
		try {
			TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
			fail("Missing expected exception on bad URI format");
		} catch (Exception e) {
			
			assertTrue("Exception is missing 'Anthony^'", e.getMessage().contains("Anthony^"));
		}
		
		// uri: Bad. lookup-by-name: Anthony on Child
		// ERROR
		csv = "name, Child, child_name\n" +
			  "Dad,http://Family#Anthony^,Anthony\n";
		try {
			TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
			fail("Missing expected exception on bad URI format");
		} catch (Exception e) {
			
			assertTrue("Exception is missing 'Anthony^'", e.getMessage().contains("Anthony^"));
		}
		
	}
	
	// Repeat tests above with enumerated class Parent instead of Child
	@Test
	public void test_UriLookupAndNodeUriEnum() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "Family.owl");		
				
		String csv;
		Table tab;
		int rows;
	
		
		// empty uri and lookup-by-name, is lookup on Parent
		// no-op. no errors.
		csv = "name, Parent, parent_name\n" +
			  "Greg,,\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		assertEquals("Wrong number of rows returned: \n" + tab.toCSVString(), 6, tab.getNumRows());
		
		// empty uri. lookup-by-name of Dad, is lookup on Parent
		// insert relationship
		csv = "name, Parent, parent_name\n" +
			  "Greg,,Dad\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert parent: \n" + tab.toCSVString(), "Dad", tab.getCell(2, 6));
		
		// uri: Dad. lookup-by-name: Empty on parent
		// insert relationship
		csv = "name, Parent, parent_name\n" +
			  "Jeff,http://Family#Dad,\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert child: \n" + tab.toCSVString(), "Dad", tab.getCell(3, 6));
		
		
		// uri: Greg. lookup-by-name: Greg
		// insert relationship
		csv = "name, Parent, parent_name\n" +
			  "Anthony,http://Family#Dad,Dad\n";
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert child: \n" + tab.toCSVString(), "Dad", tab.getCell(0, 6));
		
		// uri: Greg. lookup-by-name: Jeff   
		// ignore the Node URI since lookup succeeded
		csv = "name, Parent, parent_name\n" +
			  "Greg,http://Family#Mom,Dad\n";
		
		TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
		tab = TestGraph.execSelectFromResource(this.getClass(), "/family.json");
		tab.sortByAllCols();
		assertEquals("Wrong number of rows returned: " + tab.toCSVString(), 6, tab.getNumRows());
		assertEquals("Did not insert lookup child: \n" + tab.toCSVString(), "Dad", tab.getCell(2, 6));

		
		
		// uri: Bad. lookup-by-name: empty on Child 
		// ERROR
		csv = "name, Parent, parent_name\n" +
				"Anthondy,http://Family#Dad^,\n";
		try {
			TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
			fail("Missing expected exception on bad URI format");
		} catch (Exception e) {
			
			assertTrue("Exception is missing 'Dad^'", e.getMessage().contains("Dad^"));
		}
		
		// uri: Bad. lookup-by-name: Anthony on Child
		// ERROR
		csv = "name, Parent, parent_name\n" +
			  "Anthony,http://Family#Dad^,Dad\n";
		try {
			TestGraph.ingestCsvString(this.getClass(), "/family.json", csv);
			fail("Missing expected exception on bad URI format");
		} catch (Exception e) {
			
			assertTrue("Exception is missing 'Dad^'", e.getMessage().contains("Dad^"));
		}		
	}

}
