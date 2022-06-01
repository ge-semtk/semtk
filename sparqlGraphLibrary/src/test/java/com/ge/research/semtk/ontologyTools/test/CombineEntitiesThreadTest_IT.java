package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.ontologyTools.CombineEntitiesThread;
import com.ge.research.semtk.ontologyTools.DataDictionaryGenerator;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
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
		TestGraph.uploadOwlResource(this, "AnimalSubProps.owl");
		TestGraph.uploadOwlResource(this, "AnimalsToCombineData.owl");	
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = JobTracker.generateJobId();
		ArrayList<String> skipProps = new ArrayList<String>();
		skipProps.add("http://AnimalSubProps#name");
		CombineEntitiesThread combiner = new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalSubProps#Tiger", "http://AnimalsToCombineData#auntyEm", "http://AnimalsToCombineData#auntyEmDuplicate", 
				null, skipProps
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
				"http://AnimalSubProps#Tiger", "http://AnimalsToCombineData#auntyEmDuplicate", "http://AnimalsToCombineData#auntyEm", 
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
		TestGraph.uploadOwlResource(this, "AnimalSubProps.owl");
		TestGraph.uploadOwlResource(this, "AnimalsToCombineData.owl");	
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		
		ArrayList<String> skipProps = new ArrayList<String>();
		skipProps.add("http://AnimalSubProps#nameBAD"); 
		
		try {
			String jobId = JobTracker.generateJobId();
			new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalSubProps#BAD_CLASS", "http://AnimalsToCombineData#auntyEmDuplicate", "http://AnimalsToCombineData#auntyEm", 
				null, null
				);
			fail("Missing exception for bad class");
		} catch (Exception e) {
			assertTrue("Exception is missing BAD_CLASS", e.getMessage().contains("BAD_CLASS"));
		}
		
		try {
			String jobId = JobTracker.generateJobId();
			new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalSubProps#BAD_CLASS", "http://AnimalsToCombineData#auntyEmDuplicate", "http://AnimalsToCombineData#auntyEm", 
				skipProps, null
				);
			fail("Missing exception for bad target props");
		} catch (Exception e) {
			assertTrue("Exception is missing BAD_CLASS", e.getMessage().contains("BAD_CLASS"));
		}
		
		try {
			String jobId = JobTracker.generateJobId();
			new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalSubProps#BAD_CLASS", "http://AnimalsToCombineData#auntyEmDuplicate", "http://AnimalsToCombineData#auntyEm", 
				null, skipProps
				);
			fail("Missing exception for bad duplicate props");
		} catch (Exception e) {
			assertTrue("Exception is missing BAD_CLASS", e.getMessage().contains("BAD_CLASS"));
		}
		
		try {
			String jobId = JobTracker.generateJobId();
			CombineEntitiesThread combiner= new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalSubProps#Tiger", "http://AnimalsToCombineData#BAD_TARGET_URI", "http://AnimalsToCombineData#auntyEm", 
				null, null
				);
			combiner.start();
			tracker.waitForSuccess(jobId, 300 * 1000);
			
			fail("Missing exception for bad target uri");
		} catch (Exception e) {
			assertTrue("Exception is missing BAD_TARGET_URI", e.getMessage().contains("BAD_TARGET_URI"));
		}
		
		try {
			String jobId = JobTracker.generateJobId();
			CombineEntitiesThread combiner = new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalSubProps#Tiger", "http://AnimalsToCombineData#auntyEm", "http://AnimalsToCombineData#BAD_DUPLICATE_URI", 
				null, null
				);
			combiner.start();
			tracker.waitForSuccess(jobId, 300 * 1000);
			fail("Missing exception for bad target uri");
		} catch (Exception e) {
			assertTrue("Exception is missing BAD_DUPLICATE_URI", e.getMessage().contains("BAD_DUPLICATE_URI"));
		}
		
	}
	
}
