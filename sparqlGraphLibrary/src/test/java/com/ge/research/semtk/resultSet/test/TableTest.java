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


package com.ge.research.semtk.resultSet.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import com.ge.research.semtk.resultSet.Table;

public class TableTest {

	
	@Test
	public void testTableToCSV() throws Exception {
		
		String[] cols = {"colA","colB","colC"};
		String[] colTypes = {"String","String","String"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		rowFruit.add("banana");
		rowFruit.add("coconut,comma");
		rows.add(rowFruit);
		ArrayList<String> rowNames = new ArrayList<String>();
		rowNames.add("adam");
		rowNames.add("barbara");
		rowNames.add("chester");
		rows.add(rowNames);
		Table table = new Table(cols, colTypes, rows);
		
		String result = table.toCSVString();
		String expected = "colA,colB,colC\napple,banana,\"coconut,comma\"\nadam,barbara,chester\n";	
		assertEquals(expected, result);
	}
	
	@Test
	public void testSortStr() throws Exception {
		
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
		
		// sort colA
		table.sortByColumnStr("colA");
		String result = table.toCSVString();
		String expected = "colA,colB,colC\nadam,barbara,chester\napple,banana,coconut\n";	
		assertEquals(expected, result);
		
		// sort colB
		table.sortByColumnStr("colB");
		result = table.toCSVString();
		expected = "colA,colB,colC\napple,banana,coconut\nadam,barbara,chester\n";	
		assertEquals(expected, result);
	}
	
	@Test
	public void testSortInt() throws Exception {
		
		String[] cols = {"colA","colB","colC"};
		String[] colTypes = {"String","String","Integer"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		rowFruit.add("banana");
		rowFruit.add("30");
		rows.add(rowFruit);
		ArrayList<String> rowNames = new ArrayList<String>();
		rowNames.add("adam");
		rowNames.add("barbara");
		rowNames.add("5");
		rows.add(rowNames);
		Table table = new Table(cols, colTypes, rows);
		
		// no sort
		String result = table.toCSVString();
		String expected = "colA,colB,colC\napple,banana,30\nadam,barbara,5\n";	
		assertEquals(expected, result);
		
		// sort colB
		table.sortByColumnInt("colC");
		result = table.toCSVString();
		expected = "colA,colB,colC\nadam,barbara,5\napple,banana,30\n";	
		assertEquals(expected, result);
	}
	
	
	@Test
	public void testTableToCSV_WithInternalQuotes() throws Exception {
		
		String[] cols = {"colA","colB","colC","colD"};
		String[] colTypes = {"String","String","String","String"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		rowFruit.add("banana");
		rowFruit.add("coconut,comma");
		rowFruit.add("\"dingleberry, doo\"");
		rows.add(rowFruit);
		ArrayList<String> rowNames = new ArrayList<String>();
		rowNames.add("adam");
		rowNames.add("barbara");
		rowNames.add("chester");
		rowNames.add("daniel (\"dan\")");
		rows.add(rowNames);
		Table table = new Table(cols, colTypes, rows);
		
		String result = table.toCSVString();
		// confirmed correct expected result by printing a file and opening it in Excel
		String expected = "colA,colB,colC,colD\napple,banana,\"coconut,comma\",\"\"\"dingleberry, doo\"\"\"\nadam,barbara,chester,\"daniel (\"\"dan\"\")\"\n";
		assertEquals(result,expected);
	}
	
	@Test 
	public void testReplaceColumnNames() throws Exception {
		String[] cols = {"colA","colB","colC"};
		String[] colTypes = {"String","String","String"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		
		Table table = new Table(cols, colTypes, rows);
		String[] colRename_tooLarge = {"1", "2", "3", "4"};
		String[] colRename_tooSmall = {"1"};
		String[] colRename_justRight = {"1", "2", "3"};
		
		try{
			table.replaceColumnNames(colRename_tooLarge);
			throw new Exception("table accepted new columns names when too few values were provided ?!?!");
		}
		catch(Exception e){
			// expected. continue
		}
		try{
			table.replaceColumnNames(colRename_tooSmall);
			throw new Exception("table accepted new columns names when too few values were provided ?!?!");
		}
		catch(Exception e){
			// expected. continue
		}
		
		// no try here. we expect this one works:
		table.replaceColumnNames(colRename_justRight);
		
		
	}
	
	@Test
	public void testTableWithCommas() throws Exception {
		
		String[] cols = {"colA","colB","colC"};
		String[] colTypes = {"String","String","String"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		rowFruit.add("banana");
		rowFruit.add("coconut");
		rows.add(rowFruit);
		ArrayList<String> rowNames = new ArrayList<String>();
		rowNames.add("adam,adamson");
		rowNames.add("barbara,barbrason");
		rowNames.add("chester,chesterton");
		rows.add(rowNames);
		Table table = new Table(cols, colTypes, rows);
		
		assertEquals(table.toCSVString(),"colA,colB,colC\napple,banana,coconut\n\"adam,adamson\",\"barbara,barbrason\",\"chester,chesterton\"\n");
		assertEquals(table.getRowAsCSVString(0),"apple,banana,coconut");
		assertEquals(table.getRowAsCSVString(1),"\"adam,adamson\",\"barbara,barbrason\",\"chester,chesterton\"");
	}

	@Test
	public void testTable_addRowToEmptyTable() throws Exception{
		String[] cols = {"colA"};
		String[] colTypes = {"String"};
		Table table = new Table(cols, colTypes); // tests the no-row constructor
		
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		table.addRow(rowFruit);
		
		assertEquals(table.getNumRows(),1);
		assertEquals(table.getCell(0, 0),"apple");
	}
	
	@Test
	public void testTableToCSV_1Row1Col() throws Exception {
		
		String[] cols = {"colA"};
		String[] colTypes = {"String"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		rows.add(rowFruit);
		Table table = new Table(cols, colTypes, rows);
		
		assertEquals(table.toCSVString(),"colA\napple\n");
		assertEquals(table.getRowAsCSVString(0),"apple");
	}
	
	@Test
	public void testTableToCSV_0Cols0Rows() throws Exception {
		String[] cols = {};
		String[] colTypes = {};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		Table table = new Table(cols, colTypes, rows);
		
		assertEquals(table.toCSVString(),"");
	}

	@Test
	public void testTableToJson() throws Exception {
		
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
		assertEquals(table.toJson().get(Table.JSON_KEY_COL_COUNT), 3);
		
		// test getColumn()
		String[] columnA = {"apple","adam"};
		assertEquals(table.getColumn("colA")[0], columnA[0]);
		assertEquals(table.getColumn("colA")[1], columnA[1]);
	}	
	
	@Test
	public void testTableToJsonTruncate() throws Exception {
		
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
		
		assertEquals(table.toJson().get(Table.JSON_KEY_COL_COUNT), 3);
		assertEquals(table.toJson().get(Table.JSON_KEY_ROW_COUNT), 2);
		
		table.truncate(10);
		assertEquals(table.toJson().get(Table.JSON_KEY_COL_COUNT), 3);
		assertEquals(table.toJson().get(Table.JSON_KEY_ROW_COUNT), 2);
		
		table.truncate(1);
		assertEquals(table.toJson().get(Table.JSON_KEY_COL_COUNT), 3);
		assertEquals(table.toJson().get(Table.JSON_KEY_ROW_COUNT), 1);
	}	
	
	

	@Test
	public void testTableToJson_1Row1Col() throws Exception {
		
		String[] cols = {"colA"};
		String[] colTypes = {"String"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		rows.add(rowFruit);
		Table table = new Table(cols, colTypes, rows);
		assertEquals(table.toJson().get(Table.JSON_KEY_COL_COUNT), 1);
		assertEquals(table.toJson().get(Table.JSON_KEY_ROW_COUNT), 1);
	}		
	
	@Test
	public void testTableFromJson() throws Exception{
		String jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
		JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
		Table table = Table.fromJson(jsonObj);
		
		assertEquals(table.getColumnNames().length,3);
		assertEquals(table.getColumnNames()[0],"colA");
		assertEquals(table.getColumnNames()[1],"colB");
		assertEquals(table.getColumnNames()[2],"colC");
		assertEquals(table.getColumnTypes()[0],"String");
		assertEquals(table.getRows().get(0).get(0),"apple");
		assertEquals(table.getRows().get(1).get(2),"chester");
	}
	
	@Test
	public void testTableMerge() throws Exception {
		String jsonStr1 = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
		JSONObject jsonObj1 = (JSONObject) new JSONParser().parse(jsonStr1);
		Table table1 = Table.fromJson(jsonObj1);

		String jsonStr2 = "{\"col_names\":[\"colC\",\"colB\",\"colA\"],\"rows\":[[\"cheesewhiz\",\"bonbons\",\"apple pie\"],[\"cider\",\"bourbon\",\"apple juice\"],[\"Chisholm\",\"Bobberson\",\"Anderson\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":3}";
		JSONObject jsonObj2 = (JSONObject) new JSONParser().parse(jsonStr2);
		Table table2 = Table.fromJson(jsonObj2);
		
		ArrayList<Table> tables = new ArrayList<Table>();
		tables.add(table1);
		tables.add(table2);
		Table tableMerged = Table.merge(tables);
		
		assertEquals(tableMerged.getNumRows(),5);		
		assertEquals(tableMerged.getNumColumns(),3);
	
		String res = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"],[\"apple pie\",\"bonbons\",\"cheesewhiz\"],[\"apple juice\",\"bourbon\",\"cider\"],[\"Anderson\",\"Bobberson\",\"Chisholm\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":5}";
		assertEquals(tableMerged.toJson().toString(),res);
	}
	
	@Test
	public void testTableGetSubsetWhereMatches() throws Exception {
		String jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
		JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
		Table table = Table.fromJson(jsonObj);
		
		// test retrieving all columns
		Table tableSubset = table.getSubsetWhereMatches("colB", "banana");  // 2 arguments
		assertEquals(tableSubset.getNumColumns(),3);
		assertEquals(tableSubset.getNumRows(), 1);
		assertEquals(tableSubset.getRows().get(0).get(0), "apple");
		assertEquals(tableSubset.getRows().get(0).get(1), "banana");
		assertEquals(tableSubset.getRows().get(0).get(2), "coconut");
		
		// test retrieving column C only
		tableSubset = table.getSubsetWhereMatches("colB", "banana", new String[]{"colC"}); // 3 arguments
		assertEquals(tableSubset.getNumColumns(),1);
		assertEquals(tableSubset.getNumRows(), 1);
		assertEquals(tableSubset.getRows().get(0).get(0), "coconut");
	}
	
	@Test
	public void testTable_WrongNumColTypes() throws Exception {
		boolean thrown = false;
		try{
			String jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
			JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
			Table.fromJson(jsonObj);
		}catch(Exception e){
			thrown = true;
		}
		assertTrue(thrown);
	}
	
	@Test
	public void testTable_ColumnsAndIndexes() throws Exception {
		boolean thrown = false;
		try{
			String jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
			JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
			Table table = Table.fromJson(jsonObj);
			
			assertTrue(table.hasColumn("colA"));
			assertFalse(table.hasColumn("colZ"));
			assertEquals(table.getColumnIndex("colB"), 1);
			assertEquals(table.getColumnIndex("colX"), -1);
		}catch(Exception e){
			thrown = true;
		}
		assertTrue(thrown);
	}
	
	
	@Test
	public void testTable_getColumnUniqueValues() throws Exception {
		boolean thrown = false;
		try{
			String jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"],[\"adam\",\"barbara\",\"chester\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\"],\"col_count\":3,\"row_count\":4}";
			JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
			Table table = Table.fromJson(jsonObj);
			
			assertEquals(table.getColumn("colC").length,4);
			assertEquals(table.getColumnUniqueValues("colC").length,2);
		}catch(Exception e){
			thrown = true;
		}
		assertTrue(thrown);
	}
	
	
	@Test
	public void testTableToCSV_NullRow() throws Exception {
		
		boolean thrown = false;
		try{
			String[] cols = {"colA"};
			String[] colTypes = {"String"};
			ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
			rows.add(null);
			new Table(cols, colTypes, rows);
		}catch(Exception e){
			thrown = true;
			assertTrue(e.getMessage().contains("Cannot create a Table: row is null"));
		}
		assertTrue(thrown);
	}
	
	@Test
	public void testTable_WrongNumRowEntries() throws Exception {
		boolean thrown = false;
		try{
			String jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\"],[\"adam\",\"barbara\"]],\"col_type\":[\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
			JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
			Table.fromJson(jsonObj);
		}catch(Exception e){
			thrown = true;
		}
		assertTrue(thrown);
	}
	
	@Test
	public void testTableMerge_MismatchedColumns() throws Exception {
		
		boolean thrown = false;
		try{
			String jsonStr1 = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
			JSONObject jsonObj1 = (JSONObject) new JSONParser().parse(jsonStr1);
			Table table1 = Table.fromJson(jsonObj1);
	
			String jsonStr2 = "{\"col_names\":[\"colD\",\"colB\",\"colA\"],\"rows\":[[\"cheesewhiz\",\"bonbons\",\"apple pie\"],[\"cider\",\"bourbon\",\"apple juice\"],[\"Chisholm\",\"Bobberson\",\"Anderson\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":3}";
			JSONObject jsonObj2 = (JSONObject) new JSONParser().parse(jsonStr2);
			Table table2 = Table.fromJson(jsonObj2);
			
			ArrayList<Table> tables = new ArrayList<Table>();		
			tables.add(table1);
			tables.add(table2);
			Table.merge(tables);  // should fail, because col names don't match across tables
		}catch(Exception e){
			thrown = true;
		}
		assertTrue(thrown);
		
	}
	
	@Test
	public void testTableSubsetBySubstring() throws Exception {
		String jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"],[\"\",\"tropicana\",\"chester\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":3}";
		JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
		Table table = Table.fromJson(jsonObj);
		
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("colA","pple");	
		map.put("colB","ana");
		
		Table tableSubset = table.getSubsetBySubstring(map);
		assertEquals(tableSubset.getNumColumns(),3);
		assertEquals(tableSubset.getNumRows(), 1);
		assertEquals(tableSubset.getRows().get(0).get(0), "apple");
		assertEquals(tableSubset.getRows().get(0).get(1), "banana");
		assertEquals(tableSubset.getRows().get(0).get(2), "coconut");
	}
	
	@Test
	public void testTableAllRowsMatch() throws Exception{
		String jsonStr;
		JSONObject jsonObj;
		Table table;
		
		jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"],[\"\",\"tropicana\",\"chester\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":3}";
		jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
		table = Table.fromJson(jsonObj);
		assertFalse(table.allRowsMatch());
		
		jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"apple\",\"banana\",\"coconut\"],[\"apple\",\"banana\",\"coconut\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":3}";
		jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
		table = Table.fromJson(jsonObj);
		assertTrue(table.allRowsMatch());
		
		jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":0}";
		jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
		table = Table.fromJson(jsonObj);
		assertTrue(table.allRowsMatch());
	}
	
	@Test
	public void testSlice() throws Exception{
		
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
		ArrayList<String> rowLastNames = new ArrayList<String>();
		rowLastNames.add("adamson");
		rowLastNames.add("barberson");
		rowLastNames.add("chesterton");
		rows.add(rowLastNames);
		ArrayList<String> rowVegetables = new ArrayList<String>();
		rowVegetables.add("asparagus");
		rowVegetables.add("broccoli");
		rowVegetables.add("cauliflower");
		rows.add(rowVegetables);
		Table table = new Table(cols, colTypes, rows);
		
		Table tableSlice;
		
		tableSlice = table.slice(0,2);
		assertEquals(tableSlice.getNumRows(),2);
		assertEquals(tableSlice.getRows().get(0),rowFruit);
		assertEquals(tableSlice.getRows().get(1),rowNames);
		
		tableSlice = table.slice(1,10);					// asking for more rows than exist in the table
		assertEquals(tableSlice.getNumRows(),3);	
		assertEquals(tableSlice.getRows().get(0),rowNames);
		assertEquals(tableSlice.getRows().get(1),rowLastNames);
		assertEquals(tableSlice.getRows().get(2),rowVegetables);
		
		// expect 1 row if offset is at the last row
		tableSlice = table.slice(3,12);		
		assertEquals(tableSlice.getNumRows(),1);
		
		// expect empty table if specify an offset that is beyond the number of rows
		tableSlice = table.slice(4,12);			
		assertEquals(tableSlice.getNumRows(),0);
		
		// expect empty table if specify an offset that is equal to the number of rows
		tableSlice = table.slice(10,12);			
		assertEquals(tableSlice.getNumRows(),0);
		

		
	}
	
	@Test
	public void testAppend() throws Exception{
		
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
		
		table.appendColumn("colD", "String");
		assertTrue("appendColumn() produced\n" + table.toCSVString(), table.toCSVString().equals("colA,colB,colC,colD\napple,banana,coconut,\nadam,barbara,chester,\n"));
		
		// join a column using other columns
		table.appendJoinedColumn("joined", "String", new String[] {"colA", "colD", "colB"}, "_" );
		assertTrue("appendJoinedColumn() produced\n" + table.toCSVString(), table.toCSVString().equals("colA,colB,colC,colD,joined\napple,banana,coconut,,apple__banana\nadam,barbara,chester,,adam__barbara\n"));
		table.removeColumn("joined");
		
		// join a column using other columns, and a constant
		table.appendJoinedColumn("joined_with_constant", "String", new String[] {"colA", "\"to\"", "colB"}, "_" );
		assertTrue("appendJoinedColumn() produced\n" + table.toCSVString(), table.toCSVString().equals("colA,colB,colC,colD,joined_with_constant\napple,banana,coconut,,apple_to_banana\nadam,barbara,chester,,adam_to_barbara\n"));
		table.removeColumn("joined_with_constant");
		
		// confirm error if give a column a name that already exists
		boolean thrown = false;
		try{
			table.appendJoinedColumn("colA", "String", new String[] {"colA", "colB"}, "_" );
		}catch(Exception e){
			thrown = true;
			assertTrue(e.getMessage().contains("already exists"));
		}
		assertTrue(thrown);
		
		// confirm error if give a nonexistent column that is also not in quotes
		thrown = false;
		try{
			table.appendJoinedColumn("joined_nonexistentcolumn", "String", new String[] {"colA", "colZ"}, "_" );
		}catch(Exception e){
			thrown = true;
			assertTrue(e.getMessage().contains("is not a column name or a quoted constant string"));
		}
		assertTrue(thrown);
		
		Table sub = table.getSubTable(new String[] {"colA", "colD"});
		assertTrue("getSubTable() produced\n" + sub.toCSVString(), sub.toCSVString().equals("colA,colD\napple,\nadam,\n"));
		assertTrue("getSubTable() produced\n" + sub.toCSVString(), sub.toCSVString(1).equals("colA,colD\napple,\n"));
	}
	
	@Test
	public void testUniquify() throws Exception{
		String jsonStr;
		JSONObject jsonObj;
		Table table;
		
		jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"],[\"\",\"tropicana\",\"chester\"],[\"apple\",\"banana\",\"non-match\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":4}";
		jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
		table = Table.fromJson(jsonObj);
		assertEquals("4 row table was not created properly", 4, table.getNumRows());
		
		table.uniquify(new String [] {"colA", "colB"} );
		assertEquals("uniquify did not remove last row", 3, table.getNumRows());

	}
}
