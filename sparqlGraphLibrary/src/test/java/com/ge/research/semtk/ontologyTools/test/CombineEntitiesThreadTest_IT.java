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
	
	@Test
	public void test1() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "animalSubProps.owl");
		TestGraph.uploadOwlResource(this, "animalsToCombineData.owl");	
		
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = JobTracker.generateJobId();
		ArrayList<String> skipProps = new ArrayList<String>();
		skipProps.add("http://AnimalSubProps#name");
		CombineEntitiesThread combiner = new CombineEntitiesThread(
				tracker, jobId, 
				TestGraph.getOInfo(), TestGraph.getSparqlConn(), 
				"http://AnimalSubProps#Tiger", "http://AnimalsToCombine#auntyEm", "http://AnimalsToCombine#auntyEmDuplicate", 
				skipProps
				);
		combiner.start();
		tracker.waitForSuccess(jobId, 300 * 1000);
		assertTrue("test", true);

		
	}
	
}
