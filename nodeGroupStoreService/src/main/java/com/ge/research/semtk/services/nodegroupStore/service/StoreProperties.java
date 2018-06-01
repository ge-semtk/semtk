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

package com.ge.research.semtk.services.nodegroupStore.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="store", ignoreUnknownFields = true)
public class StoreProperties {

	// for write operations. 
	private String ingestorLocation = "";
	private String ingestorProtocol = "";
	private String ingestorPort     = "";
	
	// for sparql operations.
	private String sparqlServiceServer = "";
	private String sparqlServicePort = "";
	private String sparqlServiceProtocol = "";
	private String sparqlServiceEndpoint = "";
	
	private String sparqlServiceUser = "";
	private String sparqlServicePass = "";
	
	// sparql server and named graph info
	private String sparqlServerAndPort = "";
	private String sparqlServerDataDataset = "";
	private String sparqlServerModelDataset = "";
	private String sparqlServerDomain = "";
	private String sparqlServerType = "";
	
	// info on our ingestion server
	public String getIngestorLocation() {
		return ingestorLocation;
	}
	public void setIngestorLocation(String ingestorLocation) {
		this.ingestorLocation = ingestorLocation;
	}
	public String getIngestorProtocol() {
		return ingestorProtocol;
	}
	public void setIngestorProtocol(String ingestorProtocol) {
		this.ingestorProtocol = ingestorProtocol;
	}
	public String getIngestorPort() {
		return ingestorPort;
	}
	public void setIngestorPort(String ingestorPort) {
		this.ingestorPort = ingestorPort;
	}

	// info on our query server
	public String getSparqlServiceServer() {
		return sparqlServiceServer;
	}
	public void setSparqlServiceServer(String sparqlServiceServer) {
		this.sparqlServiceServer = sparqlServiceServer;
	}
	public int getSparqlServicePort() {
		return Integer.parseInt(sparqlServicePort);
	}
	public void setSparqlServicePort(String sparqlServicePort) {
		this.sparqlServicePort = sparqlServicePort;
	}
	public String getSparqlServiceProtocol() {
		return sparqlServiceProtocol;
	}
	public void setSparqlServiceProtocol(String sparqlServiceProtocol) {
		this.sparqlServiceProtocol = sparqlServiceProtocol;
	}
	public String getSparqlServiceEndpoint() {
		return sparqlServiceEndpoint;
	}
	public void setSparqlServiceEndpoint(String sparqlServiceEndpoint) {
		this.sparqlServiceEndpoint = sparqlServiceEndpoint;
	}
	
	// info on our sparql endpoint itself
	public String getSparqlServerAndPort() {
		return sparqlServerAndPort;
	}
	public void setSparqlServerAndPort(String sparqlServerAndPort) {
		this.sparqlServerAndPort = sparqlServerAndPort;
	}
	public String getSparqlServerDataDataset() {
		return sparqlServerDataDataset;
	}
	public void setSparqlServerDataDataset(String sparqlServerDataDataset) {
		this.sparqlServerDataDataset = sparqlServerDataDataset;
	}
	public String getSparqlServerModelDataset() {
		return sparqlServerModelDataset;
	}
	public void setSparqlServerModelDataset(String sparqlServerModelDataset) {
		this.sparqlServerModelDataset = sparqlServerModelDataset;
	}
	public String getSparqlServerDomain() {
		return sparqlServerDomain;
	}
	public void setSparqlServerDomain(String sparqlServerDomain) {
		this.sparqlServerDomain = sparqlServerDomain;
	}
	public String getSparqlServerType() {
		return sparqlServerType;
	}
	public void setSparqlServerType(String sparqlServerType) {
		this.sparqlServerType = sparqlServerType;
	}
	
	
	// needed for authenticated access. 
	public String getSparqlServiceUser() {
		return sparqlServiceUser;
	}
	public void setSparqlServiceUser(String sparqlServiceUser) {
		this.sparqlServiceUser = sparqlServiceUser;
	}
	public String getSparqlServicePass() {
		return sparqlServicePass;
	}
	public void setSparqlServicePass(String sparqlServicePass) {
		this.sparqlServicePass = sparqlServicePass;
	}	
	

}
