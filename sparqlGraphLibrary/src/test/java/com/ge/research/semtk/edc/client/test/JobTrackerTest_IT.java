/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.edc.client.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.google.common.io.Files;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.resultsStorage.TableResultsStorage;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.edc.JobEndpointProperties;

public class JobTrackerTest_IT {

	static TableResultsStorage trstore = null;
	static File tempFolder = null;
	
	/**
	 * Before any tests run, load the Jobs model into TestGraph
	 */
	@BeforeClass
	public static void loadTestGraph() throws Exception{
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/serviceJob.owl");
		
		tempFolder = Files.createTempDir();
		trstore = new TableResultsStorage(tempFolder.getPath());
	}
	
	@AfterClass
	public static void done() {
		tempFolder.delete();
	}
	
	private JobEndpointProperties getProp() throws Exception {
		JobEndpointProperties prop = new JobEndpointProperties();
		prop.setJobEndpointDataset(TestGraph.getDataset());
		prop.setJobEndpointDomain("http//research.ge.com");
		prop.setJobEndpointServerUrl(TestGraph.getSparqlServer());
		prop.setJobEndpointType(TestGraph.getSparqlServerType());
		prop.setJobEndpointPassword(TestGraph.getPassword());
		prop.setJobEndpointUsername(TestGraph.getUsername());
		return prop;
	}
	
	@Test
	public void test_create_set_read_delete() throws Exception {
		
		// Create job, set %, read it back, delete job
		String jobId = "test7";
		int percent = 10;
		
		JobTracker tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId, trstore);   // clean up mess of any previously failed test
		tracker.createJob(jobId);
		tracker.setJobPercentComplete(jobId, percent);
		String status = tracker.getJobStatus(jobId);
		int ret = tracker.getJobPercentComplete(jobId);
		tracker.deleteJob(jobId, trstore);
			
		assertTrue(ret == percent);
		assertTrue(status.equals("InProgress"));
	}
	
	@Test
	public void test_set_read_delete() throws Exception {
		// DON'T Create job, set %, read it back, delete job
		String jobId = "test7";
		int percent = 99;
		
		JobTracker tracker = new JobTracker(getProp());
		tracker.setJobPercentComplete(jobId, percent);
		int ret = tracker.getJobPercentComplete(jobId);
		tracker.deleteJob(jobId, trstore);
		assertTrue(ret == percent);

	}
	
	@Test
	public void test_bad_get() throws Exception {
		// try to retrieve getPercentComplete from non-existant job
		String jobId = "test7";
		JobTracker tracker = null;
		
		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId, trstore);
		
		try {
			// this should throw and exception
			tracker.getJobPercentComplete(jobId);
			fail();
			
		} catch(Exception e){
			assertTrue(e.toString().contains("Can't find"));
		}
	}
	
	@Test
	public void test_bad_set_percent_complete() throws Exception {
		// try to retrieve getPercentComplete from non-existant job
		String jobId = "test7";
		JobTracker tracker = null;
		
		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId);
		
		try {
			// this should throw and exception
			tracker.setJobPercentComplete(jobId, -1);
			fail();
			
		} catch(Exception e){
			assertTrue(e.toString().contains("negative percent"));
		}
		
		try {
			// this should throw and exception
			tracker.setJobPercentComplete(jobId, 100);
			fail();
			
		} catch(Exception e){
			assertTrue(e.toString().contains("success or failure instead"));
		}
	}
	
	@Test
	public void test_set_success() throws Exception {
		// try to retrieve getPercentComplete from non-existant job
		String jobId = "test7";
		JobTracker tracker = null;
		
		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId);
			
		// create job if needed, and set success things
		tracker.setJobSuccess(jobId);
			
		// query things back
		int percent = tracker.getJobPercentComplete(jobId);
		String status = tracker.getJobStatus(jobId);
		String msg = tracker.getJobStatusMessage(jobId);
			
		assertTrue(msg.equals(""));
		assertTrue(percent == 100);
		assertTrue(status.equals("Success"));
	}
	
	@Test
	public void test_set_urls() throws Exception {
		// test setting URLs
		String jobId = "test7";
		JobTracker tracker = null;
		
		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId);
		
		URL FULL_URL = new URL("http://machine:80/fullResults.csv");
		int PERCENT = 2;
			
		tracker.setJobPercentComplete(jobId, PERCENT);
		int percent0 = tracker.getJobPercentComplete(jobId);
		String status0 = tracker.getJobStatus(jobId);
			
		// create job if needed, and set success things
		tracker.setJobResultsURL(jobId, FULL_URL);
			
		// query things back
		URL fullURL = tracker.getFullResultsURL(jobId);
		URL sampleURL = tracker.getSampleResultsURL(jobId);
		int percent = tracker.getJobPercentComplete(jobId);
		String status = tracker.getJobStatus(jobId);
		String statusMessage = tracker.getJobStatusMessage(jobId);
			
		assertTrue(fullURL.toString().equals(FULL_URL.toString()));
		assertTrue(percent == PERCENT);
		assertTrue(percent0 == PERCENT);
		assertTrue(status.equals(status0));
		assertTrue(statusMessage.equals(""));
		
	}
	
	@Test
	public void test_with_trstore() throws Exception {
		// test setting URLs
		String jobId = "test8" + UUID.randomUUID().toString();
		JobTracker tracker = null;
		
		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId);
		
		JSONObject data = new JSONObject();
		data.put("test_with_trstore", "test value");
		
		this.trstore.storeTableResultsJsonInitialize(jobId, data);
		this.trstore.storeTableResultsJsonAddIncremental(jobId, "contents");
		URL fullURL = this.trstore.storeTableResultsJsonFinalize(jobId);
		tracker.setJobResultsURL(jobId, fullURL);
		tracker.setJobSuccess(jobId);
		
		assertTrue(tempFolder.list().length > 0);
		assertTrue(tracker.getFullResultsURL(jobId).toString().equals(fullURL.toString()));
		assertTrue(tracker.getJobPercentComplete(jobId) == 100);
		assertTrue(tracker.getJobStatus(jobId).equals(JobTracker.STATUS_SUCCESS));
		
		// delete job and make sure table results files disappear too
		tracker.deleteJob(jobId, this.trstore);
		assertTrue(tempFolder.list().length == 0);
		assertTrue(! tracker.jobExists(jobId));

		
	}
	
	@Test
	public void test_get_jobs_info() throws Exception {
		String jobId = "test9" + UUID.randomUUID().toString();
		JobTracker tracker =  new JobTracker(getProp());
		tracker.deleteJob(jobId);
		tracker.createJob(jobId);
		
		Table t = tracker.getJobsInfo();
		
		// delete job and make sure table results files disappear too
		tracker.deleteJob(jobId, this.trstore);
		assertTrue(t.getNumRows() > 0);
		ArrayList<String> colNames = new ArrayList<String>();
		colNames.addAll(Arrays.asList(t.getColumnNames()));
		assertTrue(colNames.contains("creationTime"));
		assertTrue(colNames.contains("id"));
		assertTrue(colNames.contains("percentComplete"));
		assertTrue(colNames.contains("statusMessage"));
		assertTrue(colNames.contains("userName"));
		assertTrue(colNames.contains("status"));
		
	}

	@Test
	public void test_set_urls_blank_sample() throws Exception {
		// test setting URLs
		String jobId = "test7";
		JobTracker tracker = null;

		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId);
		
		URL FULL_URL = new URL("http://machine:80/fullResults.csv");
		int PERCENT = 2;
			
		tracker.setJobPercentComplete(jobId, PERCENT);
		int percent0 = tracker.getJobPercentComplete(jobId);
		String status0 = tracker.getJobStatus(jobId);
			
		// create job if needed, and set success things
		tracker.setJobResultsURL(jobId, FULL_URL);
			
		// query things back
		URL fullURL = tracker.getFullResultsURL(jobId);
		URL sampleURL = tracker.getSampleResultsURL(jobId);
		int percent = tracker.getJobPercentComplete(jobId);
		String status = tracker.getJobStatus(jobId);
		String statusMessage = tracker.getJobStatusMessage(jobId);
			
		assertTrue(sampleURL == null);
		assertTrue(fullURL.toString().equals(FULL_URL.toString()));
		
	}

	@Test
	public void test_set_failure() throws Exception {
		// test setting URLs
		String jobId = "test7";
		JobTracker tracker = null;
		
		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId);

		final String STATUS_MESSAGE = "Failure status message";
		tracker.setJobFailure(jobId, STATUS_MESSAGE);
			
		int percent = tracker.getJobPercentComplete(jobId);
		String status = tracker.getJobStatus(jobId);
		String message = tracker.getJobStatusMessage(jobId);
			
		tracker.deleteJob(jobId);
		assertTrue(percent == 100);
		assertTrue(status.equals("Failure"));
		assertTrue(message.equals(STATUS_MESSAGE));

	}
	@Test
	public void test_set_failure_tricky() throws Exception {
		// test setting URLs
		String jobId = "test7";
		final String STATUS_MESSAGE = "Failure message. with 'quoted' and \"double-quoted\" and new\nlines\n";
		JobTracker tracker = null;
		
		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId);
		
		tracker.setJobFailure(jobId, STATUS_MESSAGE);
			
		int percent = tracker.getJobPercentComplete(jobId);
		String status = tracker.getJobStatus(jobId);
		String message = tracker.getJobStatusMessage(jobId);
			
		assertTrue(percent == 100);
		assertTrue(status.equals("Failure"));
		assertTrue(message.equals(STATUS_MESSAGE));

	}
	@Test
	public void test_bad_jobId() throws Exception {
		// try to retrieve getPercentComplete from non-existant job
		String jobId = "test7";
		String MESSAGE = "test failure";

		JobTracker tracker = null;
		tracker = new JobTracker(getProp());
		tracker.deleteJob(jobId);

		// create job if needed, and set success things
		tracker.setJobPercentComplete(jobId, 50);
		tracker.setJobFailure(jobId, MESSAGE);
			
		// query things back
		String status = tracker.getJobStatus(jobId);
		String message = tracker.getJobStatusMessage(jobId);
		int percent = tracker.getJobPercentComplete(jobId);

		assertTrue(message.equals(MESSAGE));
		assertTrue(status.equals("Failure"));
		assertTrue(percent == 100);
	}
}
