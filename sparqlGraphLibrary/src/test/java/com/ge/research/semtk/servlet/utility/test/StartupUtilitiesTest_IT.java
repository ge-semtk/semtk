package com.ge.research.semtk.servlet.utility.test;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.servlet.utility.StartupUtilities;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class TestStartupUtilities_IT {
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
	}
	
	@AfterClass
	public static void cleanup() throws Exception {
		TestGraph.clearGraph();
		
	}
	
	/**
	 * Testing combo of  StartupUtilities.updateOwlIfNeeded() with:
	 *     Utility.getInfoFromOwlRdf()
	 *     sei.getVersionOfOntologyLoaded()
	 * @throws Exception
	 */
//	@Test
//	public void testUploadOwlIfNeeded() throws Exception {
//		TestGraph.clearGraph();
//		
//		OntologyInfoClient oInfoClient = IntegrationTestUtility.getOntologyInfoClient();
//		SparqlEndpointInterface sei = TestGraph.getSei();
//
//		Utility.OwlRdfInfo fileInfo = Utility.getInfoFromOwlRdf(getClass().getResourceAsStream("/pet.owl"));
//		// test that null is returned when ontology is not yet loaded
//		assertEquals("Version from triplestore is not null when graph is empty", 
//				null, sei.getVersionOfOntologyLoaded(fileInfo.getBase()));
//		
//		// load ontology with a version 1
//		StartupUtilities.updateOwlIfNeeded(sei, oInfoClient, getClass(), "/pet.owl");
//		// test that version is returned
//		assertEquals("Version number from triplestore does not match version in owl", 
//				fileInfo.getVersion(), sei.getVersionOfOntologyLoaded(fileInfo.getBase()));
//		
//		// now try version 2
//		int triples = sei.getNumTriples();
//		fileInfo = Utility.getInfoFromOwlRdf(getClass().getResourceAsStream("/pet2.owl"));
//		StartupUtilities.updateOwlIfNeeded(sei, oInfoClient, getClass(), "/pet2.owl");
//		// version should have changed
//		assertEquals("Version number from triplestore does not match version in owl", 
//				fileInfo.getVersion(), sei.getVersionOfOntologyLoaded(fileInfo.getBase()));
//		
//		// triples should actually have changed (to smaller number in pet2.owl) also
//		int triples2 = sei.getNumTriples();
//		assertTrue("Triples did not change when new smaller version was loaded", triples2 < triples);
//		
//		
//		// try another with no version
//		fileInfo = Utility.getInfoFromOwlRdf(getClass().getResourceAsStream("/SimpleBoolean.owl"));
//		String emptyVersion = fileInfo.getVersion();
//		assertEquals("Owl file with no version didn't get empty string version", "", emptyVersion);
//		assertEquals("Version number from triplestore is not null when graph is empty", 
//				null, sei.getVersionOfOntologyLoaded(fileInfo.getBase()));
//		StartupUtilities.updateOwlIfNeeded(sei, oInfoClient, getClass(), "/SimpleBoolean.owl");
//		assertEquals("Version from triplestore does not match version in owl", 
//				sei.getVersionOfOntologyLoaded(fileInfo.getBase()), sei.getVersionOfOntologyLoaded(fileInfo.getBase()));
//		int triples3 = sei.getNumTriples();
//		assertTrue("Triples did not change when another ontology was loaded", triples3 > triples2);
//		
//		// reload exact same file should do nothing
//		boolean ret = StartupUtilities.updateOwlIfNeeded(sei, oInfoClient, getClass(), "/SimpleBoolean.owl");
//		assertFalse("Wrong return when re-updating same owl", ret);
//		int triples4 = sei.getNumTriples();
//		assertTrue("Triples did changed when nothing was reported updated", triples4 == triples3);
//	}

}
