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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Loads OWL to a semantic store.
 */
public class BulkLoader {

	/**
	 * Main method
	 * 
	 * Note: this does not clear the ontology cache.
	 *       To do so, change so it knows and uses the queryClient instead of going directly to the triplestore
	 */
	public static void main(String[] args) throws Exception{

		try{

			Options options = buildOptions(args);
			CommandLine cmdLine = (new BasicParser()).parse(options,args);

			SparqlEndpointInterface sei = getSei(cmdLine, options);

			LocalLogger.logToStdOut("Loading to server: " + sei.getServerAndPort());
			LocalLogger.logToStdOut("Loading to graph:  " + sei.getGraph());

			if (cmdLine.getArgList().size() == 0) {
				throw new Exception("No files specified");
			}
			
			// loop through files
			for (Object arg : cmdLine.getArgList()) {
				String dataFilePath = (String) arg;

				LocalLogger.logToStdOut("Loading data: " + dataFilePath);
				File dataFile = new File(dataFilePath);
				byte[] data = Files.readAllBytes(dataFile.toPath());

				if (dataFilePath.endsWith(".ttl")) {
					sei.executeAuthUploadTurtle(data);
					
				} else if (dataFilePath.endsWith(".owl")) {
					sei.executeAuthUploadOwl(data);	
					
				} else {
					throw new Exception("Error: Data file " + dataFilePath + " is not an OWL or TURTLE file");
				}		
			}
			LocalLogger.logToStdOut("done.");

		} catch(Exception e){
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
	}

	static Options buildOptions(String [] args) throws Exception {
		Options options = new Options();

		Option input =    new Option("c", "conn", true, "connection file path");
		options.addOption(input);

		Option endpoint = new Option("e", "endpoint", true, "which endpoint in conn, e.g. m0 or d1");
		options.addOption(endpoint);

		Option url =      new Option("s", "server", true, "sparql endpoint server:port");
		options.addOption(url);

		Option type =     new Option("t", "type", true, "sparql server type, e.g. virtuoso, neptune, fuseki");
		options.addOption(type);

		Option graph =    new Option("g", "graph", true, "graph name");
		options.addOption(graph);

		Option user =     new Option("u", "user", true, "user name");
		options.addOption(user);

		Option password = new Option("p", "password", true, "password");
		options.addOption(password);

		return options;
	}

	static SparqlEndpointInterface getSei(CommandLine cmdLine, Options options) throws Exception {
		// check either -c -e    
		if (cmdLine.hasOption('c')) {
			if (!cmdLine.hasOption('e') || cmdLine.hasOption('s') || cmdLine.hasOption('t') || cmdLine.hasOption('g')) {
				(new HelpFormatter()).printHelp("bulkLoader", options);
				throw new Exception("-c requires -e and none of -s -t -g ");
			}
			// or -s -t -g
		} else {
			if (!cmdLine.hasOption('s') || !cmdLine.hasOption('t') || !cmdLine.hasOption('g')) {
				(new HelpFormatter()).printHelp("bulkLoader", options);
				throw new Exception("either ( -c -e ) or ( -s -t -g ) combinations are required ");
			}
		}

		SparqlEndpointInterface sei;
		if (cmdLine.hasOption('c')) {

			// get and check connection file
			String connectionJSONFilePath = cmdLine.getOptionValue('c');
			if(!connectionJSONFilePath.endsWith(".json")){
				(new HelpFormatter()).printHelp("bulkLoader", options);
				throw new Exception("Connection file " + connectionJSONFilePath + " is not a JSON file");
			}

			// get and check connection file endpoint
			String endpointSpec = cmdLine.getOptionValue('e');
			if(!endpointSpec.matches("[dm][0-9]")) {
				(new HelpFormatter()).printHelp("bulkLoader", options);
				throw new Exception("Endpoint specification does not match m0, m1, d0, d1 etc.");
			}
			String modelOrData = endpointSpec.substring(0, 1);
			int index = Integer.parseInt(endpointSpec.substring(1));

			SparqlConnection conn = new SparqlConnection(Utility.getJSONObjectFromFilePath(connectionJSONFilePath).toJSONString());
			if (modelOrData.equals("m")) {
				sei = conn.getModelInterface(index);
			} else {
				sei = conn.getDataInterface(index);
			}
		} else {
			sei = SparqlEndpointInterface.getInstance(cmdLine.getOptionValue('t'), cmdLine.getOptionValue('s'), cmdLine.getOptionValue('g'));
		}

		// check either none or both username and password
		if (cmdLine.hasOption('u')) {
			if (!cmdLine.hasOption('p')) {
				throw new Exception("missing -p password");
			} else {
				sei.setUserAndPassword(cmdLine.getOptionValue('u'), cmdLine.getOptionValue('p'));
			}
		}

		return sei;
	}

}
