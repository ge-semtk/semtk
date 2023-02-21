/**
 ** Copyright 2023 General Electric Company
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
package com.ge.research.semtk.services.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ConnectException;

import com.ge.research.semtk.connutil.EndpointNotFoundException;

/**
 * Client for UtilityService
 */
public class UtilityClient extends RestClient {

	/**
	 * Constructor
	 */
	public UtilityClient (RestClientConfig config) {
		this.conf = config;
	}

	private void cleanUp() {
		conf.setServiceEndpoint(null);
		this.parametersJSON.clear();
		this.fileParameter = null;
	}

	/**
	 * Load an ingestion package
	 * @param ingestionPackageFile 	the ingestion package (zip file)
	 * @param serverAndPort			the triple store location, e.g. http://host:port (or http://host:port/DATASET for fuseki)
	 * @param serverType			the triple store type (e.g. "fuseki")
	 * @param clear					if true, clears the footprint graphs (before loading)
	 * @param defaultModelGraph		use where no model graph specified
	 * @param defaultDataGraph		use where no data graph specified
	 * @return 						a BufferedReader providing updates while the load is running
	 */
	@SuppressWarnings("unchecked")
	public BufferedReader execLoadIngestionPackage(File ingestionPackageFile, String serverAndPort, String serverType, boolean clear, String defaultModelGraph, String defaultDataGraph) throws ConnectException, EndpointNotFoundException, Exception {

		if(!ingestionPackageFile.exists()) {
			throw new Exception("File does not exist: " + ingestionPackageFile.getAbsolutePath());
		}

		this.parametersJSON.clear();
		parametersJSON.put("serverAndPort", serverAndPort);
		parametersJSON.put("serverType", serverType);
		parametersJSON.put("clear", String.valueOf(clear));
		parametersJSON.put("defaultModelGraph", defaultModelGraph);
		parametersJSON.put("defaultDataGraph", defaultDataGraph);
		this.fileParameter = ingestionPackageFile;  // send a file as part of request
				
		this.conf.setServiceEndpoint("utility/loadIngestionPackage");
		this.conf.setMethod(RestClientConfig.Methods.POST);

		try {
			return new BufferedReader(new InputStreamReader( super.executeToStream() ));
		} finally {
			this.cleanUp();
		}
	}

}
