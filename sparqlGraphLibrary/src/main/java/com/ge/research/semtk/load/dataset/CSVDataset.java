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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.utility.LocalLogger;

/*
 * Load and read a CSV data file.
 */
public class CSVDataset extends Dataset {

	private String csvPath;						// will have either path to a CSV file OR a string containing CSV
	private String csvString;					// will have either path to a CSV file OR a string containing CSV
	private Iterator<CSVRecord> recordIterator;	// iterator of CSV records
	private String[] headers;					// ordered list of headers to return
	
	private CSVParser parser;
	private Reader reader;
	
	/**
	 * Constructor that takes a path to a CSV file
	 * 
	 * NOTE: if want to only specify a path without headers, then use constructor below with isFileContent=FALSE
	 */
	public CSVDataset(String path, String[] headers) throws Exception{
		
		// strip the potential Byte Order Marker from the first header before moving on... 
		if(headers[0].charAt(0) == 65279){
			// log this specific alteration:
			LocalLogger.logToStdErr("first header is led by a Byte Order Marker. it has been removed to prevent issues in parsing.");
			String newFirstHeader = headers[0].substring(1);
			headers[0] = newFirstHeader;
		}
		
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
			// check for a BOM at the start. if it is there, remove it.
			if(filePathOrContent.charAt(0) == 65279){ 
				LocalLogger.logToStdErr("first header is led by a Byte Order Marker. it has been removed to prevent issues in parsing.");
				filePathOrContent = filePathOrContent.substring(1); 
			}
			
			this.csvString = filePathOrContent;	
		}else{
			this.csvString = FileUtils.readFileToString(new File(filePathOrContent));
			if(this.csvString.charAt(0) == 65279){
				LocalLogger.logToStdErr("first header is led by a Byte Order Marker. it has been removed to prevent issues in parsing.");
				this.csvString = this.csvString.substring(1);
			}
		}
		this.reader = new StringReader(this.csvString);
		this.parser = getParser();
		this.recordIterator = parser.iterator();
		
		// get and set the headr info
		Map<String, Integer> headerMap = parser.getHeaderMap();		
		this.headers = new String[headerMap.size()];	
		
		// TODO: this test was causing other problems so it was removed.
		if (false) {
			throw new Exception("Duplicate or empty column headers on CSV file");
		}
		
		String ss = "";
		try {
			for (String s : headerMap.keySet()) {
				ss = s;
				int location = headerMap.get(s);		
				this.headers[location] = s;
			}	
		} catch (Exception e) {
			throw new Exception((ss.isEmpty() ? "Empty CSV column header." : "Problem reading CSV headers."), e);
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
		this.reader = new FileReader(path);
		this.parser = getParser();
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
		
		// print all the headers we find
		Set<String> parserHeaders = parser.getHeaderMap().keySet();
	}
	
	/**
	 * Get the parser that reads in ingestion csv's
	 * @return
	 * @throws Exception
	 */
	private CSVParser getParser() throws Exception{
		// return (CSVFormat.EXCEL.withHeader().withIgnoreHeaderCase(true).parse(reader));
		return (CSVFormat.EXCEL.withHeader()
				.withIgnoreHeaderCase(true)
		//		.withQuote('"')       
		//		.withEscape('\\')       // this changed \n to actual line returns and \t to actual tabs.  Not normal excel/CSV behavior.
				.withIgnoreEmptyLines(true)
				.withIgnoreSurroundingSpaces(true)
				.parse(reader)); // changed toward handling quotes in stream. the were breaking
																										// solution suggestion: http://stackoverflow.com/questions/26729799/invalid-char-between-encapsulated-token-and-delimiter-in-apache-commons-csv-libr
	}
	
	@Override
	/**
	 * Read the next set of rows from the CSV file
	 * Typically used when ingesting a CSV
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
                	LocalLogger.logToStdOut("Empty CSV row, continuing...");
                	continue;  // this is an empty line, skip it
                }
				
				for(int j = 0; j < headers.length; j++){
					// add the next entry to the list. 
					try{
						
						currRow.add(record.get(headers[j])
								.replaceAll("\"\"", "\"")  // CSVParser is leaving "" for some reason.  unescapeCsv() didn't help.
								);
					}
					catch( Exception eee){
						LocalLogger.logToStdOut("exception getting data for header");
					}
				}
				rows.add(currRow);
			}catch(NoSuchElementException e){
//				LocalLogger.logToStdOut("ran into an exception for a missing element when getting records.... out of rows.");
				break; // got to the end of the file
			}
		}	
		
		// what is the count of Rows we want to return?
//		LocalLogger.logToStdOut("number of CSV rows returned this run: " + rows.size());
		
		return rows;
	}
	
	/**
	 * Get the index of a column
	 * @return the index, or -1 if the column does not exist
	 */
	@Override
	public int getColumnIndex(String columnName) throws Exception {
		String colNameLower = columnName.toLowerCase().trim();
		return getColumnNamesinOrder().indexOf(colNameLower);
	}
	
	/**
	 * Get the column names in order.
	 * This class' column names are trimmed lower case
	 * @return an arraylist of column names
	 */
	@Override
	public ArrayList<String> getColumnNamesinOrder() throws Exception {
		ArrayList<String> retval = new ArrayList<String>();
		for(String curr : this.headers){
			retval.add(curr.toLowerCase().trim());
		}
		return retval;
	}
	
	/**
	 * Reset the dataset to the first record
	 */
	@Override
	public void reset() throws Exception {
		if(this.reader != null){
			this.reader.close();  // need this
		}
		if(csvPath != null){
			this.reader = new FileReader(this.csvPath);
		}else if(csvString != null){
			this.reader = new StringReader(this.csvString);
		}else{
			throw new Exception("No CSV path or content available");
		}
		this.parser = getParser();
		this.recordIterator = parser.iterator();
	}	
	
	/**
	 * Close the dataset
	 */
	public void close() throws Exception{
		if(this.parser != null) {
			this.parser.close();
		}
		if(this.reader != null){
			this.reader.close();
		}
	}

}
