/**
 ** Copyright 2019 General Electric Company
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

import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Loads data to a triple store from a CSV file into database.
 * 
 * Sample command:
 * mvn exec:java -Dstart-class="com.ge.research.semtk.standaloneExecutables.CSVDataLoaderRunner" -Dexec.args="../sparqlGraphLibrary/src/test/resources/testTransforms.json virtuosoUser virtuosoPassword ../sparqlGraphLibrary/src/test/resources/testTransforms.csv 10" 
 *
 * Sample command with SPARQL connection override:
 * mvn exec:java -Dstart-class="com.ge.research.semtk.standaloneExecutables.CSVDataLoaderRunner" -Dexec.args="../sparqlGraphLibrary/src/test/resources/testTransforms.json virtuosoUser virtuosoPassword ../sparqlGraphLibrary/src/test/resources/testTransforms.csv 10 ../sparqlGraphLibrary/src/test/resources/SPARQLConnection-test.json"
 */
public class CSVDataLoaderRunner {
		
	/**
	 * Main method
	 */
	public static void main(String[] args) throws Exception{
		
		SparqlConnection connectionOverride = null;		// stays null if no connection override provided
		try{
			if(args.length != 5 && args.length != 6){
				throw new Exception("Invalid argument list.  Usage: main(templateJSONFilePath, sparqlEndpointUser, sparqlEndpointPassword, dataCSVFilePath, batchSize, connectionOverrideFilePath (optional))");
			}
			String templateJSONFilePath = args[0];
			String sparqlEndpointUser = args[1];
			String sparqlEndpointPassword = args[2];
			String dataCSVFilePath = args[3];	
			String batchSize = args[4];      // this should be deprecated
			if(args.length == 6 ){
				String connectionOverrideFilePath = args[5]; 	
				connectionOverride = new SparqlConnection(Utility.getJSONObjectFromFilePath(connectionOverrideFilePath).toJSONString());	// override the connection	
			}
			
			try{
				Integer.parseInt(batchSize);
			}catch(Exception e){
				throw new Exception("Error: Cannot parse batch size to int: '" + batchSize + "'");
			}
			
			// perform the load
			// TODO: use the version with no batch size and overrideMaxThread at the end (-1 or 0 does nothing)
			DataLoader.loadFromCsv(templateJSONFilePath, dataCSVFilePath, sparqlEndpointUser, sparqlEndpointPassword, Integer.parseInt(batchSize), connectionOverride);
			System.exit(0);  // explicitly exit to avoid maven exec:java error ("thread was interrupted but is still alive")
		
		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
	}
}
