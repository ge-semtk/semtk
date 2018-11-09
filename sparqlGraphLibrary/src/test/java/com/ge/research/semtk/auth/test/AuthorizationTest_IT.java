/**
 ** Copyright 2018 General Electric Company
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
package com.ge.research.semtk.auth.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.AuthorizationProperties;
import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.EndpointProperties;
import com.ge.research.semtk.edc.SemtkEndpointProperties;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/*
 * @author: 200001934 
 * This performs all integration tests on Authorization functions including clients
 * 
 * Strategy:
 * Integration tests in Jenkins use the normal stood-up services.
 * 
 * Tests will only pass if these are loaded to auth.graphName:
 * 			authorization.owl 
 *          src/test/resources files: auth_*.csv  (ingested with their namesake in src/main/resources/nodegroups) 
 * 
 * This affects "production" security by 
 *    adding some graphs starting with "http://securityTest"
 *    adding some users starting with "testuser"
 *    adding some user groups starting with "securityTest"
 *    adding "testuser_job_admin" to the JobAdmin group
 * 
 * If they are not loaded, the @beforeTest will load them.
 * But tests will not pass until all the services re-cache their AuthorizationManager data,
 * which occurs every auth.refreshFreqSec seconds (usually 5 minutes)
 * 
 */
public class AuthorizationTest_IT {
	public static ArrayList<String> cleanupJobIds = new ArrayList<String>();
	/**
	 * Checks and if needed loads auth information needed for testing.
	 * See notes at the top of this file.
	 * @throws Exception
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
		
	}
	
	private static void setupAuthorization() throws Exception {

		// Set up authorization, should be same location as all services participating in the test
		AuthorizationProperties auth_prop = IntegrationTestUtility.getAuthorizationProperties();
		auth_prop.setSettingsFilePath("src/test/resources/auth_test.json");
		if (! AuthorizationManager.authorize( auth_prop ) ) {
			throw new Exception("Authorization.authorize failed");
		}
		
	}
	
	@AfterClass
	public static void cleanupJobs() {
		ArrayList<String> cleaned = new ArrayList<String>();
		
		for (String jobId : cleanupJobIds) {
			try {
				StatusClient status = IntegrationTestUtility.getStatusClient(jobId);
				status.execDeleteJob();
				cleaned.add(jobId);
			} catch (Exception e) {
				LocalLogger.printStackTrace(e);
			}
		}
		
		for (String jobId : cleaned) {
			cleanupJobIds.remove(cleaned);
		}
	}
	
	private String getTestJobId() {
		String jobId = IntegrationTestUtility.generateJobId("AuthorizationTest_IT");
		cleanupJobIds.add(jobId);
		return jobId;
	}
	private void testAllStatusResultsEndpointsSucceed(String createUser, String defaultUser) throws Exception {
		// create jobId and client
		String jobId = this.getTestJobId();
		String jobId2 = this.getTestJobId();
	
		// get "createUser" clients
		ThreadAuthenticator.authenticateThisThread(createUser);
		StatusClient status = IntegrationTestUtility.getStatusClient(jobId);
		StatusClient status2 = IntegrationTestUtility.getStatusClient(jobId2);
		
		// create jobs
		status.execSetPercentComplete(10);
		status2.execSetPercentComplete(10);
		
		// test status
		ThreadAuthenticator.authenticateThisThread(defaultUser);
		status = IntegrationTestUtility.getStatusClient(jobId);
		ResultsClient results = IntegrationTestUtility.getResultsClient();
		
		status.execGetPercentComplete();
		status.execGetStatus();
		status.execGetStatusMessage();
		status.execSetFailure("test failure");
		status.execSetName("testName");
		status.execSetPercentComplete(99, "message");
		status.execSetSuccess();
		status.execSetSuccess("message");
		status.execWaitForPercentOrMsec(100, 1);
		status2.execDeleteJob();
		
		// test results
		String fileId = results.storeBinaryFile(status.getJobId(), IntegrationTestUtility.getSampleFile(this));
		results.execReadBinaryFile(fileId);
		
		results.execStoreTableResults(jobId, IntegrationTestUtility.getSampleTable());
		results.getTableResultsCSV(jobId, 10);
		results.getTableResultsJson(jobId, 10);
		results.execDeleteJob(jobId);
	
	}
	
	private void testAllStatusResultsEndpointsFail(String createUser, String defaultUser) throws Exception {
		// create jobId and client
		String jobId = this.getTestJobId();
		String jobIdNonEmpty = this.getTestJobId();
		
		// create the job
		ThreadAuthenticator.authenticateThisThread(createUser);
		StatusClient status = IntegrationTestUtility.getStatusClient(jobId);
		ResultsClient results = IntegrationTestUtility.getResultsClient();
		ResultsClient resultsNonEmpty = IntegrationTestUtility.getResultsClient();

		
		// create a job and a fileId and some results
		status.execSetPercentComplete(10);
		String fileId = results.storeBinaryFile(status.getJobId(), IntegrationTestUtility.getSampleFile(this));;
		resultsNonEmpty.execStoreTableResults(jobIdNonEmpty, IntegrationTestUtility.getSampleTable());
		
		// test with defaultUser
		ThreadAuthenticator.authenticateThisThread(defaultUser);
		status = IntegrationTestUtility.getStatusClient(jobId);
		results = IntegrationTestUtility.getResultsClient();
		
		if ( AuthorizationManager.FORGIVE_ALL) {
			return;
		}

		// --- test status ---
		try {
			status.execGetPercentComplete();
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execGetStatus();
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execGetStatusMessage();
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execSetFailure("test failure");
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execSetName("testName");
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execSetPercentComplete(99, "message");
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execSetSuccess();
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execSetSuccess("message");
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execWaitForPercentOrMsec(100, 1);
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			status.execDeleteJob();
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		
		// --- test results ---
		try {
			results.storeBinaryFile(status.getJobId(), IntegrationTestUtility.getSampleFile(this));;
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			results.execReadBinaryFile(fileId);
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			results.execStoreTableResults(jobId, IntegrationTestUtility.getSampleTable());
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			// make sure empty jobId gets the Auth error, no the empty error
			results.getTableResultsCSV(jobId, 10);
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			// repeat for non-empty file
			results.getTableResultsCSV(jobIdNonEmpty, 10);
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			// make sure empty jobId gets the Auth error, no the empty error
			results.getTableResultsJson(jobId, 10);
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			// repeat for non-empty file
			results.getTableResultsJson(jobIdNonEmpty, 10);
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		try {
			results.execDeleteJob(jobId);
			fail("Expected AuthorizationException did not occur");
		} catch (AuthorizationException ae) {
		}
		
	}
	
	@Test
	public void testCheckJobOwnership() throws Exception {
		ThreadAuthenticator.authenticateThisThread("user1");
	
		// same owner
		try {
			AuthorizationManager.throwExceptionIfNotJobOwner("user1", "item");
			
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			e.printStackTrace();
			fail("Authorization failed");
		}
		
		if (! AuthorizationManager.FORGIVE_ALL) {
			// different owner
			try {
				AuthorizationManager.throwExceptionIfNotJobOwner("user2", "item");
				fail("No exception thrown for bad ownership");
			} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			}
		}
		
	}
	
	@Test
	public void testCheckAdminStatusClient() throws Exception {
		String user1 = IntegrationTestUtility.generateUser("AuthorizationTest_IT", "1");
		String user2 = IntegrationTestUtility.generateUser("AuthorizationTest_IT", "2");
		testAllStatusResultsEndpointsFail(user1, user2);
		testAllStatusResultsEndpointsSucceed(user1, user1);
	}
	
	@Test
	public void testCheckAdmin() throws Exception {
		String user1 = IntegrationTestUtility.generateUser("AuthorizationTest_IT", "1");
		String user5 = IntegrationTestUtility.generateUser("AuthorizationTest_IT", "5");

		ThreadAuthenticator.authenticateThisThread(user1);
		ThreadAuthenticator.setJobAdmin(true);
		
		try {
			AuthorizationManager.throwExceptionIfNotJobOwner(user1, "item");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			e.printStackTrace();
			fail("Authorization failed");
		} 
		
		if ( !AuthorizationManager.FORGIVE_ALL) {
		
			ThreadAuthenticator.setJobAdmin(false);
			try {
				AuthorizationManager.throwExceptionIfNotJobOwner(user5, "item");
				fail("Admin didn't reset");
			} catch (com.ge.research.semtk.auth.AuthorizationException e) {
	
			} 
		}
		
	}
	
	@Test
	public void testJobAdmins() throws Exception {
		setupAuthorization();
		
		// make sure some JobAdmin loaded
		if (AuthorizationManager.getJobAdmins().size() == 0) {
			fail("No job admins are loaded");
		}
		
		// authenticate thread with first jobAdmin
		String jobAdmin = AuthorizationManager.getJobAdmins().get(0);
		ThreadAuthenticator.authenticateThisThread(jobAdmin);
		
		// tests
		try {
			String otherUser = IntegrationTestUtility.generateUser("AuthorizationTest_IT", "other");
			// since I'm jobAdmin I own others' jobs
			AuthorizationManager.throwExceptionIfNotJobOwner(otherUser, "item");
			// since I'm jobAdmin I own my own jobs
			AuthorizationManager.throwExceptionIfNotJobOwner(jobAdmin, "item");

		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			e.printStackTrace();
			fail("Authorization failed for a jobAdmin: " + jobAdmin);
		} 
		
	}

}
