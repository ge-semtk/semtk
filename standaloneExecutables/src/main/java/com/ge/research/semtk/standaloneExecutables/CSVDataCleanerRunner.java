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


package com.ge.research.semtk.standaloneExecutables;

import org.json.simple.JSONObject;

import com.ge.research.semtk.load.DataCleaner;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.utility.Utility;

/**
 * Cleans a CSV file and writes output to another CSV file
 * 
 * Sample command to execute in maven:
 * mvn exec:java -Dstart-class="com.ge.research.semtk.standaloneExecutables.CSVDataCleanerRunner" -Dexec.args="src/test/resources/datacleanertest-input.csv src/test/resources/datacleanertest-output.csv src/test/resources/datacleanertest-spec.json"
 */
public class CSVDataCleanerRunner {
	
	public static void main(String[] args) throws Exception{

		try{
		
			// get arguments
			if(args.length != 3){
				throw new Exception("Invalid argument list.  Usage: main(inputCsvFilePath, outputCsvFilePath, cleaningSpecJsonFilePath)");
			}
			String inputCsvFilePath = args[0];
			String outputCsvFilePath = args[1];
			String cleaningSpecJsonFilePath = args[2];
			
			// validate arguments
			if(!inputCsvFilePath.endsWith(".csv")){
				throw new Exception("Error: Data file " + inputCsvFilePath + " is not a CSV file");
			}
			if(!outputCsvFilePath.endsWith(".csv")){
				throw new Exception("Error: Data file " + outputCsvFilePath + " is not a CSV file");
			}
			if(!cleaningSpecJsonFilePath.endsWith(".json")){
				throw new Exception("Error: Template file " + cleaningSpecJsonFilePath + " is not a JSON file");
			}	
			
			System.out.println("--------- Clean CSV data... ---------------------------------------");
			System.out.println("Input CSV file:            " + inputCsvFilePath);
			System.out.println("Output CSV file:           " + outputCsvFilePath);
			System.out.println("Cleaning spec JSON file:   " + cleaningSpecJsonFilePath);
			
			// create the dataset
			Dataset dataset = null;
			try{
				dataset = new CSVDataset(inputCsvFilePath, false);
			}catch(Exception e){
				throw new Exception("Could not instantiate CSV dataset: " + e.getMessage());
			}
			
			// load the cleaning spec 
			JSONObject cleaningSpecJson;
			try{
				cleaningSpecJson = Utility.getJSONObjectFromFilePath(cleaningSpecJsonFilePath);
				System.out.println("Cleaning spec JSON:" + cleaningSpecJson.toString());
			}catch(Exception e){
				throw new Exception("Could not load cleaning spec: " + e.getMessage());
			}
					
			// clean the data
			try{			
				DataCleaner cleaner = new DataCleaner(dataset, outputCsvFilePath, cleaningSpecJson);
				int numCleanRecordsProduced = cleaner.cleanData();
				System.out.println("Wrote " + numCleanRecordsProduced + " cleaned records to " + outputCsvFilePath);				
			}catch(Exception e){
				e.printStackTrace();
				throw new Exception("Could not clean data: " + e.getMessage());
			}			
		
		}catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);  // need this to catch errors in the calling script
		}
		
	}
		
}
