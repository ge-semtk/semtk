package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Hashtable;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.ontologyTools.CombineEntitiesInConnThread;
import com.ge.research.semtk.ontologyTools.CombineEntitiesInputTable;
import com.ge.research.semtk.ontologyTools.CombineEntitiesTableThread;
import com.ge.research.semtk.ontologyTools.CombineEntitiesThread;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;


public class CombineEntitiesInConnTest_IT {
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	/**
	 * Run CombineEntitiesInConnThread on the TestGraph
	 * @throws Exception
	 */
	private void combineAndCheck(String csvResultsResource, String [] matchErrTab) throws Exception {
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = JobTracker.generateJobId();
		CombineEntitiesInConnThread combiner = new CombineEntitiesInConnThread(
				tracker, IntegrationTestUtility.getResultsClient(), jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn()
				);
		combiner.start();
		String csvErrStr = "";
		try {
			tracker.waitForSuccess(jobId, 300 * 1000);
			
		} catch (Exception e) {
			// failure
			csvErrStr = IntegrationTestUtility.getResultsClient().getTableResultsJson(jobId, 100).toCSVString();
			if (matchErrTab == null)
				fail("Unexpected error during combining:\n" + csvErrStr);
		}
			
		// check for any expected errors
		if (matchErrTab != null) {
			for (String match : matchErrTab) {
				if (!csvErrStr.contains(match)) {
					fail("Missing expected error during combining.\nExpected: " + match + "\nActual:\n" + csvErrStr);
				}
			}
		}
		
		// check for expected success
		if (csvResultsResource != null) {
			// HELPFUL HINT:
			// entity_res_results_test.json can be run as CONSTRUCT and you can actually understand the results
			// for testing it is run as DISTINCT with sorting as it is easier for machine to compare
			TestGraph.queryAndCheckResults(this, "entity_res_results_tester.json", csvResultsResource);
		}
		
	}
	
	
	@Test
	public void testSuccessResults1() throws Exception {
		// TESTS:
		// combines a chain
		// drops identifiers due to cardinality
		// drops duplicate types
		// keeps val which has no cardinality
		// combines incoming object properties
		// combines outgoing object properties
		
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "EntityResolution.owl");
		TestGraph.uploadOwlResource(this, "EntityResolutionTest.owl");	
		
		TestGraph.ingestCsvString(this.getClass(), 
				"entity_res_ingest_same_as_item.json",
				"target_id,duplicate_id\n" +
				"sub_item_b,item b \n" +
				"sub_item_b,\"Sub Item B\" \n");

		combineAndCheck("entity_res_results1.csv", null);
	}
	
	@Test
	public void testFailures() throws Exception {
		
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "EntityResolution.owl");
		TestGraph.uploadOwlResource(this, "EntityResolutionTest.owl");	
		
		// duplicate is a superclass
		TestGraph.ingestCsvString(this.getClass(), 
				"entity_res_ingest_same_as_item.json",
				"target_id, duplicate_id\n" +
				"item b,    sub_item_b \n");

		combineAndCheck(null, new String [] {"EntityResolutionTest#SubItem is not a superClass"});
		TestGraph.getSei().clearPrefix("uri://semtk");
		
		// duplicate to multiple targets
		TestGraph.ingestCsvString(this.getClass(), 
				"entity_res_ingest_same_as_item.json",
				"target_id,      duplicate_id\n" +
				"sub_item_b,     item b \n" +
				"\"Sub Item B\", item b \n");

		combineAndCheck(null, new String [] {"Object is duplicate to multiple targets"});
		TestGraph.getSei().clearPrefix("uri://semtk");
		
		// duplicate == target
		TestGraph.ingestCsvString(this.getClass(), 
				"entity_res_ingest_same_as_item.json",
				"target_id,  duplicate_id\n" +
				"item b,     item b \n");

		combineAndCheck(null, new String [] {"SameAs object has target==duplicate"});
		TestGraph.getSei().clearPrefix("uri://semtk");
		
		// loop
		TestGraph.ingestCsvString(this.getClass(), 
				"entity_res_ingest_same_as_item.json",
				"target_id,  duplicate_id\n" +
				"sub_item_a, sub_item_b \n" +
				"sub_item_b, sub_item_d \n" +
				"sub_item_d, sub_item_a \n"
				);

		combineAndCheck(null, new String [] {"Remains after merging"});
		TestGraph.getSei().clearPrefix("uri://semtk");
		
		// cardinality 2   (need to insert triples instead of normal ingestion)
		String SAME_AS = "<" + CombineEntitiesInConnThread.SAME_AS_CLASS_URI + ">";
		String TARGET = "<" + CombineEntitiesInConnThread.TARGET_PROP_URI + ">";
		String DUPLICATE = "<" + CombineEntitiesInConnThread.DUPLICATE_PROP_URI+ ">";
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#same>", "a", SAME_AS));
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#same>", TARGET,    "<uri:semtk#item1>"));
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#same>", DUPLICATE, "<uri:semtk#item2>"));
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#same>", TARGET,    "<uri:semtk#item3>"));
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#item1>", "a", "<http://research.ge.com/semtk/EntityResolutionTest#Item>"));
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#item2>", "a", "<http://research.ge.com/semtk/EntityResolutionTest#Item>"));
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#item3>", "a", "<http://research.ge.com/semtk/EntityResolutionTest#Item>"));

		combineAndCheck(null, new String [] {"SameAs does not have exactly 1 target and 1 duplicate"});
		TestGraph.getSei().clearPrefix("uri://semtk");
		
		// cardinality 0	
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#same>", "a", SAME_AS));
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#same>", TARGET,    "<uri:semtk#item1>"));
		TestGraph.getSei().executeQueryAndConfirm(SparqlToXUtils.generateInsertTripleQuery(TestGraph.getSei(), 
				"<uri:semtk#item1>", "a", "<http://research.ge.com/semtk/EntityResolutionTest#Item>"));
		
		combineAndCheck(null, new String [] {"SameAs does not have exactly 1 target and 1 duplicate"});
		TestGraph.getSei().clearPrefix("uri://semtk");
		
	}
	
	
}
