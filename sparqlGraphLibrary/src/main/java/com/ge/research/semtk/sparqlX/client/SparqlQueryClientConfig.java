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


package com.ge.research.semtk.sparqlX.client; 

import com.ge.research.semtk.services.client.RestClientConfig;

/**
 * Configuration for SparqlQueryClient (non-auth query)
 */
public class SparqlQueryClientConfig extends RestClientConfig {

	private String sparqlServerAndPort;  // e.g. http://vesuvius37.crd.ge.com:2420
	private String sparqlServerType;		// e.g. virtuoso
	private String sparqlDataset;		// e.g. http://research.ge.com/energy/turbineeng-sandbox
	
	public SparqlQueryClientConfig(String serviceProtocol,String serviceServer, int servicePort, String serviceEndpoint,
			String sparqlServerAndPort, String sparqlServerType, String sparqlDataset)
			throws Exception {
		super(serviceProtocol, serviceServer, servicePort, serviceEndpoint);
		this.sparqlServerAndPort = sparqlServerAndPort;
		this.sparqlServerType = sparqlServerType;
		this.sparqlDataset = sparqlDataset;
	}
	
	public String getSparqlServerAndPort(){
		return sparqlServerAndPort;
	}
	
	public String getSparqlServerType(){
		return sparqlServerType;
	}
	
	public String getSparqlDataset(){
		return sparqlDataset;
	}

}
