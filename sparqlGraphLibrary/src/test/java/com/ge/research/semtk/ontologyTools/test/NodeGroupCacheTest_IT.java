package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.NodeGroupCache;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.test.TestGraph;

public class NodeGroupCacheTest_IT {

	private static OntologyInfo oInfo;
	private static NodeGroupCache cache;
	
	@BeforeClass
	public static void init() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(NodeGroupCacheTest_IT.class, "sampleBattery.owl");
		oInfo = new OntologyInfo(TestGraph.getSparqlConn());
		cache = new NodeGroupCache(TestGraph.getSei(), oInfo);
	}
	@Test
	public void testPutGet() throws Exception {
		
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		NodeGroup ng1 = sgJson.getNodeGroup(oInfo);
		String sparql1 = ng1.generateSparqlSelect();
		
		// put and get to same cache
		cache.put("testPutGet", ng1, TestGraph.getSparqlConn(), "comments");
		NodeGroup ng2 = cache.get("testPutGet");
		String sparql2 = ng2.generateSparqlSelect();
		assertTrue("nodegroup returned from same cache generates different sparql", sparql2.equals(sparql1));
		
		// mess with ng1 without re-caching it
		ng1.changeSparqlID(ng1.getNode(0), "?messed_with_ng1");
		ng2 = cache.get("testPutGet");
		sparql2 = ng2.generateSparqlSelect();
		assertTrue("changing nodegroup after caching changed the return", sparql2.equals(sparql1));
		
	}
	
	@Test
	public void testThroughDisk() throws Exception {
		
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		NodeGroup ng1 = sgJson.getNodeGroup(oInfo);
		String sparql1 = ng1.generateSparqlSelect();
		
		// put and get to same cache
		cache.put("testThroughDisk", ng1, TestGraph.getSparqlConn(), "comments");
		
		
		// now try with a new cache (must read from triplestore)
		NodeGroupCache cache2 = new NodeGroupCache(TestGraph.getSei(), oInfo);
		NodeGroup ng2 = cache2.get("testThroughDisk");
		String sparql2 = ng2.generateSparqlSelect();
		
		assertTrue("nodegroup returned from new cache generates different sparql", sparql2.equals(sparql1));
	}


}
