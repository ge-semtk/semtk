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


package com.ge.research.semtk.load.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import com.ge.research.semtk.load.DataCleaner;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.utility.Utility;

public class DataCleanerTest {

	@Test 
	public void test_WithJSONSpecs() throws IOException {

		String originalFilePathStr = "src/test/resources/datacleanertest-input.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		BufferedReader reader = null;
		
		try {	
			
			String cleaningSpecJsonStr = "{\"" + DataCleaner.JSON_KEY_LOWERCASE + "\":[\"child_names\",\"has_pool\"],\"" + DataCleaner.JSON_KEY_SPLIT + "\":{\"pet_names\":\"##\",\"child_names\":\"~\"},\"REMOVE_NULLS\":\"false\"}";			
			JSONObject cleaningSpecJson = (JSONObject) (new JSONParser()).parse(cleaningSpecJsonStr);
			
			Dataset dataset = new CSVDataset(originalFilePathStr, false);
			DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr, cleaningSpecJson);
			
			// do the cleaning
			int cleanedRows = cleaner.cleanData();
			
			// check results
			assertEquals(cleanedRows,13);
			reader = new BufferedReader(new FileReader(cleanedFilePathStr));
			String s = reader.readLine();
			assertEquals(s,"parent_name,parent_age,child_names,street_name,has_pool,pet_names,extra");
			s = reader.readLine();
			assertEquals(s,"Robert,40,robbie,Rockington St,yes,Ruff,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,robbie,Rockington St,yes,Rocky,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,ronnie,Rockington St,yes,Ruff,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,ronnie,Rockington St,yes,Rocky,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,rhonda,Rockington St,yes,Ruff,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,rhonda,Rockington St,yes,Rocky,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,billy,Barbington Ave,no,Bambam,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,billy,Barbington Ave,no,Buffy,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,billy,Barbington Ave,no,Bippy,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,babs,Barbington Ave,no,Bambam,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,babs,Barbington Ave,no,Buffy,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,babs,Barbington Ave,no,Bippy,");
			s = reader.readLine();
			assertEquals(s,"Michael,39,mikey,Marlington Dr,no,,");
			
			(new File(cleanedFilePathStr)).delete();
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if(reader != null){	reader.close(); }
		}
	}	
	
	
	@Test 
	public void test_RemoveNulls() throws IOException {

		String originalFilePathStr = "src/test/resources/datacleanertest-input-nulls.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		BufferedReader reader = null;
		
		try {	
			
			JSONObject cleaningSpecJson = Utility.getJSONObjectFromFilePath("src/test/resources/datacleanertest-spec-nulls.json");
			
			Dataset dataset = new CSVDataset(originalFilePathStr, false);
			DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr, cleaningSpecJson);
			
			// do the cleaning
			int cleanedRows = cleaner.cleanData();
			
			// check results
			assertEquals(cleanedRows,3);
			reader = new BufferedReader(new FileReader(cleanedFilePathStr));
			String s = reader.readLine();
			assertEquals(s,"parent_name,parent_age,has_pool,pet_name");
			s = reader.readLine();
			assertEquals(s,"robert,40,YES,");
			s = reader.readLine();
			assertEquals(s,"barbara,,NO,Bippy");
			s = reader.readLine();
			assertEquals(s, "michael,39,NO,Snulls");
			s = reader.readLine();
			
			(new File(cleanedFilePathStr)).delete();
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if(reader != null){	reader.close(); }
		}
	}	
	
	
	@Test 
	public void test_RemoveNA() throws IOException {

		String originalFilePathStr = "src/test/resources/datacleanertest-input-na.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		BufferedReader reader = null;
		
		try {	
			
			JSONObject cleaningSpecJson = Utility.getJSONObjectFromFilePath("src/test/resources/datacleanertest-spec-na.json");
			
			Dataset dataset = new CSVDataset(originalFilePathStr, false);
			DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr, cleaningSpecJson);
			
			// do the cleaning
			int cleanedRows = cleaner.cleanData();
			
			// check results
			assertEquals(cleanedRows,3);
			reader = new BufferedReader(new FileReader(cleanedFilePathStr));
			String s = reader.readLine();
			assertEquals(s,"parent_name,parent_age,has_pool,pet_name");
			s = reader.readLine();
			assertEquals(s,"robert,40,YES,Bobby");
			s = reader.readLine();
			assertEquals(s,"barbara,37,NO,");
			s = reader.readLine();
			assertEquals(s, "\"\",39,NO,Snulls");
			s = reader.readLine();
			
			(new File(cleanedFilePathStr)).delete();
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if(reader != null){	reader.close(); }
		}
	}		
	
	
	@Test 
	public void test_WithoutJSONSpecs() throws IOException {

		String originalFilePathStr = "src/test/resources/datacleanertest-input.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		BufferedReader reader = null;
		
		try {				
			Dataset dataset = new CSVDataset(originalFilePathStr, false);
			DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr);
			cleaner.addSplit("child_names","~");
			cleaner.addToLowerCase("child_names");
			cleaner.addToLowerCase("has_pool");
			cleaner.addSplit("pet_names","##");
			cleaner.addRemoveNulls(false);
			
			// do the cleaning
			int cleanedRows = cleaner.cleanData();
			
			// check results
			assertEquals(cleanedRows,13);
			reader = new BufferedReader(new FileReader(cleanedFilePathStr));
			String s = reader.readLine();
			assertEquals(s,"parent_name,parent_age,child_names,street_name,has_pool,pet_names,extra");
			s = reader.readLine();
			assertEquals(s,"Robert,40,robbie,Rockington St,yes,Ruff,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,robbie,Rockington St,yes,Rocky,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,ronnie,Rockington St,yes,Ruff,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,ronnie,Rockington St,yes,Rocky,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,rhonda,Rockington St,yes,Ruff,");
			s = reader.readLine();
			assertEquals(s,"Robert,40,rhonda,Rockington St,yes,Rocky,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,billy,Barbington Ave,no,Bambam,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,billy,Barbington Ave,no,Buffy,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,billy,Barbington Ave,no,Bippy,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,babs,Barbington Ave,no,Bambam,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,babs,Barbington Ave,no,Buffy,");	
			s = reader.readLine();
			assertEquals(s,"Barbara,35,babs,Barbington Ave,no,Bippy,");
			s = reader.readLine();
			assertEquals(s,"Michael,39,mikey,Marlington Dr,no,,");
			
			(new File(cleanedFilePathStr)).delete();
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if(reader != null){	reader.close(); }
		}
	}		
	

	@Test 
	public void test_NoLowerCase() throws IOException {

		String originalFilePathStr = "src/test/resources/datacleanertest-input.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		BufferedReader reader = null;
		
		try {	
			
			String cleaningSpecJsonStr = "{\"" + DataCleaner.JSON_KEY_SPLIT + "\":{\"pet_names\":\"##\",\"child_names\":\"~\"}}";			
			JSONObject cleaningSpecJson = (JSONObject) (new JSONParser()).parse(cleaningSpecJsonStr);
			
			Dataset dataset = new CSVDataset(originalFilePathStr, false);
			DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr, cleaningSpecJson);
			
			// do the cleaning
			int cleanedRows = cleaner.cleanData();
			
			// check results
			assertEquals(cleanedRows,13);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if(reader != null){	reader.close(); }
		}
		
		(new File(cleanedFilePathStr)).delete();
		
	}	
	
	
	@Test 
	public void test_disallowMultipleSplitsOnSameColumn() throws IOException {

		String originalFilePathStr = "src/test/resources/datacleanertest-input.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		
		boolean thrown = false;
		try {				
			Dataset dataset = new CSVDataset(originalFilePathStr, false);
			DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr);
			cleaner.addSplit("child_names","~");
			cleaner.addSplit("child_names","*");
			
			(new File(cleanedFilePathStr)).delete();
						
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("Already splitting"));
			thrown = true;
		} 
		if(!thrown){
			fail();
		}
		
	}	
	
	
	@Test 
	public void test_badColumnName() throws Exception {
		
		String originalFilePathStr = "src/test/resources/datacleanertest-input.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		Dataset dataset = new CSVDataset(originalFilePathStr, false);
		DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr);
		
		// try using bad column name for split 
		boolean thrown = false;
		try {				
			cleaner.addSplit("bad_column_name","\n");			
			(new File(cleanedFilePathStr)).delete();						
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("nonexistent column"));
			thrown = true;
		} 
		if(!thrown){
			fail();
		}
		
		// try using bad column name for toLowerCase 
		thrown = false;
		try {				
			cleaner.addToLowerCase("bad_column_name");			
			(new File(cleanedFilePathStr)).delete();						
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("nonexistent column"));
			thrown = true;
		} 
		if(!thrown){
			fail();
		}
	}	
	
	@Test 
	public void test_disallowUnsupportedSplitDelimiter() throws IOException {

		String originalFilePathStr = "src/test/resources/datacleanertest-input.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		
		boolean thrown = false;
		try {				
			Dataset dataset = new CSVDataset(originalFilePathStr, false);
			DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr);
			cleaner.addSplit("child_names","\n");
			cleaner.addSplit("child_names","*");
			
			(new File(cleanedFilePathStr)).delete();
						
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("delimiter"));
			thrown = true;
		} 
		if(!thrown){
			fail();
		}
	}	
	
	@Test 
	public void test_SOHDelimiter() throws IOException {

		String originalFilePathStr = "src/test/resources/datacleanertest-input-SOH.csv";
		String cleanedFilePathStr = "src/test/resources/datacleanertest-output.csv";
		BufferedReader reader = null;
		
		try {	
			
			JSONObject cleaningSpecJson = Utility.getJSONObjectFromFilePath("src/test/resources/datacleanertest-spec-SOH.json");
			
			Dataset dataset = new CSVDataset(originalFilePathStr, false);
			DataCleaner cleaner = new DataCleaner(dataset, cleanedFilePathStr, cleaningSpecJson);
			
			// do the cleaning
			int cleanedRows = cleaner.cleanData();
			
			// check results
			assertEquals(cleanedRows,13);
			reader = new BufferedReader(new FileReader(cleanedFilePathStr));
			String s = reader.readLine();
			assertEquals(s,"parent_name,parent_age,child_names,street_name,has_pool,pet_names,extra");
			s = reader.readLine();
			assertEquals(s,"Cobert,40,cobbie,Rockington St,yes,Ruff,");
			s = reader.readLine();
			assertEquals(s,"Cobert,40,cobbie,Rockington St,yes,Rocky,");
			
			(new File(cleanedFilePathStr)).delete();
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			if(reader != null){	reader.close(); }
		}
	}	

	
}
