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
	public UtilityClient (UtilityClientConfig config) {
		this.conf = config;
	}

	@Override
	public UtilityClientConfig getConfig() {
		return (UtilityClientConfig) this.conf;
	}

	/**
	 * Create a params JSON object
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void buildParametersJSON() throws Exception {

		if(! (this.conf instanceof UtilityClientConfig)){
			throw new Exception("Unrecognized config for UtilityClient");
		}

		if(this.conf.getServiceEndpoint().endsWith("loadIngestionPackage")){ 
			// only need these for some UtilityService endpoints.  See SparqlQueryClient for more examples
			parametersJSON.put("serverAndPort", ((UtilityClientConfig)this.conf).getSparqlServerAndPort());
			parametersJSON.put("serverType", ((UtilityClientConfig)this.conf).getSparqlServerType());
		}
	}

	private void cleanUp() {
		conf.setServiceEndpoint(null);
		this.parametersJSON.clear();
		this.fileParameter = null;
	}

	/**
	 * Load an ingestion package
	 * @param ingestionPackageFile the ingestion package (zip file)
	 * @return a BufferedReader providing updates while the load is running
	 */
	public BufferedReader execLoadIngestionPackage(File ingestionPackageFile) throws ConnectException, EndpointNotFoundException, Exception {
		
		if(!ingestionPackageFile.exists()) {
			throw new Exception("File does not exist: " + ingestionPackageFile.getAbsolutePath());
		}

		this.parametersJSON.clear();
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
