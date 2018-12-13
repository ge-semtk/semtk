package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.sparqlX.NeptuneSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.S3BucketConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class NeptuneSparqlEndpointInterfaceTest_IT {

	private static NeptuneSparqlEndpointInterface nsei = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();		

		SparqlEndpointInterface sei = TestGraph.getSei();
		if (sei instanceof NeptuneSparqlEndpointInterface) {
			nsei = (NeptuneSparqlEndpointInterface) sei;
			S3BucketConfig s3Config = IntegrationTestUtility.getS3Config();
			s3Config.verifySetup();
			nsei.setS3Config(s3Config);
		} else {
			System.out.println("Skipping Neptune tests since we aren't on neptune");
		}
		
	}
	@Test
	public void testUploadOwl() throws Exception {
		// skipping if there's no neptune
		if (nsei == null) return;
		
		TestGraph.clearGraph();
		String s = Utility.getResourceAsString(this, "/Pet.owl");
		nsei.executeUpload(s.getBytes());
      
		// check the ontology now
	}

}
