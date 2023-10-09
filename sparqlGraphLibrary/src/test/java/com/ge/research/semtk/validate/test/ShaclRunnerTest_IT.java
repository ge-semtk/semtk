/**
 ** Copyright 2023 General Electric Company
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
package com.ge.research.semtk.validate.test;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jena.shacl.validation.Severity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.validate.ShaclRunner;

public class ShaclRunnerTest_IT {
	
	// test the processed results JSON
	@Test
	public void testResults() throws Exception{

		ShaclRunner runner = getShaclRunnerForDeliveryBasketExample();
		assertFalse("data conforms when it should not", runner.conforms());

		// compare results to expected results
		final String EXPECTED_RESULTS_FILE = "DeliveryBasketExample-shacl-results.json";
		JSONObject resultsJson;
		JSONObject expectedJson;
		
		// test at Info level
		resultsJson = runner.getResults(Severity.Info);
		expectedJson = Utility.getResourceAsJson(this, EXPECTED_RESULTS_FILE);
		System.out.println(resultsJson);  // TODO REMOVE
		assertEquals(((JSONArray)resultsJson.get(ShaclRunner.JSON_KEY_ENTRIES)).size(), ((JSONArray)expectedJson.get(ShaclRunner.JSON_KEY_ENTRIES)).size());
		assertTrue(Utility.equals(resultsJson, expectedJson));
		
		// test at Warning level
		resultsJson = runner.getResults(Severity.Warning);
		expectedJson = removeEntriesBelowSeverityLevel(Utility.getResourceAsJson(this, EXPECTED_RESULTS_FILE), Severity.Warning);
		assertEquals(((JSONArray)resultsJson.get(ShaclRunner.JSON_KEY_ENTRIES)).size(), ((JSONArray)expectedJson.get(ShaclRunner.JSON_KEY_ENTRIES)).size());
		assertTrue(Utility.equals(resultsJson, expectedJson));
		
		// test at Violation level
		resultsJson = runner.getResults(Severity.Violation);
		expectedJson = removeEntriesBelowSeverityLevel(Utility.getResourceAsJson(this, EXPECTED_RESULTS_FILE), Severity.Violation);
		assertEquals(((JSONArray)resultsJson.get(ShaclRunner.JSON_KEY_ENTRIES)).size(), ((JSONArray)expectedJson.get(ShaclRunner.JSON_KEY_ENTRIES)).size());
		assertTrue(Utility.equals(resultsJson, expectedJson));
	}
	
	// test the raw turtle output
	@Test
	public void testResultsRawTurtle() throws Exception {

		ShaclRunner runner = getShaclRunnerForDeliveryBasketExample();
		String resultsTurtle = runner.getResultsRawTurtle();  // turtle containing raw shape and validation result info

		// load the turtle and query via SPARQL
		TestGraph.clearGraph();
		TestGraph.uploadTurtleString(resultsTurtle);  
		
		// note: query below may not be complete/robust enough for purposes beyond the row count used below - but it's a good start.
		// note: would have preferred to compare against Turtle from a file, but not possible because blank node URIs change
		String query = "select * from <" + TestGraph.getDataset() + "> where {" +
				"?result a <http://www.w3.org/ns/shacl#ValidationResult> ." +
				
				// these parameters must have exactly 1 value
				"?result <http://www.w3.org/ns/shacl#focusNode> ?focusNode ." +									// e.g. http://DeliveryBasketExample#basket100
				"?result <http://www.w3.org/ns/shacl#resultSeverity> ?resultSeverity ." +						// e.g. http://www.w3.org/ns/shacl#Violation
				"?result <http://www.w3.org/ns/shacl#sourceConstraintComponent> ?sourceConstraintComponent ." +	// e.g. http://www.w3.org/ns/shacl#MaxCountConstraintComponent

				// these parameters are optional
				"OPTIONAL { ?result <http://www.w3.org/ns/shacl#resultPath> ?resultPath } ." + 
				"OPTIONAL { ?result <http://www.w3.org/ns/shacl#resultMessage> ?resultMessage } ." +	// not more than 1 per language tag.  May be auto-generated if not specified in shape.
				
				// link the NodeShape
				"OPTIONAL { " + 
				"?result <http://www.w3.org/ns/shacl#sourceShape> ?prop ." +
				"?nodeShape a <http://www.w3.org/ns/shacl#NodeShape> ." +
				"?nodeShape <http://www.w3.org/ns/shacl#property> ?prop ." +
				"OPTIONAL { ?nodeShape <http://www.w3.org/ns/shacl#targetClass> ?targetClass } ." +
				"OPTIONAL { ?nodeShape <http://www.w3.org/ns/shacl#targetSubjectsOf> ?targetSubjectsOf } ." +
				"OPTIONAL { ?nodeShape <http://www.w3.org/ns/shacl#targetObjectsOf> ?targetObjectsOf } ." +
				" } . " + 
				
				"} " +
				"ORDER BY ?nodeShape";		
	
		Table res = TestGraph.execQueryToTable(query);
		int expectedCount = ((JSONArray)Utility.getResourceAsJson(this, "DeliveryBasketExample-shacl-results.json").get(ShaclRunner.JSON_KEY_ENTRIES)).size();
		assertEquals(res.getNumRows(), expectedCount);
	}	
	
	// test loading data from TTL file 
	@Test
	public void testLoadDataFromTTL() throws Exception {
		File shaclFile = Utility.getResourceAsTempFile(this, "musicTestDataset-shacl.ttl");
		ShaclRunner runner = new ShaclRunner(new FileInputStream(shaclFile), new FileInputStream(new File("src/test/resources/musicTestDataset_2017.q2.ttl")));
		JSONObject resultsJson = runner.getResults();
		assertEquals(((JSONArray)resultsJson.get(ShaclRunner.JSON_KEY_ENTRIES)).size(), 3);
		assertTrue(resultsJson.toJSONString().contains("Expect a int less than 300 (got 318)"));
		assertTrue(resultsJson.toJSONString().contains("Expect a int less than 300 (got 323)"));
		assertTrue(resultsJson.toJSONString().contains("Expect a int less than 300 (got 334)"));
	}
	
	@Test
	public void testCompareSeverity() throws Exception {
		assertEquals(ShaclRunner.compare(Severity.Violation, Severity.Info), 1);
		assertEquals(ShaclRunner.compare(Severity.Warning, Severity.Info), 1);
		assertEquals(ShaclRunner.compare(Severity.Violation, Severity.Warning), 1);
		assertEquals(ShaclRunner.compare(Severity.Violation, Severity.Violation), 0);
		assertEquals(ShaclRunner.compare(Severity.Warning, Severity.Warning), 0);
		assertEquals(ShaclRunner.compare(Severity.Info, Severity.Info), 0);
		assertEquals(ShaclRunner.compare(Severity.Info, Severity.Violation), -1);
		assertEquals(ShaclRunner.compare(Severity.Warning, Severity.Violation), -1);
		assertEquals(ShaclRunner.compare(Severity.Info, Severity.Warning), -1);
	}
	
	
	// helper function for test setup
	private ShaclRunner getShaclRunnerForDeliveryBasketExample() throws Exception {

		// create a SPARQL connection, populate it
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "DeliveryBasketExample.owl");
		SparqlConnection sparqlConn = TestGraph.getSparqlConn();

		// perform SHACL validation
		File shaclFile = Utility.getResourceAsTempFile(this, "DeliveryBasketExample-shacl.ttl");
		return new ShaclRunner(new FileInputStream(shaclFile), sparqlConn);
	}
	
	// helper function to filter expected results to a given severity level
	@SuppressWarnings("unchecked")
	public static JSONObject removeEntriesBelowSeverityLevel(JSONObject resultsJson, Severity severityLevel) throws Exception {

		JSONArray entriesToKeep = new JSONArray();
		
		// iterate through entries, keeping the ones at the appropriate level
		JSONArray entries = (JSONArray) resultsJson.get(ShaclRunner.JSON_KEY_ENTRIES);
		for(int i = 0; i < entries.size(); i++) {
			JSONObject entry = (JSONObject) entries.get(i);
			String severityStr = (String) entry.get(ShaclRunner.JSON_KEY_SEVERITY);
			if(severityStr.equals("Violation")) {
				entriesToKeep.add(entry);
			}else if(severityStr.equals("Warning") && ShaclRunner.compare(Severity.Warning, severityLevel) >= 0) {
				entriesToKeep.add(entry);
			}else if(severityStr.equals("Info") && ShaclRunner.compare(Severity.Info, severityLevel) >= 0) {
				entriesToKeep.add(entry);
			}
		}

		resultsJson.put(ShaclRunner.JSON_KEY_ENTRIES, entriesToKeep);
		return resultsJson;
	}

}
