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


package com.ge.research.semtk.edc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ge.research.semtk.edc.resultsStorage.TableResultsSerializer;
import com.ge.research.semtk.edc.resultsStorage.TableResultsStorage;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.resultSet.Table;

public class TableResultsStorageTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	@Test
	public void test() {

		TableResultsStorage rs = null;
		URL fullJsonUrl = null;
	
		try {
			rs = new TableResultsStorage(tempFolder.getRoot().getPath());
			
			// write a result set
			String jobId = "12451345"; 
			String[] colNames = {"colA","colB","colC"};
			String[] colTypes = {"String","String","String"};
			ArrayList<ArrayList<String>> fakeRows = new ArrayList<>();
			ArrayList<String> fakeRow0 = new ArrayList<>();
			fakeRow0.add("0");
			fakeRow0.add("1");
			fakeRow0.add("3");
			
			ArrayList<String> fakeRow1 = new ArrayList<>();
			fakeRow1.add("0");
			fakeRow1.add("1");
			fakeRow1.add("3");

			ArrayList<String> fakeRow2 = new ArrayList<>();
			fakeRow2.add("0");
			fakeRow2.add("1");
			fakeRow2.add("3");

			ArrayList<String> fakeRow3 = new ArrayList<>();
			fakeRow3.add("0");
			fakeRow3.add("1");
			fakeRow3.add("3");

			ArrayList<String> fakeRow4 = new ArrayList<>();
			fakeRow4.add("0");
			fakeRow4.add("1");
			fakeRow4.add("3");

			
			fakeRows.add(fakeRow0);
			fakeRows.add(fakeRow1);
			fakeRows.add(fakeRow2);
			fakeRows.add(fakeRow3);
			fakeRows.add(fakeRow4);
			
			Table tblForHeader = new Table(colNames, colTypes, fakeRows);
			JSONObject headerInfo = tblForHeader.getHeaderJson();
			
			rs.storeTableResultsJsonInitialize(jobId, headerInfo);	

			String row1 = "[\"apple\",\"banana\",\"coconut\"]";
			String row2 = "[\"avocado\",\"bread\",\"canteloupe\"]";
			String row3 = "[\"apricot\",\"bran\",\"chives\"]";
			rs.storeTableResultsJsonAddIncremental(jobId, row1 + "\n" + row2 + "\n" + row3);
			String row4 = "[\"asparagus\",\"broccoli\",\"celery\"]";
			String row5 = "[\"arugula\",\"baklava\",\"capers\"]";
			rs.storeTableResultsJsonAddIncremental(jobId, row4 + "\n" + row5);			
			fullJsonUrl = rs.storeTableResultsJsonFinalize(jobId);

			// now do various checks
			
			// check that the file was stored properly		
			String s;
			JSONObject jsonObj;
			Table table;
			
			// check retrieving full result as JSON
			TableResultsSerializer tss = rs.getJsonTable(fullJsonUrl);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(baos);
			tss.writeToStream(pw);

			s = baos.toString(); // convert from byte array
			jsonObj = (JSONObject) (new JSONParser().parse(s));
			table = Table.fromJson(jsonObj);			
			assertEquals(table.getNumColumns(),3);
			assertEquals(table.getNumRows(),5);
			assertEquals(table.getColumnNames()[0],"colA");
			assertEquals(table.getColumnNames()[1],"colB");
			assertEquals(table.getColumnNames()[2],"colC");
			assertEquals(table.getColumnTypes()[0],"String");
			assertEquals(table.getColumnTypes()[1],"String");
			assertEquals(table.getColumnTypes()[2],"String");
			assertEquals(table.getCell(0, 2),"coconut");
			assertEquals(table.getCell(4, 2),"capers");
			// check retrieving full result as CSV
			TableResultsSerializer tss1 = rs.getCsvTable(fullJsonUrl);
			ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
			PrintWriter pw1 = new PrintWriter(baos1);
			tss1.writeToStream(pw1);
			
			
			s = baos1.toString(); 
			
			CSVDataset testCSV = new CSVDataset(s, true);
			ArrayList<String> colNamesTest = testCSV.getColumnNamesinOrder();
			assertEquals(colNamesTest.get(0), "colA".toLowerCase());
			assertEquals(colNamesTest.get(1), "colB".toLowerCase());
			assertEquals(colNamesTest.get(2), "colC".toLowerCase());
			ArrayList<ArrayList<String>> rowsFromCsv = testCSV.getNextRecords(2);
			
			assertEquals(rowsFromCsv.get(0).get(0), "apple");
		
			// check retrieving truncated result as JSON
			TableResultsSerializer tss2 = rs.getJsonTable(fullJsonUrl, 2, 0);
			ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
			PrintWriter pw2 = new PrintWriter(baos2);
			tss2.writeToStream(pw2);
			
			
			s = baos2.toString();
			jsonObj = (JSONObject) (new JSONParser().parse(s));
			table = Table.fromJson(jsonObj);			
			assertEquals(table.getNumColumns(),3);
			assertEquals(table.getNumRows(),2);
			assertEquals(table.getColumnNames()[0],"colA");
			assertEquals(table.getColumnNames()[1],"colB");
			assertEquals(table.getColumnTypes()[0],"String");
			assertEquals(table.getColumnTypes()[1],"String");
			assertEquals(table.getCell(0, 2),"coconut");
			assertEquals(table.getCell(1, 2),"canteloupe");
			
			// check retrieving truncated result as CSV
			TableResultsSerializer tss3 = rs.getCsvTable(fullJsonUrl, 2, 0);
			ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
			PrintWriter pw3 = new PrintWriter(baos3);
			tss3.writeToStream(pw3);
			
			s = baos3.toString();

			CSVDataset testCSV2 = new CSVDataset(s, true);
			ArrayList<String> colNamesTest2 = testCSV2.getColumnNamesinOrder();
			assertEquals(colNamesTest2.get(0), "colA".toLowerCase());
			assertEquals(colNamesTest2.get(1), "colB".toLowerCase());
			assertEquals(colNamesTest2.get(2), "colC".toLowerCase());
			ArrayList<ArrayList<String>> rowsFromCsv2 = testCSV2.getNextRecords(2);
			
			assertEquals(rowsFromCsv2.get(0).get(0), "apple");
			
		} catch(Exception e){
			e.printStackTrace();
			fail();			
		}
	}
	
}
