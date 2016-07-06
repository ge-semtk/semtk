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


/**
 * Cleans a dataset and writes it to a CSV file.
 */
public class DataCleaner {

	// keys for json cleaning spec
	public final static String JSON_KEY_SPLIT = "SPLIT";
	public final static String JSON_KEY_LOWERCASE = "LOWERCASE";
	
	private final static String[] UNSUPPORTED_SPLIT_DELIMITERS = {"\n","^"," "}; // noticed that these don't work so disallow them for now...add to this list as we find more		

	private Dataset dataset;			// the dataset to clean
	private ArrayList<String> headers;	// headers from the dataset to clean
	private BufferedWriter writer;		// writer for the cleaned dataset
	private CSVPrinter csvPrinter;  	// csv writer for the cleaned dataset
	private final int BATCH_SIZE = 2;	// TODO make this configurable?

	// store setup info about how to clean	TODO add more as needed
	private HashMap<String,String> columnsToSplit = new HashMap<String,String>(); // key is column header, value is split delimiter (e.g. ~)	
	private HashSet<String> columnsToLowerCase = new HashSet<String>(); // key is column header

	// store counts
	private int numRowsProcessed;		// num original records 
	private int numRowsProduced;		// num cleaned records produced


	/**
	 * Constructor that takes no cleaning spec - expect it to be added programattically using addSplit(), etc
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
	 * @param cleanSpecJson the cleaning spec in JSON format, e.g. {"LOWERCASE":["child_names","has_pool"],"SPLIT":{"pet_names":"##","child_names":"~"}}
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
	private void parseCleanSpecJson(JSONObject cleanSpecJson) throws Exception{

		if(cleanSpecJson == null){
			return;
		}

		// add lower case specs
		JSONArray lowercaseObj = (JSONArray) cleanSpecJson.get(JSON_KEY_LOWERCASE);
		if(lowercaseObj != null){
			for(Object jObj : (JSONArray) cleanSpecJson.get(JSON_KEY_LOWERCASE)){
				addToLowerCase((String)jObj);
			}
		}

		// add split specs
		JSONObject splitObj = (JSONObject) cleanSpecJson.get(JSON_KEY_SPLIT);
		if(splitObj != null){
			for(String s : (Set<String>)splitObj.keySet()){
				addSplit(s, (String)splitObj.get(s));
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

		// error if column does not exist
		if(headers.indexOf(columnHeader) == -1){
			throw new Exception("Cannot clean nonexistent column " + columnHeader);
		}

		// error if delimiter is unsupported
		if(ArrayUtils.indexOf(UNSUPPORTED_SPLIT_DELIMITERS, delimiter) > -1){			
			throw new Exception("Cannot yet support splitting on delimiter " + delimiter);
		}
		// error if there is already a split on this column
		if(columnsToSplit.get(columnHeader) != null){
			throw new Exception("Already splitting column using " + columnsToSplit.get(columnHeader) + ", cannot add another split");
		}

		// add the split
		columnsToSplit.put(columnHeader, delimiter);
	}

	/**
	 * Specify a column to change to lower case
	 * @param columnHeader (e.g. "personnel")
	 * @throws Exception 
	 */
	public void addToLowerCase(String columnHeader) throws Exception{

		// error if column does not exist
		if(headers.indexOf(columnHeader) == -1){
			throw new Exception("Cannot clean nonexistent column " + columnHeader);
		}

		// add the column
		columnsToLowerCase.add(columnHeader);
	}



	/**
	 * Clean each row and write to CSV
	 * @return number of clean rows produced (may exceed the number of input rows)
	 */
	public int cleanData() throws Exception{

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
		writer.close();
		dataset.close();	// TODO should these be elsewhere to guarantee closure?
		System.out.println("Processed " + numRowsProcessed + " records, produced " + numRowsProduced + " clean records. (DONE)");

		return numRowsProduced;  
	}


	/**
	 * Clean a single row of data, producing 1+ rows of cleaned data.
	 * @param row the row of data to clean
	 */
	private void cleanRow(ArrayList<String> row, ArrayList<String> headers) throws Exception{

		// check inputs
		if(row.size() != headers.size()){
			throw new Exception("Row does not have the same number of fields as the header list");
		}

		// perform to lower case
		row = performLowerCase(row);

		// perform splits
		ArrayList<ArrayList<String>> rowsToWrite = performSplits(row);

		// write to CSV
		for(ArrayList<String> rowToWrite : rowsToWrite){
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
	 * Take a row of data and perform splits, returning a set of rows.
	 * @param row the input row of data
	 * @return rows of data containing split fields
	 */
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

}
