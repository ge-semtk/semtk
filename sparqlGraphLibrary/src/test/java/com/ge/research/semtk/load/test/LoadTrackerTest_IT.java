package com.ge.research.semtk.load.test;

import static org.junit.Assert.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.load.LoadTracker;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.VirtuosoSparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class LoadTrackerTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		TestGraph.clearGraph();
	}
	
	@Test
	public void test() throws Exception {
		TestGraph.clearGraph();
		SparqlEndpointInterface sei2 = new VirtuosoSparqlEndpointInterface("http://server:0001", "http://graph001");

		LoadTracker tracker = this.buildLoadTracker();
		tracker.trackLoad("key1", "file1", TestGraph.getSei());
		tracker.trackLoad("key2", "file2", sei2);
		tracker.trackClear(TestGraph.getSei());
		
		Table tab = tracker.queryAll();
		System.out.println(tab.toCSVString());

		// check keys
		assertEquals("1st file key doesn't match", "key1", tab.getCell(0, "fileKey"));
		assertEquals("2nd file key doesn't match", "key2", tab.getCell(1, "fileKey"));
		assertEquals("3rd file key doesn't match", LoadTracker.CLEAR, tab.getCell(2, "fileKey"));
		
		// check files
		assertEquals("1st file doesn't match", "file1", tab.getCell(0, "fileName"));
		assertEquals("2nd file doesn't match", "file2", tab.getCell(1, "fileName"));
		
		// sei search gets 2 of 3
		tab = tracker.query(null, TestGraph.getSei(), null, null, null);
		assertEquals("Query matching on sei returned wrong number of rows", 2, tab.getNumRows());
		long epoch = tab.getCellAsLong(1, "epoch");
				
		// adding valid user also gets 2
		tab = tracker.query(null, TestGraph.getSei(), ThreadAuthenticator.getThreadUserName(), null, null);
		assertEquals("Query matching on sei/user returned wrong number of rows", 2, tab.getNumRows());

		// adding invalid user gets 0
		tab = tracker.query("Fred Unknown", TestGraph.getSei(), null, null, null);
		assertEquals("Query matching on sei/user returned wrong number of rows", 0, tab.getNumRows());

		// <=  last time gets all
		tab = tracker.query(null, null, null, null, epoch);
		assertEquals("Query matching on endTime returned wrong number of rows", 3, tab.getNumRows());

		// <= >= last time gets one
		tab = tracker.query(null, null, null, epoch, epoch);
		for (int i = 0; i < tab.getNumRows(); i++) {
			assertEquals("Query matching on startTime/endTime returned row with wrong epoch", epoch, tab.getCellAsInt(i,  "epoch"));
		}
			
		// delete last
		tracker.delete("key1", null, null, null, null);
		tab = tracker.queryAll();
		assertEquals("Wrong number of results after deleting one", 2, tab.getNumRows());
		
		// delete rest
		tracker.deleteAll();
		tab = tracker.queryAll();
		assertEquals("Wrong number of results after deleting all", 0, tab.getNumRows());
	}

	
	
	LoadTracker buildLoadTracker() throws Exception {
		return new LoadTracker(TestGraph.getSei(), TestGraph.getSei(), TestGraph.getUsername(), TestGraph.getPassword());
	}
}
