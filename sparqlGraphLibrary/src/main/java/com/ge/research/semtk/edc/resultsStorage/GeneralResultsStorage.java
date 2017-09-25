/**
 ** Copyright 2017 General Electric Company
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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public abstract class GeneralResultsStorage {
	protected String fileLocation = null;
	
	protected static final String DATARESULTSFILELOCATION = "ResultsDataLocation";
	
	public GeneralResultsStorage(String fileLocation){
		this.fileLocation = fileLocation;
	}
	
	/**
	 * Write line(s) of data to the results file for a given job id.
	 * @param jobID the job id
	 * @param contents the data to write
	 * @param writeToResultsFile "true" if writing to the results data, "false" for writing to the metadata file
	 * @return the file name
	 */
	protected String writeToFile(String jobID, String contents, Boolean writeToResultsFile) throws Exception {
		
		if(contents == null){ contents = ""; }
		else{ contents += "\n"; }
		
		String filename = "results_" + jobID;
		
		if(writeToResultsFile){ filename = filename + "_data.dat";}
		else{ filename = filename + "_metadata.json"; }
		
		Path path = Paths.get(fileLocation, filename);
		
		try {
			Files.write(path, contents.getBytes(), StandardOpenOption.APPEND);
		} catch(NoSuchFileException nosuchfile){
			
			
			if(contents == null ||  contents.isEmpty()){
				
				(path.toFile()).createNewFile();
			}
			else{
				Files.write(path, contents.getBytes(), StandardOpenOption.CREATE);
				
			}
			
			
		}
		if(!writeToResultsFile){ return filename; }
		else{ return path.toString(); }
	}
	
	/**
	 * Get a URL for a given file name
	 */
	protected URL getURL(String filename) throws MalformedURLException {
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
	protected Path urlToPath(URL url) throws Exception {
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
