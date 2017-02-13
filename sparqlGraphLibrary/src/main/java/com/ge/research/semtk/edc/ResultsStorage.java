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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.UUID;

import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.resultSet.Table;


/*
 * Handles storage of file types on local storage and mapping to URL
 * Splits stored files into sample and full files
 */
public class ResultsStorage {
	
	private URL baseURL = null;
	private String fileLocation = null;
	
	public ResultsStorage(URL base_URL, String file_location) {
		this.baseURL = base_URL;
		this.fileLocation = file_location;
	}
	
	/**
	 * Store a table twice: as a full CSV and a sample JSON
	 * @param tab
	 * @param sampleLines
	 * @return  URL[] = {sampleURL, fullURL}
	 * @throws Exception
	 */
	public URL[] storeTable(Table tab, int sampleLines) throws Exception {
		// store full csv file
		String fullname = storeFile(tab.toCSVString(), "csv");
		
		// store truncated json
		Table truncated = Table.fromJson(tab.toJson());
		truncated.truncate(sampleLines);
		
		String samplename = storeFile(truncated.toJson().toString(), "json");
		
		// return
		URL[] ret = { getURL(samplename), getURL(fullname) };
		return ret;
	}
	
	/**
	 * Stores a csv file twice:  full and truncated to sampleRows
	 * @param contents
	 * @return URL[] = {sampleURL, fullURL}
	 * @throws Exception
	 */
	public URL[] storeCsvFile(String contents, int sampleRows) throws Exception {
		
		String fullname = storeFile(contents, "csv");
		
		String samplename = storeTruncatedFile(contents, sampleRows + 1, "csv");
		
		URL[] ret = { getURL(samplename), getURL(fullname) };
		return ret;
	}

	/**
	 * Store full csv file, and sample as json.
	 */
	public URL[] storeCsvFileIncremental(String contents, int sampleRows, String jobID, int segment) throws Exception {
		
		Boolean newSample = true;
		if(segment > 0){ newSample = false; }
		
		String fullname = storeFileIncrement(jobID, contents, "csv");
		
		String samplename = storeTruncatedFileIncrementalJson(jobID, contents, sampleRows + 1, "json", newSample);
		
		URL[] ret = { getURL(samplename), getURL(fullname) };
		return ret;
	}
	
	
	/**
	 * Store any file with no sample file, no format changing, etc.
	 * @param contents
	 * @param extension
	 * @return URL
	 * @throws Exception
	 */
	public URL storeSingleFile(String contents, String extension) throws Exception {
		return getURL(storeFile(contents, extension));
	}
	
	/**
	 * Truncate a text file and store it
	 * @param contents
	 * @param sampleLines
	 * @param fileExtension
	 * @return filename fragment
	 * @throws Exception
	 */
	private String storeTruncatedFile(String contents, int sampleLines, String fileExtension) throws Exception {
		String truncated = "";
		BufferedReader reader = new BufferedReader(new StringReader(contents));
		String line;
		int rows = 0;
		
		while ((line = reader.readLine()) != null && rows < sampleLines) {
			truncated += line + "\n";
			rows += 1;
		}
		
		return storeFile(truncated, fileExtension);
	}


	private String storeTruncatedFileIncrementalJson(String jobID, String contents, int sampleLines, String fileExtension, Boolean generateNewSample) throws Exception {
		
		// make jobID contain a "_sample" tag so that it does not have the same name as the regular result:
		jobID = jobID + "_sample";
		
		if(generateNewSample){
			
			// assumptions: 
			// 1. the first row (as in up to the first newline) are column headers.
			// 2. the rest of the set is remaining rows.
			
			Dataset ds = new CSVDataset(contents, true);
		
			String[] headers = ds.getColumnNamesinOrder().toArray(new String[ds.getColumnNamesinOrder().size()]);
			
			// set up some dummy column types. for convenience, they are all strings. 
			String[] headerTypes = new String[headers.length];
			for(int colnum = 0; colnum < headers.length; colnum += 1){ 
				headerTypes[colnum] = "string";
			}
			
			ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();
			
			while(true && temp.size() < sampleLines){
				try{
					ArrayList<ArrayList<String>> curr = ds.getNextRecords(1);
					for(int y = 0; y < curr.size(); y += 1){
						temp.add(curr.get(y));
					}
					if(curr.size() == 0){ break; }
				}
				catch(Exception e){
					break;
				}
				
			}
			
			// build a table to store results.
			Table tbl = new Table(headers, headerTypes, temp);
			String sampleSet = tbl.toJson().toString();
			
			return storeFileIncrement(jobID, sampleSet, fileExtension);
		}
		else
		{
			return ("results_" + jobID + "." + fileExtension);
		}
	}

	
	/**
	 * store a file in this.fileLocation
	 * @param contents
	 * @param fileExtension
	 * @return the file name fragment (not the whole path)
	 * @throws Exception
	 */
	private String storeFile(String contents, String fileExtension) throws Exception {
		
		String filename = "results_" + UUID.randomUUID().toString() + "." + fileExtension;
		Path path = Paths.get(fileLocation, filename);
		File fullFile = path.toFile();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(fullFile));
		try {
			writer.write(contents);
		} finally {
			writer.close();
		}
		
		return filename;
	}

	private String storeFileIncrement(String jobID, String contents, String fileExtension) throws Exception {
		
		String filename = "results_" + jobID + "." + fileExtension;
		Path path = Paths.get(fileLocation, filename);
		//File fullFile = path.toFile();
		
		try {
			  Files.write(path, contents.getBytes(), StandardOpenOption.APPEND);
		}
		catch(NoSuchFileException nosuchfile){
			Files.write(path, contents.getBytes(), StandardOpenOption.CREATE);
			  
		} finally {
	
		}
		
		return filename;
	}
	
	/**
	 * Use filesystem to read contents of a url generated here
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public String getUrlContentsFromFile(URL url) throws Exception {
		// change URL back to a local path
		Path path = urlToPath(url);
	    byte [] contents = Files.readAllBytes(path);
	    return new String(contents);
	}
	
	/**
	 * Delete file if it exists, given a URL
	 * @param url
	 * @throws Exception if url wasn't generated by a ResultsStorage configured like this one
	 */
	public void deleteStoredFile(URL url) throws Exception {
		Path path = urlToPath(url);
	    Files.deleteIfExists(path);
	}
	
	/**
	 * Turns a url into a filename
	 * @param url
	 * @return
	 * @throws Exception if the url didn't come from this ResultsStorage
	 */
	private Path urlToPath(URL url) throws Exception {
		if (! url.toString().startsWith(this.baseURL.toString())) {
			throw new Exception (String.format("Invalid URL wasn't created by this ResultsStorage: %s", url.toString()));
		}
		String fullURL = url.toString();
		return Paths.get(this.fileLocation, 
						 fullURL.substring(  this.baseURL.toString().length() + 1, 
								             fullURL.length()                     ));
	}
	
	private URL getURL(String filename) throws MalformedURLException {
		return new URL(baseURL + "/" + filename);
	}
	
}
