package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.ontologyTools.ConnectedDataConstructor;
import com.ge.research.semtk.ontologyTools.Triple;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;


public class ConnectedDataConstructorTest_IT {
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	private String tripleString(ArrayList<Triple> triples) {
		StringBuilder ret = new StringBuilder();
		for (Triple t : triples) {
			ret.append(t.toCsvString() + "\n");
		}
		return ret.toString();
	}
	/**
	 * Combine two entities where duplicate's name is removed, and duplicate has all the desired outgoing properties
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "chainWithData.owl");	
		ResultsClient resClient = IntegrationTestUtility.getResultsClient();
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		
		// normal
		ConnectedDataConstructor c = new ConnectedDataConstructor("http://kdl.ge.com/junit/chain#link1", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, -1, null, false, false, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		ArrayList<Triple> triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("Normal ConnectedDataConstructor returned wrong number of triples", 7, triples.size());
		
		// extran pred
		ArrayList<String> xList = new ArrayList<String>();
		xList.add("http://kdl.ge.com/junit/chain#linkName");
		c = new ConnectedDataConstructor("http://kdl.ge.com/junit/chain#link1", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, -1, null, false, false, 
				xList,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("Normal ConnectedDataConstructor returned wrong number of triples", 8, triples.size());
		assertTrue("extra predicate was not returned ", tripleString(triples).contains("\"link2\""));

		// empty
		c = new ConnectedDataConstructor("http://kdl.ge.com/junit/chain#link7", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, -1, null, false, false, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("Empty ConnectedDataConstructor returned wrong number of triples", 0, triples.size());
		
		// blank node
		c = new ConnectedDataConstructor("_:Blank234lkmsdfhblank", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, -1, null, false, false, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("Blank node ConnectedDataConstructor returned wrong number of triples", 0, triples.size());
		
		// whitelist
		ArrayList<String> chainList = new ArrayList<String>();
		chainList.add("http://kdl.ge.com/junit/chain#ChainSuper");
		ArrayList<String> linkList = new ArrayList<String>();
		linkList.add("http://kdl.ge.com/junit/chain#LinkSuper");
			
		c = new ConnectedDataConstructor("http://kdl.ge.com/junit/chain#link1", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, -1, chainList, true, true, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("Whitelist ConnectedDataConstructor returned wrong number of triples", 3, triples.size());
		assertFalse("Whitelist returned links ", tripleString(triples).contains("#Link"));
		
		// whitelist w/o superclass
			
		c = new ConnectedDataConstructor("http://kdl.ge.com/junit/chain#link1", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, -1, chainList, true, false, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("Whitelist w/o superclass ConnectedDataConstructor returned wrong number of triples", 1, triples.size());

		// blacklist			
		c = new ConnectedDataConstructor("http://kdl.ge.com/junit/chain#link1", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, -1, chainList, false, true, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("Blacklist ConnectedDataConstructor returned wrong number of triples", 5, triples.size());
		assertFalse("Blacklist returned chain ", tripleString(triples).contains("#Chain"));

		// blacklist w/o superclass			
		c = new ConnectedDataConstructor("http://kdl.ge.com/junit/chain#link1", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, -1, chainList, false, false, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("Blanklist w/o superclass ConnectedDataConstructor returned wrong number of triples", 7, triples.size());
		
		// limit
		c = new ConnectedDataConstructor("http://kdl.ge.com/junit/chain#link1", XSDSupportedType.URI, SparqlResultTypes.N_TRIPLES, 4, chainList, false, true, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		// WARNING: limit is not perfect, can be off by a few
		assertTrue("limit ConnectedDataConstructor returned wrong number of triples", triples.size() < 7);
		
		// string
		c = new ConnectedDataConstructor("link1", XSDSupportedType.STRING, SparqlResultTypes.N_TRIPLES, -1, null, false, false, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("limit ConnectedDataConstructor returned wrong number of triples", 2, triples.size());
		
		// string untyped
		c = new ConnectedDataConstructor("link1", null, SparqlResultTypes.N_TRIPLES, -1, null, false, false, 
				null,
				TestGraph.getSparqlConn(), TestGraph.getOInfo(), 
				tracker, resClient );
		c.run();
		tracker.waitForSuccess(c.getJobId(), 60);
		triples = resClient.getNTriplesResult(c.getJobId());
		assertEquals("limit ConnectedDataConstructor returned wrong number of triples", 2, triples.size());
	}
	
}
