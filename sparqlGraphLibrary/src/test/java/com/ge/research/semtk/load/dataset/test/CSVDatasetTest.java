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


package com.ge.research.semtk.load.dataset.test;


import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.Test;

import com.ge.research.semtk.load.dataset.CSVDataset;


public class CSVDatasetTest {
	
	
	@Test
	public void testCSVDatasetFromFileContent() throws Exception{
		String content = "cell,size in,lot,material,Extra\ncell1_import_0,1,lotA,iron something else,\ncell2_import_0,2,lotB,kryptonite,\ncell3_import_0,3,lotC,moon rocks,";
		CSVDataset csvDataset = new CSVDataset(content, true);
		ArrayList<ArrayList<String>> records = csvDataset.getNextRecords(10);
		assertEquals(records.size(),3);
		assertEquals(records.get(0).get(0),"cell1_import_0");		
	}
	
	
	@Test
	public void testCSVDatasetFromPath_NoHeaders() throws Exception{
		String path = "src/test/resources/CSVDatasetTest1.csv";
		CSVDataset csvDataset = new CSVDataset(path, false); 
		ArrayList<ArrayList<String>> records = csvDataset.getNextRecords(10);
		assertEquals(records.size(),3);
		assertEquals(records.get(0).get(0),"cell1_import_0");	
	}
	
	
	@Test
	public void testCSVDatasetFromPath() {

		try {
			
			String[] headers = {"HEADER3","HEADER1"}; // picked a subset of the existing headers, in reverse order than they appear in the file
			CSVDataset csvDataset = new CSVDataset("src/test/resources/test.csv", headers);
			ArrayList<ArrayList<String>> records;
			
			// confirm that we read the data correctly
			records = csvDataset.getNextRecords(5);
			assertEquals(records.size(), 5);
			
			ArrayList<String> g0 = new ArrayList<String>();
			g0.add("a3");
			g0.add("a1");
			
			assertEquals(records.get(0), g0);
			
			g0.clear();
			g0.add("b3");
			g0.add("b1");
			
			assertEquals(records.get(1), g0);
			
			g0.clear();
			g0.add("c3");
			g0.add("c1");
			
			assertEquals(records.get(2), g0);
			
			g0.clear();
			g0.add("d3");
			g0.add("d1");
			
			assertEquals(records.get(3), g0);
			
			g0.clear();
			g0.add("e3");
			g0.add("e1");
			
			assertEquals(records.get(4), g0);
			
			// confirm that we handle the end of the file correctly
			records = csvDataset.getNextRecords(5);  // ask for 5, should get 2 (file has only 7 non-header lines)
			assertEquals(records.size(), 2);
			ArrayList<String> t0 = new ArrayList<String>();
			t0.add("f3");
			t0.add("f1");
			
			ArrayList<String> t1 = new ArrayList();
			t1.add("g3");
			t1.add("g1");
			
			assertEquals(records.get(0), t0);
			assertEquals(records.get(1), t1);
			
			// confirm that we can retrieve the headers
			assertEquals(csvDataset.getColumnNamesinOrder().get(0), headers[0].toLowerCase());
			assertEquals(csvDataset.getColumnNamesinOrder().get(1), headers[1].toLowerCase());
			
			// reset and get more
			csvDataset.reset();
			records = csvDataset.getNextRecords(5);
			assertEquals(records.size(), 5);
			assertEquals(records.get(4), g0);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void testCSVDatasetFromString() throws Exception{
		
		// read from a path to a string
		String csvPath = "src/test/java/com/ge/research/semtk/load/dataset/test/test.csv";
		byte[] encoded = Files.readAllBytes(Paths.get(csvPath));
		String csvString = new String(encoded, StandardCharsets.UTF_8);
		
		String[] headers = {"HEADER3","HEADER1"}; // picked a subset of the existing headers, in reverse order than they appear in the file
		CSVDataset csvDataset = new CSVDataset("src/test/java/com/ge/research/semtk/load/dataset/test/test.csv", headers);
		ArrayList<ArrayList<String>> records;
		
		// confirm that we read the data correctly
		records = csvDataset.getNextRecords(5);
		assertEquals(records.size(), 5);
		
		ArrayList<String> g0 = new ArrayList<String>();
		g0.add("a3");
		g0.add("a1");
		assertEquals(records.get(0), g0);

		records = csvDataset.getNextRecords(5);  

		// confirm that we can retrieve the headers
		assertEquals(csvDataset.getColumnNamesinOrder().get(0), headers[0].toLowerCase());
		assertEquals(csvDataset.getColumnNamesinOrder().get(1), headers[1].toLowerCase());
		
		// reset and get more
		csvDataset.reset();
		records = csvDataset.getNextRecords(5);
		assertEquals(records.size(), 5);
		assertEquals(records.get(0), g0);		
		
	}
	
}