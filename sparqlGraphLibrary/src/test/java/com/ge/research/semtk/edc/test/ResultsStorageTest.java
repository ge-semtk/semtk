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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.ResultsStorage;
import com.ge.research.semtk.resultSet.Table;

public class ResultsStorageTest {

	private static String BASE_URL;
	private static String FILE_LOC;
	private String CSV_CONTENTS = "one, two, three\n1,2,3\n10,20,30\n100,200,300\n";
	private String CSV_SAMPLE =   "one, two, three\n1,2,3\n10,20,30\n";
	private int SAMPLE_LINES = 2;
	
	@BeforeClass
	public static void setup() throws IOException {
		FILE_LOC = (new java.io.File( "." ).getCanonicalPath());  // write test files to current directory (they will be deleted)
		BASE_URL = "file://" + FILE_LOC;	
	}
	
	
	@Test
	public void test_basic_csv() {
		// store csv and read it back

		ResultsStorage rs = null;
		URL urls[] = null;
		
		try {
			rs = new ResultsStorage(new URL(BASE_URL), FILE_LOC);

			urls = rs.storeCsvFile(CSV_CONTENTS, SAMPLE_LINES);
			
			String sample = IOUtils.toString(urls[0]);
			assertTrue(sample.equals(CSV_SAMPLE)); 
		
			String contents = IOUtils.toString(urls[1]);
			assertTrue(contents.equals(CSV_CONTENTS));
			
		} catch(Exception e){
			e.printStackTrace();
			fail();
		} finally {
			cleanup(rs, urls);
		}
	}
	
	@Test
	public void test_single_file() {
		// store csv and read it back

		ResultsStorage rs = null;
		URL url = null;
		
		try {
			rs = new ResultsStorage(new URL(BASE_URL), FILE_LOC);

			url = rs.storeSingleFile(CSV_CONTENTS, "csv");
			
			String contents = IOUtils.toString(url);
			assertTrue(contents.equals(CSV_CONTENTS));
			assertTrue(url.toString().endsWith(".csv"));
			
		} catch(Exception e){
			e.printStackTrace();
			fail();
		} finally {
			URL[] urls = { url };
			cleanup(rs,  urls );
		}
	}
	
	@Test
	public void test_basic_table() {
		// store csv and read it back

		ResultsStorage rs = null;
		URL urls[] = null;
		
		try {
			rs = new ResultsStorage(new URL(BASE_URL), FILE_LOC);
			
			// create table
			String[] cols = {"colA","colB","colC"};
			String[] colTypes = {"String","String","String"};
			ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
			ArrayList<String> rowFruit = new ArrayList<String>();
			rowFruit.add("apple");
			rowFruit.add("banana");
			rowFruit.add("coconut");
			rows.add(rowFruit);
			ArrayList<String> rowNames = new ArrayList<String>();
			rowNames.add("adam");
			rowNames.add("barbara");
			rowNames.add("chester");
			rows.add(rowNames);
			Table table = new Table(cols, colTypes, rows);
			
			urls = rs.storeTable(table, 1);
			
			String sample = IOUtils.toString(urls[0]);
			assertTrue(sample.equals("{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":1}"));
		
			String contents = IOUtils.toString(urls[1]);
			String tableStr = table.toCSVString();
			assertTrue(contents.equals(tableStr));
			
		} catch(Exception e){
			e.printStackTrace();
			fail();			
		} finally {
			cleanup(rs, urls);
		}
	}
	
	@Test
	public void test_delete() {
		// store csv and then delete it

		ResultsStorage rs = null;
		URL urls[] = null;
		
		try {
			rs = new ResultsStorage(new URL(BASE_URL), FILE_LOC);

			urls = rs.storeCsvFile(CSV_CONTENTS, SAMPLE_LINES);
			
			rs.deleteStoredFile(urls[0]);
			
			// expect failure trying to access the deleted file
			IOUtils.toString(urls[0]);
			fail("No exception retrieving deleted URL");
			
		} catch(Exception e){
			assertTrue(e.toString().contains("No such file or directory"));			
		} finally {
			cleanup(rs, urls);
		}
	}
	
	
	private void cleanup(ResultsStorage rs, URL [] urls) {
		try {
			if (rs != null && urls != null) {
				rs.deleteStoredFile(urls[0]);
				rs.deleteStoredFile(urls[1]);
			}
		} catch (Exception e) {
			
		}
	}
}
