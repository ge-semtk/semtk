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


package com.ge.research.semtk.load.dataset;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.dataset.Dataset;

/*
 * Load and read a CSV data file.
 */
public class CSVDataset extends Dataset {

	private String csvPath;						// will have either path to a CSV file OR a string containing CSV
	private String csvString;					// will have either path to a CSV file OR a string containing CSV
	private Iterator<CSVRecord> recordIterator;	// iterator of CSV records
	private String[] headers;					// ordered list of headers to return
	
	/**
	 * Constructor that takes a path to a CSV file
	 * 
	 * NOTE: if want to only specify a path without headers, then use constructor below with isFileContent=FALSE
	 */
	public CSVDataset(String path, String[] headers) throws Exception{
		initialize(path, headers);
	}
	
	/**
	 * Constructor that takes a string (either a file path or file content)
	 * 
	 * @param filePathOrContent file path, or file content
	 * @param isFileContent true for file contents, false for file path
	 */
	public CSVDataset(String filePathOrContent, boolean isFileContent) throws Exception {
		
		if(isFileContent){
			this.csvString = filePathOrContent;	
		}else{
			this.csvString = FileUtils.readFileToString(new File(filePathOrContent));
		}
		CSVParser parser = getParser(new StringReader(this.csvString));
		this.recordIterator = parser.iterator();
		
		// get and set the header info
		Map<String, Integer> headerMap = parser.getHeaderMap();		
		this.headers = new String[headerMap.size()];		
		for (String s : headerMap.keySet()) {
			int location = headerMap.get(s);			
			this.headers[location] = s;
		}		
	}
	
	/**
	 * Constructor that takes a JSON object describing a CSV location
	 */
	protected void fromJSON(JSONObject config) throws Exception{
		// read inputs from JSON...and then initialize
		String path = config.get("File").toString();
		String[] headers = (String[]) config.get("Headers");
		initialize(path, headers);
	}
	
	
	/**
	 * Initialize
	 * @param path the CSV file path
	 * @param headers the headers needed for this dataset
	 * @throws Exception
	 */
	private void initialize(String path, String[] headers) throws Exception {
		this.csvPath = path;
		CSVParser parser = getParser(new FileReader(path));
		this.recordIterator = parser.iterator();
		this.headers = headers;				
		
		// confirm that headers passed in are available in the CSVParser (case-insensitive)
		boolean found;
		for(String header: headers){
			found = false;
			Set<String> parserHeaders = parser.getHeaderMap().keySet();
			for(String parserHeader: parserHeaders){
				if(parserHeader.equalsIgnoreCase(header)){
					found = true;
					break;
				}
			}
			if(!found){
				throw new Exception("Header '" + header + "' not found in CSV file");
			}
		}
		
	}
	
	private CSVParser getParser(Reader reader) throws Exception{
		return (CSVFormat.EXCEL.withHeader().withIgnoreHeaderCase(true).parse(reader));
	}
	
	@Override
	/**
	 * Read the next set of rows from the CSV file
	 */
	public ArrayList<ArrayList<String>> getNextRecords(int numRecords) throws Exception {
		if(headers == null){
			throw new Exception("Dataset headers are not available");
		}
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		CSVRecord record;
		
		for(int i = 0; i < numRecords; i++){ // read the specified number of records
			try{
				ArrayList<String> currRow = new ArrayList<String>();
				record = this.recordIterator.next();
                if(record.size() == 1 && record.get(0).trim().isEmpty()) {
                	continue;  // this is an empty line, skip it
                }
				
				for(int j = 0; j < headers.length; j++){
					// add the next entry to the list. 
					currRow.add(record.get(headers[j]) );
				}
				rows.add(currRow);
			}catch(NoSuchElementException e){
				break; // got to the end of the file
			}
		}		
		return rows;
	}
	
	/**
	 * Get the column names in order
	 * @return an arraylist of column names
	 */
	@Override
	public ArrayList<String> getColumnNamesinOrder() throws Exception {
		ArrayList<String> retval = new ArrayList<String>();
		for(String curr : this.headers){
			retval.add(curr.toLowerCase());
		}
		return retval;
	}
	
	/**
	 * Reset the dataset to the first record
	 */
	@Override
	public void reset() throws Exception {
		if(csvPath != null){
			this.recordIterator = getParser(new FileReader(this.csvPath)).iterator();
		}else if(csvString != null){
			this.recordIterator = getParser(new StringReader(this.csvString)).iterator();
		}else{
			throw new Exception("No CSV path or content available");
		}
	}	
	
	/**
	 * Close the dataset
	 */
	public void close(){
		// no action needed
	}

}
