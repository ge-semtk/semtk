/**
 ** Copyright 2020 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.fdccache.test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.fdccache.FdcCacheSpecRunner;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class FdcCacheSpecRunnerTest_IT {
	private final static String CREATOR = "Junit CacheSpecRunnerTest_IT.java";
	private static SparqlConnection cacheConn = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		// load all the FDC nodegroups and config
		IntegrationTestUtility.setupFdcTests(FdcCacheSpecRunnerTest_IT.class);
		
		IntegrationTestUtility.getNodeGroupStoreRestClient().deleteStoredNodeGroup("fdcSampleCacheGetLocations");


		IntegrationTestUtility.getNodeGroupStoreRestClient().executeStoreNodeGroup("fdcSampleCacheGetLocations", "no comment", CREATOR, 
				Utility.getResourceAsJson(FdcCacheSpecRunnerTest_IT.class, "/fdcSampleCacheGetLocations.json"));
		
		String owl = Utility.getResourceAsString(FdcCacheSpecRunnerTest_IT.class, "/fdcCacheSpec_sampleSpec.owl");
		// rename one nodegroup for nodegroupstore testing
		owl = owl.replace("fdcSampleElevation", "fdcSampleElevation-STORE");
		// add fdcCacheSpec_sampleSpec.sadl
		TestGraph.uploadOwlContents(owl);
		
		// create a connection for caching:  TestGraph stuff and model in model[0].  Cache in data[0].
		cacheConn = SparqlConnection.deepCopy(TestGraph.getSparqlConn());
		cacheConn.clearDataInterfaces();
		cacheConn.addDataInterface(TestGraph.getSei(TestGraph.generateGraphName("cache")));
		cacheConn.getDataInterface(0).clearGraph();
	}
	
	@Test
	public void testSimple() throws Exception {
		cacheConn.getDataInterface(0).clearGraph();
		FdcCacheSpecRunner runner = new FdcCacheSpecRunner(
				"sampleSpec", 
				cacheConn,
				60,
				TestGraph.getSei(), 
				IntegrationTestUtility.getOntologyInfoClient(), 
				IntegrationTestUtility.getNodeGroupExecutionRestClient(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient());
		
		// non-standard, so no runStandard()
		assertEquals("Wrong number of steps in sampleSpec", 2, runner.getNumSteps());
		
		// get a bootstrap table with aircraftUri and tailNumber
		Table startTab = TestGraph.execTableSelect(Utility.getResourceAsJson(this, "/fdc_cache_aircraft_select.json"), IntegrationTestUtility.getOntologyInfoClient());
		runner.setBootstrapTable(startTab);
		
		runner.run();
		
		// wait for results
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		String jobId = runner.getJobId();
		tracker.waitForPercentOrMsec(jobId, 100, 60 * 3 * 1000);
		if (tracker.jobSucceeded(jobId) == false) {
			fail(tracker.getJobStatusMessage(jobId));
		}
		
		verifyResults();
	}
	
	private FdcCacheSpecRunner runStandard(long maxEpoch) throws Exception {
		FdcCacheSpecRunner runner = new FdcCacheSpecRunner(
				"sampleSpec", 
				cacheConn,
				maxEpoch,
				TestGraph.getSei(), 
				IntegrationTestUtility.getOntologyInfoClient(), 
				IntegrationTestUtility.getNodeGroupExecutionRestClient(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient());
		
		// get a bootstrap table with aircraftUri and tailNumber
		Table startTab = TestGraph.execTableSelect(Utility.getResourceAsJson(this, "/fdc_cache_aircraft_select.json"), IntegrationTestUtility.getOntologyInfoClient());
		runner.setBootstrapTable(startTab);
		runner.run();
		
		// wait for results
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		tracker.waitForPercentOrMsec(runner.getJobId(), 100, 60 * 3 * 1000);
				
		return runner;
	}
	
	private void verifyResults() throws Exception {
		// check results
		Table resTab = SparqlGraphJson.executeSelectToTable(
				Utility.getResourceAsJson(this, "/FdcSampleCacheSelectAircraftToElev.json"), 
				cacheConn,
				IntegrationTestUtility.getOntologyInfoClient());
		if (resTab.getNumRows() != 1) {
			fail("Expected table with a single row of aircraft, location and elevation.  Got table:\n" + resTab.toCSVString());
		}
	}
	
	@Test
	public void testDontRecache() throws Exception {
		cacheConn.getDataInterface(0).clearGraph();
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		
		FdcCacheSpecRunner runner = runStandard(60);
		
		if (tracker.jobSucceeded(runner.getJobId()) == false) {
			fail(tracker.getJobStatusMessage(runner.getJobId()));
		}
		
		runner = runStandard(60);
		if (tracker.jobSucceeded(runner.getJobId()) == false) {
			fail(tracker.getJobStatusMessage(runner.getJobId()));
		}
		
		verifyResults();
		
		assertTrue("Data was re-cached when prev cache should have been used.", tracker.getJobStatusMessage(runner.getJobId()).contains("previously cached"));
	}
	
	@Test
	public void testRecache() throws Exception {
		cacheConn.getDataInterface(0).clearGraph();
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		
		FdcCacheSpecRunner runner = runStandard(60);
		
		if (tracker.jobSucceeded(runner.getJobId()) == false) {
			fail(tracker.getJobStatusMessage(runner.getJobId()));
		}
		
		runner = runStandard(0);
		if (tracker.jobSucceeded(runner.getJobId()) == false) {
			fail(tracker.getJobStatusMessage(runner.getJobId()));
		}
		
		verifyResults();
		assertTrue("Expired cache was not re-cached.", tracker.getJobStatusMessage(runner.getJobId()).contains("Successfully cached data"));
	}
	
	@Test
	public void testErrChangeCacheSpecId() throws Exception {
		cacheConn.getDataInterface(0).clearGraph();
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		
		FdcCacheSpecRunner runner = runStandard(60);
		
		if (tracker.jobSucceeded(runner.getJobId()) == false) {
			fail(tracker.getJobStatusMessage(runner.getJobId()));
		}
		
		//------ run non-standard with different specID ------
		runner = new FdcCacheSpecRunner(
				"otherSpec", 
				cacheConn,
				60,
				TestGraph.getSei(), 
				IntegrationTestUtility.getOntologyInfoClient(), 
				IntegrationTestUtility.getNodeGroupExecutionRestClient(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient());

		// get a bootstrap table with aircraftUri and tailNumber
		Table startTab = TestGraph.execTableSelect(Utility.getResourceAsJson(this, "/fdc_cache_aircraft_select.json"), IntegrationTestUtility.getOntologyInfoClient());
		runner.setBootstrapTable(startTab);
		runner.run();

		// wait for results
		tracker.waitForPercentOrMsec(runner.getJobId(), 100, 60 * 3 * 1000);
		assertFalse("Recaching same graph with new specId did not fail.", tracker.jobSucceeded(runner.getJobId()));
	}
	
	@Test
	public void testErrChangeCacheBootstrap() throws Exception {
		cacheConn.getDataInterface(0).clearGraph();
		JobTracker tracker = new JobTracker(TestGraph.getSei());
		
		FdcCacheSpecRunner runner = runStandard(60);
		
		if (tracker.jobSucceeded(runner.getJobId()) == false) {
			fail(tracker.getJobStatusMessage(runner.getJobId()));
		}
		
		//------ run non-standard with different specID ------
		runner = new FdcCacheSpecRunner(
				"sampleSpec", 
				cacheConn,
				60,
				TestGraph.getSei(), 
				IntegrationTestUtility.getOntologyInfoClient(), 
				IntegrationTestUtility.getNodeGroupExecutionRestClient(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient());

		// get a bootstrap table with aircraftUri and tailNumber
		Table startTab = TestGraph.execTableSelect(Utility.getResourceAsJson(this, "/fdc_cache_aircraft_select.json"), IntegrationTestUtility.getOntologyInfoClient());
		startTab.getRow(0).set(0, "changed");
		runner.setBootstrapTable(startTab);
		runner.run();

		// wait for results
		tracker.waitForPercentOrMsec(runner.getJobId(), 100, 60 * 3 * 1000);
		assertFalse("Recaching same graph with new bootstrap table did not fail.", tracker.jobSucceeded(runner.getJobId()));
	}
	

}
