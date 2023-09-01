/**
 ** Copyright 2016-2020 General Electric Company
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

package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

/**
 * Tests the TestGraph SparqlEndpointInterface (integrationtest.properties)
 * Could be Virtuoso, Neptune, Fuseki... etc.
 * @author 200001934
 */
public class SparqlEndpointInterfaceTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();		
	}
	
	@Test 
	public void testQuery() throws Exception{
		TestGraph.clearGraph();
		TestGraph.initGraphWithData(this.getClass(), "sampleBattery");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromResource(this, "/sampleBattery.json");
		TestGraph.queryAndCheckResults(sgJson, this, "/sampleBatteryResults.csv");
		assertEquals(TestGraph.getSei().getNumTriples(), 74);	
	}
	
	@Test
	public void testSelectAskCount() throws Exception {
		// Test combos of running queries to tables
		// select, ask count
		// return something, empty, error
		
		TestGraph.clearGraph();
		TestGraph.initGraphWithData(this.getClass(), "sampleBattery");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromResource(this, "/sampleBattery.json");
		NodeGroup ng = sgJson.getNodeGroup(TestGraph.getOInfo());
		
		// build queries
		String selectQuery = ng.generateSparqlSelect();
		String askQuery = ng.generateSparqlAsk();
		String countQuery = ng.generateSparqlCount();
		
		// build queries that return nothing
		PropertyItem pItem = ng.getPropertyItemBySparqlID("CellId");
		pItem.addConstraint(ValueConstraint.buildValuesConstraint(pItem, "zizzyballooba", TestGraph.getSei()));
		
		String selectEmpty = ng.generateSparqlSelect();
		String askEmpty = ng.generateSparqlAsk();
		String countEmpty = ng.generateSparqlCount();

		// build queries with syntax errors
		String selectErr = selectQuery.replaceAll("\\{", "X");
		String askErr = selectQuery.replaceAll("\\{", "X");
		String countErr = selectQuery.replaceAll("\\{", "X");
		
		// run all queries that return something to Table
		Table tab;
		tab = TestGraph.execQueryToTable(selectQuery);  // select to table
		assertTrue("select to table did not return rows", tab.getNumRows() > 0);
		tab = TestGraph.execQueryToTable(askQuery);     // ask to table
		assertTrue("ask to table did not return rows", tab.getNumRows() > 0);
		tab = TestGraph.execQueryToTable(countQuery);   // count to table
		assertTrue("count to table did not return rows", tab.getNumRows() > 0);
		
		// run qll queries that return nothing to Table
		tab = TestGraph.execQueryToTable(selectEmpty); // select to empty table
		assertTrue("select to table returned rows", tab.getNumRows() == 0);
		tab = TestGraph.execQueryToTable(askEmpty);    // ask to table false
		assertFalse("ask to table didn't return false", tab.getCellAsBoolean(0, 0));
		tab = TestGraph.execQueryToTable(countEmpty);  // count to table 0
		assertTrue("count to table didn't return 0", tab.getCellAsInt(0, 0) == 0);
		
		// run error queries to Table
		try {
			tab = TestGraph.execQueryToTable(selectErr);  // select to table err
			fail("missing expected exception on bad select to table sparql");
		} catch (Exception e) {}
		
		try {
			tab = TestGraph.execQueryToTable(askErr);    // ask to table err
			fail("missing expected exception on bad ask to table sparql");
		} catch (Exception e) {}
		
		try {
			tab = TestGraph.execQueryToTable(countErr);  // count to table err
			fail("missing expected exception on bad ask to table sparql");
		} catch (Exception e) {}
		
	}
	
	@Test
	public void testSelectConstruct() throws Exception {
		TestGraph.clearGraph();
		TestGraph.initGraphWithData(this.getClass(), "sampleBattery");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromResource(this, "/sampleBattery.json");
		NodeGroup ng = sgJson.getNodeGroup(TestGraph.getOInfo());
		
		// build queries
		String constructQuery = ng.generateSparqlConstruct();
		
		// build queries that return nothing
		PropertyItem pItem = ng.getPropertyItemBySparqlID("CellId");
		pItem.addConstraint(ValueConstraint.buildValuesConstraint(pItem, "zizzyballooba", TestGraph.getSei()));
		
		String constructEmpty = ng.generateSparqlConstruct();

		// build queries with syntax errors
		String constructErr = constructQuery.replaceAll("\\{", "X");
		
		// run to NT table, which exercises the plain string return
		Table tab = TestGraph.executeQueryToNTTable(constructQuery);  // construct to NT
		assertTrue("construct to ntriples did not return anything", tab.getNumRows() > 0);
		
		tab = TestGraph.executeQueryToNTTable(constructEmpty);       // construct empty to NT
		assertTrue("construct to ntriples returned something", tab.getNumRows() == 0);
		
		try {
			tab = TestGraph.executeQueryToNTTable(constructErr);  // construct error to NT
			fail("missing expected exception on bad construct to triples table sparql");
		} catch (Exception e) {}
		
		// run to RDF, which exercises the plain string return
		String rdf = TestGraph.executeQueryToRDF(constructQuery);  // construct to RDF
		assertTrue("construct to ntriples did not return anything", rdf.length() > 0);
		
		rdf = TestGraph.executeQueryToRDF(constructEmpty);  // construct empty to RDF
		for (String tag : rdf.split(">")) {
			if (!tag.isBlank() && !tag.strip().startsWith("<rdf:RDF") && !tag.strip().startsWith("</rdf:RDF"))
				fail("construct to ntriples returned something: " + rdf);

		}
		
		try {
			rdf = TestGraph.executeQueryToRDF(constructErr);  // construct err to RDF
			fail("missing expected exception on bad construct to triples table sparql");
		} catch (Exception e) {}
	}
	
	@Test
	public void testLoadOwl() throws Exception {
		TestGraph.clearGraph();
		String s = Utility.getResourceAsString(this, "/Pet.owl");
		TestGraph.getSei().executeUploadOwl(s.getBytes());
		
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false);
		assertTrue("Can't find the 'Dog' class", oInfo.getClassNames().contains("http://research.ge.com/kdl/pet#Dog"));
	}
	
	@Test
	public void testLoadOwlQueryRDF() throws Exception {
		TestGraph.clearGraph();
		String s = Utility.getResourceAsString(this, "/Pet.owl");
		SparqlEndpointInterface sei = TestGraph.getSei();
		sei.executeUploadOwl(s.getBytes());
		
		String sparql = SparqlToXUtils.generateConstructSPOSparql(sei, null) + " LIMIT 1 ";
		String rdf = sei.executeQueryToRdf(sparql);
		assertTrue("RDF wasn't returned", rdf.contains("<rdf:RDF"));
	}

	@Test
	public void testUploadTurtleAsBytes() throws Exception{
		TestGraph.clearGraph();
		SparqlEndpointInterface sei = TestGraph.getSei();
		byte[] ttlBytes = Utility.getResourceAsBytes(this, "musicTestDataset_2017.q2.ttl");
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeUploadTurtle(ttlBytes));
		assertTrue(resultSet.getRationaleAsString("\n"), resultSet.getSuccess());
		assertEquals(215, sei.getNumTriples());
	}

	@Test
	public void testUploadTurtleAsStream() throws Exception{
		TestGraph.clearGraph();
		SparqlEndpointInterface sei = TestGraph.getSei();
		InputStream ttlInputStream = new FileInputStream(Utility.getResourceAsTempFile(this, "musicTestDataset_2017.q2.ttl"));
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadStreamed(ttlInputStream, "musicTestDataset_2017.q2.ttl"));
		assertTrue(resultSet.getSuccess());
		assertEquals(215, sei.getNumTriples());
	}

}
