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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.propertygraph.SQLPropertyGraphUtils;
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
	 * Constructor from array list of column names
	 * @param cols
	 * @throws Exception
	 */
	public Table(ArrayList<String> cols) throws Exception{
		this(cols.toArray(new String[0]), null, new ArrayList<ArrayList<String>>());
	}
	
	/**
	 * Constructor from array lists of column names and types
	 * @param cols
	 * @param colTypes
	 * @throws Exception
	 */
	public Table(ArrayList<String> cols, ArrayList<String> colTypes) throws Exception{
		this(cols.toArray(new String[0]), colTypes.toArray(new String[0]), new ArrayList<ArrayList<String>>());
	}
	
	/**
	 * Constructor from lists of column names, types, and rows
	 * @param cols
	 * @param colTypes
	 * @param rows
	 * @throws Exception
	 */
	public Table(ArrayList<String> cols, ArrayList<String> colTypes, ArrayList<ArrayList<String>> rows) throws Exception{
		this(cols.toArray(new String[0]), colTypes.toArray(new String[0]), rows);
	}
	
	/**
	 * Construct from array of column names
	 * @param cols
	 * @throws Exception
	 */
	public Table(String[] cols) throws Exception{
		this(cols, null, new ArrayList<ArrayList<String>>());
	}
	
	/**
	 * Construct from arrays of column names and types
	 * @param cols
	 * @param colTypes
	 * @throws Exception
	 */
	public Table(String[] cols, String[] colTypes) throws Exception{
		this(cols, colTypes, new ArrayList<ArrayList<String>>());
	}
	
	/**
	 * Construct from arrays of column names and types, and array list of row array lists
	 * @param cols
	 * @param colTypes
	 * @param rows
	 * @throws Exception
	 */
	public Table(String[] cols, String[] colTypes, ArrayList<ArrayList<String>> rows) throws Exception{
		
		// validate
		if(cols == null){
			throw new Exception("Cannot create a Table: no columns provided");
		}
		if(colTypes == null){
			colTypes = new String[cols.length];
			for (int i=0; i < cols.length; i++) {
				colTypes[i] = "unknown";
			}
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
		
		this.hashColumnPositions();
		
	}
	

	/**
	 * Create from a csv file
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static Table fromCsvFile(String filename) throws Exception {
		String [] colnames = null;
		String [] coltypes = null;
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		
		Reader in = new FileReader(filename);
	    Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
	    
	    for (CSVRecord record : records) {
	    	if (colnames == null) {
	    		colnames = new String[record.size()];
	    		coltypes = new String[record.size()];
	    		int i=0;
		        for (String cell : record) {
		        	colnames[i] = cell;
		        	coltypes[i] = "unknown";
		        	i++;
		        }
	    	} else {
	    		ArrayList<String> row = new ArrayList<String>();
	    		for (String cell : record) {
	    			row.add(cell);
		        }
	    		rows.add(row);
	    	}
	    }
	    return new Table(colnames, coltypes, rows);
	}
	private void hashColumnPositions() {
		int colNum = 0;
		// add all of the columns to the hash so we can make lookups faster.
		for(String c : this.columnNames){
			columnPositionInfo.put(c, colNum);
			colNum++;
		}
	}
	
	/**
	 * Remove the named column
	 * @param colName
	 * @throws Exception
	 */
	public void removeColumn(String colName) throws Exception {
		// find colName's pos
		int pos = this.getColumnIndex(colName);
		if (pos < 0) {
			throw new Exception("Column doesn't exist in table: " + colName);
		}
		
		// delete and rehash column header
		this.columnNames = (String[])ArrayUtils.remove(this.columnNames, pos);
		this.columnTypes = (String[])ArrayUtils.remove(this.columnTypes, pos);
		this.hashColumnPositions();

		// delete data
		for (ArrayList<String> row : this.rows) {
			row.remove(pos);
		}
	}
	
	/**
	 * Return a sub-table containing only the given columns
	 */
	public Table getSubTable(String [] colNames) throws Exception {
		int colIndices[] = new int[colNames.length];
		
		for (int i=0; i < colNames.length; i++) {
			colIndices[i] = this.getColumnIndex(colNames[i]);
			if (colIndices[i] == -1) {
				throw new Exception("Column doesn't exist in table: " + colNames[i]);
			}
		}
		
		String names[] = new String[colNames.length];
		String types[] = new String[colNames.length];
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		for (int r=0; r < this.getNumRows(); r++) {
			rows.add(new ArrayList<String>());
		}
		
		for (int c=0; c < colNames.length; c++) {
			int col = colIndices[c];
			names[c] = this.columnNames[col];
			types[c] = this.columnTypes[col];
			for (int r=0; r < this.getNumRows(); r++) {
				rows.get(r).add(this.getCell(r, col));
			}
		}
		return new Table(names, types, rows);
	}
	
	/**
	 * Append a column
	 * @param colName
	 * @param colType
	 * @throws Exception
	 */
	public void appendColumn(String colName, String colType) throws Exception {
		this.insertColumn(colName, colType, this.getNumColumns(), "");
	}
	
	/**
	 * insert a column of data at given pos
	 * @param colName
	 * @param colType
	 * @param pos
	 * @param defaultValue
	 * @throws Exception
	 */
	public void insertColumn(String colName, String colType, int pos, String defaultValue) {
		
		// delete and rehash column header
		this.columnNames = (String[])ArrayUtils.add(this.columnNames, pos, colName);
		this.columnTypes = (String[])ArrayUtils.add(this.columnTypes, pos, colType);
		this.hashColumnPositions();

		// add data
		for (ArrayList<String> row : this.rows) {
			row.add(pos, defaultValue);
		}
	}
	
	/**
	 * Insert a new column that is a concatenation of other column values
	 */
	public void appendJoinedColumn(String colName, String colType, String[] colNames, String delim) throws Exception {
		this.insertJoinedColumn(colName, colType, this.getNumColumns(), colNames, delim);
	}
	
	/**
	 * Insert a new column that is a concatenation of other column values
	 */
	public void insertJoinedColumn(String colName, String colType, int pos, String[] colNames, String delim) throws Exception {
		int colIndices[] = new int[colNames.length];
		
		for (int i=0; i < colNames.length; i++) {
			colIndices[i] = this.getColumnIndex(colNames[i]);
			if (colIndices[i] == -1) {
				throw new Exception("Column doesn't exist in table: " + colNames[i]);
			}
		}
		
		// delete and rehash column header
		this.columnNames = (String[])ArrayUtils.add(this.columnNames, pos, colName);
		this.columnTypes = (String[])ArrayUtils.add(this.columnTypes, pos, colType);
		this.hashColumnPositions();

		// add data
		for (ArrayList<String> row : this.rows) {
			String [] vals = new String[colNames.length];
			for (int i=0; i < colNames.length; i++) {
				vals[i] = row.get(colIndices[i]);
			}
			row.add(pos, String.join(delim, vals));
		}
	}
	
	/**
	 * insert a column of data at given pos
	 * @param colName
	 * @param colType
	 * @param pos
	 * @param defaultValue
	 * @throws Exception
	 */
	public void appendColumn(String colName, String colType, String defaultValue) {
		
		// delete and rehash column header
		this.columnNames = (String[])ArrayUtils.add(this.columnNames, colName);
		this.columnTypes = (String[])ArrayUtils.add(this.columnTypes, colType);
		this.hashColumnPositions();

		// add data
		for (ArrayList<String> row : this.rows) {
			row.add(defaultValue);
		}
	}

	 /**
	  *  Replace the existing column names with a new set.
	  * @param newColumnNames
	  * @throws Exception
	  */
	
	public void replaceColumnNames(String [] newColumnNames) throws Exception{
		if(this.columnNames.length != newColumnNames.length){
			throw new Exception("replaceColumnNames: the incoming column name count (" + newColumnNames.length + ") does not match the target column names count (" + this.columnNames.length + ")");
		}	
		this.columnNames = newColumnNames;
	}
	
	/**
	 * Get the number of rows in the table.
	 * @return
	 */

	public int getNumRows(){
		return rows.size();
	}
	
	/**
	 * Get the number of columns in the table.
	 * @return
	 */
	public int getNumColumns(){
		return columnNames.length;
	}
	
	/**
	 * Get the column names.
	 * @return
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
	 * @param colName
	 * @return
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
	 * @param columnName
	 * @return
	 */
	public String[] getColumn(String columnName){
		return getColumn(getColumnIndex(columnName));
	}
	
	/**
	 * Return the values for a particular column
	 * @param index
	 * @return
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
	 * @param columnName
	 * @return
	 * @throws Exception
	 */
	public String[] getColumnUniqueValues(String columnName) throws Exception {
		return getColumnUniqueValues(getColumnIndexOrError(columnName));
	}
	
	/**
	 * Return the values for a particular column, removing duplicates
	 * @param index
	 * @return
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
	
	/**
	 * Add a row of data to the bottom
	 * @param newRow
	 * @throws Exception
	 */
	public void addRow(ArrayList<String> newRow) throws Exception{
		if(newRow.size() != this.columnNames.length){
			// panic
			throw new Exception("Incoming row has " + newRow.size() + " columns but " + this.columnNames.length + " were expected.");
		}
		this.rows.add(newRow);
	}
	
	/** 
	 * Add a row of data to the bottom
	 * @param newRow
	 * @throws Exception
	 */
	public void addRow(String [] newRow)  throws Exception {
		this.addRow(new ArrayList<String>(Arrays.asList(newRow)));
	}
	
	/**
	 * Add a new row of data to the bottom
	 * @param newRow
	 * @throws Exception
	 */
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
	 * @param colName
	 * @return
	 */
	public int getColumnIndex(String colName){
		int retval = -1;
		String name = colName.startsWith("?") ? colName.substring(1) : colName;
		
		if(this.columnPositionInfo.get(name) != null){
			retval = this.columnPositionInfo.get(name);
		}		
		return retval;
	}
	
	/**
	 * Get the index for a given column name, or exception if it does not exist
	 * @param colName
	 * @return
	 * @throws Exception
	 */
	private int getColumnIndexOrError(String colName) throws Exception {
		int pos = this.getColumnIndex(colName);
		if(pos > -1){
			return pos;
		} else {
			throw new Exception ("Can't find column in table: " + colName);
		}
		
	}
	
	/**
	 * Get all table data
	 * @return
	 */
	public ArrayList<ArrayList<String>> getRows(){
		return this.rows;
	}
	
	/**
	 * Get a single table row by index
	 * @param rowNum
	 * @return
	 */
	public ArrayList<String> getRow(int rowNum){
		return this.rows.get(rowNum);
	}
	
	/**
	 * Get a single table row by index, and convert it to a CSV string.
	 * @param rowNum
	 * @return
	 * @throws IOException
	 */
	public String getRowAsCSVString(int rowNum) throws IOException{		
		ArrayList<String> row = this.getRow(rowNum);
		return Utility.getCSVString(row);
	}
	
	/**
	 * Get only the headers converted to a CSV string
	 * @return
	 * @throws IOException
	 */
	public String getHeaderAsCSVString() throws IOException {
		ArrayList<String> headers = new ArrayList<String>( Arrays.asList( this.getColumnNames() ) ); // gets col names in order
		return (Utility.getCSVString(headers));
	}
	
	/**
	 * Replace an existing cell value with no checking
	 * @param row
	 * @param col
	 * @param val
	 */
	public void setCell(int row, int col, String val) {
		this.rows.get(row).set(col, val);
	}
	
	public void setCell(int row, String colName, String val) throws Exception {
		this.rows.get(row).set(this.getColumnIndexOrError(colName), val);
	}
	
	public void setCell(int row, String colName, double val) throws Exception {
		this.rows.get(row).set(this.getColumnIndexOrError(colName), String.valueOf(val));
	}
	
	public void setCell(int row, String colName, int val) throws Exception {
		this.rows.get(row).set(this.getColumnIndexOrError(colName), String.valueOf(val));
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
	
	public String getCell(int row, String colName) throws Exception {
		return this.rows.get(row).get(this.getColumnIndexOrError(colName));
	}
	
	public String getCellAsString(int row, String colName) throws Exception {
		return getCell(row, colName);
	}
	
	public int getCellAsInt(int row, String colName) throws Exception {
		return Integer.parseInt(getCell(row, colName));
	}
	
	public long getCellAsLong(int row, String colName) throws Exception {
		return Long.parseLong(getCell(row, colName));
	}
	
	public float getCellAsFloat(int row, String colName) throws Exception {
		return Float.parseFloat(getCell(row, colName));
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
	 * Join with another table with identical column names
	 * @param other
	 * @throws Exception
	 */
	public void append(Table other) throws Exception {
		String [] myCols = Arrays.copyOf(this.getColumnNames(), Math.toIntExact(this.getNumColumns()));
		String [] otherCols = Arrays.copyOf(other.getColumnNames(), Math.toIntExact(other.getNumColumns()));
		Arrays.sort(myCols);
		Arrays.sort(otherCols);
		if (! Arrays.equals(myCols, otherCols)) {
			throw new Exception("Can not append tables with different columns.\nExpected: " + String.join(",", myCols) + "\nFound: " + String.join(",", otherCols));
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
	 * Remove rows whose values in given columns duplicate an earlier row
	 * @param colNames
	 * @throws Exception - bad value in colnames
	 */
	public void uniquify(String [] colNames) throws Exception {
		
		// get key colum indices, checking for errors
		int [] keyCols = new int[colNames.length];
		for (int i=0; i < colNames.length; i++) {
			keyCols[i] = this.getColumnIndexOrError(colNames[i]);
		}
		
		// use rowHash to add only unique rows to newRows
		ArrayList<ArrayList<String>> newRows = new ArrayList<ArrayList<String>>();   
		HashSet<String> rowHash = new HashSet<String>();
		for (ArrayList<String> row : this.rows) {
			String hashVal = "";
			for (int i=0; i < keyCols.length; i++) {
				hashVal += row.get(keyCols[i]) + "|";
			}
			if (! rowHash.contains(hashVal)) {
				newRows.add(row);
				rowHash.add(hashVal);
			}
		}
		
		// update rows
		this.rows = newRows;
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
	
	public String toCSVString() throws IOException{
		return this.toCSVString(-1);
	}
	
	/**
	 * Create a CSV string containing the table rows and columns.
	 */
	public String toCSVString(int maxRows) throws IOException{
		
		if(this.getColumnNames().length == 0){
			return "";
		}
		
		StringBuffer buf = new StringBuffer();
		
		// get CSV for column names	
		ArrayList<String> headers = new ArrayList<String>( Arrays.asList( this.getColumnNames() ) ); // gets col names in order
		buf.append(Utility.getCSVString(headers));
		buf.append("\n");

		// get CSV for data rows
		for(int i = 0; i < this.getNumRows() && i != maxRows; i++){	
			buf.append(getRowAsCSVString(i));			
			buf.append("\n");
		}		
		
		return buf.toString();
	}
	
	
	/**
	 * Get the table as a Gremlin-loadable CSV string
	 */
	public String toGremlinCSVString() throws Exception, IOException{
		return this.toGremlinCSVString(null, null, null, null);
	}

	/**
	 * Get the table as a Gremlin-loadable CSV string
	 */

	public String toGremlinCSVString(String idColumn, String labelColumn) throws Exception, IOException{
		return this.toGremlinCSVString(idColumn, labelColumn, null, null);
	}

	/**
	 * Get the table as a Gremlin-loadable CSV string
	 * @param idColumn  	column to rename "~id"
	 * @param labelColumn	column to rename "~label"
	 */
	public String toGremlinCSVString(String idColumn, String labelColumn, String fromColumn, String toColumn) throws Exception, IOException{
		
		if(this.getColumnNames().length == 0){
			return "";
		}
		
		StringBuffer buf = new StringBuffer();
		
		// get Gremlin for column names	
		String [] colNames = this.getColumnNames(); 
		String [] colTypes = this.getColumnTypes();
		
		// make sure special column names exist
		for (String col : new String [] {idColumn, labelColumn, fromColumn, toColumn} ) {		
			if (col != null && !Arrays.stream(colNames).anyMatch(col::equals)) {
				throw new Exception("Column does not exist: " + col);
			}
		}
		
		// format datetime (where needed)
		// Gremlin supports the following formats: yyyy-MM-dd, yyyy-MM-ddTHH:mm, yyyy-MM-ddTHH:mm:ss, yyyy-MM-ddTHH:mm:ssZ (https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format-gremlin.html)
		for(int colIndex = 0; colIndex < colTypes.length;  colIndex++){
			if(SQLPropertyGraphUtils.SQLtoGremlinType(colTypes[colIndex]).equals("Date")){
				for(int rowIndex = 0; rowIndex < getNumRows(); rowIndex++){
					String dateTimeOrig = getCell(rowIndex, colIndex);
					String dateTimeStrFormatted;
					if(dateTimeOrig == null){
						continue;
					}else if(dateTimeOrig.length() >= 19){  			
						if(dateTimeOrig.length() > 19){
							dateTimeOrig = dateTimeOrig.substring(0, dateTimeOrig.indexOf("."));  // Gremlin does not support microseconds
						}
						dateTimeStrFormatted = Utility.formatDateTime(dateTimeOrig, Utility.DATETIME_FORMATTER_yyyyMMddHHmmss, Utility.DATETIME_FORMATTER_ISO8601);  
						setCell(rowIndex, colIndex, dateTimeStrFormatted);
					}else if(dateTimeOrig.length() != 10 && dateTimeOrig.length() != 15 && dateTimeOrig.length() != 18 && dateTimeOrig.length() != 20){
						throw new Exception("Unrecognized date format: " + dateTimeOrig);
					}				
				}
			}
		}
		
		// create column names
		for (int i=0; i < colNames.length; i++) {
			if (i>0) buf.append(",");
			
			if (colNames[i].equals(idColumn!=null?idColumn:"")) {
				// change idColumn to ~id
				buf.append("~id");
			} else if (colNames[i].equals(labelColumn!=null?labelColumn:"")) {
				// change labelColumn to ~label
				buf.append("~label");
			} else if (colNames[i].equals(fromColumn!=null?fromColumn:"")) {
				// change fromColumn to ~label
				buf.append("~from");
			} else if (colNames[i].equals(toColumn!=null?toColumn:"")) {
				// change toColumn to ~label
				buf.append("~to");
			} else if (colNames[i].charAt(0) == '~') {
				// leave alone any column starting with ~
				buf.append(colNames[i]);
			} else {
				// append :gremlin-type to any other column
				buf.append(colNames[i] + ":" + SQLPropertyGraphUtils.SQLtoGremlinType(colTypes[i]));
			}
		}
		buf.append("\n");

		// get CSV for data rows
		for(int i = 0; i < this.getNumRows(); i++){	
			buf.append(getRowAsCSVString(i));			
			buf.append("\n");
		}		
		
		return buf.toString();
	}
	
	/**
	 * hash table to unique MD5 string
	 * @return
	 * @throws Exception
	 */
	public String hashMD5() throws Exception {
		// get csv and split into rows
		String [] rows = this.toCSVString().split("\n");
		String header = rows[0];
		
		// sort non-header rows
		List<String> r = Arrays.asList(Arrays.copyOfRange(rows, 1, rows.length));
		Collections.sort(r);
		
		// build input and run hash
		String hashInput = header + "\n" + String.join("\n", r);
		return Utility.hashMD5(hashInput);
	}
	
	/**
	 * Get headers as JSON
	 * @return
	 * @throws Exception
	 */
	public JSONObject getHeaderJson() throws Exception{
		return toJson(false);
	}
	
	/**
	 * Get table as json
	 * @return
	 * @throws Exception
	 */
	public JSONObject toJson() throws Exception{
		return toJson(true);
	}
	
	/**
	 * Get table as json
	 * @param includeDataRows
	 * @return
	 * @throws Exception
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

	/**
	 * Clear all data
	 */
	public void clearRows() {
		this.rows.clear();
	}

	
	/**
	 * Merge multiple tables into a single table
	 * @param tables
	 * @return
	 * @throws Exception
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
		if (matchColIndex < 0) {
			throw new Exception("Can't find column in table: " + matchColName);
		}
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
	
	/**
	 * Sort by a string column
	 * @param colName
	 */
	public void sortByColumnStr(String colName) {
		int col = this.getColumnIndex(colName);
		this.rows.sort	(	(ArrayList<String> rowA, ArrayList<String> rowB) -> 
								rowA.get(col).compareTo(rowB.get(col))
						);
	}
	
	/** 
	 * Sort by an int column
	 * @param colName
	 */
	public void sortByColumnInt(String colName) {
		int col = this.getColumnIndex(colName);
		this.rows.sort	(	(ArrayList<String> rowA, ArrayList<String> rowB) -> 
								Integer.parseInt(rowA.get(col)) - Integer.parseInt(rowB.get(col))
						);	
	}
	
	/**
	 * Sort by a double column
	 * @param colName
	 */
	public void sortByColumnDouble(String colName) {
		int col = this.getColumnIndex(colName);
		this.rows.sort	(	(ArrayList<String> rowA, ArrayList<String> rowB) -> 
								Double.compare(Double.parseDouble(rowA.get(col)), Double.parseDouble(rowB.get(col)))
						);	
	}
	
	/**
	 * Sort reverse by double column
	 * @param colName
	 */
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