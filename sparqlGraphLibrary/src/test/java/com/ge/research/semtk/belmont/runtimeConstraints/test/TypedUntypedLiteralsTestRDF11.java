/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.belmont.runtimeConstraints.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

/**
 * Test typed and untyped literals.
 * SemTK's old friend opensource Virtuoso is RDF1.0 so check compatibility.
 * 
 * @author 200001934
 *
 */
public class TypedUntypedLiteralsTestRDF11 {
	static NodeGroupExecutor nodeGroupExecutor = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();	
		
		TestGraph.clearGraph();
		Class c = TypedUntypedLiteralsTestRDF11.class;
		// Upload model
		TestGraph.uploadOwlResource(c, "/book.owl");
		
		// Manually insert data
		// Note that date variants are always typed
		String insertQuery = "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT DATA " + 
				"  { GRAPH <" + TestGraph.getSei().getGraph() + ">   { " + 
				"        <http:/test/book1> a                                         <http://semtk/junit/book#Book> . " + 
				"        <http:/test/book1> <http://semtk/junit/book#title>           \"Typed Raw SPARQL\"^^XMLSchema:string . " + 
				"        <http:/test/book1> <http://semtk/junit/book#publishDate>     \"1961-01-01\"^^XMLSchema:date . " + 
				"        <http:/test/book1> <http://semtk/junit/book#publishDateTime> \"1961-01-01T01:00:00\"^^XMLSchema:dateTime . " + 
				"        <http:/test/book1> <http://semtk/junit/book#publishTime>     \"1:00:00\"^^XMLSchema:time . " + 
				"        <http:/test/book1> <http://semtk/junit/book#pages>           \"100\"^^XMLSchema:int . " + 
				"        <http:/test/book1> <http://semtk/junit/book#price>           \"10.99\"^^XMLSchema:double . " + 

				"        <http:/test/book2> a                                         <http://semtk/junit/book#Book> . " + 
				"        <http:/test/book2> <http://semtk/junit/book#title>           \"Untyped Raw SPARQL\" . " + 
				"        <http:/test/book2> <http://semtk/junit/book#publishDate>     \"1962-02-02\"^^XMLSchema:date . " + 
				"        <http:/test/book2> <http://semtk/junit/book#publishDateTime> \"1962-02-02T02:00:00\"^^XMLSchema:dateTime . " + 
				"        <http:/test/book2> <http://semtk/junit/book#publishTime>     \"2:00:00\"^^XMLSchema:time . " + 
				"        <http:/test/book2> <http://semtk/junit/book#pages>           200 . " + 
				"        <http:/test/book2> <http://semtk/junit/book#price>           20.99 . " + 

				"      }  } ";
		
		TestGraph.getSei().executeQueryAndConfirm(insertQuery);
		TestGraph.ingest(c, "/book.json", "/book.csv");
	}
	
	@Test
	public void testSelect() throws Exception {
		Table tab = TestGraph.execSelectFromResource(this, "/book.json");
		assertEquals("Query did not return all three books: \n" + tab.toCSVString(), 3, tab.getNumRows());
	}
	
	@Test
	public void testStringMatches() throws Exception {
		queryAllThreeBooks(
				"?title", 
				SupportedOperations.MATCHES, 
				new String[] {"Typed Raw SPARQL","Untyped Raw SPARQL","Ingested"});
	}
	
	@Test
	public void testStringRegex() throws Exception {
		queryAllThreeBooks(
				"?title", 
				SupportedOperations.REGEX, 
				new String[] {"e"});
	}
	
	@Test
	public void testDateMatches() throws Exception {
		queryAllThreeBooks(
				"?publishDate", 
				SupportedOperations.MATCHES, 
				new String[] {"1961-01-01","1962-02-02","1963-03-03"});
	}
	@Test
	public void testDateGreater() throws Exception {
		queryAllThreeBooks(
				"?publishDate", 
				SupportedOperations.GREATERTHAN, 
				new String[] {"1960-01-01"});
	}
	
	@Test
	public void testDateTimeMatches() throws Exception {
		queryAllThreeBooks(
				"?publishDateTime", 
				SupportedOperations.MATCHES, 
				new String[] {"1961-01-01T01:00:00","1962-02-02T02:00:00","1963-03-03T03:00:00"});
	}
	@Test
	public void testDateTimeGreater() throws Exception {
		queryAllThreeBooks(
				"?publishDateTime", 
				SupportedOperations.GREATERTHAN, 
				new String[] {"1960-01-01T01:00:00"});
	}
//
// It looks like XMLSchema:time is not handled like this and SemTK hasn't ever supported it
//
//	@Test
//	public void testTimeMatches() throws Exception {
//		queryAllThreeBooks(
//				"?publishTime", 
//				SupportedOperations.MATCHES, 
//				new String[] {"01:00:00","02:00:00","03:00:00"});
//	}
//	@Test
//	public void testTimeGreater() throws Exception {
//		queryAllThreeBooks(
//				"?publishTime", 
//				SupportedOperations.GREATERTHAN, 
//				new String[] {"00:00:01"});
//	}
	
	@Test
	public void testIntMatches() throws Exception {
		queryAllThreeBooks(
				"?pages", 
				SupportedOperations.MATCHES, 
				new String[] {"100","200","300"});
	}
	@Test
	public void testIntGreater() throws Exception {
		queryAllThreeBooks(
				"?pages", 
				SupportedOperations.GREATERTHAN, 
				new String[] {"10"});
	}
	@Test
	public void testPriceMatches() throws Exception {
		queryAllThreeBooks(
				"?price", 
				SupportedOperations.MATCHES, 
				new String[] {"10.99","20.99","30.99"});
	}
	@Test
	public void testPriceGreater() throws Exception {
		queryAllThreeBooks(
				"?price", 
				SupportedOperations.GREATERTHAN, 
				new String[] {"9.99"});
	}
	
	/**
	 * Build runtime constraint and run query, expecting all three books to come back
	 * @param sparqlID
	 * @param operator
	 * @param operandList
	 * @throws Exception
	 */
	private void queryAllThreeBooks(String sparqlID, SupportedOperations operator, String [] operandList) throws Exception {
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this, "/book.json");
		NodeGroup ng = sgjson.getNodeGroup();
		
		// Try to call through the highest level of runtime constraint through value constraint code
		RuntimeConstraintManager rtci = new RuntimeConstraintManager(ng);
		JSONObject constraint = RuntimeConstraintManager.buildRuntimeConstraintJson(
				sparqlID, 
				operator,
				new ArrayList<String>(Arrays.asList(operandList)));
		JSONArray runtimeConstraints = new JSONArray();
		runtimeConstraints.add(constraint);
		rtci.applyConstraintJson(runtimeConstraints);
		String constraintSparql = ng.getItemBySparqlID(sparqlID).getValueConstraint().toString();
		sgjson.setNodeGroup(ng);
		
		// run the query
		Table tab = TestGraph.execTableSelect(sgjson);
		assertEquals("With runtime constraint: \n" + constraintSparql + "\nQuery did not return all three books: \n" + tab.toCSVString(), 3, tab.getNumRows());
	}
	
	

}
