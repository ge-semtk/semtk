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

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.Utility;

public class ResultsClientTest_IT {
	
	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	
	private final String CSV_CONTENTS = "one,two,three\n1,2,3\n10,20,30\n100,200,300\n";
	private final String EXTENSION = "csv";
	
	private static ResultsClient client = null;
	
	@BeforeClass
	public static void setup() throws Exception{
		SERVICE_PROTOCOL = IntegrationTestUtility.getServiceProtocol();
		SERVICE_SERVER = IntegrationTestUtility.getResultsServiceServer();
		SERVICE_PORT = IntegrationTestUtility.getResultsServicePort();
		client = new ResultsClient(new ResultsClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT));
	}

	
	
	@Test
	public void testStoreTable() throws Exception {

		String jobId = "results_test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"col1", "col2"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("one");
		row.add("two");
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			table.addRow(row);
			
			client.execStoreTableResults(jobId, table);
			URL[] urls = client.execGetResults(jobId);
			
			assertTrue(urls[0].toString().endsWith("_sample.json")); 
			assertTrue(urls[1].toString().endsWith(".csv")); 

			// check the results.
			String resultString = Utility.getURLContentsAsString(urls[1]);
			String expectedResultString = "col1,col2\none,two\none,two\n";		
			assertEquals(expectedResultString, resultString);
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	@Test
	public void testStoreTable_WithCommasAndQuotes() throws Exception {
		
		String jobId = "results_test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"colA", "colB","colC","colD"};
		String [] types = {"String", "String", "String","String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("apple,ant");  					// this element has a comma
		row.add("bench");
		row.add("\"cabana\"");					// this element has quotes
		row.add("Dan declared \"hi, dear\"");	// this element has quotes and a comma
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);

			client.execStoreTableResults(jobId, table);
			URL[] urls = client.execGetResults(jobId);
			
			String resultString = Utility.getURLContentsAsString(urls[1]);
			String expectedResultString = "colA,colB,colC,colD\n\"apple,ant\",\"bench\",\"\"\"cabana\"\"\",\"Dan declared \"\"hi, dear\"\"\"\n";
			assertEquals(expectedResultString, resultString);
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	@Test
	public void testStoreTable_Medium() throws Exception {

		String jobId = "results_test_jobid_" + UUID.randomUUID();
		
		try {
			long startTime = System.nanoTime();
			
			JSONObject jsonObj = Utility.getJSONObjectFromFilePath("src/test/resources/table9000.json");
			Table table = Table.fromJson(jsonObj);
			
			long endTime = System.nanoTime();
			double elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> Utility.getJSONEObjectFromFilePath()=%.2f sec", elapsed));

			// --- store results ----
			startTime = System.nanoTime();		
			client.execStoreTableResults(jobId, table);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execStoreTableResults()=%.2f sec", elapsed));
			
			// --- test results ---
			URL[] urls = client.execGetResults(jobId);
			
			assertTrue(urls[0].toString().endsWith(".json")); 
			assertTrue(urls[1].toString().endsWith(".csv")); 

			// trust ResultsStorageTest.java to test the contents
		} finally {
			cleanup(client, jobId);
		}
	}
	
	/**
	 * Adjust NUM_COLS and NUM_ROWS to make this bigger when performance testing.  
	 * (Committing them on the smaller size)
	 */
	@Test
	public void testStoreTable_Huge() throws Exception {

		String jobId = "results_test_jobid_" + UUID.randomUUID();
		
		try {
			
			// construct a huge table
			final int NUM_COLS = 100;
			final int NUM_ROWS = 1000;
			String[] cols = new String[NUM_COLS];
			String[] colTypes = new String[NUM_COLS];
			ArrayList<String> row = new ArrayList<String>();
			for(int i = 0; i < NUM_COLS; i++){
				cols[i] = "col" + i;
				colTypes[i] = "String";
				row.add("Element" + i);
			}
			Table table = new Table(cols, colTypes, null);
			for(int i = 0; i < NUM_ROWS; i++){
				table.addRow(row);
			}
			
			// --- store results ----
			long startTime = System.nanoTime();		
			client.execStoreTableResults(jobId, table);
			long endTime = System.nanoTime();
			double elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execStoreTableResults()=%.2f sec", elapsed));
			
			// --- test results ---
			URL[] urls = client.execGetResults(jobId);
			
			assertTrue(urls[0].toString().endsWith(".json")); 
			assertTrue(urls[1].toString().endsWith(".csv")); 

			// trust ResultsStorageTest.java to test the contents
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	@Test
	public void testStoreSingleFile() throws Exception {

		String jobId = "results_test_jobid_" + UUID.randomUUID();
		
		try {	
			client.execStoreSingleFileResults(jobId, CSV_CONTENTS, EXTENSION);
			URL[] urls = client.execGetResults(jobId);
			
			assertTrue(urls[0] == null); 
			String contents = IOUtils.toString(urls[1]);
			assertTrue(contents.equals(CSV_CONTENTS));
			assertTrue(urls[1].toString().endsWith(EXTENSION));
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	@Test
	public void testStoreCsv() throws Exception {

		String jobId = "results_test_jobid_" + UUID.randomUUID();
		
		try {	
			client.execStoreCsvResults(jobId, CSV_CONTENTS);
			URL[] urls = client.execGetResults(jobId);
			
			String sample = IOUtils.toString(urls[0]);
			assertTrue(sample.equals(CSV_CONTENTS)); 
		
			String contents = IOUtils.toString(urls[1]);
			assertTrue(contents.equals(CSV_CONTENTS));
		
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	@Test
	public void test_delete() throws Exception {
		
		String jobId = "results_test_jobid_" + UUID.randomUUID();
		
		try {
			client.execStoreCsvResults(jobId, CSV_CONTENTS);
			client.execDeleteStorage(jobId);
			URL[] urls = client.execGetResults(jobId);

			String sample = IOUtils.toString(urls[0]);
			assertTrue(sample == null);
			
		} catch (FileNotFoundException e) {
			// success			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	private void cleanup(ResultsClient client, String jobId) throws Exception {
		client.execDeleteStorage(jobId);
	}

}
