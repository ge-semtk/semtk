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

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Configuration for UtilityClient
 */
public class UtilityClientConfig extends RestClientConfig {

	private String sparqlServerAndPort;  	// e.g. http://host:port (or http://host:port/DATASET for fuseki)
	private String sparqlServerType;		// e.g. virtuoso, fuseki
	
	public UtilityClientConfig(String serviceProtocol,String serviceServer, int servicePort, String serviceEndpoint,
			String sparqlServerAndPort, String sparqlServerType)
			throws Exception {
		super(serviceProtocol, serviceServer, servicePort, serviceEndpoint);
		this.sparqlServerAndPort = sparqlServerAndPort;
		this.sparqlServerType = sparqlServerType;
	}
	
	public UtilityClientConfig(UtilityClientConfig other) throws Exception {
		super(other.serviceProtocol, other.serviceServer, other.servicePort, other.serviceEndpoint);
		this.sparqlServerAndPort = other.sparqlServerAndPort;
		this.sparqlServerType = other.sparqlServerType;
	}
	
	public void setEndpointInterfaceFields(SparqlEndpointInterface sei) {
		this.sparqlServerAndPort = sei.getServerAndPort();
		this.sparqlServerType = sei.getServerType();
	}
	
	public String getSparqlServerAndPort(){
		return sparqlServerAndPort;
	}
	
	public String getSparqlServerType(){
		return sparqlServerType;
	}

}
