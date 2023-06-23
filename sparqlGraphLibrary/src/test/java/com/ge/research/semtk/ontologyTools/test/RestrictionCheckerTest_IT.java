package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.ontologyTools.RestrictionChecker;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class RestrictionCheckerTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	public void cardinalityTest() throws Exception {
		
		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "Cardinality.owl");	// load Cardinal.sadl : the classes, restrictions, and instance data
		SparqlConnection conn = TestGraph.getSparqlConn();
		OntologyInfo oInfo = new OntologyInfo(conn);
		RestrictionChecker checker = new RestrictionChecker(conn, oInfo);
		
		// test whether restrictions are present
		String[] classes = {"http://Cardinality#Cardinal", "http://Cardinality#SubCardinal"};
		for(String theClass : classes) {
			assertFalse(checker.hasCardinalityRestriction(theClass, "http://Cardinality#anyData"));
			assertFalse(checker.hasCardinalityRestriction(theClass, "http://Cardinality#anyObject"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#atLeast1Data"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#atLeast1Object"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#atMost1Data"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#atMost1Object"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#exactly1Data"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#exactly1Object"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#range12Data"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#range12Object"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#singleData"));
			assertTrue(checker.hasCardinalityRestriction(theClass, "http://Cardinality#singleObject"));
		}
		
		// test if number satisfies cardinality
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#anyData", 500));
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#anyData", 0));
		assertFalse(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#atLeast1Data", 0));
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#atLeast1Data", 1));
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#atLeast1Data", 2));
		assertFalse(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#atMost1Data", 2));
		
		// test violations table
		Table violationsTable = checker.checkCardinality();
		IntegrationTestUtility.compareResults(violationsTable.toCSVString(), this, "cardinality_test1_results.csv");  // csv file contains expected violations
	}

}
