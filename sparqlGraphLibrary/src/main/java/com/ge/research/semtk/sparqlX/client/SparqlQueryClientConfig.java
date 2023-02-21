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

import java.util.ArrayList;

import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Configuration for SparqlQueryClient (non-auth query)
 */
public class SparqlQueryClientConfig extends RestClientConfig {

	private String sparqlServerAndPort;  	// e.g. http://host:port
	private String sparqlServerType;		// e.g. virtuoso
	private String sparqlDataset;			// e.g. http://dataset
	
	public SparqlQueryClientConfig(String serviceProtocol,String serviceServer, int servicePort, String serviceEndpoint,
			String sparqlServerAndPort, String sparqlServerType, String sparqlDataset)
			throws Exception {

		super(serviceProtocol, serviceServer, servicePort, serviceEndpoint);
		this.sparqlServerAndPort = sparqlServerAndPort;
		this.sparqlServerType = sparqlServerType;
		this.sparqlDataset = sparqlDataset;
	}
	
	public SparqlQueryClientConfig(SparqlQueryClientConfig other) throws Exception {
		super(other.serviceProtocol, other.serviceServer, other.servicePort, other.serviceEndpoint);
		this.sparqlServerAndPort = other.sparqlServerAndPort;
		this.sparqlServerType = other.sparqlServerType;
		this.sparqlDataset = other.sparqlDataset;
	}
	
	@Deprecated
	public void setEndpointInterfaceFields(SparqlEndpointInterface sei) {
		this.setSei(sei);
	}
	
	public void setSei(SparqlEndpointInterface sei) {
		this.sparqlServerAndPort = sei.getServerAndPort();
		this.sparqlServerType = sei.getServerType();
		this.sparqlDataset = sei.getGraph();
	}
	
	/**
	 * Translate this Config and a list of SparqlEndpointInterfaces into a list of Configs, one for each sei
	 * @param seiList
	 * @return
	 * @throws Exception
	 */
	public ArrayList<SparqlQueryClientConfig> getArrayForEndpoints(ArrayList<SparqlEndpointInterface> seiList) throws Exception {
		ArrayList<SparqlQueryClientConfig> ret = new ArrayList<SparqlQueryClientConfig>();
		
		for (int i=0; i < seiList.size(); i++) {
			// note: this code has a hack to allow the creation of auth cients.... 
			// the auth version specifically overrides the this.getSparqlQueryClientConfigFromExistingConfig(this) method
			// to allow it to then be used to make either kind, even though the super class is not aware of the auth subclass.
			
			//SparqlQueryClientConfig config = new SparqlQueryClientConfig(this);
			SparqlQueryClientConfig config = this.getSparqlQueryClientConfigFromExistingConfig(this);
			config.setSei(seiList.get(i));
			ret.add(config);
		}
		return ret;
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
	
	public SparqlEndpointInterface buildSei() throws Exception {
		return SparqlEndpointInterface.getInstance(this.getSparqlServerType(), this.getServiceURL(), this.getGraph());
	}
	
	/**
	 * Newer naming convention
	 * @return
	 */
	public String getGraph() {
		return sparqlDataset;
	}
	
	public void setGraph(String graphName) {
		this.sparqlDataset = graphName;
	}

	public SparqlQueryClientConfig getSparqlQueryClientConfigFromExistingConfig(SparqlQueryClientConfig config) throws Exception{
		return (new SparqlQueryClientConfig(config) );
	}
}
