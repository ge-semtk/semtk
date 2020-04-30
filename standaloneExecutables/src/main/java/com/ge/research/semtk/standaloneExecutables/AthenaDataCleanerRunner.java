/**
 ** Copyright 2020 General Electric Company
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

import com.ge.research.semtk.edc.client.AthenaClient;
import com.ge.research.semtk.load.DataCleaner;
import com.ge.research.semtk.load.dataset.TableDataset;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;


/**
 * Cleans data from an Athena query and writes output to a CSV file
 * 
 * Sample command: 
 * java -cp standaloneExecutables-jar-with-dependencies.jar com.ge.research.semtk.standaloneExecutables.AthenaDataCleanerRunner http XX.XX.XX.XX 12062 athena/query myDatabase "select id from table where id = '3907'" cleaned-data.csv clean-spec.json
 * 
 */
public class AthenaDataCleanerRunner {
	
	/**
	 * Main method
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{
		
		try{
		
			// get arguments
			if(args.length != 8){
				throw new Exception("Wrong number of arguments.\nUsage: main(athenaServiceProtocol, athenaServiceHost, athenaServicePort, athenaServiceEndpoint, database, query, outputCsvFilePath, cleaningSpecJsonFilePath)");
			}
			String athenaServiceProtocol = args[0];				// protocol to connect to SemTK Athena Service
			String athenaServiceHost = args[1];					// host where SemTK Athena Service is running
			int athenaServicePort = Integer.valueOf(args[2]); 	// port where SemTK Athena Service is running
			String athenaServiceEndpoint = args[3];				// query endpoint for SemTK Athena Service  	(e.g. athena/query)
			String database = args[4];							// database name in Athena
			String query = args[5];								
			String outputCsvFilePath = args[6];
			String cleaningSpecJsonFilePath = args[7];
			
			// validate arguments
			if(!outputCsvFilePath.endsWith(".csv")){
				throw new Exception("Error: Data file " + outputCsvFilePath + " is not a CSV file");
			}
			if(!cleaningSpecJsonFilePath.endsWith(".json")){
				throw new Exception("Error: Template file " + cleaningSpecJsonFilePath + " is not a JSON file");
			}	

			LocalLogger.logToStdOut("--------- Clean Athena data... ---------------------------------------");
			LocalLogger.logToStdOut("Athena Service Protocol:     " + athenaServiceProtocol);
			LocalLogger.logToStdOut("Athena Service Host:     " + athenaServiceHost);
			LocalLogger.logToStdOut("Athena Service Port:     " + athenaServicePort);
			LocalLogger.logToStdOut("Athena Service Endpoint: " + athenaServiceEndpoint);
			LocalLogger.logToStdOut("Database:                " + database);
			LocalLogger.logToStdOut("Query:                   " + query);
			LocalLogger.logToStdOut("Output CSV file:         " + outputCsvFilePath);
			LocalLogger.logToStdOut("Cleaning spec JSON file: " + cleaningSpecJsonFilePath);
			
			// create the dataset
			TableDataset dataset = null;
			try{
				// get a Table from Athena
				LocalLogger.logToStdOut("Connecting to Athena...");
				AthenaClient athenaClient = new AthenaClient(athenaServiceProtocol, athenaServiceHost, athenaServicePort, athenaServiceEndpoint, database);
				TableResultSet tableResultSet = athenaClient.execute(query);
				dataset = new TableDataset(tableResultSet.getTable());
			}catch(Exception e){
				throw new Exception("Could not instantiate Athena dataset: " + e.getMessage());
			}
			
			// load the cleaning spec 
			JSONObject cleaningSpecJson;
			try{
				cleaningSpecJson = Utility.getJSONObjectFromFilePath(cleaningSpecJsonFilePath);
				LocalLogger.logToStdOut("Cleaning spec JSON:" + cleaningSpecJson.toString());
			}catch(Exception e){
				throw new Exception("Could not load cleaning spec: " + e.getMessage());
			}
					
			// clean the data
			try{			
				DataCleaner cleaner = new DataCleaner(dataset, outputCsvFilePath, cleaningSpecJson);
				int numCleanRecordsProduced = cleaner.cleanData();
				LocalLogger.logToStdOut("Wrote " + numCleanRecordsProduced + " cleaned records to " + outputCsvFilePath);				
			}catch(Exception e){
				LocalLogger.printStackTrace(e);
				throw new Exception("Could not clean data: " + e.getMessage());				
			}	
			
			System.exit(0);  // other DataCleanerRunners needed this - keep just in case
	
		}catch(Exception e){
			LocalLogger.logToStdOut(e.getMessage());
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
		
	}
			
}
