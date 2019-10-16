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
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

import comge.research.semtk.standaloneExecutables.util.ArgParser;

/**
 * Run SPARQL util queries
 * 
 */
public class SparqlUtilRunner {
		
	/**
	 * Main method
	 */
	public static void main(String[] args) throws Exception{
		
		SparqlConnection connectionOverride = null;		// stays null if no connection override provided
		try{
			ArgParser ap = new ArgParser(
					null, null, 
					new String[] {"connectionFilePath", "sparqlEndpointUser", "sparqlEndpointPassword"},
					new String[] {"-clearGraph"},
					new String[] {"-data", "-model"}, new String[] {"data_endpoint_number", "model_endpoint_number"},
					null       
					);
			ap.parse("main", args);

			String user = ap.get("sparqlEndpointUser");
			String pass = ap.get("sparqlEndpointPassword");
			String connectionFilePath = ap.get("connectionFilePath");
			
			boolean clearGraph = ap.get("-clearGraph") != null;
			String dataIndex = ap.get("-data");
			String modelIndex = ap.get("-model");
			
			if (clearGraph) {
				if (dataIndex == null && modelIndex == null) {
					ap.throwUsageException("-clearGraph requires one of -data or -model");
				} else if (dataIndex != null && modelIndex != null) {
					ap.throwUsageException("-clearGraph requires only one of -data or -model");
				}
			
				SparqlConnection conn = new SparqlConnection(Utility.getStringFromFilePath(connectionFilePath));
				SparqlEndpointInterface sei = null;
				if (modelIndex != null) {
					int i = Integer.parseInt(modelIndex);
					sei = conn.getModelInterface(i);
				} else {
					int i = Integer.parseInt(dataIndex);
					sei = conn.getDataInterface(i);
				}
				sei.setUserAndPassword(user, pass);
				
				System.out.println("CLEAR GRAPH  server: " + sei.getServerAndPort() + ", graph: " + sei.getGraph());
				sei.clearGraph();
			}
			System.exit(0);  // explicitly exit to avoid maven exec:java error ("thread was interrupted but is still alive")
		
		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
	}
}
