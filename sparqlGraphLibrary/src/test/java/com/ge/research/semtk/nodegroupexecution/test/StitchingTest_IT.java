package com.ge.research.semtk.nodegroupexecution.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.StitchingStep;
import com.ge.research.semtk.api.nodeGroupExecution.StitchingThread;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.nodegroupStore.NgStore.StoredItemTypes;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class StitchingTest_IT {
	private static NodeGroupStoreRestClient nodeGroupStoreRestClient = null;	
	private static String ngPrefix = "junit-";
	private static final String CREATOR = "JUnit StitchingTest_IT";
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();	
		
		// model and data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(StitchingTest_IT.class, "/annotationBattery.owl");
		TestGraph.uploadOwlResource(StitchingTest_IT.class, "/annotationBatteryStitchData.owl");
				
		// manage store
		nodeGroupStoreRestClient = IntegrationTestUtility.getNodeGroupStoreRestClient();		
		IntegrationTestUtility.cleanupNodegroupStore(nodeGroupStoreRestClient, CREATOR);
		
		String nodegroups[] = new String [] {
				"annotationBatteryIdName", 
				"annotationBatteryIdCellId"
				};
		
		for (String ng :  nodegroups) {
			String path = StitchingTest_IT.class.getResource("/" + ng + ".json").getPath();
			nodeGroupStoreRestClient.storeItem(ngPrefix + ng, "junit", CREATOR, path, StoredItemTypes.PrefabNodeGroup, null, true);
		}
	}
	@AfterClass
    public static void teardown() throws Exception {
        // delete stored nodegroups when done with all tests
		IntegrationTestUtility.cleanupNodegroupStore(nodeGroupStoreRestClient, CREATOR);
		TestGraph.clearGraph();
    } 
	
	@Test
	public void test() throws Exception {
		// [{ "nodegroupId": "junit-annotationBatteryIdName" }, { "nodegroupId": "junit-annotationBatteryIdCellId", "keyColumns": ["id"]}]
		StitchingStep steps[] = new StitchingStep[] {
				new StitchingStep(ngPrefix + "annotationBatteryIdName", null),
				new StitchingStep(ngPrefix + "annotationBatteryIdCellId", new String [] { "id" })
				};
		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(steps, new HashSet<String>(), TestGraph.getSparqlConn(),
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
				
		thread.start();
		JobTracker tracker = new JobTracker(IntegrationTestUtility.getServicesSei());
		tracker.waitForSuccess(jobId, 90000);
		Table tab = IntegrationTestUtility.getResultsClient().getTableResultsJson(jobId, null);
		assertEquals("Wrong number of rows stitched", 4, tab.getNumRows());
		assertEquals("Wrong number of cols stitched", 3, tab.getNumColumns());
	}
	
	@Test
	public void testOppositeOrder() throws Exception {
		StitchingStep steps[] = new StitchingStep[] {
				new StitchingStep(ngPrefix + "annotationBatteryIdCellId", null),
				new StitchingStep(ngPrefix + "annotationBatteryIdName", new String [] { "id" })
				};
		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(steps, new HashSet<String>(), TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
				
		thread.start();
		JobTracker tracker = new JobTracker(IntegrationTestUtility.getServicesSei());
		tracker.waitForSuccess(jobId, 90000);
		Table tab = IntegrationTestUtility.getResultsClient().getTableResultsJson(jobId, null);
		assertEquals("Wrong number of rows stitched", 4, tab.getNumRows());
		assertEquals("Wrong number of cols stitched", 3, tab.getNumColumns());
	}

	@Test
	public void unitTestStitch() throws Exception {

		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(null, new HashSet<String>(), TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
		Table t;
		StitchingStep s;
		Table res;
		// table 1
		t = Table.fromCsvData(
				"col1, col2\n" +
			    "11, 12\n" +
				"21, 22\n");
		s = new StitchingStep("ng1", null);
		thread.testStitchTable(t, s );
		
		// table 2
		t = Table.fromCsvData(
				"col1, col3\n" +
			    "11, 13\n" +
				"21, 23\n");
		s = new StitchingStep("ng2", new String[] {"col1"} );
		res = thread.testStitchTable(t, s );
		
		// test results
		thread.testFinalizeStitchedTable();
		for (String c : new String [] {"col1", "col2", "col3"} ) {
			assertTrue("Missing column " + c, res.getColumnIndex(c) > -1);
		}
		// check two cells
		assertEquals("Cell(0,0)", res.getCellAsInt(0, 0), 11);
		assertEquals("Cell(1,2)", res.getCellAsInt(1, 2), 23);
		
	}
	
	@Test
	public void unitTestStitchCombinatorial() throws Exception {

		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(null, new HashSet<String>(), TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
		Table t;
		StitchingStep s;
		Table res;
		// table 1
		t = Table.fromCsvData(
				"col1, col2\n" +
			    "11, 12\n" +
				"21, 22\n");
		s = new StitchingStep("ng1", null);
		thread.testStitchTable(t, s );
		
		// table 2
		t = Table.fromCsvData(
				"col1, col3\n" +
			    "11, 13\n" +
				"11, 23\n" +
			    "100,200\n" +
			    "101,201\n");
		s = new StitchingStep("ng2", new String[] {"col1"} );
		res = thread.testStitchTable(t, s );
		
		// test results
		thread.testFinalizeStitchedTable();
		for (String c : new String [] {"col1", "col2", "col3"} ) {
			assertTrue("Missing column " + c, res.getColumnIndex(c) > -1);
		}
		// check two cells
		assertEquals("Wrong number of rows were stitched: \n" + res.toCSVString(), res.getNumRows(), 5);
		res.sortByAllCols();
		String csv = res.toCSVString();
		for (String row : new String [] {
				"col1,col2,col3",
				"100,,200", 
				"101,,201",
				"11,12,13",
				"11,12,23",
				"21,22,"}) {
			assertTrue("Stitched table is missing row: " + row, csv.contains(row));
		}
		
	}
	
	@Test
	public void unitTestStitchMultipleCols() throws Exception {

		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(null, new HashSet<String>(), TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
		Table t;
		StitchingStep s;
		Table res;
		// table 1
		t = Table.fromCsvData(
				"col1, col2, col3\n" +
			    "11, 12, 131\n" +
		        "11, 12, 132\n" +
				"21, 22, 231\n");
		s = new StitchingStep("ng1", null);
		thread.testStitchTable(t, s );
		
		// table 2
		t = Table.fromCsvData(
				"col1, col2, col4\n" +
			    "11, 12, 141\n" +
			    "11, 12, 142\n" +
				"11, 99, 999\n" +
			    "21,22, 23\n");
		s = new StitchingStep("ng2", new String[] {"col1", "col2"} );
		res = thread.testStitchTable(t, s );
		
		// test results
		thread.testFinalizeStitchedTable();
		for (String c : new String [] {"col1", "col2", "col3"} ) {
			assertTrue("Missing column " + c, res.getColumnIndex(c) > -1);
		}
		// check two cells
		assertEquals("Wrong number of rows were stitched: \n" + res.toCSVString(), res.getNumRows(), 6);
		List<String> col = Arrays.asList(res.getColumn("col3"));
		res.sortByAllCols();
		String csv = res.toCSVString();
		for (String row : new String [] {
				"col1,col2,col3,col4",
				"11,12,131,141", 
				"11,12,131,142",
				"11,12,132,141",
				"11,12,132,142",
				"11,99,,999",
				"21,22,231,23"}) {
			assertTrue("Stitched table is missing row: " + row, csv.contains(row));
		}
		
	}
	
	@Test
	public void unitTestStitchDuplicateCol() throws Exception {

		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(null, new HashSet<String>(), TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
		
		Table t;
		StitchingStep s;
		Table res;
		// table 1
		t = Table.fromCsvData(
				"col1, col2\n" +
			    "11, 12\n" +
				"21, 22\n");
		s = new StitchingStep("ng1", null);
		thread.testStitchTable(t, s );
		
		// table 2
		t = Table.fromCsvData(
				"col1, col3\n" +
			    "11, 13\n" +
				"21, 23\n");
		s = new StitchingStep("ng2", new String[] {"col1"} );
		res = thread.testStitchTable(t, s );
		
		// table 3 : repeats "col3" 
		t = Table.fromCsvData(
				"col1, col3\n" +
			    "11, 100\n" +
				"21, 101\n");
		s = new StitchingStep("ng3", new String[] {"col1"} );
		res = thread.testStitchTable(t, s );
		
		
		// test results
		thread.testFinalizeStitchedTable();
		// does col3 now have one for each ng2 and ng3
		for (String c : new String [] {"col1", "col2", "col3-ng2", "col3-ng3"} ) {
			assertTrue("Missing column " + c, res.getColumnIndex(c) > -1);
		}
		assertEquals("Cell(0,0)", res.getCellAsInt(0, 0), 11);  // check a cell from table 1
		assertEquals("Cell(1,2)", res.getCellAsInt(1, 2), 23);  // check a cell from table 2
		assertEquals("Cell(1,3)", res.getCellAsInt(1, 3), 101); // check a cell from table 3
	}
	
	@Test
	public void unitTestStitchFailures() throws Exception {

		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(null, new HashSet<String>(), TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
		
		Table t;
		StitchingStep s;
		Table res;
		// table 1
		t = Table.fromCsvData(
				"col1, col2\n" +
			    "11, 12\n" +
				"21, 22\n");
		s = new StitchingStep("ng1", null);
		thread.testStitchTable(t, s );
		
		// table 2
		t = Table.fromCsvData(
				"col1x, col3\n" +
			    "11, 13\n" +
				"21, 23\n");
		s = new StitchingStep("ng2", new String[] {"col1x"} );
		try {
			res = thread.testStitchTable(t, s );
			assertTrue("Missing expected exception : stitching col has not appeared in previous tables", false);
		} catch (Exception e) {}
		
		// table 2 again, this time it repeats col2 unstitched
		t = Table.fromCsvData(
				"col1, col2\n" +
			    "11, 13\n" +
				"21, 23\n");
		s = new StitchingStep("ng2", new String[] {"col1"} );
		res = thread.testStitchTable(t, s );

		// table 3 tries to stitch to previously duplicated unstitched "col2"
		t = Table.fromCsvData(
				"col2, col4\n" +
			    "11, 14\n" +
				"21, 24\n");
		s = new StitchingStep("ng2", new String[] {"col2"} );
		try {
			res = thread.testStitchTable(t, s );
			assertTrue("Missing expected exception : stitching col was unstitched previous tables", false);
		} catch (Exception e) {}
	}
		
	@Test
	public void unitTestCommonCol() throws Exception {

		String jobId = JobTracker.generateJobId();
		
		// commonCol
		HashSet<String> commonCol = new HashSet<String>();
		commonCol.add("common");
		
		StitchingThread thread = new StitchingThread(null, commonCol, TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
		Table t;
		StitchingStep s;
		Table res;
		// table 1
		t = Table.fromCsvData(
				"col1, col2, common\n" +
			    "11, 12,one\n" +
				"21, 22,\n" +
			    "31, 32,\n");
		s = new StitchingStep("ng1", null);
		thread.testStitchTable(t, s );
		
		// table 2
		t = Table.fromCsvData(
				"col1, col3, common\n" +
			    "11, 13,\n" +
				"21, 23,two\n" +
			    "31, 33,\n");
		
		s = new StitchingStep("ng2", new String[] {"col1"} );
		res = thread.testStitchTable(t, s );
		
		// test results
		thread.testFinalizeStitchedTable();
		for (String c : new String [] {"col1", "col2", "col3", "common"} ) {
			assertTrue("Missing column " + c, res.getColumnIndex(c) > -1);
		}
		// check two cells
		assertEquals("Cell(0,0)", res.getCellAsInt(0, "col1"), 11);
		assertEquals("Cell(1,2)", res.getCellAsInt(1, "col3"), 23);
		
		// check common col
		assertEquals("commonCol filled by first query", "one", res.getCellAsString(0, "common"));
		assertEquals("commonCol filled by first query", "two", res.getCellAsString(1, "common"));
		assertEquals("commonCol filled by first query", "", res.getCellAsString(2, "common"));

	}
	
	@Test
	public void unitTestCommonColError() throws Exception {

		String jobId = JobTracker.generateJobId();
		
		// commonCol
		HashSet<String> commonCol = new HashSet<String>();
		commonCol.add("common");
		
		StitchingThread thread = new StitchingThread(null, commonCol, TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
		Table t;
		StitchingStep s;
		Table res;
		// table 1
		t = Table.fromCsvData(
				"col1, col2, common\n" +
			    "31, 32,one\n");
		s = new StitchingStep("ng1", null);
		thread.testStitchTable(t, s );
		
		// table 2
		t = Table.fromCsvData(
				"col1, col3, common\n" +
			    "31, 33,two\n");
		
		s = new StitchingStep("ng2", new String[] {"col1"} );
		
		try {
			res = thread.testStitchTable(t, s );
			assertTrue("Missing expected error which commonCol values mismatch", false);
		} catch (Exception e) {
			assertTrue("Wrong error message when commonCol values mismatch", e.getMessage().contains("two"));
		}
	}
}
