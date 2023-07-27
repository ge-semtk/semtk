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
	public void testCardinality() throws Exception {
		
		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "Cardinality.owl");	// load Cardinal.sadl : the classes, restrictions, and instance data
		SparqlConnection conn = TestGraph.getSparqlConn();
		OntologyInfo oInfo = new OntologyInfo(conn);
		RestrictionChecker checker = new RestrictionChecker(conn, oInfo);
		
		// test whether restrictions are present
		String[] classes = {"http://Cardinality#Cardinal", "http://Cardinality#SubCardinal"};  // these two should have identical restrictions
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
		assertFalse(checker.hasCardinalityRestriction("http://Cardinality#Unrestricted", "http://Cardinality#exactly1Data"));
		assertTrue(checker.hasCardinalityRestriction("http://Cardinality#RestrictTheUnrestricted", "http://Cardinality#exactly1Data"));
		
		// test if number satisfies cardinality
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#anyData", 500));
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#anyData", 0));
		assertFalse(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#atLeast1Data", 0));
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#atLeast1Data", 1));
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#atLeast1Data", 2));
		assertFalse(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#atMost1Data", 2));
		
		// test violations table
		Table violationsTable = checker.getCardinalityViolations();
		assertEquals(violationsTable.getNumRows(), 36);
		IntegrationTestUtility.compareResults(violationsTable.toCSVString(), this, "cardinality_test1_results.csv");  // csv file contains expected violations

		// test violations table with row max
		violationsTable = checker.getCardinalityViolations(10, false);
		assertEquals(violationsTable.getNumRows(), 10);
		
		// test violations table in concise format
		violationsTable = checker.getCardinalityViolations(true);
		assertEquals(violationsTable.getNumRows(), 22);
		IntegrationTestUtility.compareResults(violationsTable.toCSVString(), this, "cardinality_test1_results_concise.csv");  // csv file contains expected violations in concise format
	}

	@Test
	public void testOverrideStricter() throws Exception {
		
		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "CardinalityOverrideStricter.owl");
		SparqlConnection conn = TestGraph.getSparqlConn();
		OntologyInfo oInfo = new OntologyInfo(conn);
		RestrictionChecker checker = new RestrictionChecker(conn, oInfo);
		
		// superclass requires at least 2 codes.  subclass requires at least 4 codes
		assertTrue(checker.hasCardinalityRestriction("http://Cardinality#Cardinal", "http://Cardinality#code"));
		assertTrue(checker.hasCardinalityRestriction("http://Cardinality#StricterCardinal", "http://Cardinality#code"));
		assertTrue(checker.satisfiesCardinality("http://Cardinality#Cardinal", "http://Cardinality#code", 2));
		assertFalse(checker.satisfiesCardinality("http://Cardinality#StricterCardinal", "http://Cardinality#code", 2));
		assertTrue(checker.satisfiesCardinality("http://Cardinality#StricterCardinal", "http://Cardinality#code", 4));
		
		// note table has 2 entries for subclass: one for superclass, one for subclass
		Table violationsTable = checker.getCardinalityViolations(false);
		assertEquals(violationsTable.getNumRows(), 4);
		IntegrationTestUtility.compareResults(violationsTable.toCSVString(), this, "cardinality_testStricter_results.csv");  // csv file contains expected violations
		
		// test violations table in concise format
		violationsTable = checker.getCardinalityViolations(true);
		assertEquals(violationsTable.getNumRows(), 3);
		IntegrationTestUtility.compareResults(violationsTable.toCSVString(), this, "cardinality_testStricter_results_concise.csv");  // csv file contains expected violations in concise format
	}
	
}
