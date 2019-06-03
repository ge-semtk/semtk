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

import java.io.File;
import java.nio.file.Files;

import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.sparqlX.NeptuneSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.S3BucketConfig;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Loads OWL to a semantic store.
 */
public class OwlLoader {
	
	/**
	 * Main method
	 * 
	 * Note: this does not clear the ontology cache.
	 *       To do so, change so it knows and uses the queryClient instead of going directly to the triplestore
	 */
public static void main(String[] args) throws Exception{
		
		try{
		
			// get arguments
			if(args.length != 4){
				throw new Exception("Invalid argument list.  Usage: main(connectionJSONFilePath, sparqlEndpointUser, sparqlEndpointPassword, owlFilePath)");
			}
			String connectionJSONFilePath = args[0];  // this file is used to get the SPARQL connection info
			String sparqlEndpointUser = args[1];
			String sparqlEndpointPassword = args[2];
			String dataFilePath = args[3];	
			
			// validate arguments
			if(!connectionJSONFilePath.endsWith(".json")){
				throw new Exception("Error: Connection file " + connectionJSONFilePath + " is not a JSON file");
			}
			if(!dataFilePath.endsWith(".owl") && !dataFilePath.endsWith(".ttl")){
				throw new Exception("Error: Data file " + dataFilePath + " is not an OWL or TURTLE file");
			}
			
			// get the SPARQL endpoint interface
			SparqlEndpointInterface sei;
			try{
				
				SparqlConnection conn = new SparqlConnection(Utility.getJSONObjectFromFilePath(connectionJSONFilePath).toJSONString());
				sei = conn.getModelInterface(0);
				sei.setUserAndPassword(sparqlEndpointUser, sparqlEndpointPassword);
				
				LocalLogger.logToStdOut("Ontology Dataset: " + sei.getGraph());
			}catch(Exception e){
				throw new Exception("Cannot get SPARQL connection: ", e);
			}
			
			// upload the OWL
			try{	
				File dataFile = new File(dataFilePath);
				byte[] owlFileBytes = Files.readAllBytes(dataFile.toPath());
				
				if (dataFilePath.endsWith(".ttl")) {
					sei.executeAuthUploadTurtle(owlFileBytes);
				} else {
					sei.executeAuthUploadOwl(owlFileBytes);	
				}
				LocalLogger.logToStdOut("Loaded data: " + dataFilePath);			
			}catch(Exception e){
				LocalLogger.printStackTrace(e);
			}
		
		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
	}
		
}
