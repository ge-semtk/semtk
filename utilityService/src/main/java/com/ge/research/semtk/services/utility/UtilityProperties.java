/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.services.utility;
 
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.Properties;

@Configuration
@ConfigurationProperties(prefix="utility", ignoreUnknownFields = true)
public class UtilityProperties extends Properties {

	private String sparqlServiceServer;
	private int    sparqlServicePort;
	private String sparqlServiceProtocol;
	private String sparqlServiceEndpoint;
	private String sparqlServiceAuthEndpoint;
	private String sparqlServiceUser = null;
	private String sparqlServicePass = null;
	
	private String nodegroupExecutionServiceServer;
	private int nodegroupExecutionServicePort;
	private String nodegroupExecutionServiceProtocol;
	
	private String ingestionServiceServer;
	private int ingestionServicePort;
	private String ingestionServiceProtocol;

	// this is old-fashioned.  Please don't add more properties here.
	// Rest controller should instead @autowire new Utility*Properties classes that extend the correct subclass of Properties
	// -Paul
	// jan 2020
	
	public UtilityProperties() {
		super();
		this.setPrefix("utility");
	}
	
	public void validate() throws Exception {
		super.validate();
		this.checkNotEmpty("sparqlServiceServer", sparqlServiceServer);
		this.checkNotEmpty("sparqlServicePort", sparqlServicePort);
		this.checkNotEmpty("sparqlServiceProtocol", sparqlServiceProtocol);
		this.checkNotEmpty("sparqlServiceEndpoint", sparqlServiceEndpoint);
		this.checkNotEmpty("sparqlServiceAuthEndpoint", sparqlServiceAuthEndpoint);
		this.checkNotEmpty("sparqlServiceUser", sparqlServiceUser);
		this.checkNotEmpty("sparqlServiceUser", sparqlServiceUser);
		this.checkNotEmpty("sparqlServicePass", sparqlServicePass);
		this.checkNotEmpty("nodegroupExecutionServiceServer", nodegroupExecutionServiceServer);
		this.checkNotEmpty("nodegroupExecutionServicePort", nodegroupExecutionServicePort);
		this.checkNotEmpty("nodegroupExecutionServiceProtocol", nodegroupExecutionServiceProtocol);
		this.checkNotEmpty("ingestionServiceServer", ingestionServiceServer);
		this.checkNotEmpty("ingestionServicePort", ingestionServicePort);
		this.checkNotEmpty("ingestionServiceProtocol", ingestionServiceProtocol);
	}
	
	public String getSparqlServiceServer() {
		return sparqlServiceServer;
	}
	public void setSparqlServiceServer(String sparqlServiceServer) {
		this.sparqlServiceServer = sparqlServiceServer;
	}
	public int getSparqlServicePort() {
		return sparqlServicePort;
	}
	public void setSparqlServicePort(int sparqlServicePort) {
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
	public String getSparqlServiceAuthEndpoint() {
		return sparqlServiceAuthEndpoint;
	}
	public void setSparqlServiceAuthEndpoint(String sparqlServiceAuthEndpoint) {
		this.sparqlServiceAuthEndpoint = sparqlServiceAuthEndpoint;
	}
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
	
	
	public String getNodegroupExecutionServiceServer() {
		return nodegroupExecutionServiceServer;
	}
	public void setNodegroupExecutionServiceServer(
			String nodegroupExecutionServiceServer) {
		this.nodegroupExecutionServiceServer = nodegroupExecutionServiceServer;
	}
	public int getNodegroupExecutionServicePort() {
		return nodegroupExecutionServicePort;
	}
	public void setNodegroupExecutionServicePort(
			int nodegroupExecutionServicePort) {
		this.nodegroupExecutionServicePort = nodegroupExecutionServicePort;
	}
	public String getNodegroupExecutionServiceProtocol() {
		return nodegroupExecutionServiceProtocol;
	}
	public void setNodegroupExecutionServiceProtocol(
			String nodegroupExecutionServiceProtocol) {
		this.nodegroupExecutionServiceProtocol = nodegroupExecutionServiceProtocol;
	}
	
	public String getIngestionServiceServer() {
		return ingestionServiceServer;
	}
	public void setIngestionServiceServer(String ingestionServiceServer) {
		this.ingestionServiceServer = ingestionServiceServer;
	}
	public int getIngestionServicePort() {
		return ingestionServicePort;
	}
	public void setIngestionServicePort(int ingestionServicePort) {
		this.ingestionServicePort = ingestionServicePort;
	}
	public String getIngestionServiceProtocol() {
		return ingestionServiceProtocol;
	}
	public void setIngestionServiceProtocol(String ingestionServiceProtocol) {
		this.ingestionServiceProtocol = ingestionServiceProtocol;
	}

}
