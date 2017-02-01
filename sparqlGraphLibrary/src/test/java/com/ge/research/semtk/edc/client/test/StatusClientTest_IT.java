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
import com.ge.research.semtk.utility.Utility;

public class StatusClientTest_IT {
	
	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	
	private final String JOB_ID1 = "client_test_job_1";
	
	
	@BeforeClass
	public static void setup() throws Exception{
		SERVICE_PROTOCOL = Utility.getPropertyFromFile(Utility.INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.protocol");
		SERVICE_SERVER = Utility.getPropertyFromFile(Utility.INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.statusservice.server");
		SERVICE_PORT = Integer.valueOf(Utility.getPropertyFromFile(Utility.INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.statusservice.port")).intValue();
	}
	
	@Test
	public void testSetSuccess() {
		StatusClientConfig config = null;
		StatusClient client = null;
		
		try {	
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, 
											JOB_ID1);
			client = new StatusClient(config);
			
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
		StatusClientConfig config = null;
		StatusClient client = null;
		
		try {	
			
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, 
											JOB_ID1);
			client = new StatusClient(config);
			
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
	public void testSetFailureTricky() {
		final String MESSAGE = "Failure message. with 'quoted' and \"double-quoted\" and new\nlines\n";
		StatusClientConfig config = null;
		StatusClient client = null;
		
		try {
			
			
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, 
											JOB_ID1);
			client = new StatusClient(config);
			
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
	public void testSetFailure() {
		final String MESSAGE = "Failure message.";
		StatusClientConfig config = null;
		StatusClient client = null;
		
		try {
			
			
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, 
											JOB_ID1);
			client = new StatusClient(config);
			
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
		
		StatusClientConfig config = null;
		StatusClient client = null;
		int percent;

		try {			
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, JOB_ID1);
			client = new StatusClient(config);
			
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
		
		StatusClientConfig config = null;
		StatusClient client = null;
		int percent;

		try {			
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, JOB_ID1);
			client = new StatusClient(config);
			
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
	public void testCreateDeleteJob() {
		
		StatusClientConfig config = null;
		StatusClient client = null;
		int percent;

		try {			
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, JOB_ID1);
			client = new StatusClient(config);
			
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
		
		StatusClientConfig config = null;
		StatusClient client = null;
		
		try {			
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, JOB_ID1);
			client = new StatusClient(config);
			
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
	public void testWaitForPercentCompleteImmediate() {
		final int PERCENT = 5;
		
		StatusClientConfig config = null;
		StatusClient client = null;

		try {			
			// create
			config = new StatusClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, JOB_ID1);
			client = new StatusClient(config);
			
			// set and wait
			client.execSetPercentComplete(PERCENT);
			client.execWaitForPercentComplete(PERCENT - 1, 100);
			// succeed
			
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
