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

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Configuration for SparqlQueryClient auth query
 */
public class SparqlQueryAuthClientConfig extends SparqlQueryClientConfig {

	private String sparqlServerUser;
	private String sparqlServerPassword;
	
	public SparqlQueryAuthClientConfig(String serviceProtocol,String serviceServer, int servicePort, String serviceEndpoint,
			String sparqlServerAndPort, String sparqlServerType, String sparqlDataset,
			String sparqlServerUser, String sparqlServerPassword)
			throws Exception {
		super(serviceProtocol, serviceServer, servicePort, serviceEndpoint, sparqlServerAndPort, sparqlServerType, sparqlDataset);
		this.sparqlServerUser = sparqlServerUser;
		this.sparqlServerPassword = sparqlServerPassword;
	}
	
	public SparqlQueryAuthClientConfig(SparqlQueryClientConfig other) throws Exception{
		super(other.getServiceProtocol(), other.getServiceServer(), other.getServicePort(), other.getServiceEndpoint(), other.getSparqlServerAndPort(), other.getSparqlServerType(), other.getSparqlDataset());
	
		// selectively add the username and password.
		if(other instanceof SparqlQueryAuthClientConfig) {
			// we can set the user name and pass
			this.sparqlServerUser 		= ((SparqlQueryAuthClientConfig) other).getSparqlServerUser();
			this.sparqlServerPassword	= ((SparqlQueryAuthClientConfig) other).getSparqlServerPassword();
		}
		else{
			// we cannot
			this.sparqlServerUser 		= null;
			this.sparqlServerPassword 	= null;
		}
	}
	
	@Override
	@Deprecated
	public void setEndpointInterfaceFields(SparqlEndpointInterface sei) {
		this.setSei(sei);
	}
	
	@Override
	public void setSei(SparqlEndpointInterface sei) {
		super.setSei(sei);
		this.setSparqlServerUser(sei.getUserName());
		this.setSparqlServerPassword(sei.getPassword());
		
	}
	public String getSparqlServerUser(){
		return sparqlServerUser;
	}
	
	public String getSparqlServerPassword(){
		return sparqlServerPassword;
	}
	
	public void setSparqlServerUser(String u) {
		sparqlServerUser = u;
	}
	public void setSparqlServerPassword(String p) {
		sparqlServerPassword = p;
	}
	
	@Override
	public SparqlEndpointInterface buildSei() throws Exception {
		return SparqlEndpointInterface.getInstance(this.getSparqlServerType(), this.getServiceURL(), this.getGraph(), this.getSparqlServerUser(), this.getSparqlServerPassword());
	}
	
	@Override
	public SparqlQueryClientConfig getSparqlQueryClientConfigFromExistingConfig(SparqlQueryClientConfig config) throws Exception{
		return (new SparqlQueryAuthClientConfig(config) );
	}

}
