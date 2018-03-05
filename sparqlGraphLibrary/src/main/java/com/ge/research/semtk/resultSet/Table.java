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


package com.ge.research.semtk.resultSet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.Utility;

/**
 * A table of data
 */
public class Table {
	
	// json keys
	public final static String JSON_KEY_COL_NAMES = "col_names";
	public final static String JSON_KEY_COL_TYPES = "col_type";
	public final static String JSON_KEY_ROWS = "rows";
	public final static String JSON_KEY_ROW_COUNT = "row_count";
	public final static String JSON_KEY_COL_COUNT = "col_count";
	public final static String JSON_TYPE = "type";

	private String[] columnNames;
	private String[] columnTypes;
	private ArrayList<ArrayList<String>> rows;   
	private HashMap<String, Integer> columnPositionInfo = new HashMap<String, Integer>();

	/**
	 * Create an empty table with the given column names and column types.
	 */
	public Table(String[] cols, String[] colTypes) throws Exception{
		this(cols, colTypes, new ArrayList<ArrayList<String>>());
	}
	
	/**
	 * Create a table with the given column names, column types, and rows.
	 */
	public Table(String[] cols, String[] colTypes, ArrayList<ArrayList<String>> rows) throws Exception{
		
		// validate
		if(cols == null){
			throw new Exception("Cannot create a Table: no columns provided");
		}
		if(colTypes == null){
			throw new Exception("Cannot create a Table: no column types provided");
		}
		if(cols.length != colTypes.length){
			throw new Exception("Cannot create a Table: must provide the same number of columns and column types");
		}
		// allow rows to be null
		
		if(rows != null && rows.size() > 0){
			if(rows.get(0) == null){
				throw new Exception("Cannot create a Table: row is null");
			}
			if(rows.get(0).size() != cols.length){
				throw new Exception("Cannot create a Table: wrong number of entries in rows");
			}
			this.rows = rows;
		}else{
			this.rows = new ArrayList<ArrayList<String>>();
		}
		this.columnNames = cols;
		this.columnTypes = colTypes;
		
		int colNum = 0;
		// add all of the columns to the hash so we can make lookups faster.
		for(String c : cols){
			columnPositionInfo.put(c, colNum);
			colNum++;
		}
		
	}
	
	/**
	 * Replace the existing column names with a new set.
	 */
	public void replaceColumnNames(String [] newColumnNames) throws Exception{
		if(this.columnNames.length != newColumnNames.length){
			throw new Exception("replaceColumnNames: the incoming column name count (" + newColumnNames.length + ") does not match the target column names count (" + this.columnNames.length + ")");
		}	
		this.columnNames = newColumnNames;
	}
	
	/**
	 * Get the number of rows in the table.
	 */
	public int getNumRows(){
		return rows.size();
	}
	
	/**
	 * Get the number of columns in the table.
	 */
	public int getNumColumns(){
		return columnNames.length;
	}
	
	/**
	 * Get the column names.
	 */
	public String[] getColumnNames(){
		return columnNames;
	}
	
	/**
	 * Get slice of table from row offset to offset + limit - 1
	 * @param offset the offset index
	 * @param size - if 0, then go til the end
	 * @return the subset of Table rows
	 * @throws Exception
	 */
	public Table slice(int offset, int size) throws Exception {
		
		// if offset is bigger than the number of rows, return an empty table
		// (chose an empty table instead of an Exception because this allows calling functions to more easily know when they've reached the end of the data)
		if(offset >= this.getNumRows()){
			return new Table(this.columnNames, this.columnTypes );	// return an empty table 
		}

		// make sure upper limit is in bounds
		int upper;
		if (size == 0 || offset + size > this.getNumRows()) {
			upper = this.getNumRows();
		} else {
			upper = offset + size;
		}
		
		Table ret = new Table(this.columnNames, this.columnTypes, new ArrayList<ArrayList<String>> ( this.rows.subList(offset, upper)) );
		return ret;
	}
	
	/**
	 * Returns true if the column exists in this table, else false
	 */
	public boolean hasColumn(String colName) {
		if(getColumnIndex(colName) == -1){
			return false;
		}
		return true;
	}	
	
	public String[] getColumnTypes(){
		return columnTypes;
	}
	
	public String getColumnType(String columnName){
		return columnTypes[getColumnIndex(columnName)];
	}
	
	/**
	 * Return the values for a particular column
	 */
	public String[] getColumn(String columnName){
		return getColumn(getColumnIndex(columnName));
	}
	
	/**
	 * Return the values for a particular column
	 */
	public String[] getColumn(int index){
		ArrayList<String> column = new ArrayList<String>();
		for(ArrayList<String> row : rows){
			column.add(row.get(index));
		}
		return column.toArray(new String[column.size()]);
	}
	
	/**
	 * Return the values for a particular column, removing duplicates
	 */
	public String[] getColumnUniqueValues(String columnName){
		return getColumnUniqueValues(getColumnIndex(columnName));
	}
	
	/**
	 * Return the values for a particular column, removing duplicates
	 */
	public String[] getColumnUniqueValues(int index){
		ArrayList<String> column = new ArrayList<String>();
		for(ArrayList<String> row : rows){
			if(!column.contains(row.get(index))){  // enforce uniqueness
				column.add(row.get(index));
			}
		}
		return column.toArray(new String[column.size()]);
	}
	
	public void addRow(ArrayList<String> newRow) throws Exception{
		if(newRow.size() != this.columnNames.length){
			// panic
			throw new Exception("Incoming row has " + newRow.size() + " columns but " + this.columnNames.length + " were expected.");
		}
		this.rows.add(newRow);
	}
	
	public void addRow(String [] newRow)  throws Exception {
		this.addRow(new ArrayList<String>(Arrays.asList(newRow)));
	}
	
	public void addRow(Object [] newRow) throws Exception {
		ArrayList<String> row = new ArrayList<String>();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		for (int i=0; i < newRow.length; i++) {
			
			if (newRow[i] instanceof Date) {
				row.add(formatter.format(newRow[i]));
			} else {
				row.add(newRow[i].toString());
			}
		}
		this.addRow(row);
	}
	
	/**
	 * Get the index for a given column name, or -1 if does not exist
	 */
	public int getColumnIndex(String colName){
		int retval = -1;
		if(this.columnPositionInfo.get(colName) != null){
			retval = this.columnPositionInfo.get(colName);
		}		
		return retval;
	}
	
	/**
	 * Get the table rows
	 */
	public ArrayList<ArrayList<String>> getRows(){
		return this.rows;
	}
	
	/**
	 * Get a single table row by index
	 */
	public ArrayList<String> getRow(int rowNum){
		return this.rows.get(rowNum);
	}
	
	/**
	 * Get a single table row by index, and convert it to a CSV string.
	 */
	public String getRowAsCSVString(int rowNum) throws IOException{		
		ArrayList<String> row = this.getRow(rowNum);
		return Utility.getCSVString(row);
	}
	
	public String getHeaderAsCSVString() throws IOException {
		ArrayList<String> headers = new ArrayList<String>( Arrays.asList( this.getColumnNames() ) ); // gets col names in order
		return (Utility.getCSVString(headers));
	}
	
	public String getCell(int row, int col) {
		return this.rows.get(row).get(col);
	}
	
	public String getCellAsString(int row, int col) {
		return getCell(row, col);
	}
	
	public int getCellAsInt(int row, int col) {
		return Integer.parseInt(getCell(row, col));
	}
	
	public long getCellAsLong(int row, int col) {
		return Long.parseLong(getCell(row, col));
	}
	
	public float getCellAsFloat(int row, int col) {
		return Float.parseFloat(getCell(row, col));
	}
	
	/**
	 * Truncate the table to <length> rows
	 * @param length
	 */
	public void truncate (int length) {
		int size = this.rows.size();
		if (size > length) {
			this.rows.subList(length, size).clear();
		}
	}
	
	public void append(Table other) throws Exception {
		String [] myCols = Arrays.copyOf(this.getColumnNames(), Math.toIntExact(this.getNumColumns()));
		String [] otherCols = Arrays.copyOf(other.getColumnNames(), Math.toIntExact(other.getNumColumns()));
		Arrays.sort(myCols);
		Arrays.sort(otherCols);
		if (! Arrays.equals(myCols, otherCols)) {
			throw new Exception("Can not append tables with different columns");
		}
		
		// build   map(myCol) = otherCol
		ArrayList<Integer> map = new ArrayList<Integer>();
		for (int i=0; i < this.getNumColumns(); i++) {
			map.add(i, other.getColumnIndex(this.getColumnNames()[i]));
		}
		
		// loop through other rows
		for (int i=0; i < other.getNumRows(); i++) {
			ArrayList<String> rawRow = other.getRow(i);
			ArrayList<String> newRow = new ArrayList<String>();
			
			// swap column order as defined by map
			for (int j=0; j < other.getNumColumns(); j++) {
				newRow.add(rawRow.get(map.get(j)));
			}
			
			this.addRow(newRow);
		}
		
	}
		
	
	/**
	 * Get a table instance from a JSON object
	 * Json object looks like this: {"col_names":["colA","colB","colC"],"col_type":["String","String","String"],"rows":[["apple","banana","coconut"],["adam","barbara","chester"]],"col_count":3\"row_count":2}
	 * @return
	 * @throws Exception 
	 */
	public static Table fromJson(JSONObject jsonObj) throws Exception{
		
		// gather columns
		JSONArray colNamesJson = (JSONArray) jsonObj.get(JSON_KEY_COL_NAMES);
		String[] cols = new String[colNamesJson.size()];
		for(int i = 0; i < colNamesJson.size(); i++){
			cols[i] = colNamesJson.get(i).toString();
		}
		
		// gather column types
		JSONArray colTypesJson = (JSONArray) jsonObj.get(JSON_KEY_COL_TYPES);
		if(colTypesJson == null){
			throw new Exception("Cannot create Table from json: no column types specified");
		}
		String[] colTypes = new String[colTypesJson.size()];
		for(int i = 0; i < colTypesJson.size(); i++){
			if(colTypesJson.get(i) == null){
				throw new Exception("Cannot create Table with null column type");
			}
			colTypes[i] = colTypesJson.get(i).toString();
		}
		
		// gather rows
		String s;
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		JSONArray rowsJson = (JSONArray) jsonObj.get(JSON_KEY_ROWS);
		for(int i = 0; i < rowsJson.size(); i++){
			ArrayList<String> row = new ArrayList<String>();
			JSONArray rowJson = (JSONArray) rowsJson.get(i);
			for(int j = 0; j < rowJson.size(); j++){
				s = (String) rowJson.get(j);
				if(s != null){
					row.add(s);
				}else{
					row.add("null"); 
				}
			}
			rows.add(row);
		}
		
		return new Table(cols, colTypes, rows);
	}
	
	/**
	 * Create a CSV string containing the table rows and columns.
	 */
	public String toCSVString() throws IOException{
		
		if(this.getColumnNames().length == 0){
			return "";
		}
		
		StringBuffer buf = new StringBuffer();
		
		// get CSV for column names	
		ArrayList<String> headers = new ArrayList<String>( Arrays.asList( this.getColumnNames() ) ); // gets col names in order
		buf.append(Utility.getCSVString(headers));
		buf.append("\n");

		// get CSV for data rows
		for(int i = 0; i < this.getNumRows(); i++){	
			buf.append(getRowAsCSVString(i));			
			buf.append("\n");
		}		
		
		return buf.toString();
	}
	
	public JSONObject getHeaderJson() throws Exception{
		return toJson(false);
	}
	
	public JSONObject toJson() throws Exception{
		return toJson(true);
	}
	
	/**
	 * Create a JSON object from a data table
	 */
	@SuppressWarnings({ "unchecked" })
	public JSONObject toJson(Boolean includeDataRows) throws Exception {

		JSONArray allRows = new JSONArray();
		int rowCount = 0;	
		
		// collect data rows in a JSONArray
		try {
			// we can do this faster. 
			int counter = 0;
			Integer[] columnNumbersInOrderIwanted = new Integer[this.columnNames.length];
			for (String colName : this.columnNames) {
				columnNumbersInOrderIwanted[counter] = getColumnIndex(colName);
				counter++;
			}
			if(includeDataRows){
				for (ArrayList<String> row : this.getRows()) {
					if(row != null){   // do not include null rows...
						JSONArray currRow = new JSONArray();
						for (Integer k : columnNumbersInOrderIwanted) {
							currRow.add(row.get(k));
						}
						allRows.add(currRow);
						rowCount += 1;
					}
				}
			}
			else{
				rowCount = this.getRows().size();
			}
		} catch(Exception e){
			throw new Exception("Unable to collect row data for JSON table result set: " + e.getMessage());
		}

		// get the columns into a JSON Array
		JSONArray colHeaders = new JSONArray();
		JSONArray colTypes = new JSONArray();
		try{
			for(int i = 0; i < this.columnNames.length; i++){
				colHeaders.add(columnNames[i]);
				colTypes.add(columnTypes[i]);  
			}
		}catch(Exception e){
			throw new Exception("Unable to collect column names/types for JSON table result set: " + e.getMessage());
		}

		// assemble the JSON object
		try{
			// create the table	
			JSONObject tbl = new JSONObject();
			if(includeDataRows){
				tbl.put(JSON_KEY_ROWS, allRows);			
			}
			tbl.put(JSON_TYPE, "TABLE");
			tbl.put(JSON_KEY_ROW_COUNT, rowCount);
			tbl.put(JSON_KEY_COL_NAMES, colHeaders);
			tbl.put(JSON_KEY_COL_TYPES, colTypes);
			tbl.put(JSON_KEY_COL_COUNT, this.columnNames.length);
			return tbl;

		} catch(Exception e){
			throw new Exception("Error assembling JSON table result set: " + e.getMessage());
		}
		
	}

	public void clearRows() {
		this.rows.clear();
	}

	
	/**
	 * Merge multiple tables into a single table
	 */
	public static Table merge(ArrayList<Table> tables) throws Exception {

		if(tables == null || tables.size() == 0){
			throw new Exception("Cannot merge tables: no tables provided");
		}
		if(tables.size() == 1){
			return tables.get(0);  	// if only one table given, then pass it back
		}
		
		String[] mergedTableCols = null;
		String[] mergedTableColTypes = null;
		ArrayList<ArrayList<String>> mergedTableRows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowReordered;
		
		for(Table t : tables){
		
			if(mergedTableCols == null){  	// for the first table...				
				mergedTableCols = t.getColumnNames();
				mergedTableColTypes = t.getColumnTypes();
				mergedTableRows = t.getRows();
			}else{							// for subsequent tables...											
				if(!Utility.arraysSameMinusOrder(mergedTableCols, t.getColumnNames())){
					throw new Exception("Cannot merge tables: column set is not the same");
				}
				if(!Utility.arraysSameMinusOrder(mergedTableColTypes, t.getColumnTypes())){
					throw new Exception("Cannot merge tables: column type set is not the same");
				}
				// TODO should also check that the column-type PAIRS are the same
				
				// reorder and collect the table's rows
				for(ArrayList<String> row : t.getRows()){
					rowReordered = new ArrayList<String>();
					if(Arrays.equals(mergedTableCols, t.getColumnNames())){						
						rowReordered = row; // columns are in the same order, faster to just add the row as is
					}else{
						// columns are not in the same order - reorder the row 
						for(String col : mergedTableCols){
							int i = Arrays.asList(t.getColumnNames()).indexOf(col);
							rowReordered.add(row.get(i));
						}						
					}
					mergedTableRows.add(rowReordered);  // collect the (reordered) row
				}			
			}			
		}
		
		// return the merged table
		return new Table(mergedTableCols, mergedTableColTypes, mergedTableRows);
	}

	/**
	 * Retrieve a subset of the table, where a given column name has a given value.
	 * @param matchColName the name of the column to match
	 * @param matchColValue the value of the column to match
	 * @return
	 * @throws Exception 
	 */
	public Table getSubsetWhereMatches(String matchColName, String matchColValue) throws Exception {
		return getSubsetWhereMatches(matchColName, matchColValue, this.getColumnNames());
	}
	
	
	/**
	 * Retrieve a subset of the table, where a given column name has a given value.
	 * @param matchColName the name of the column to match
	 * @param matchColValue the value of the column to match
	 * @param returnColNames the names of the columns to return
	 * @return
	 * @throws Exception 
	 */
	public Table getSubsetWhereMatches(String matchColName, String matchColValue, String[] returnColNames) throws Exception {

		for(String s : returnColNames){
			if(!this.hasColumn(s)){
				throw new Exception("Requested return column \"" + s + "\" does not exist in the table");
			}
		}
		
		// get column types
		ArrayList<String> returnColTypesList = new ArrayList<String>();
		for(String s : returnColNames){
			returnColTypesList.add(getColumnType(s));
		}
		String[] returnColTypes = (String[]) returnColTypesList.toArray(new String[returnColTypesList.size()]);
		
		// create table
		Table ret = new Table(returnColNames, returnColTypes, null);
		
		// add rows to the table
		int matchColIndex = getColumnIndex(matchColName);  // get the index of the column we need to match
		for(ArrayList<String> row : getRows()){
			if(row.get(matchColIndex).equals(matchColValue)){  // met the match condition
				ArrayList<String> newRow = new ArrayList<String>();  // assemble only the columns requested
				for(String retCol : returnColNames){
					newRow.add(row.get(getColumnIndex(retCol)));
				}
				ret.addRow(newRow);
			}
		}
		return ret;
	}	
	
	/**
	 * Retrieve a table containing the subset of rows by matching on multiple columns.  
	 * Matching is determined by case-insensitive substring.
	 *
	 * @param filterMap keys are header names (case sensitive), values are filters
	 * @return the table subset
	 * @throws Exception
	 * 
	 * TODO: merge functionality with Table.getSubsetWhereMatches()
	 */
	public Table getSubsetBySubstring(HashMap<String,String> filterMap) throws Exception{
		
		// create a new table to add the filtered rows to
		Table ret = new Table(getColumnNames(), getColumnTypes(), null);
		
		int index;
		String filterValue;
		boolean failed;
		
		// for each row
		for(ArrayList<String> row : getRows()){
			failed = false;
			for(String filterKey : filterMap.keySet()){
				index = getColumnIndex(filterKey);
				    
				filterValue = filterMap.get(filterKey);
	            if (row.get(index) == null || !row.get(index).toLowerCase().contains(filterValue.toLowerCase())) {
	            	failed = true;
	            	break;
	            }	       
			}
			if(!failed){
				ret.addRow(row);  // the row met the criteria
			}
		}
			
		return ret;		
	}
	
	/**
	 * Check that all columns in other are in this table, with the same types
	 * @param other
	 * @return {String} error messages or ""
	 */
	public String getMissingColMessages(Table other) {
		List<String> otherNames = Arrays.asList(other.getColumnNames());
		List<String> otherTypes = Arrays.asList(other.getColumnTypes());
		List<String> myNames =    Arrays.asList(this.getColumnNames());
		List<String> myTypes =    Arrays.asList(this.getColumnTypes());
		
		String ret = new String();
		
		for (int i=0; i < otherNames.size(); i++) {
			int pos = myNames.indexOf(otherNames.get(i));
			if (pos == -1) {
				ret += "Table does not contain column " + otherNames.get(i) + "\n";
			} else {
				if (!otherTypes.get(i).equals(myTypes.get(pos))) {
					ret += String.format("Column %s expected type: %s, found: %s.\n", otherNames.get(i), otherTypes.get(i), myTypes.get(pos));
				}
			}
		}
		
		return ret;
	}
	
	// some sorting samples.
	// we'll need multi columns and descending, etc.
	public void sortByColumnStr(String colName) {
		int col = this.getColumnIndex(colName);
		this.rows.sort	(	(ArrayList<String> rowA, ArrayList<String> rowB) -> 
								rowA.get(col).compareTo(rowB.get(col))
						);
	}
	
	public void sortByColumnInt(String colName) {
		int col = this.getColumnIndex(colName);
		this.rows.sort	(	(ArrayList<String> rowA, ArrayList<String> rowB) -> 
								Integer.parseInt(rowA.get(col)) - Integer.parseInt(rowB.get(col))
						);	
	}
	
	public void sortByColumnDouble(String colName) {
		int col = this.getColumnIndex(colName);
		this.rows.sort	(	(ArrayList<String> rowA, ArrayList<String> rowB) -> 
								Double.compare(Double.parseDouble(rowA.get(col)), Double.parseDouble(rowB.get(col)))
						);	
	}
	
	public void sortByColumnDoubleRev(String colName) {
		int col = this.getColumnIndex(colName);
		this.rows.sort	(	(ArrayList<String> rowA, ArrayList<String> rowB) -> 
								Double.compare(Double.parseDouble(rowB.get(col)), Double.parseDouble(rowA.get(col)))
						);	
	}
	
	/**
	 * Returns true if all the rows in the table are the same.
	 */
	public boolean allRowsMatch(){
		// check that each column only has 1 unique value.
		for(int i = 0; i < getNumColumns(); i++){
			if(getColumnUniqueValues(i).length > 1){
				return false;
			}
		}
		return true;
	}
}