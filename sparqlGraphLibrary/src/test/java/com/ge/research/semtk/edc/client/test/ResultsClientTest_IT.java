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

import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.Utility;

public class ResultsClientTest_IT {
	
	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	
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

		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"col1", "col2"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("one");
		row.add("two");
		ArrayList<String> row2 = new ArrayList<String>();
		row2.add("three");
		row2.add("four");
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			table.addRow(row2);
			
			client.execStoreTableResults(jobId, table);

			String expectedJsonString = "{\"col_names\":[\"col1\",\"col2\"],\"rows\":[[\"one\",\"two\"],[\"three\",\"four\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\"],\"col_count\":2,\"row_count\":2}";		
			String expectedCSVString = "col1,col2\n\"one\",\"two\"\n\"three\",\"four\"\n";

			URL[] urls = client.execGetResults(jobId);
			// check the URLs from getResults
			assertTrue(urls[0].toString().endsWith("/results/getTableResultsJsonForWebClient?jobId=" + jobId + "&maxRows=200")); 
			assertTrue(urls[1].toString().endsWith("/results/getTableResultsCsvForWebClient?jobId=" + jobId)); 			
			// check the contents from getResults
			
			TableResultSet tblresset = client.execTableResultsJson(jobId, null);
			Table tbl = tblresset.getTable();
			String resultJSONString = tbl.toJson().toJSONString();
			
			
			assertEquals(resultJSONString, expectedJsonString); 		// check the JSON results
			
			CSVDataset data = client.execTableResultsCsv(jobId, null);
			CSVDataset compare = new CSVDataset(expectedCSVString, true);
			
			
			ArrayList<String> compareColumnNames = compare.getColumnNamesinOrder();
			ArrayList<String> resultColumnNames  = data.getColumnNamesinOrder();
					
			assertEquals(compareColumnNames.get(0), resultColumnNames.get(0));
			assertEquals(compareColumnNames.get(1), resultColumnNames.get(1));
			
			assertEquals(Utility.getURLContentsAsString(urls[1]), expectedCSVString);		// check the CSV result
			
			// check getting results as json
			TableResultSet res = client.execTableResultsJson(jobId, null);
			assertEquals(res.getTable().toJson().toString(), expectedJsonString);
			
			// check getting results as csv
			CSVDataset resCsvDataset = client.execTableResultsCsv(jobId, null);
			assertEquals(resCsvDataset.getColumnNamesinOrder().get(0), "col1");
			assertEquals(resCsvDataset.getColumnNamesinOrder().get(1), "col2");
			ArrayList<ArrayList<String>> resCsvRows = resCsvDataset.getNextRecords(5);
			assertEquals(resCsvRows.size(), 2);				// 2 rows
			assertEquals(resCsvRows.get(0).size(), 2);		// 2 columns
			assertEquals(resCsvRows.get(0).get(0), "one");
			assertEquals(resCsvRows.get(0).get(1), "two");
			assertEquals(resCsvRows.get(1).get(0), "three");
			assertEquals(resCsvRows.get(1).get(1), "four");
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	@Test
	public void testStoreTable_WithQuotesAndCommas() throws Exception {
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		
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
			
			// check the JSON results
			TableResultSet tblresset = client.execTableResultsJson(jobId, null);
			Table tbl = tblresset.getTable();
			String resultJSONString = tbl.toJson().toJSONString();
			
			String expectedJSONString = "{\"col_names\":[\"colA\",\"colB\",\"colC\",\"colD\"],\"rows\":[[\"apple,ant\",\"bench\",\"\\\"cabana\\\"\",\"Dan declared \\\"hi, dear\\\"\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\",\"String\",\"String\"],\"col_count\":4,\"row_count\":1}";  // validated json
			//assertEquals(expectedJSONString, resultJSONString);
			assertEquals(expectedJSONString.length(), resultJSONString.length());
			
			for(int i = 0; i < expectedJSONString.length(); i += 1){
				assertEquals(expectedJSONString.charAt(i), resultJSONString.charAt(i));
			}		
			
			// check the CSV results
			CSVDataset data = client.execTableResultsCsv(jobId, null);
			String expectedCSVString = "colA,colB,colC,colD\n\"apple,ant\",bench,\"\"\"cabana\"\"\",\"Dan declared \"\"hi, dear\"\"\"\n";  // validated by opening in Excel
			CSVDataset compare = new CSVDataset(expectedCSVString, true);			
			ArrayList<String> compareColumnNames = compare.getColumnNamesinOrder();
			ArrayList<String> resultColumnNames  = data.getColumnNamesinOrder();
			assertEquals(compareColumnNames.get(0), resultColumnNames.get(0));
			assertEquals(compareColumnNames.get(1), resultColumnNames.get(1));
			assertEquals(compareColumnNames.get(2), resultColumnNames.get(2));
			assertEquals(compareColumnNames.get(3), resultColumnNames.get(3));
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	@Test
	public void testStoreTable_WithBackslash() throws Exception {
		// try backslash and quotes
		//
		// PEC 9/28/17  I don't understand why 
		// 		this works in Java but not in the browser or python, which both choke on the backslash
		// 		this works in Java when the RestClient TableFormatter is fixing double quotes but not backslashes
		// 		we're allowed to put TableFormatter in ResultsClient.java
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"colA"};
		String [] types = {"String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("\"Tor\" likes blue\\purple jello");  	
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			String tableJsonStrOrig = table.toJson().toJSONString();
			client.execStoreTableResults(jobId, table);	
			String tableJsonStrPostStore = table.toJson().toJSONString();
			
			// check the JSON results
			TableResultSet tblresset = client.execTableResultsJson(jobId, null);
			Table tbl = tblresset.getTable();
			String tableJsonStrRetrieved = tbl.toJson().toJSONString();

			System.out.println("jobId: " + jobId);
			System.out.println("json orig:      " + tableJsonStrOrig);
			System.out.println("json post_send: " + tableJsonStrPostStore);
			System.out.println("json retrieved: " + tableJsonStrRetrieved);
			
			assert(tableJsonStrRetrieved.equals(tableJsonStrOrig));

			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	@Test
	public void delete_me() throws Exception {
		// --- private experiment ---
		System.out.println("Notice how JSON simple parser treats these the same");
		String s1 = "{\"str\": \"Tor likes blue\\purple jello	\n\u0001\"}";    // illegal
		String s2 = "{\"str\": \"Tor likes blue\\\\purple jello	\n\u0001\"}";
		JSONParser parser = new JSONParser();
		JSONObject j1 = (JSONObject) parser.parse(s1);
		JSONObject j2 = (JSONObject) parser.parse(s2);
		System.out.println(j1.toJSONString());
		System.out.println(j2.toJSONString());
	}
	/**
	 * Test a row with quotes but no commas (in the past this triggered different logic in the ResultsClient)
	 */
	@Test
	public void testStoreTable_WithQuotesNoCommas() throws Exception {
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"colA", "colB","colC","colD"};
		String [] types = {"String", "String", "String","String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("apple");  				
		row.add("bench");
		row.add("\"cabana\"");			// this element has internal quotes and no commas
		row.add("Dan declared \"hi\"");	// this element has internal quotes and no commas
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			client.execStoreTableResults(jobId, table);	
			
			// check the JSON results
			TableResultSet tblresset = client.execTableResultsJson(jobId, null);
			Table tbl = tblresset.getTable();
			String resultJSONString = tbl.toJson().toJSONString();
			String expectedJSONString = "{\"col_names\":[\"colA\",\"colB\",\"colC\",\"colD\"],\"rows\":[[\"apple\",\"bench\",\"\\\"cabana\\\"\",\"Dan declared \\\"hi\\\"\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\",\"String\",\"String\"],\"col_count\":4,\"row_count\":1}";  // validated json		
			System.err.println(expectedJSONString);
			System.err.println(resultJSONString);
			
			assertEquals(expectedJSONString.length(), resultJSONString.length());
			
			for(int i = 0; i < expectedJSONString.length(); i += 1){
				assertEquals(expectedJSONString.charAt(i), resultJSONString.charAt(i));
			}
			
			// check the CSV results
			CSVDataset data = client.execTableResultsCsv(jobId, null);

			String expectedCSVString = "colA,colB,colC,colD\napple,bench,\"\"\"cabana\"\"\",\"Dan declared \"\"hi\"\"\"\n";  // validated by opening in Excel
			CSVDataset compare = new CSVDataset(expectedCSVString, true);
			
			
			ArrayList<String> compareColumnNames = compare.getColumnNamesinOrder();
			ArrayList<String> resultColumnNames  = data.getColumnNamesinOrder();
					
			assertEquals(compareColumnNames.get(0), resultColumnNames.get(0));
			assertEquals(compareColumnNames.get(1), resultColumnNames.get(1));
			assertEquals(compareColumnNames.get(2), resultColumnNames.get(2));
			assertEquals(compareColumnNames.get(3), resultColumnNames.get(3));
			
			
		} finally {
			//cleanup(client, jobId);
		}
	}
	
	
	/**
	 * TODO: fix this comment.  SOH passes through 
	 * Test that SOH is stripped when writing results.
	 * If SOH is not stripped, the ResultsClient would succeed - but the browser would choke 
	 * (org.simple.JSONParser is less strict than Chrome/Firefox which uses the JSON standard)
	 */
	@Test
	public void testStoreTable_WithSOH() throws Exception {
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"colA", "colB"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("apple\u0001ant");
		row.add("bench");

		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);	
			client.execStoreTableResults(jobId, table);	
			
			// check the JSON results:   SOH comes back as an escape sequence
			TableResultSet tblresset = client.execTableResultsJson(jobId, null);
			String resultJSONString = tblresset.getTable().toJson().toJSONString();
			String expectedJSONString = "{\"col_names\":[\"colA\",\"colB\"],\"rows\":[[\"apple\\u0001ant\",\"bench\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\"],\"col_count\":2,\"row_count\":1}";  // validated json
			assertEquals(expectedJSONString, resultJSONString);
	
			// check the CSV results:   SOH is in the string
			CSVDataset data = client.execTableResultsCsv(jobId, null);
			String expectedCSVString = "colA,colB\napple\u0001ant,bench\n";  
			CSVDataset compare = new CSVDataset(expectedCSVString, true);
			ArrayList<String> compareColumnNames = compare.getColumnNamesinOrder();
			ArrayList<String> resultColumnNames  = data.getColumnNamesinOrder();					
			assertEquals(compareColumnNames.get(0), resultColumnNames.get(0));
			assertEquals(compareColumnNames.get(1), resultColumnNames.get(1));
						
		} finally {
			cleanup(client, jobId);
		}
	}
	
	/**
	 * Test a storing a medium-size dataset
	 */
	@Test
	public void testStoreTable_Medium() throws Exception {

		String jobId = "test_jobid_" + UUID.randomUUID();
		
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
			
			// test that we got 200 (truncated by getResults()) rows of JSON
			String resultJsonString = Utility.getURLContentsAsString(urls[0]);
			JSONObject resultJsonObject = (JSONObject) ((new JSONParser()).parse(resultJsonString));
			Table tbl = Table.fromJson(resultJsonObject);
			assertEquals(tbl.getNumRows(), 200);	
			
			// test that we got full 9000 rows of CSV 
			String resultCsvString = Utility.getURLContentsAsString(urls[1]);
			String[] resultCsvLines = resultCsvString.split("\n");
			assertEquals(resultCsvLines.length, 9001);
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	/**
	 * Test a storing a large dataset
	 * Adjust NUM_COLS and NUM_ROWS to make this bigger when performance testing.  
	 * (Committing them on the smaller size)
	 */
	@Test
	public void testStoreTable_Huge() throws Exception {

		String jobId = "test_jobid_" + UUID.randomUUID();
		
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
				row.add(UUID.randomUUID().toString().substring(0,10));	// non-repetitive to realistically test compression
			}
			Table table = new Table(cols, colTypes, null);
			for(int i = 0; i < NUM_ROWS; i++){
				table.addRow((ArrayList<String>) row.clone());  // need to clone, otherwise formatter recursively formats the same row
			}
			
			long startTime, endTime;
			double elapsed;
			
			// --- store results ----
			startTime = System.nanoTime();		
			client.execStoreTableResults(jobId, table);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execStoreTableResults()=%.2f sec (%s columns, %s rows)", elapsed, NUM_COLS, NUM_ROWS));
			
			// --- test retrieving json results ---
			startTime = System.nanoTime();
			TableResultSet res = client.execTableResultsJson(jobId, null);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execTableResultsJson()=%.2f sec (%s columns, %s rows)", elapsed, NUM_COLS, NUM_ROWS));
			assertEquals(res.getTable().getNumRows(), NUM_ROWS);
			assertEquals(res.getTable().getNumColumns(), NUM_COLS);
			assertEquals(res.getTable().getCell(0,0).length(), 10);
			
			// --- test retrieving csv results ---
			startTime = System.nanoTime();
			CSVDataset resultCsv = client.execTableResultsCsv(jobId, null);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execTableResultsCsv()=%.2f sec (%s columns, %s rows)", elapsed, NUM_COLS, NUM_ROWS));
		} catch (Exception e){
			e.printStackTrace();
			fail();
		} finally {
			cleanup(client, jobId);
		}
	}
		
	@Test
	public void test_delete() throws Exception {
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		String [] cols = {"col1", "col2"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("one");
		row.add("two");
		
		boolean retrievedResultsAfterStoring = false;
		try {			
			Table table = new Table(cols, types, null);
			table.addRow(row);
			table.addRow(row);
			
			client.execStoreTableResults(jobId, table);
			client.execGetResults(jobId);				// this should succeed
			retrievedResultsAfterStoring = true;
			client.execDeleteStorage(jobId);
			client.execGetResults(jobId); 	// this should throw an exception, because the results are deleted
			fail();  // expect it to not get here
		} catch (Exception e) {
			// success			
		} 
		
		// confirm that it could retrieve results after storing
		if(!retrievedResultsAfterStoring){
			fail();	
		}
	}	
	
	private void cleanup(ResultsClient client, String jobId) throws Exception {
		client.execDeleteStorage(jobId);
	}

}
