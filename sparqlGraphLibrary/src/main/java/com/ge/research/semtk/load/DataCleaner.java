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


package com.ge.research.semtk.load;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.utility.LocalLogger;


/**
 * Cleans a dataset and writes it to a CSV file.
 */
public class DataCleaner {

	// keys for json cleaning spec
	public final static String JSON_KEY_SPLIT = "SPLIT";	// plain splits can now be achieved using the paired split spec, but keeping this for backward compatibility
	public final static String JSON_KEY_PAIRED_SPLIT = "PAIRED_SPLIT";
	public final static String JSON_KEY_LOWERCASE = "LOWERCASE";
	public final static String JSON_KEY_REMOVE_NULLS = "REMOVE_NULLS";
	public final static String JSON_KEY_REMOVE_NA = "REMOVE_NA";
	
	private final static String[] UNSUPPORTED_SPLIT_DELIMITERS = {"\n","^"," "}; // noticed that these don't work so disallow them for now...add to this list as we find more		

	private Dataset dataset;			// the dataset to clean
	private ArrayList<String> headers;	// headers from the dataset to clean
	private BufferedWriter writer;		// writer for the cleaned dataset
	private CSVPrinter csvPrinter;  	// csv writer for the cleaned dataset
	private final int BATCH_SIZE = 2;	// TODO make this configurable?

	// store setup info about how to clean
	private HashMap<String,String> columnsToSplit = new HashMap<String,String>(); // key is column header, value is split delimiter (e.g. ~)
	private HashSet<HashMap<String,String>> columnsToPairedSplit = new HashSet<HashMap<String,String>>(); // a set of column-delimiter mappings to split together
	private HashSet<String> columnsToLowerCase = new HashSet<String>(); // key is column header	
	private boolean removeNulls = false;  // if true, replace all occurrences of "null" string (as a whole entry) with ""
	private boolean removeNA = false;  // if true, replace all occurrences of "N/A" or "n/a" string (as a whole entry) with ""
	
	// store counts
	private int numRowsProcessed;		// num original records 
	private int numRowsProduced;		// num cleaned records produced


	/**
	 * Constructor that takes no cleaning spec - expect it to be added programmatically using addSplit(), etc
	 * @param dataset the dataset to clean
	 * @param cleanedFilePathStr the file to write cleaned data to
	 */
	public DataCleaner(Dataset dataset, String cleanedFilePathStr) throws Exception {
		this(dataset, cleanedFilePathStr, null);
	}

	/**
	 * Constructor that takes the cleaning spec as JSON
	 * @param dataset the dataset to clean
	 * @param cleanedFilePathStr the file to write cleaned data to
	 * @param cleanSpecJson the cleaning spec in JSON format, e.g. {"LOWERCASE":["child_names","has_pool"],"SPLIT":{"pet_names":"##","child_names":"~"},"REMOVE_NULLS":"true"}
	 */
	public DataCleaner(Dataset dataset, String cleanedFilePathStr, JSONObject cleanSpecJson) throws Exception {
		this.dataset = dataset;		
		this.headers = dataset.getColumnNamesinOrder();
		this.writer = new BufferedWriter(new FileWriter(cleanedFilePathStr));
		this.csvPrinter = new CSVPrinter(this.writer, CSVFormat.DEFAULT);

		// add the specs for cleaning
		parseCleanSpecJson(cleanSpecJson);

		// write the headers
		this.csvPrinter.printRecord(this.headers);
	}

	/**
	 * Parse a JSON object containing the cleaning specs
	 */
	@SuppressWarnings("unchecked")
	private void parseCleanSpecJson(JSONObject cleanSpecJson) throws Exception{

		if(cleanSpecJson == null){
			return;
		}

		// add lower case specs
		// e.g. "LOWERCASE":["child_names","has_pool"]
		JSONArray lowercaseObj = (JSONArray) cleanSpecJson.get(JSON_KEY_LOWERCASE);
		if(lowercaseObj != null){
			for(Object jObj : lowercaseObj){
				addToLowerCase((String)jObj);
			}
		}

		// add split specs
		// e.g. "SPLIT":{"pet_names":"##","child_names":"~"}
		JSONObject splitObj = (JSONObject) cleanSpecJson.get(JSON_KEY_SPLIT);
		if(splitObj != null){
			for(String s : (Set<String>)splitObj.keySet()){
				addSplit(s, (String)splitObj.get(s));
			}
		}
		
		// add paired split specs
		// support 2+ columns per paired split, and support an arbitrary number of paired splits
		// e.g. "PAIRED_SPLIT":[{"first_name":"##","middle_name":"~","last_name":"~"}]
		// e.g. "PAIRED_SPLIT":[{"first_name":"##","middle_name":"~","last_name":"~"},{"pet_name":"##","pet_color":"~"}]
		JSONArray pairedSplitArray = (JSONArray) cleanSpecJson.get(JSON_KEY_PAIRED_SPLIT);
		if(pairedSplitArray != null){
			for(Object jObj : pairedSplitArray){  // e.g. {"first_name":"##","middle_name":"~","last_name":"~"}
				HashMap<String, String> columnsAndDelimiters = (HashMap<String,String>)jObj;	
				addPairedSplit(columnsAndDelimiters);
			}
		}
		
		// add remove nulls specs
		// e.g. "REMOVE_NULLS":"true"
		String removeNullsVal = (String) cleanSpecJson.get(JSON_KEY_REMOVE_NULLS);
		if(removeNullsVal != null){
			if(removeNullsVal.toString().toLowerCase().equals("true")){
				removeNulls = true;
			}
		}
		
		// add remove N/A specs
		// e.g. "REMOVE_NA":"true"
		String removeNAVal = (String) cleanSpecJson.get(JSON_KEY_REMOVE_NA);
		if(removeNAVal != null){
			if(removeNAVal.toString().toLowerCase().equals("true")){
				removeNA = true;
			}
		}		
		
	}


	/**
	 * Specify a column to split
	 * @param columnHeader (e.g. "personnel")
	 * @param delimiter (e.g. "~")
	 * @throws Exception 
	 */
	public void addSplit(String columnHeader, String delimiter) throws Exception{
		
		validateColumnHeader(columnHeader);
		validateDelimiter(delimiter);
		validateNotSplitYet(columnHeader);

		// add the split
		columnsToSplit.put(columnHeader, delimiter);
	}
	
	/**
	 * Specify a paired split. 
	 * @param columnsAndDelimiters a HashMap of column headers to delimiters (e.g first_name->"##", middle_name->"~", last_name->"~")
	 * @throws Exception 
	 */
	public void addPairedSplit(HashMap<String,String> columnsAndDelimiters) throws Exception{

		for(String columnHeader : columnsAndDelimiters.keySet()){
			validateColumnHeader(columnHeader);
			validateDelimiter(columnsAndDelimiters.get(columnHeader));
			validateNotSplitYet(columnHeader);
		}
		
		// add the paired split
		columnsToPairedSplit.add(columnsAndDelimiters);
	}

	/**
	 * Specify a column to change to lower case
	 * @param columnHeader (e.g. "personnel")
	 * @throws Exception 
	 */
	public void addToLowerCase(String columnHeader) throws Exception{

		validateColumnHeader(columnHeader);

		// add the column
		columnsToLowerCase.add(columnHeader);
	}
	
	/**
	 * Specify removing nulls or not
	 * @param removeNulls true to remove nulls, else false
	 * @throws Exception 
	 */
	public void addRemoveNulls(boolean removeNulls) throws Exception{
		this.removeNulls = removeNulls;
	}

	/**
	 * Specify removing "not applicables" or not
	 * @param removeNA true to remove N/A and n/a, else false
	 * @throws Exception 
	 */
	public void addRemoveNA(boolean removeNA) throws Exception{
		this.removeNA = removeNA;
	}
	

	/**
	 * Clean each row and write to CSV
	 * @return number of clean rows produced (may exceed the number of input rows)
	 */
	public int cleanData() throws Exception{

		try{

			numRowsProcessed = 0;
			numRowsProduced = 0;

			// iterate through the dataset
			ArrayList<ArrayList<String>> rows;
			while(true){					

				// get more data to clean
				rows = dataset.getNextRecords(BATCH_SIZE);
				if(rows.size() == 0){
					break;  // no more data, done
				}

				// clean each row
				for(ArrayList<String> row : rows){
					cleanRow(row, headers);
					numRowsProcessed++;
				}
			}

			// clean up
			csvPrinter.flush();
			LocalLogger.logToStdOut("Processed " + numRowsProcessed + " records, produced " + numRowsProduced + " clean records. (DONE)");		
		
		}catch(Exception e){
			throw new Exception("Exception cleaning data: " + e);
		}finally{
			writer.close();
			dataset.close();	
		}

		return numRowsProduced;  
	}


	/**
	 * Clean a single row of data, producing 1+ rows of cleaned data.
	 * @param row the row of data to clean
	 */
	@SuppressWarnings("unchecked")
	private void cleanRow(ArrayList<String> row, ArrayList<String> headers) throws Exception{
		
		// check inputs
		if(row.size() != headers.size()){
			throw new Exception("Row does not have the same number of fields as the header list");
		}

		// perform to lower case
		row = performLowerCase(row);

		// remove nulls
		if(removeNulls){
			row = performRemoveNulls(row);
		}
		
		// remove "N/A" and "n/a"
		if(removeNA){
			row = performRemoveNA(row);
		}		
		
		// perform splits
		ArrayList<ArrayList<String>> rows = performSplits(row);
		
		// perform paired splits
		if(columnsToPairedSplit.size() > 0){
			ArrayList<ArrayList<String>> rowsTmp = new ArrayList<ArrayList<String>>();
			for(ArrayList<String> r : rows){
				for(ArrayList<String> tmpRow : performPairedSplits(r)){  // perform paired splits
					rowsTmp.add((ArrayList<String>) tmpRow.clone());
				}
			}
			rows = rowsTmp;
		}
		
		// write to CSV
		for(ArrayList<String> rowToWrite : rows){
			this.csvPrinter.printRecord(rowToWrite);
			numRowsProduced++;  // num clean records produced
		}
	}


	/**
	 * Take a row of data and converts appropriate values to lower case.
	 * @param row the input row of data
	 * @return cleaned rows of data
	 */
	private ArrayList<String> performLowerCase(ArrayList<String> row){
		String header;
		String value;
		for(int i = 0; i < row.size(); i++){  // for each value in the row			
			value = row.get(i);
			header = headers.get(i);

			if(columnsToLowerCase.contains(header)){
				row.set(i, value.toLowerCase());
			}
		}
		return row;
	}	
	
	
	/**
	 * Take a row of data and remove null strings
	 * @param row the input row of data
	 * @return cleaned rows of data
	 */
	private ArrayList<String> performRemoveNulls(ArrayList<String> row){
		String value;
		for(int i = 0; i < row.size(); i++){  // for each value in the row			
			value = row.get(i);
			if(value.trim().equalsIgnoreCase("null")){  // if's null string
				row.set(i,"");  // change to empty string
			}
		}
		return row;
	}	
	
	/**
	 * Take a row of data and remove n/a strings
	 * @param row the input row of data
	 * @return cleaned rows of data
	 */
	private ArrayList<String> performRemoveNA(ArrayList<String> row){
		String value;
		for(int i = 0; i < row.size(); i++){  // for each value in the row			
			value = row.get(i);
			if(value.trim().equalsIgnoreCase("n/a")){  
				row.set(i,"");  // change to empty string
			}
		}
		return row;
	}		


	/**
	 * Take a row of data and perform splits, returning a set of rows.
	 * @param row the input row of data
	 * @return rows of data containing split fields
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<ArrayList<String>> performSplits(ArrayList<String> row){

		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		rows.add(row);	// start with the input row

		String delimiter;
		String header;
		String value;
		for(int i = 0; i < row.size(); i++){  // for each value in the row			
			value = row.get(i);
			header = headers.get(i);
			delimiter = columnsToSplit.get(header);

			if(delimiter != null && value.contains(delimiter)){  	// if this value needs to be split

				String[] splitValues = value.split(delimiter);  // split the value
				ArrayList<ArrayList<String>> rowsNew = new ArrayList<ArrayList<String>>();

				// for each previous row, replace with new set of rows with split values
				for(ArrayList<String> rowOld : rows){
					for(String splitValue : splitValues){
						ArrayList<String> rowNew = (ArrayList<String>) rowOld.clone();
						rowNew.set(i,splitValue.trim());	// replace the unsplit value with the split value  (trim to remove unwanted spaces, e.g. after commas)
						rowsNew.add(rowNew);	
					}						
				}
				rows = rowsNew;  // overwrite old rows with new rows 
			}
		}
		return rows;
	}
	
	/**
	 * Perform all paired splits on a single row of data.
	 */
	private ArrayList<ArrayList<String>> performPairedSplits(ArrayList<String> row) throws Exception{

		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		rows.add(row);  // start with the original incoming row	
		
		// for each paired split in the spec, build rowsTmp as a replacement for rows, and then replace it
		for(HashMap<String,String> pairedSplit : columnsToPairedSplit){
			ArrayList<ArrayList<String>> rowsTmp = new ArrayList<ArrayList<String>>();
			for(ArrayList<String> r : rows){
				rowsTmp.addAll(performPairedSplit(r, pairedSplit));  
			}
			rows = rowsTmp; // replace it
		}	
		return rows;
	}
	
	
	/**
	 * Perform a single paired split on a single row of data.
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<ArrayList<String>> performPairedSplit(ArrayList<String> row, HashMap<String,String> pairedSplit) throws Exception{
		
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		int numReturnRows = -1;
		
		// for each column in the paired split
		for(String columnHeader : pairedSplit.keySet()){

			String delimiter = pairedSplit.get(columnHeader);	// the delimiter
			int index = headers.indexOf(columnHeader);			// the index of the column to split
			String value = row.get(index);						// the content to split

			if(numReturnRows == -1){
				// we're on the first column to split
				numReturnRows = value.split(delimiter).length; // determine how many elements will result from the split
				for(int i = 0; i < numReturnRows; i++){
					// for each element that will result from the split, add a row
					rows.add((ArrayList<String>) row.clone());	// the new row is a clone of the original for now...values will get replaced
				}
			}else{
				// we're past the first column to split. If the current column does not split into the right number of elements, then error.
				if(numReturnRows != value.split(delimiter).length){
					throw new Exception("Mismatched number of split items across columns: cannot split '" + value + "' into " + numReturnRows + " elements");
				}
			}

			// replace the unsplit row values with the split row values
			for(int i = 0; i < numReturnRows; i++){
				rows.get(i).set(index, value.split(delimiter)[i].trim());
			}
		}
		
		return rows;
	}
	

	// confirm that the column header exists
	private void validateColumnHeader(String columnHeader) throws Exception{
		if(headers.indexOf(columnHeader) == -1){
			throw new Exception("Cannot clean nonexistent column " + columnHeader);
		}
	}
	
	// confirm that the delimiter is supported
	private void validateDelimiter(String delimiter) throws Exception{
		if(ArrayUtils.indexOf(UNSUPPORTED_SPLIT_DELIMITERS, delimiter) > -1){			
			throw new Exception("Cannot yet support splitting on delimiter '" + delimiter + "'");
		}
	}
	
	// confirm that there is no split (or paired split) on this column yet
	private void validateNotSplitYet(String columnHeader) throws Exception{
		
		// confirm not in split list
		if(columnsToSplit.get(columnHeader) != null){
			throw new Exception("Already splitting column " + columnHeader + ", cannot add another split");
		}
		
		// confirm not in paired split list
		for(HashMap<String,String> map: columnsToPairedSplit){
			if(map.containsKey(columnHeader)){
				throw new Exception("Already splitting column " + columnHeader + ", cannot add another split");
			}
		}
	}
	
}
