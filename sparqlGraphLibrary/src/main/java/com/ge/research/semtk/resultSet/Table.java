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

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.utility.Utility;

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

	private String[] columnNames;
	private String[] columnTypes;
	private ArrayList<ArrayList<String>> rows;   
	
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
		
		if(rows != null){
			if(rows.size() > 0 && rows.get(0).size() != cols.length){
				throw new Exception("Cannot create a Table: wrong number of entries in rows");
			}
			this.rows = rows;
		}
		else{
			this.rows = new ArrayList<ArrayList<String>>();
		}
		this.columnNames = cols;
		this.columnTypes = colTypes;
	}
	
	public int getNumRows(){
		return rows.size();
	}
	
	public int getNumColumns(){
		return columnNames.length;
	}
	
	public String[] getColumnNames(){
		return columnNames;
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
	
	/**
	 * Get the index for a given column name, or -1 if does not exist
	 */
	public int getColumnIndex(String colName){
		return ArrayUtils.indexOf(this.columnNames, colName);
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
	 * Truncate the table to <length> rows
	 * @param length
	 */
	public void truncate (int length) {
		int size = this.rows.size();
		if (size > length) {
			this.rows.subList(length, size).clear();
		}
	}
	
	/**
	 * Get a table instance from a JSON object
	 * Json object looks like this: {"col_names":["colA","colB","colC"],"col_types":["String","String","String"],"rows":[["apple","banana","coconut"],["adam","barbara","chester"]],"col_count":3\"row_count":2}
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
	 * TODO should use a package like Apache Commons CSV
	 */
	public String toCSVString(){
		
		if(this.getColumnNames().length == 0){
			return "";
		}
		
		StringBuffer buf = new StringBuffer();
		
		// gather column names
		String[] columnNamesInOrder = this.getColumnNames();
		for(String c : columnNamesInOrder){
			buf.append(c).append(",");
		}
		buf.setLength(buf.length() - 1); // strip off the tailing comma
		buf.append("\n");

		// gather data rows
		for (ArrayList<String> row : this.getRows()) {
			for (String e : row) {
				if(e.indexOf(",") > -1){
					e = "\"" + e + "\"";  // if the element contains a comma, wrap in quotes				
				}
				buf.append(e).append(",");
			}
			buf.setLength(buf.length() - 1); // strip off the tailing comma
			buf.append("\n");
		}		
		
		return buf.toString();
	}
	
	/**
	 * Create a JSON object from a data table
	 */
	@SuppressWarnings({ "unchecked" })
	public JSONObject toJson() throws Exception {

		JSONArray allRows = new JSONArray();
		int rowCount = 0;	
		
		// collect data rows in a JSONArray
		try {
			for (ArrayList<String> row : this.getRows()) {
				JSONArray currRow = new JSONArray();
				for (String colName : this.columnNames) {
					currRow.add(row.get(getColumnIndex(colName)));
				}
				allRows.add(currRow);
				rowCount += 1;
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
			tbl.put(JSON_KEY_ROWS, allRows);			
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
	
	
}