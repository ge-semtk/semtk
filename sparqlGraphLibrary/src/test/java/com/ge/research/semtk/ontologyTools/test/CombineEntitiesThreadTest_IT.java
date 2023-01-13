package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.ontologyTools.CombineEntitiesInputTable;
import com.ge.research.semtk.ontologyTools.CombineEntitiesTableThread;
import com.ge.research.semtk.ontologyTools.CombineEntitiesThread;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;


public class CombineEntitiesThreadTest_IT {
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	/**
	 * Combine two entities where duplicate's name is removed, and duplicate has all the desired outgoing properties
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "AnimalsToCombineData.owl");	
		TestGraph.uploadOwlResource(this, "AnimalSubProps.owl");
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = JobTracker.generateJobId();
		ArrayList<String> propsToSkip = new ArrayList<String>();
		propsToSkip.add("http://AnimalSubProps#name");
		CombineEntitiesThread combiner = new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalsToCombineData#auntyEm", "http://AnimalsToCombineData#auntyEmDuplicate", 
				null, propsToSkip
				);
		combiner.start();
		tracker.waitForSuccess(jobId, 300 * 1000);
		
		/** 
		 * Should combine AuntyEmDuplicate with AuntyEm
		 * Remove the name from AuntyEmDuplicate
		 * In table form, the name and scaryName combinatorials make it hard to read, but easier to check then JSON-LD results
		 */
		TestGraph.queryAndCheckResults(this, "animalsToCombineTigerTree.json", "animalsToCombineTigerTree_results1.csv");

		
	}
	
	/**
	 * Combine two entities where target's name is removed, and target has all the desired outgoing properties
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "AnimalSubProps.owl");
		TestGraph.uploadOwlResource(this, "AnimalsToCombineData.owl");	
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = JobTracker.generateJobId();
		ArrayList<String> skipProps = new ArrayList<String>();
		skipProps.add("http://AnimalSubProps#name"); 
		CombineEntitiesThread combiner = new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalsToCombineData#auntyEmDuplicate", "http://AnimalsToCombineData#auntyEm", 
				skipProps, null
				);
		combiner.start();
		tracker.waitForSuccess(jobId, 300 * 1000);
		
		/** 
		 * Same results as Test1, we just combined in the other direction.
		 * In table form, the name and scaryName combinatorials make it hard to read, but easier to check then JSON-LD results
		 */
		TestGraph.queryAndCheckResults(this, "animalsToCombineTigerTree.json", "animalsToCombineTigerTree_results1.csv");
		
	}
	
	/**
	 * Test bad inputs
	 * @throws Exception
	 */
	@Test
	public void testErrors() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "AnimalsToCombineData.owl");	
		TestGraph.uploadOwlResource(this, "AnimalSubProps.owl");
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		
		ArrayList<String> skipProps = new ArrayList<String>();
		skipProps.add("http://AnimalSubProps#nameBAD"); 
		
		// bad target uri
		try {
			String jobId = JobTracker.generateJobId();
			CombineEntitiesThread combiner= new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalsToCombineData#BAD_TARGET_URI", "http://AnimalsToCombineData#auntyEm", 
				null, null
				);
			combiner.start();
			tracker.waitForSuccess(jobId, 300 * 1000);
			
			fail("Missing exception for bad target uri");
		} catch (Exception e) {
			assertTrue("Exception is missing BAD_TARGET_URI", e.getMessage().contains("BAD_TARGET_URI"));
		}
		
		// bad duplicate uri
		try {
			String jobId = JobTracker.generateJobId();
			CombineEntitiesThread combiner = new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalsToCombineData#auntyEm", "http://AnimalsToCombineData#BAD_DUPLICATE_URI", 
				null, null
				);
			combiner.start();
			tracker.waitForSuccess(jobId, 300 * 1000);
			fail("Missing exception for bad target uri");
		} catch (Exception e) {
			assertTrue("Exception is missing BAD_DUPLICATE_URI", e.getMessage().contains("BAD_DUPLICATE_URI"));
		}
		
		// target is a superclass of duplicate
		try {
			String jobId = JobTracker.generateJobId();
			CombineEntitiesThread combiner = new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalsToCombineData#drummer", "http://AnimalsToCombineData#auntyEm", 
				null, null
				);
			combiner.start();
			tracker.waitForSuccess(jobId, 300 * 1000);
			fail("Missing exception when duplicate is a subclass of target");
		} catch (Exception e) {
			assertTrue("Exception is missing 'not a superClass'", e.getMessage().contains("n"));
		}
		
		// should not error when duplicate is a superclass of target
		String jobId = JobTracker.generateJobId();
		CombineEntitiesThread combiner = new CombineEntitiesThread(
			tracker, jobId, 
			TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
			"http://AnimalsToCombineData#auntyEm", "http://AnimalsToCombineData#drummer", 
			null, null
			);
		combiner.start();
		tracker.waitForSuccess(jobId, 300 * 1000);
		
	}
	
	
	/**
	 * TableThread version
	 * Combine by name and type 
	 * @throws Exception
	 */
	@Test
	public void testTableNameAndType() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "AnimalSubProps.owl");
		TestGraph.uploadOwlResource(this, "AnimalsToCombineData.owl");	
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = JobTracker.generateJobId();
		ResultsClient resClient = IntegrationTestUtility.getResultsClient();
		
		final String NAME_PROP = "http://AnimalSubProps#name";
		final String PRIMARY_NAME_COL = "target_name";
		final String SECONDARY_NAME_COL = "duplicate_name";
		final String TYPE_PROP = "#type";
		final String SECONDARY_TYPE_COL = "duplicate_type";
		
		Hashtable<String, String> targetHash = new Hashtable<String,String>();
		targetHash.put(PRIMARY_NAME_COL, NAME_PROP);
		
		Hashtable<String, String> duplicateHash = new Hashtable<String,String>();
		duplicateHash.put(SECONDARY_NAME_COL, NAME_PROP);
		duplicateHash.put(SECONDARY_TYPE_COL, TYPE_PROP);
		
		Table inputTable = new Table(new String [] {PRIMARY_NAME_COL, SECONDARY_NAME_COL, SECONDARY_TYPE_COL});
		inputTable.addRow(new String [] {"auntyEm", "AUNTY_EM", "http://AnimalSubProps#Tiger"});
		
		CombineEntitiesInputTable tab = new CombineEntitiesInputTable(targetHash, duplicateHash, inputTable);
		CombineEntitiesTableThread thread = new CombineEntitiesTableThread(tracker, resClient, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlAuthConn(), 
				null, null,
				tab);
		thread.run();
		
		try {
			tracker.waitForSuccess(jobId, 300 * 1000);
		} catch (Exception e) {
			
			System.err.print(IntegrationTestUtility.getResultsClient().getTableResultsJson(jobId, 10).toCSVString());
			throw(e);
		}
		
		/** 
		 * Should combine AuntyEmDuplicate with AuntyEm
		 * Remove the name from AuntyEmDuplicate
		 * In table form, the name and scaryName combinatorials make it hard to read, but easier to check then JSON-LD results
		 */
		TestGraph.queryAndCheckResults(this, "animalsToCombineTigerTree.json", "animalsToCombineTigerTree_results1.csv");

	}
	
	/**
	 * TableThread version
	 * Combine by name.
	 * @throws Exception
	 */
	@Test
	public void testTableSimple() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "AnimalSubProps.owl");
		TestGraph.uploadOwlResource(this, "AnimalsToCombineData.owl");	
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = JobTracker.generateJobId();
		ResultsClient resClient = IntegrationTestUtility.getResultsClient();
		
		final String NAME_PROP = "http://AnimalSubProps#name";
		final String PRIMARY_NAME_COL = "target_name";
		final String SECONDARY_NAME_COL = "duplicate_name";
		
		Hashtable<String, String> targetHash = new Hashtable<String,String>();
		targetHash.put(PRIMARY_NAME_COL, NAME_PROP);
		
		Hashtable<String, String> duplicateHash = new Hashtable<String,String>();
		duplicateHash.put(SECONDARY_NAME_COL, NAME_PROP);
		
		Table inputTable = new Table(new String [] {PRIMARY_NAME_COL, SECONDARY_NAME_COL});
		inputTable.addRow(new String [] {"auntyEm", "AUNTY_EM"});
		
		CombineEntitiesInputTable tab = new CombineEntitiesInputTable(targetHash, duplicateHash, inputTable);
		CombineEntitiesTableThread thread = new CombineEntitiesTableThread(tracker, resClient, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlAuthConn(), 
				null, null,
				tab);
		thread.run();
		
		// TODO: test for empty target or duplicate hash
		try {
			tracker.waitForSuccess(jobId, 300 * 1000);
		} catch (Exception e) {
			
			System.err.print(IntegrationTestUtility.getResultsClient().getTableResultsJson(jobId, 10).toCSVString());
			throw(e);
		}
		
		/** 
		 * Should combine AuntyEmDuplicate with AuntyEm
		 * Remove the name from AuntyEmDuplicate
		 * In table form, the name and scaryName combinatorials make it hard to read, but easier to check then JSON-LD results
		 */
		TestGraph.queryAndCheckResults(this, "animalsToCombineTigerTree.json", "animalsToCombineTigerTree_results1.csv");

		
	}
	
	/**
	 * TableThread version
	 * Combine by name and type 
	 * @throws Exception
	 */
	@Test
	public void testTableNameAndTypeAndDeletes() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "AnimalSubProps.owl");
		TestGraph.uploadOwlResource(this, "AnimalsToCombineData.owl");	
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = JobTracker.generateJobId();
		ResultsClient resClient = IntegrationTestUtility.getResultsClient();
		
		final String NAME_PROP = "http://AnimalSubProps#name";
		final String SCARY_NAME_PROP = "http://AnimalSubProps#scaryName";
		final String PRIMARY_NAME_COL = "target_name";
		final String SECONDARY_NAME_COL = "duplicate_name";
		final String TYPE_PROP = "#type";
		final String SECONDARY_TYPE_COL = "duplicate_type";
		
		Hashtable<String, String> targetHash = new Hashtable<String,String>();
		targetHash.put(PRIMARY_NAME_COL, NAME_PROP);
		
		Hashtable<String, String> duplicateHash = new Hashtable<String,String>();
		duplicateHash.put(SECONDARY_NAME_COL, NAME_PROP);
		duplicateHash.put(SECONDARY_TYPE_COL, TYPE_PROP);
		
		// use the name from the duplicate
		ArrayList<String> delTarget = new ArrayList<String>();
		delTarget.add(NAME_PROP);
		// remove the scaryName and type from the duplicate
		ArrayList<String> delDuplicate = new ArrayList<String>();
		delDuplicate.add(SCARY_NAME_PROP);
		delDuplicate.add(TYPE_PROP);
		
		// combine these pairs, looking up duplicate by name and type
		Table inTab = new Table(new String [] {PRIMARY_NAME_COL, SECONDARY_NAME_COL, SECONDARY_TYPE_COL});
		inTab.addRow(new String [] {"auntyEm", "AUNTY_EM", "http://AnimalSubProps#Tiger"});
		inTab.addRow(new String [] {"Michaela", "Animal", "http://AnimalSubProps#Animal"});
		
		CombineEntitiesInputTable tab = new CombineEntitiesInputTable(targetHash, duplicateHash, inTab);
		CombineEntitiesTableThread thread = new CombineEntitiesTableThread(tracker, resClient, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlAuthConn(), 
				delTarget, delDuplicate,
				tab);
		thread.run();
		
		try {
			tracker.waitForSuccess(jobId, 300 * 1000);
		} catch (Exception e) {
			
			System.err.print(IntegrationTestUtility.getResultsClient().getTableResultsJson(jobId, 10).toCSVString());
			throw(e);
		}
		
		/** 
		 * Should combine AuntyEmDuplicate with AuntyEm.  
		 * 		Use the name AUNTY_EM (from duplicate)
		 * 		Remove the scaryName from AuntyEmDuplicate
		 * In table form, the name and scaryName combinatorials make it hard to read, but easier to check then JSON-LD results
		 */
		TestGraph.queryAndCheckResults(this, "animalsToCombineTigerTree.json", "animalsToCombineTigerTree_results2.csv");

	}
}
