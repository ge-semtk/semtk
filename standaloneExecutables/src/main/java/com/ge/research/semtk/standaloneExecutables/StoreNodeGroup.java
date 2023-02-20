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

import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;

import java.io.PrintWriter;

/**
 * Utility to load nodegroups from local disk to the semantic store.
 */
public class StoreNodeGroup {


	/**
	 * Main method
	 */
	public static void main(String[] args) throws Exception{

		// get arguments
		if(args.length != 2 && args.length != 3) {
			System.out.println("\nUsage: http://endpoint:port inputFile.csv [sparqlConnectionOverride.json]\n");
			System.exit(0);
		}
		String endpointUrlWithPort = args[0]; //e.g. http://localhost:12056
		String csvFile = args[1];
		String sparqlConnOverrideFile = (args.length == 3) ? args[2] : null;

		String endpointPart[] = endpointUrlWithPort.split(":/*");
		NodeGroupStoreConfig config = new NodeGroupStoreConfig(endpointPart[0], endpointPart[1], Integer.parseInt(endpointPart[2]));
		NodeGroupStoreRestClient client = new NodeGroupStoreRestClient(config);
		try {
			PrintWriter out = new PrintWriter(System.out);
			client.loadStoreDataCsv(csvFile, sparqlConnOverrideFile, out);
			out.close();
		} catch(Exception e){
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
	}
}
