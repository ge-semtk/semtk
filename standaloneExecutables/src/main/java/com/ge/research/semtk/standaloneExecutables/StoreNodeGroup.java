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

import com.opencsv.CSVReader;
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
import java.util.Arrays;

/**
 * Loads OWL to a semantic store.
 */
public class StoreNodeGroup {

	private static final String CSV_SPLIT_CHARACTER = ",";
	private static final String formatInfo = "Input file should be in the format: ID, comments, creationDate, creator, jsonFile";
	private static final String[] headers= {"Context","ID","comments","creationDate","creator","jsonFile"};


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

			try {
				processCSVFile(endpointUrlWithPort, csvFile);

			} catch(Exception e){
				LocalLogger.printStackTrace(e);
				System.exit(1);  // need this to catch errors in the calling script
			}
		
		System.exit(0);
	}

	public static void processCSVFile(String endpointUrlWithPort, String csvFile) throws Exception {

		try ( CSVReader br = new CSVReader(new FileReader(csvFile)) ) {


			String[] parsedLine = br.readNext(); // header line
			if (parsedLine.length != headers.length) {
				throw new Exception("Wrong number of columns on header: "+Arrays.toString(parsedLine));
			}

			int i=0;
			for (String headerName: parsedLine) {
				if (! headerName.trim().equalsIgnoreCase(headers[i].trim())) {
					LocalLogger.logToStdErr("Wrong column name: "+headerName+". Was expecting: "+headers[i]);
				}
				i++;
			}


			while ((parsedLine = br.readNext()) != null) {

				if (parsedLine.length == 0) {
					LocalLogger.logToStdOut("Ignoring line without column values: "+ Arrays.toString(parsedLine));
				} else  if (parsedLine.length < headers.length) {
					LocalLogger.logToStdOut("Ignoring! Missing column in line: "+Arrays.toString(parsedLine));
				} else if (parsedLine.length > headers.length ) {
					LocalLogger.logToStdOut("Ignoring! Found Too many: "+parsedLine.length+" columns in line: "+Arrays.toString(parsedLine));

				} else {

                    String context = parsedLine[0];
					String ngId = parsedLine[1]; // e.g. "AMP Design Curve"
					String ngComments = parsedLine[2]; // e.g. "Retrieve an AMP design curve"
					// ignore parsedLine[3]...
					String ngOwner = parsedLine[4]; // e.g. sso as "20000588"
					String ngFilePath = parsedLine[5]; // system full path of the file with json representation of the nodegroup
					String endpointPart[] = endpointUrlWithPort.split(":/*");

					if (endpointUrlWithPort != null && !"".equals(endpointUrlWithPort.trim())) {
					    try {
                            storeSingeNodeGroup(endpointUrlWithPort, ngId, ngComments, ngOwner, ngFilePath, endpointPart);
                        } catch (Exception e) {
					        LocalLogger.logToStdErr("Error processing file: "+ngFilePath+" - "+e.toString());
                        }
					} else {
						LocalLogger.logToStdOut("Ignoring line: "+Arrays.toString(parsedLine));
					}
				}

			}
			LocalLogger.logToStdOut("Finished processing file: "+csvFile);

		} catch (FileNotFoundException e) {
			LocalLogger.printStackTrace(e);
		} catch (IOException e) {
			LocalLogger.printStackTrace(e);
		}
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
