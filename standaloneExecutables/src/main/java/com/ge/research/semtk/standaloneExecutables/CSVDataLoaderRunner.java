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

import org.json.simple.JSONObject;

import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

import comge.research.semtk.standaloneExecutables.util.ArgParser;

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
			ArgParser ap = new ArgParser(
					null, null, 
					new String[] {"templateJSONFilePath", "sparqlEndpointUser", "sparqlEndpointPassword", "dataCSVFilePath"},
					new String[] {"-noLoad", "-noPrecheck"},
					null, null,
					new String[] {"batchSize", "connectionOverrideFilePath"}
					);
			ap.parse("main", args);

			String templateJSONFilePath = ap.get("templateJSONFilePath");
			String sparqlEndpointUser = ap.get("sparqlEndpointUser");
			String sparqlEndpointPassword = ap.get("sparqlEndpointPassword");
			String dataCSVFilePath = ap.get("dataCSVFilePath");	
			String batchSize = ap.get("batchSize");      // this should be deprecated
			String connectionOverrideFilePath = ap.get("connectionOverrideFilePath");
			boolean skipIngest = ap.get("-noLoad") != null;
			boolean preCheck = ap.get("-noPrecheck") == null;
			
			// backwards compatibility
			if (batchSize != null && batchSize.toLowerCase().contains(".json") && connectionOverrideFilePath == null) {
				connectionOverrideFilePath = batchSize;
				batchSize = "8";
			}
			
			if(connectionOverrideFilePath != null ){
				connectionOverride = new SparqlConnection(Utility.getJSONObjectFromFilePath(connectionOverrideFilePath).toJSONString());	// override the connection	
			}
			
			try{
				Integer.parseInt(batchSize);
			}catch(Exception e){
				throw new Exception("Error: Cannot parse batch size to int: '" + batchSize + "'");
			}
			
			DataLoader.loadFromCsv(templateJSONFilePath, dataCSVFilePath, sparqlEndpointUser, sparqlEndpointPassword, connectionOverride, preCheck, skipIngest);
			
			System.exit(0);  // explicitly exit to avoid maven exec:java error ("thread was interrupted but is still alive")
		
		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
	}
}
