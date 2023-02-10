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
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.dataset.ODBCDataset;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Cleans data from an ODBC query and writes output to a CSV file
 *
 * Sample command to execute in maven, against Hive:
 * mvn exec:java -Dstart-class="com.ge.research.semtk.standaloneExecutables.ODBCDataCleanerRunner" -Dexec.args="org.apache.hive.jdbc.HiveDriver jdbc:hive2://server:10000/db USER PW 'select col1, col2 from table' path/to/datacleanertest-output-odbc.csv path/to/datacleanertest-spec-odbc-hive.json"
 * 
 */
public class ODBCDataCleanerRunner {
	
	/**
	 * Main method
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{
		
		try{
		
			// get arguments
			if(args.length != 7){
				throw new Exception("Wrong number of arguments.\nUsage: main(dbDriver, dbUrl, dbUser, dbPassword, dbQuery, outputCsvFilePath, cleaningSpecJsonFilePath)");
			}
			String dbDriver = args[0];
			String dbUrl = args[1]; 
			String dbUser = args[2];
			String dbPassword = args[3];
			String dbQuery = args[4];	
			String outputCsvFilePath = args[5];
			String cleaningSpecJsonFilePath = args[6];
			
			// validate arguments
			if(!outputCsvFilePath.endsWith(".csv")){
				throw new Exception("Error: Data file " + outputCsvFilePath + " is not a CSV file");
			}
			if(!cleaningSpecJsonFilePath.endsWith(".json")){
				throw new Exception("Error: Template file " + cleaningSpecJsonFilePath + " is not a JSON file");
			}	
			
			LocalLogger.logToStdOut("--------- Clean ODBC data... ---------------------------------------");
			LocalLogger.logToStdOut("Database driver:   		   " + dbDriver);
			LocalLogger.logToStdOut("Database URL:      		   " + dbUrl);
			LocalLogger.logToStdOut("User:          		   	   " + dbUser);
			LocalLogger.logToStdOut("Query:          	   	   " + dbQuery);
			LocalLogger.logToStdOut("Output CSV file:           " + outputCsvFilePath);
			LocalLogger.logToStdOut("Cleaning spec JSON file:   " + cleaningSpecJsonFilePath);
			
			// create the dataset
			Dataset dataset = null;
			try{
				dataset = new ODBCDataset(dbDriver, dbUrl, dbUser, dbPassword, dbQuery);
			}catch(Exception e){
				throw new Exception("Could not instantiate ODBC dataset: " + e.toString());
			}
			
			// load the cleaning spec 
			JSONObject cleaningSpecJson;
			try{
				cleaningSpecJson = Utility.getJSONObjectFromFilePath(cleaningSpecJsonFilePath);
				LocalLogger.logToStdOut("Cleaning spec JSON:" + cleaningSpecJson.toString());
			}catch(Exception e){
				throw new Exception("Could not load cleaning spec: " + e.toString());
			}
					
			// clean the data
			try{			
				DataCleaner cleaner = new DataCleaner(dataset, outputCsvFilePath, cleaningSpecJson);
				int numCleanRecordsProduced = cleaner.cleanData();
				LocalLogger.logToStdOut("Wrote " + numCleanRecordsProduced + " cleaned records to " + outputCsvFilePath);				
			}catch(Exception e){
				LocalLogger.printStackTrace(e);
				throw new Exception("Could not clean data: " + e.toString());
			}	
			
			System.exit(0);  // need this to prevent "thread(s) did not finish despite being asked to  via interruption"
	
		}catch(Exception e){
			LocalLogger.logToStdOut(e.toString());
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
		
	}
			
}
