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

import java.io.IOException;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.TableResultsStorage;
import com.ge.research.semtk.resultSet.Table;

public class TableResultsStorageTest {

	private static String FILE_LOC;
	
	@BeforeClass
	public static void setup() throws IOException {
		FILE_LOC = (new java.io.File( "." ).getCanonicalPath());  // write test files to current directory (they will be deleted)
	}
	
	
	@Test
	public void test() {

		TableResultsStorage rs = null;
		URL fullJsonUrl = null;
		
		try {
			rs = new TableResultsStorage(FILE_LOC);
			
			// write a result set
			String jobId = "12451345"; 
			String[] colNames = {"colA","colB","colC"};
			String[] colTypes = {"String","String","String"};
			rs.storeTableResultsJsonInitialize(jobId, colNames, colTypes);			
			String row1 = "[\"apple\",\"banana\",\"coconut\"]";
			String row2 = "[\"avocado\",\"bread\",\"canteloupe\"]";
			String row3 = "[\"apricot\",\"bran\",\"chives\"]";
			rs.storeTableResultsJsonAddIncremental(jobId, row1 + "," + row2 + "," + row3);
			String row4 = "[\"asparagus\",\"broccoli\",\"celery\"]";
			String row5 = "[\"arugula\",\"baklava\",\"capers\"]";
			rs.storeTableResultsJsonAddIncremental(jobId, row4 + "," + row5);			
			fullJsonUrl = rs.storeTableResultsJsonFinalize(jobId, 5);
			
			// now do various checks
			
			// check that the file was stored properly		
			String s;
			JSONObject jsonObj;
			Table table;
			
			// check retrieving full result as JSON
			s = new String(rs.getJsonTable(fullJsonUrl)); // convert from byte array
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
			s = new String(rs.getCsvTable(fullJsonUrl)); 	// convert from byte array
			assertEquals(s, table.toCSVString());   // compare against json result above
			
			// check retrieving truncated result as JSON 
			s = new String(rs.getJsonTable(fullJsonUrl, 2)); // convert from byte array
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
			s = new String(rs.getCsvTable(fullJsonUrl, 2)); // convert from byte array
			assertEquals(s, table.toCSVString());  			// compare against json result above
			
		} catch(Exception e){
			e.printStackTrace();
			fail();			
		} finally {
			cleanup(rs, fullJsonUrl);
		}
	}
	
	
	private void cleanup(TableResultsStorage rs, URL url) {
		try {
			if (rs != null && url != null) {
				rs.deleteStoredFile(url);
			}
		} catch (Exception e) {
		}
	}
}
