package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

/**
 * Tests the TestGraph SparqlEndpointInterface (integrationtest.properties)
 * Could be Virtuoso, Neptune, Fuseki... etc.
 * @author 200001934
 *
 */
public class SparqlEndpointInterfaceTest_IT {

	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();		

		System.out.println("The TestGraph sparql connection is:\n" +
			TestGraph.getSparqlConn().toJson().toJSONString());
		
	}
	@Test
	public void testRoundTrip() throws Exception {
		
		TestGraph.clearGraph();
		String s = Utility.getResourceAsString(this, "/Pet.owl");
		
		TestGraph.getSei().executeUpload(s.getBytes());
     
		
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false );
		assertTrue("Can't find the 'Dog' class", oInfo.getClassNames().contains("http://research.ge.com/kdl/pet#Dog"));
	}

}
