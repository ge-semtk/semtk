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

import org.apache.commons.lang.ArrayUtils;
import org.apache.jena.base.Sys;
import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Loads OWL to a semantic store.
 */
public class StoreNodeGroup {

	private static final String CSV_SPLIT_CHARACTER = ",";
	private static final String formatInfo = "Input file should be in the format: ID, comments, creationDate, creator, jsonFile";


	/**
	 * Main method
	 */
	public static void main(String[] args) throws Exception{


			// get arguments
			if(args.length != 2) {
				System.out.println("\nUsage: http://endpoint:port inputFile.csv\n");
				System.exit(0);
			}

		    String endpointUrlWithPort = args[0]; //e.g. http://localhost:12056
			String csvFile = args[1];

			try ( BufferedReader br = new BufferedReader(new FileReader(csvFile)) ) {

				String line  = br.readLine();
				if (line == null) {
					throw new Exception("Could not find CSV file head line. "+formatInfo);
				}

				while ((line = br.readLine()) != null) {
					// use comma as separator
					String[] parsedLine = line.split(CSV_SPLIT_CHARACTER);

					if (parsedLine.length < 5) {
						throw new Exception("Missing column in input file. "+formatInfo);
					} else if (parsedLine.length > 5 ) {
						throw new Exception("Found Too many columns in file. "+formatInfo);
					}

					String ngId = parsedLine[0]; // e.g. "AMP Design Curve"
					String ngComments = parsedLine[1]; // e.g. "Retrieve an AMP design curve"
					// ignore parsedLine[2]...
					String ngOwner = parsedLine[3]; // e.g. sso as "20000588"
					String ngFilePath = parsedLine[4]; // system full path of the file with json representation of the nodegroup
					String endpointPart [] = endpointUrlWithPort.split(":/*");

					storeSingeNodeGroup(endpointUrlWithPort, ngId, ngComments, ngOwner, ngFilePath, endpointPart);

				}

			} catch(Exception e){
				LocalLogger.printStackTrace(e);
				System.exit(1);  // need this to catch errors in the calling script
			}
		
		System.exit(0);
	}

	private static void storeSingeNodeGroup(String endpointUrlWithPort, String ngId, String ngComments, String ngOwner, String ngFilePath, String[] endpointPart) throws Exception {

		// validate arguments
		if(!ngFilePath.endsWith(".json")){
			throw new Exception("Error: Connection file " + ngFilePath + " is not a JSON file");
		}
		JSONObject ngJson = Utility.getJSONObjectFromFilePath(ngFilePath);

		// set up client
		NodeGroupStoreConfig config = new NodeGroupStoreConfig(endpointPart[0], endpointPart[1], Integer.parseInt(endpointPart[2]));
		NodeGroupStoreRestClient client = new NodeGroupStoreRestClient(config);

		// check whether id already exists
		TableResultSet res = client.executeGetNodeGroupMetadata();
		res.throwExceptionIfUnsuccessful("Error while checking of nodegroup Id already exists");

		// delete old copy if it does
		if (ArrayUtils.contains(res.getTable().getColumn("ID"), ngId)) {

			SimpleResultSet r = client.deleteStoredNodeGroup(ngId);
			r.throwExceptionIfUnsuccessful("Error while removing preview version of nodegroup");
		}

		// store it
		SimpleResultSet r = client.executeStoreNodeGroup(ngId, ngComments, ngOwner, ngJson);
		r.throwExceptionIfUnsuccessful("Error while storing nodegroup");

		LocalLogger.logToStdOut("Successfully stored " + ngId);

	}
		
}
