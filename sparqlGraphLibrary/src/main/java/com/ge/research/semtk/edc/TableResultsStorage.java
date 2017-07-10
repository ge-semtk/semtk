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

package com.ge.research.semtk.edc;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.Utility;

/**
 * Utilities to:
 * 1) store a table result set as a JSON file 
 * 2) retrieve results as CSV or JSON (possibly truncated)
 */
public class TableResultsStorage {

	public static enum TableResultsStorageTypes { JSON, CSV };
	
	private String fileLocation = null;
	
	public TableResultsStorage(String file_location) {
		this.fileLocation = file_location;
	}

	/**
	 * Call 1 of 3 to store table result as JSON
	 * Write table metadata (column names, column types, column count) to json file.  Sample:
	 * {
	 * "col_names":["colA","colB","colC"],
	 *	"col_type":["string","string","string"],
	 *	"col_count":3,
	 *	"rows":[
	 * 
	 * @param jobID the job id
	 * @param columnNames table column names
	 * @param columnTypes table column types
	 * @throws Exception
	 */
	public void storeTableResultsJsonInitialize(String jobID, String[] columnNames, String[] columnTypes) throws Exception {	
		StringBuffer buffer = new StringBuffer();
		// open json
		buffer.append("{");
		// column names
		buffer.append("\"").append(Table.JSON_KEY_COL_NAMES).append("\":[");
		for(int i = 0; i < columnNames.length; i++){
			buffer.append("\"").append(columnNames[i]).append("\"");
			if(i < columnNames.length - 1){ buffer.append(","); }
		}
		buffer.append("],");
		// column types
		buffer.append("\"").append(Table.JSON_KEY_COL_TYPES).append("\":[");
		for(int i = 0; i < columnTypes.length; i++){
			buffer.append("\"").append(columnTypes[i]).append("\"");
			if(i < columnTypes.length - 1){ buffer.append(","); }
		}
		buffer.append("],");
		// column count
		buffer.append("\"").append(Table.JSON_KEY_COL_COUNT).append("\":").append(columnNames.length).append(",");
		// opener for rows
		buffer.append("\"").append(Table.JSON_KEY_ROWS).append("\":[");
		
		// write it to file
		writeToFile(jobID, buffer.toString());
	}
	
	/**
	 * Call 2 of 3 to store table result as JSON
	 * Write table rows to json file. 
	 * Each row has quoted elements, no spaces after delimiter commas, and enclosing brackets.
	 * Each row will be followed by a comma, except for the last row of the last segment.
	 * 
	 * ["a1","b1","c1"],
	 * ["a2","b2","c2"],
	 *
	 * @param jobID the job id
	 * @param contents a string containing a set of rows, formatted as above
	 * @throws Exception
	 */
	public void storeTableResultsJsonAddIncremental(String jobID, String contents) throws Exception {				
		writeToFile(jobID, contents);
	}
	
	/**
	 * Call 3 of 3 to store table result as JSON
	 * Write table metadata (row count) to json file.  Generate URL and return it.  Sample:
	 * ]
	 * "row_count":10
	 * }
	 *
	 * @param jobID the job id
	 * @param rowCount the number of rows written
	 */
	public URL storeTableResultsJsonFinalize(String jobID, int rowCount) throws Exception {				
		StringBuffer buffer = new StringBuffer();
		// closer for rows
		buffer.append("],");
		// row count
		buffer.append("\"").append(Table.JSON_KEY_ROW_COUNT).append("\":").append(rowCount);
		// close json
		buffer.append("}");
		
		// write it to file
		String fileName = writeToFile(jobID, buffer.toString());
		return getURL(fileName);
	}
	
	
	/**
	 * Get the full result set as json.
	 * @param url the url of the full json result
	 * @return byte array containing json result
	 */
	public byte[] getJsonTable(URL url) throws Exception{
		return getJsonTable(url, null);
	}
	
	/**
	 * Get a subset of the result as json.
 	 * @param url the url of the full json result
	 * @param maxRows limit to this number of rows
	 * @return byte array containing json result
	 */
	public byte[] getJsonTable(URL url, Integer maxRows) throws Exception{
		return getTable(url, maxRows, TableResultsStorageTypes.JSON);
	}
	
	/**
	 * Get the full result set as csv.
	 * @param url the url of the full json result
	 * @return byte array containing csv result
	 */
	public byte[] getCsvTable(URL url) throws Exception{
		return getCsvTable(url, null);
	}
	
	/**
	 * Get a subset of the result as csv. 
	 * @param url the url of the full json result
	 * @param maxRows limit to this number of rows
	 * @return byte array containing csv result
	 */
	public byte[] getCsvTable(URL url, Integer maxRows) throws Exception{
		return getTable(url, maxRows, TableResultsStorageTypes.CSV);
	}
	

	// TODO PERFORMANCE CONCERNS - what if the result set is huge
	/**
	 * For a given full json result table, generate CSV or JSON (possibly truncated)
	 * @param url the url of the full json result
	 * @param maxRows limit to this number of rows
	 * @return storageType indicates CSV or JSON
	 */
	private byte[] getTable(URL url, Integer maxRows, TableResultsStorageTypes storageType) throws Exception{
				
		System.out.println("** TableResultsStorage.getTable()");  // TODO DELETE THIS
		
		System.out.println("** Get JSON object from file...");  // TODO DELETE THIS
		Table table;
		try{
			JSONObject jsonObj = Utility.getJSONObjectFromFilePath(urlToPath(url).toString());	// read json from url
			System.out.println("** Got json");
			table = Table.fromJson(jsonObj);	
		}catch(Exception e){
			e.printStackTrace();
			throw new Exception("Could not read results from store for " + url + ": " + e.toString());
		}
		
		System.out.println("** Truncate the table...");  // TODO DELETE THIS
		// truncate the table 
		if(maxRows != null){
			table.truncate(maxRows.intValue());
		}
		
		System.out.println("** Return as a JSON or CSV string...");  // TODO DELETE THIS
		// return byte array in requested format
		if(storageType == TableResultsStorageTypes.CSV){
			return table.toCSVString().getBytes();
		}else if(storageType == TableResultsStorageTypes.JSON){
			return table.toJson().toString().getBytes();
		}else{
			throw new Exception("Unrecognized TableResultsStorageTypes element: " + storageType);
		}		
	}
	
		
	/**
	 * Write line(s) of data to the results file for a given job id.
	 * @param jobID the job id
	 * @param contents the data to write
	 * @return the file name
	 */
	private String writeToFile(String jobID, String contents) throws Exception {
		
		String filename = "results_" + jobID + ".json";
		Path path = Paths.get(fileLocation, filename);
		
		try {
			Files.write(path, contents.getBytes(), StandardOpenOption.APPEND);
		} catch(NoSuchFileException nosuchfile){
			Files.write(path, contents.getBytes(), StandardOpenOption.CREATE);
		} 		
		return filename;
	}
	
	/**
	 * Get a URL for a given file name
	 */
	private URL getURL(String filename) throws MalformedURLException {
		return new URL("file:////" +  filename);
	}
	
	/**
	 * Delete a file if it exists
	 */
	public void deleteStoredFile(URL url) throws Exception {
	    Files.deleteIfExists(urlToPath(url));
	}
		
	/**
	 * Gets a file path for a given URL
	 * @throws Exception if the url didn't come from this ResultsStorage
	 */
	private Path urlToPath(URL url) throws Exception {
	//	if (! url.toString().startsWith("file:////" + this.fileLocation.toString())) {
	//		throw new Exception (String.format("Invalid URL wasn't created by this ResultsStorage: %s", url.toString()));
	//	}
		String fullURL = url.toString();
	//	return Paths.get(this.fileLocation, fullURL.substring( this.baseURL.toString().length() + 1, fullURL.length()));
		
		if(fullURL.contains("file:")){
			fullURL = fullURL.substring(5, fullURL.length());
		}
		
		return Paths.get(this.fileLocation, fullURL);
		
	}
	
}   
