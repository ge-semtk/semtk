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

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.load.utility.Utility;
import com.ge.research.semtk.resultSet.Table;

public class ResultsClientTest_IT {
	private final String SERVICE_PROTOCOL = "http";
	private final String SERVICE_SERVER = "localhost";
	private final int SERVICE_PORT = 12052;
	private final String CSV_CONTENTS = "one,two,three\n1,2,3\n10,20,30\n100,200,300\n";
	private final String EXTENSION = "csv";

	private final String JOB_ID = "results_test_jobid";
	
	@Test
	public void testSingleFile() {
		ResultsClientConfig config = null;
		ResultsClient client = null;
		URL urls[] = null;
		
		try {	
			// create
			config = new ResultsClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT);
			client = new ResultsClient(config);
			
			client.execStoreSingleFileResults(JOB_ID, CSV_CONTENTS, EXTENSION);
			urls = client.execGetResults(JOB_ID);
			
			assertTrue(urls[0] == null); 
		
			String contents = IOUtils.toString(urls[1]);
			assertTrue(contents.equals(CSV_CONTENTS));
			assertTrue(urls[1].toString().endsWith(EXTENSION));
		
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
			cleanup(client, JOB_ID);
		}
	}
	
	@Test
	public void testTable() {
		ResultsClientConfig config = null;
		ResultsClient client = null;
		URL urls[] = null;
		String [] cols = {"col1", "col2"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("one");
		row.add("two");
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			table.addRow(row);
		
			// create
			config = new ResultsClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT);
			client = new ResultsClient(config);
			
			client.execStoreTableResults(JOB_ID, table);
			urls = client.execGetResults(JOB_ID);
			
			assertTrue(urls[0].toString().endsWith("_sample.csv")); 
			assertTrue(urls[1].toString().endsWith(".csv")); 

			// trust ResultsStorageTest.java to test the contents
		
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
			cleanup(client, JOB_ID);
		}
	}
	
	@Test
	public void testBiggerTable() {
		ResultsClientConfig config = null;
		ResultsClient client = null;
		URL urls[] = null;
		long startTime;
		long endTime;
		double elapsed;
		try {
			startTime = System.nanoTime();
			
			JSONObject jsonObj = Utility.getJSONObjectFromFilePath("src/test/resources/table9000.json");
			Table table = Table.fromJson(jsonObj);
			
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> Utility.getJSONEObjectFromFilePath()=%.2f sec", elapsed));

			// --- store results ----
			startTime = System.nanoTime();
			config = new ResultsClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT);
			client = new ResultsClient(config);
			
			client.execStoreTableResults(JOB_ID, table);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execStoreTableResults()=%.2f sec", elapsed));
			
			// --- test results ---
			urls = client.execGetResults(JOB_ID);
			
			assertTrue(urls[0].toString().endsWith(".csv")); 
			assertTrue(urls[1].toString().endsWith(".csv")); 

			// trust ResultsStorageTest.java to test the contents
		
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
			cleanup(client, JOB_ID);
		}
	}
	
	@Test
	public void testStoreCsv() {
		ResultsClientConfig config = null;
		ResultsClient client = null;
		URL urls[] = null;
		
		try {	
			// create
			config = new ResultsClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT);
			client = new ResultsClient(config);
			
			client.execStoreCsvResults(JOB_ID, CSV_CONTENTS);
			urls = client.execGetResults(JOB_ID);
			
			String sample = IOUtils.toString(urls[0]);
			assertTrue(sample.equals(CSV_CONTENTS)); 
		
			String contents = IOUtils.toString(urls[1]);
			assertTrue(contents.equals(CSV_CONTENTS));
		
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
			cleanup(client, JOB_ID);
		}
	}
	
	@Test
	public void test_delete() {

		ResultsClientConfig config = null;
		ResultsClient client = null;
		URL urls[] = null;
		
		try {
			// create
			config = new ResultsClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT);
			client = new ResultsClient(config);

			client.execStoreCsvResults(JOB_ID, CSV_CONTENTS);
			client.execDeleteStorage(JOB_ID);
			urls = client.execGetResults(JOB_ID);

			String sample = IOUtils.toString(urls[0]);
			assertTrue(sample == null);
			
		} catch (FileNotFoundException e) {
			// success
			
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
			cleanup(client, JOB_ID);
		}
	}
	
	private void cleanup(ResultsClient client, String jobId) {
		try {
			client.execDeleteStorage(JOB_ID);
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
