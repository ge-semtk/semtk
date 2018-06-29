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

package com.ge.research.semtk.edc.resultsStorage;

import java.io.File;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Utilities to:
 * 1) store a table result set as a JSON file 
 * 2) retrieve results as CSV or JSON (possibly truncated)
 */
public class TableResultsStorage extends GeneralResultsStorage{

	public static enum TableResultsStorageTypes { JSON, CSV };
	 
	public TableResultsStorage(String file_location) {
		super(file_location);
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
	public void storeTableResultsJsonInitialize(String jobID, JSONObject resultsTableMetaData) throws Exception {	
		// create the results data file
		String dataFileName = writeToFile(jobID, null, true);
		
		// write it to file
		resultsTableMetaData.put(DATARESULTSFILELOCATION, dataFileName);
		
		// create and write the results metadata file
		writeToFile(jobID, resultsTableMetaData.toJSONString(), false);
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
		writeToFile(jobID, contents, true);
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
	public URL storeTableResultsJsonFinalize(String jobID) throws Exception {				
		String fileName = writeToFile(jobID, null, false);
		return getURL(fileName);
	}
	
	
	/**
	 * Get the full result set as json.
	 * @param metaFileShortUrl the url of the full json result
	 * @return byte array containing json result
	 */
	public TableResultsSerializer getJsonTable(URL metaFileShortUrl) throws Exception{
		return getTable(metaFileShortUrl, null, 0, TableResultsStorageTypes.JSON);
	}
	
	/**
	 * Get a subset of the result as json.
 	 * @param metaFileShortUrl the url of the full json result
	 * @param maxRows limit to this number of rows
	 * @return byte array containing json result
	 */
	public TableResultsSerializer getJsonTable(URL metaFileShortUrl, Integer maxRows, Integer startRow) throws Exception{
		return getTable(metaFileShortUrl, maxRows, startRow, TableResultsStorageTypes.JSON);
	}
	
	/**
	 * Get the full result set as csv.
	 * @param metaFileShortUrl the url of the full json result
	 * @return byte array containing csv result
	 */
	public TableResultsSerializer getCsvTable(URL metaFileShortUrl) throws Exception{
		return getTable(metaFileShortUrl, null, 0, TableResultsStorageTypes.CSV);
	}
	
	/**
	 * Get a subset of the result as csv. 
	 * @param metaFileShortUrl the url of the full json result
	 * @param maxRows limit to this number of rows
	 * @return byte array containing csv result
	 */
	public TableResultsSerializer getCsvTable(URL metaFileShortUrl, Integer maxRows, Integer startRow) throws Exception{
		return getTable(metaFileShortUrl, maxRows, startRow, TableResultsStorageTypes.CSV);
	}
	

	// TODO PERFORMANCE CONCERNS - what if the result set is huge
	/**
	 * For a given full json result table, generate CSV or JSON (possibly truncated)
	 * @param metaFileShortUrl the url of the full json result
	 * @param maxRows limit to this number of rows
	 * @return storageType indicates CSV or JSON
	 */
	private TableResultsSerializer getTable(URL metaFileShortUrl, Integer maxRows, Integer startRow, TableResultsStorageTypes storageType) throws Exception{
		
		try{
			String metaFilePath = urlToPath(metaFileShortUrl).toString();
			JSONObject jsonObj = Utility.getJSONObjectFromFilePath(metaFilePath);	// read json from url
			
			String dataFileLocation = (String) jsonObj.get(DATARESULTSFILELOCATION);
			
			if(storageType == TableResultsStorageTypes.CSV){
				return new TableResultsSerializer(jsonObj, dataFileLocation, TableResultsStorageTypes.CSV, maxRows, startRow);
				
			}else if(storageType == TableResultsStorageTypes.JSON){
				return new TableResultsSerializer(jsonObj, dataFileLocation, TableResultsStorageTypes.JSON, maxRows, startRow);
			}else{
				throw new Exception("Unrecognized TableResultsStorageTypes element: " + storageType);
			}		
			
		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			throw new Exception("Could not read results from store for " + metaFileShortUrl + ": " + e.toString());
		}
		
	}
	
	public void fullDelete(URL metaFileShortUrl) throws Exception {
		String metaFilePath = urlToPath(metaFileShortUrl).toString();
		JSONObject jsonObj = Utility.getJSONObjectFromFilePath(metaFilePath);
		String dataFilePath = (String) jsonObj.get(DATARESULTSFILELOCATION);
		
		if (dataFilePath != null) {
			File f = new File(dataFilePath);
			f.delete();
		}
		
		File f = new File(metaFilePath);
		f.delete();
	}

	/**
	  * Get the size of the results table.
	 * @throws Exception 
	  */
	public int getResultsRowCount(URL metaFileShortUrl) throws Exception{
		
		try{
			JSONObject jsonObj = Utility.getJSONObjectFromFilePath(urlToPath(metaFileShortUrl).toString());	// read json from url
			Long val = (Long) jsonObj.get(Table.JSON_KEY_ROW_COUNT);
			int retval = val.intValue();
			return retval;
		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			throw new Exception("parse of row count size failed!");
		}
	}
	
}   
