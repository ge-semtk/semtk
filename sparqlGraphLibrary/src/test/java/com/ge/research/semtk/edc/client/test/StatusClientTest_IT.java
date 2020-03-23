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

import static org.junit.Assert.*;

import java.net.ConnectException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.Utility;

public class StatusClientTest_IT {
	
	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	
	private final String JOB_ID1 = "client_test_job_1";
	
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		SERVICE_PROTOCOL = IntegrationTestUtility.getServiceProtocol();
		SERVICE_SERVER = IntegrationTestUtility.getStatusServiceServer();
		SERVICE_PORT = IntegrationTestUtility.getStatusServicePort();
	}
	
	@Test
	public void testSetSuccess() {
		StatusClient client = null;
		
		try {	
			client = this.getClient();

			// set the status
			client.execSetSuccess();
			
			// check stuff
			String status = client.execGetStatus();
			int percent = client.execGetPercentComplete();
			
			assertTrue(status.equals("Success"));
			assertTrue(percent == 100);
				
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch (Exception e){
			e.printStackTrace();
			fail("Unexpected exception");
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testSetSuccessMsg() {
		final String MESSAGE = "Success message.";
		StatusClient client = null;
		
		try {	
			client = this.getClient();

			
			// set the status
			client.execSetSuccess(MESSAGE);
			
			// check stuff
			String status = client.execGetStatus();
			int percent = client.execGetPercentComplete();
			String message = client.execGetStatusMessage();
			
			assertTrue(status.equals("Success"));
			assertTrue(percent == 100);
			assertTrue(message.equals(MESSAGE));
					
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch (Exception e){
			e.printStackTrace();
			fail("Unexpected exception");
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testSetFailureTricky() throws Exception {
		final String MESSAGE = "Failure message. with 'quoted' and \"double-quoted\" and new\nlines\n";
		StatusClient client = null;
		
		try {
			client = this.getClient();

			
			// set the status
			client.execSetFailure(MESSAGE);
			
			// check stuff
			String status = client.execGetStatus();
			int percent = client.execGetPercentComplete();
			String message = client.execGetStatusMessage();
			
			assertTrue(status.equals("Failure"));
			assertTrue(percent == 100);
			assertTrue(message.equals(MESSAGE));
					
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testSetFailure() {
		final String MESSAGE = "Failure message.";
		StatusClient client = null;
		
		try {
			
			client = this.getClient();

			
			// set the status
			client.execSetFailure(MESSAGE);
			
			// check stuff
			String status = client.execGetStatus();
			int percent = client.execGetPercentComplete();
			String message = client.execGetStatusMessage();
			
			assertTrue(status.equals("Failure"));
			assertTrue(percent == 100);
			assertTrue(message.equals(MESSAGE));
					
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch (Exception e){
			e.printStackTrace();
			fail("Unexpected exception");
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testPercentComplete() {
		final int PERC1 = 5;
		final int PERC2 = 88;
		final int PERC_ERR = 100;
		
		StatusClient client = null;
		int percent;

		try {			
			// create
			client = this.getClient();
			
			// set and check
			client.execSetPercentComplete(PERC1);
			percent = client.execGetPercentComplete();
			assertTrue(percent == PERC1);
			
			// set and check
			client.execSetPercentComplete(PERC2);
			percent = client.execGetPercentComplete();
			assertTrue(percent == PERC2);
			
			String msg = client.execGetStatusMessage();
			assertTrue(msg.equals(""));
			
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch (Exception e){
			e.printStackTrace();
			fail("Unexpected exception");
		}
		
		try {			
			// set and check
			client.execSetPercentComplete(PERC_ERR);
			fail();
		
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch(Exception e){
			assertTrue(e.toString().contains("Set success or failure instead"));
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testPercentCompleteMsg() {
		final int PERC1 = 5;
		final int PERC2 = 88;
		final int PERC_ERR = 100;
		final String MESSAGE = "Message";
		
		StatusClient client = null;
		int percent;

		try {			
			client = this.getClient();

			
			// set and check
			client.execSetPercentComplete(PERC1);
			percent = client.execGetPercentComplete();
			assertTrue(percent == PERC1);
			
			String msg = client.execGetStatusMessage();
			assertTrue(msg.equals(""));
			
			// set and check
			client.execSetPercentComplete(PERC2, MESSAGE);
			percent = client.execGetPercentComplete();
			assertTrue(percent == PERC2);
			
			String msg2 = client.execGetStatusMessage();
			assertTrue(msg2.equals(MESSAGE));
			
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch (Exception e){
			e.printStackTrace();
			fail("Unexpected exception");
		}
		
		try {			
			// set and check
			client.execSetPercentComplete(PERC_ERR);
			fail();
		
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch(Exception e){
			assertTrue(e.toString().contains("Set success or failure instead"));
		} finally {
			cleanup(client);
		}
	}
	@Test
	public void testGetJobsInfo() throws Exception {
		StatusClient client = this.getClient();
		
		try {
			Table infoTable = client.getJobsInfo();
			
			assertTrue(infoTable != null);
		} finally {
			cleanup(client);
		}

	}
	
	@Test
	public void testSetName() throws Exception {
		StatusClient client = this.getClient();
		try {
			client.execSetName("myNewName");
			Table infoTable = client.getJobsInfo();

			assertTrue(infoTable != null);
			assertTrue(infoTable.toCSVString().contains("myNewName"));
		} finally {
			cleanup(client);
		}

	}
	
	@Test
	public void testCreateDeleteJob() {
		
		StatusClient client = null;
		int percent;

		try {			
			client = this.getClient();

			
			// set and check
			client.execSetPercentComplete(50);
			percent = client.execGetPercentComplete();
			assertEquals(percent,50);
			
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch (Exception e){
			e.printStackTrace();
			fail("Unexpected exception");
		}
		
		try {					
			client.execDeleteJob();
			percent = client.execGetPercentComplete();
			fail();
			
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch(Exception e){
			// expect to get here
			assertTrue(e.toString().contains("Can't find Job"));			
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testWaitForPercentCompleteTimeout() {
		final int PERCENT = 5;
		
		StatusClient client = null;
		
		try {			
			client = this.getClient();

			// set and check
			client.execSetPercentComplete(PERCENT);
			client.execWaitForPercentComplete(PERCENT + 1, 100);
			fail();
			
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
	
		} catch(Exception e){
			assertTrue(e.toString().contains("Maximum wait time of"));
			
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testWaitForPercentCompleteImmediate() throws Exception {
		final int PERCENT = 5;
		
		StatusClient client = null;

		try {			
			client = this.getClient();

			// set and wait
			client.execSetPercentComplete(PERCENT);
			client.execWaitForPercentComplete(PERCENT - 1, 100);
			// succeed
			
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testWaitForPercentOrMsec_Msec() {
		final int PERCENT = 5;
		
		StatusClient client = null;

		try {			
			client = this.getClient();

			// set and wait for timeout.   Get back original percent
			client.execSetPercentComplete(PERCENT);
			int percent = client.execWaitForPercentOrMsec(PERCENT + 5, 500);
			assertEquals(PERCENT, percent);
			
			// try again
			percent = client.execWaitForPercentOrMsec(PERCENT, 500);
			assertEquals(PERCENT, percent);
			
			// NOTE: this doesn't test detecting percent complete reached during the wait
			//       i.e. there is no threaded test.
			
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch (Exception e){
			e.printStackTrace();
			fail("Unexpected exception");
		} finally {
			cleanup(client);
		}
	}
	
	@Test
	public void testWaitForPercentComplete() {
		// TODO - get time and gumption to write multi-threaded wait test 
	}
	
	private StatusClient getClient() throws Exception {
		StatusClientConfig config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, JOB_ID1);
		return new StatusClient(config);
	}
	
	private void cleanup(StatusClient client) {
		try {	
			client.execDeleteJob();
		} catch (ConnectException e) {
			e.printStackTrace();
			fail("No service running at this location.");
		} catch (EndpointNotFoundException e) {
			e.printStackTrace();
			fail("Wrong service running at this location.");
		} catch (Exception e){
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}
}
