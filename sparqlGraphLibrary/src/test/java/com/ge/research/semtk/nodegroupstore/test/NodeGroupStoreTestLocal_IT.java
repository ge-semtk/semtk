package com.ge.research.semtk.nodegroupstore.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.services.nodegroupStore.NgStore.StoredItemTypes;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

/**
 * Test NgStore without any services, but still uses triplestore
 * @author 200001934
 *
 */
public class NodeGroupStoreTestLocal_IT {

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
		Table t = TestGraph.execQueryToTable(sparql);
		assertTrue("Dangling triples in store after delete.", t.getNumRows() == 0);
	}

	@Test
	public void testDeleteAll() throws Exception{
		TestGraph.clearGraph();
		NgStore store = new NgStore(TestGraph.getSei());
		assertEquals(store.getStoredItemIdList(StoredItemTypes.PrefabNodeGroup).getNumRows(), 0);

		// add nodegroups
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(this, "/sampleBattery.json"));
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), "Nodegroup A", "junit_comments", "junit");
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), "Nodegroup B", "junit_comments", "junit");
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), "Nodegroup C", "junit_comments", "junit");
		assertEquals(store.getStoredItemIdList(StoredItemTypes.PrefabNodeGroup).getNumRows(), 3);

		// delete all
		store.deleteAllNodeGroups();
		assertEquals(store.getStoredItemIdList(StoredItemTypes.PrefabNodeGroup).getNumRows(), 0);
	}
	
	@Test
	/**
	 * Test renaming a nodegroup
	 */
	public void testRename() throws Exception {
		TestGraph.clearGraph();
		String ID1 = "junit_test_ID1";
		String ID2 = "junit_test_ID2";
		String ID3 = "junit_test_ID3";

		// add nodegroup ID1
		NgStore store = new NgStore(TestGraph.getSei());
		SparqlGraphJson sgjson = new SparqlGraphJson(Utility.getResourceAsJson(this, "/sampleBattery.json"));
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), ID1, "junit_comments", "junit");

		assertTrue("ID1 should exist now", store.getNodegroup(ID1) != null);
		assertTrue("ID2 should not exist now", store.getNodegroup(ID2) == null);

		// rename nodegroup to ID2
		store.renameStoredItem(ID1, ID2, StoredItemTypes.PrefabNodeGroup);

		assertTrue("ID1 should not exist now", store.getNodegroup(ID1) == null);
		assertTrue("ID2 should exist now", store.getNodegroup(ID2) != null);

		// confirm errors if attempt to rename a current id that does not exist
		boolean exceptionThrown = false;
		try{
			store.renameStoredItem(ID1, ID2, StoredItemTypes.PrefabNodeGroup);
			fail(); // should not get here
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot rename item with current id 'junit_test_ID1' and type: 'PrefabNodeGroup': no such item exists"));
		}
		assertTrue(exceptionThrown);

		// confirm errors if trying to rename to a null id
		exceptionThrown = false;
		try{
			store.renameStoredItem(ID2, null, StoredItemTypes.PrefabNodeGroup);
			fail(); // should not get here
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot rename item to null or empty id"));
		}
		assertTrue(exceptionThrown);

		// confirm errors if trying to rename to a blank id
		exceptionThrown = false;
		try{
			store.renameStoredItem(ID2, "  ", StoredItemTypes.PrefabNodeGroup);
			fail(); // should not get here
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot rename item to null or empty id"));
		}
		assertTrue(exceptionThrown);

		// TODO test trying to name to an id that already exists
		exceptionThrown = false;
		store.insertNodeGroup(sgjson.toJson(), sgjson.getSparqlConnJson(), ID3, "junit_comments", "junit");
		try{
			store.renameStoredItem(ID2, ID3, StoredItemTypes.PrefabNodeGroup);
			fail(); // should not get here
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot rename item to new id 'junit_test_ID3' and type: 'PrefabNodeGroup': item with this id already exists"));
		}
		assertTrue(exceptionThrown);
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
		Table t = TestGraph.execQueryToTable(sparql);
		assertTrue("Dangling triples in store after delete.", t.getNumRows() == 0);
	}
	
	@Test
	/**
	 * Insert, get, delete a nodegroup
	 * @throws Exception
	 */
	public void testString() throws Exception {
		TestGraph.clearGraph();
		String ID = "junit_test_ID";

		// add nodegroup
		NgStore store = new NgStore(TestGraph.getSei());
		String str    = "hi there \n two \r \\ \\\\ \\n \\t \\g \' \\\' \" \\\" \\\\\" \\\\\\\" &quot; \\";
		String expect = "hi there \n two \n \\ \\\\ \\n \\t \\g \' \\\' \" \\\" \\\\\" \\\\\\\" &quot; \\";

		store.insertStringBlob(str, StoredItemTypes.Report, ID, "junit_comments", "junit");
		
		String str2 = store.getStoredItem(ID, StoredItemTypes.Report);
		assertNotEquals("String blob returned null", null, str2);
		assertTrue("String blobs do not match\nExpected: " + expect + "\nFound:" + str2, str2.equals(expect));
	}
	
	@Test
	/**
	 * Insert, get, delete a nodegroup
	 * @throws Exception
	 */
	public void testBigString() throws Exception {
		TestGraph.clearGraph();
		String ID = "junit_test_ID";

		// add nodegroup
		NgStore store = new NgStore(TestGraph.getSei());
		String str = Utility.getResourceAsString(this, "/very-big-nodegroup.json");

		store.insertStringBlob(str, StoredItemTypes.Report, ID, "junit_comments", "junit");
		
		String retrieved = store.getStoredItem(ID, StoredItemTypes.Report);
		String expected = str.replaceAll("[\n\r]+", "\n");
		
		assertNotEquals("String blob returned null", null, retrieved);
		assertTrue("String blobs do not match", retrieved.equals(expected));
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
		Table t = TestGraph.execQueryToTable(sparql);
		assertTrue("Dangling triples in store after delete.", t.getNumRows() == 0);
	}

}
