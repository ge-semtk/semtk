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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.load.utility.Utility;
import com.ge.research.semtk.logging.DetailsTuple;
import com.ge.research.semtk.logging.easyLogger.LoggerClientConfig;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;

/**
 * Loads data to a triple store from a CSV file into database.
 * 
 * Sample command to execute in maven:
 * mvn exec:java -Dstart-class="com.ge.research.semtk.standaloneExecutables.CSVDataLoaderRunner" -Dexec.args="src/main/resources/test.json virtuosoUser virtuosoPassword src/test/resources/test.csv 10"
 *
 */
public class CSVDataLoaderRunner {
	
	String loggerProtocol = null;
	String loggerServer = null;
	String loggerPort = null;
	String loggerAPILocation = null;
	LoggerRestClient lg = null;
		
	/**
	 * Main method
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{
		
		try{
		
			// get arguments
			if(args.length != 5){
				throw new Exception("Invalid argument list.  Usage: main(templateJSONFilePath, sparqlEndpointUser, sparqlEndpointPassword, dataCSVFilePath, batchSize)");
			}
			String templateJSONFilePath = args[0];
			String sparqlEndpointUser = args[1];
			String sparqlEndpointPassword = args[2];
			String dataCSVFilePath = args[3];	
			String batchSize = args[4];
			
			// validate arguments
			if(!templateJSONFilePath.endsWith(".json")){
				throw new Exception("Error: Template file " + templateJSONFilePath + " is not a JSON file");
			}
			if(!dataCSVFilePath.endsWith(".csv")){
				throw new Exception("Error: Data file " + dataCSVFilePath + " is not a CSV file");
			}
			
			try{
				int batchSizeInt = Integer.parseInt(batchSize);
				if(batchSizeInt < 1 || batchSizeInt > 100){
					throw (new Exception(""));
				}
			}catch(Exception e){
				throw new Exception("Error: Invalid batch size: " + batchSize);
			}
					
			System.out.println("--------- Load data from CSV... ---------------------------------------");
			System.out.println("Template:   " + templateJSONFilePath);
			System.out.println("CSV file:   " + dataCSVFilePath);
			System.out.println("Batch size: " + batchSize);	
			
			try{
				new CSVDataLoaderRunner().loadData(templateJSONFilePath, sparqlEndpointUser, sparqlEndpointPassword, dataCSVFilePath, Integer.parseInt(batchSize));
			}catch(Exception e){
				e.printStackTrace();
				System.exit(1); // explicitly exit to avoid maven exec:java error ("thead was interrupted but is still alive")
			}
			System.exit(0);  // explicitly exit to avoid maven exec:java error ("thead was interrupted but is still alive")
		
		}catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);  // need this to catch errors in the calling script
		}
	}
	

	public CSVDataLoaderRunner(){

		// get the config for logging stuff needed from the config file on the classpath.
		try {
			InputStream in = this.getClass().getClassLoader().getResourceAsStream("LoaderConfig.conf");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
	          
			// read the input from the config location.
			String line = reader.readLine();
            while(line != null){
            
            	// just set up the correct value.
            	String[] currKV = line.split(":");
            	
            	if(currKV[0].equalsIgnoreCase("loggerProtocol")) { this.loggerProtocol = currKV[1]; }
            	else if(currKV[0].equalsIgnoreCase("loggerServer")) { this.loggerServer = currKV[1]; }
            	else if(currKV[0].equalsIgnoreCase("loggerPort")) { this.loggerPort = currKV[1]; }
            	else if(currKV[0].equalsIgnoreCase("loggerAPILocation")) { this.loggerAPILocation = currKV[1]; }
            	else{
            		//huh? that's new
            	}
            	
                line = reader.readLine();
            }      

			if(this.loggerAPILocation != null && this.loggerPort != null && this.loggerProtocol != null && this.loggerServer != null){
				lg = new LoggerRestClient(new LoggerClientConfig("CSVDataLoaderStandalone", loggerProtocol, loggerServer, Integer.parseInt(loggerPort), loggerAPILocation));
			}
		}
		catch(Exception eee){
			System.err.println("logger initialization failed.");
		}
	}
	
	
	/**
	 * Load the data
	 * @param templateJSONFilePath the path to the JSON file containing the template
	 * @param sparqlEndpointUser username for SPARQL endpoint
	 * @param sparqlEndpointPassword password for SPARQL endpoint
	 * @param dbHost dataCSVFilePath the path to the CSV data file
	 * @throws Exception
	 */
	private void loadData(String templateJSONFilePath, String sparqlEndpointUser, String sparqlEndpointPassword, String dataCSVFilePath, int batchSize) throws Exception{
		
		// load the JSON template
		JSONObject templateJSON = Utility.getJSONObjectFromFilePath(templateJSONFilePath);
		
		// parse everything
		SparqlGraphJson sgJson = new SparqlGraphJson(templateJSON);
		
		// get needed column names from the JSON template ("colName" properties)
		String[] colNamesToIngest = sgJson.getImportSpec().getColNamesUsed();		
		System.out.println("Num columns to ingest: " + colNamesToIngest.length);
		
		// open the dataset, using the needed column names
		Dataset dataset = null;
		try{
			dataset = new CSVDataset(dataCSVFilePath, colNamesToIngest);
		}catch(Exception e){
			throw new Exception("Could not instantiate CSV dataset: " + e.getMessage());
		}
		
		// load the data
		try{
			DataLoader loader = new DataLoader(templateJSON, batchSize, dataset, sparqlEndpointUser, sparqlEndpointPassword);
			int recordsAdded = loader.importData(true);
			System.out.println("Inserted " + recordsAdded + " records");
			System.out.println("Error report:\n " + loader.getLoadingErrorReportBrief());
			
			// if the logger fully qualified name was not null (assuming the port and the logging service location are included), log this attempt:
			if(lg != null ){

				ArrayList<DetailsTuple> dt = new ArrayList<DetailsTuple>();
				dt.add(new DetailsTuple("records added", recordsAdded + "" ));
				dt.add(new DetailsTuple("target graph", loader.getDatasetGraphName() ));
				dt.add(new DetailsTuple("client", "CSVDataLoaderStandalone" ));
						
						
				
				if( loader.getLoadingErrorReportBrief() != null && !loader.getLoadingErrorReportBrief().isEmpty() ){
					// report the error
					dt.add(new DetailsTuple("error report",  loader.getLoadingErrorReportBrief() ));
				}
				// try to log it.
				lg.logEvent("Load Data From CSV", dt, "Ingestion");
			
			}

			
			
		}catch(Exception e){
			e.printStackTrace();
			throw new Exception("Could not load data: " + e.getMessage());
		}						
		
		
	}
		
}
