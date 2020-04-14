package com.ge.research.semtk.nodegroupstore.test;

import static org.junit.Assert.*;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.io.parserjavacc.JSONPrinter;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NodeGroupStoreTest {

	@BeforeClass
	public static void setup() {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	/**
	 * Insert, get, delete a nodegroup
	 * @throws Exception
	 */
	public void testInsertGetDelete() throws Exception {
		TestGraph.clearGraph();
		String ID = "junit_test_ID";

		// add nodegroup
		NgStore store = new NgStore(TestGraph.getSei());
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(this, "/sampleBattery.json"));
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), ID, "junit_comments", "junit");
		
		// check getNodegroup()
		sgjson = store.getNodegroup(ID);
		assertTrue("retrieval from ng store failed", sgjson != null);
		
		// delete and check getNodegroup()
		store.deleteNodeGroup(ID);
		sgjson = store.getNodegroup(ID);
		assertTrue("retrieval from ng succeeded after delete", sgjson == null);
		
		// check for dangling triples
		String sparql = "select ?s ?p ?o from <" + TestGraph.getDataset() + "> where { ?s ?p ?o . } limit 10 ";
		Table t = TestGraph.execTableSelect(sparql);
		assertTrue("Dangling triples in store after delete.", t.getNumRows() == 0);
	}
	
	@Test
	/**
	 * Insert, get, delete a nodegroup
	 * @throws Exception
	 */
	public void testLineReturns() throws Exception {
		TestGraph.clearGraph();
		final String ID = "junit_test_ID";
		final String COMMENTS = "junit_comments\nline2";
		// add nodegroup
		NgStore store = new NgStore(TestGraph.getSei());
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(this, "/sampleBattery.json"));
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), ID, COMMENTS, "junit");
		
		// check getNodegroup()
		sgjson = store.getNodegroup(ID);
		assertTrue("retrieval from ng store failed", sgjson != null);
		
		// check getNodegroup()
		sgjson = store.getNodegroup(ID);
		assertTrue("retrieval from ng store failed", sgjson != null);
		
		Table t = store.getNodeGroupMetadata();
		assertTrue("Comments with line return did not return properly", t.getCellAsString(0, "comments").equals(COMMENTS));
				
	}
	@Test
	/**
	 * Insert, get, delete a nodegroup
	 * @throws Exception
	 */
	public void testInsertGetDeleteVeryBig() throws Exception {
		TestGraph.clearGraph();
		String ID = "junit_test_ID";

		// add nodegroup
		NgStore store = new NgStore(TestGraph.getSei());
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(this, "/very-big-nodegroup.json"));
		int nodeCount = sgjson.getNodeGroup().getNodeCount();

		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), ID, "junit_comments", "junit");
		
		// check getNodegroup()
		sgjson = store.getNodegroup(ID);
		assertTrue("retrieval from ng store failed", sgjson != null);
		
		// random check integrity
		assertEquals("After retrieval, nodegroup has wrong number of nodes", nodeCount, sgjson.getNodeGroup().getNodeCount());
		
		// delete and check getNodegroup()
		store.deleteNodeGroup(ID);
		sgjson = store.getNodegroup(ID);
		assertTrue("retrieval from ng succeeded after delete", sgjson == null);
		
		// check for dangling triples
		String sparql = "select ?s ?p ?o from <" + TestGraph.getDataset() + "> where { ?s ?p ?o . } limit 10 ";
		Table t = TestGraph.execTableSelect(sparql);
		assertTrue("Dangling triples in store after delete.", t.getNumRows() == 0);
	}
	
	@Test
	/**
	 * Insert, get, delete two nodegroups.
	 * @throws Exception
	 */
	public void testInsertTwo() throws Exception {
		TestGraph.clearGraph();
		String ID1 = "junit_test_ID1";
		String ID2 = "junit_test_ID2";

		// insert it for each ID
		NgStore store = new NgStore(TestGraph.getSei());
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(this, "/sampleBattery.json"));
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), ID1, "junit_comments", "junit");
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), ID2, "junit_comments", "junit");

		// get each
		sgjson = store.getNodegroup(ID1);
		assertTrue("retrieval 1 from ng store failed", sgjson != null);
		sgjson = store.getNodegroup(ID2);
		assertTrue("retrieval 2 from ng store failed", sgjson != null);
		
		// delete 1 and test each getNodeGroup()
		store.deleteNodeGroup(ID1);
		sgjson = store.getNodegroup(ID1);
		assertTrue("retrieval from ng succeeded after delete", sgjson == null);
		sgjson = store.getNodegroup(ID2);
		assertTrue("retrieval 2 from ng store failed after deleting 1", sgjson != null);
		
		// delete 2
		store.deleteNodeGroup(ID2);
		sgjson = store.getNodegroup(ID2);
		assertTrue("retrieval from ng succeeded after delete", sgjson == null);
		
		// check for dangling triples
		String sparql = "select ?s ?p ?o from <" + TestGraph.getDataset() + "> where { ?s ?p ?o . } limit 10 ";
		Table t = TestGraph.execTableSelect(sparql);
		assertTrue("Dangling triples in store after delete.", t.getNumRows() == 0);
	}

}
