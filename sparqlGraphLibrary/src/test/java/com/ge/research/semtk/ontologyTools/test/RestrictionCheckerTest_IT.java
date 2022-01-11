package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.ontologyTools.RestrictionChecker;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.FusekiSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class RestrictionCheckerTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	public void cardinalityTest1() throws Exception {
		TestGraph.clearGraph();
		
		// load Cardinal.sadl : the classes, restrictions, and instance data
		TestGraph.uploadOwlResource(this, "Cardinality.owl");
		
		SparqlConnection conn = TestGraph.getSparqlConn();
		OntologyInfo oInfo = new OntologyInfo(conn);
		RestrictionChecker checker = new RestrictionChecker(conn, oInfo);
		
		Table card = checker.checkCardinality();
		
		// cardinality_test1_results.csv has every expected violation of cardinality restrictions
		IntegrationTestUtility.compareResults(card.toCSVString(), this, "cardinality_test1_results.csv");
		System.out.println(card.toCSVString());
	}

}
