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

package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="node-group-execution", ignoreUnknownFields = true)
public class NodegroupExecutionProperties {

	// all of the values we are likely to need
	private String ngStoreProtocol = "";
	private String ngStoreServer = "";
	private int ngStorePort = 0;
	
	private String dispatchProtocol = "";
	private String dispatchServer = "";
	private int dispatchPort = 0;
	
	private String resultsProtocol = "";
	private String resultsServer = "";
	private int resultsPort = 0;
	
	private String statusProtocol = "";
	private String statusServer = "";
	private int statusPort = 0;
	
	private String ingestProtocol = "";
	private String ingestServer = "";
	private int ingestPort = 0;
	
	public String getNgStoreProtocol() {
		return ngStoreProtocol;
	}
	public void setNgStoreProtocol(String ngStoreProtocol) {
		this.ngStoreProtocol = ngStoreProtocol;
	}
	public String getNgStoreServer() {
		return ngStoreServer;
	}
	public void setNgStoreServer(String ngStoreServer) {
		this.ngStoreServer = ngStoreServer;
	}
	public int getNgStorePort() {
		return ngStorePort;
	}
	public void setNgStorePort(int ngStorePort) {
		this.ngStorePort = ngStorePort;
	}
	public String getDispatchProtocol() {
		return dispatchProtocol;
	}
	public void setDispatchProtocol(String dispatchProtocol) {
		this.dispatchProtocol = dispatchProtocol;
	}
	public String getDispatchServer() {
		return dispatchServer;
	}
	public void setDispatchServer(String dispatchServer) {
		this.dispatchServer = dispatchServer;
	}
	public int getDispatchPort() {
		return dispatchPort;
	}
	public void setDispatchPort(int dispatchPort) {
		this.dispatchPort = dispatchPort;
	}
	public String getResultsProtocol() {
		return resultsProtocol;
	}
	public void setResultsProtocol(String resultsProtocol) {
		this.resultsProtocol = resultsProtocol;
	}
	public String getResultsServer() {
		return resultsServer;
	}
	public void setResultsServer(String resultsServer) {
		this.resultsServer = resultsServer;
	}
	public int getResultsPort() {
		return resultsPort;
	}
	public void setResultsPort(int resultsPort) {
		this.resultsPort = resultsPort;
	}
	public String getStatusProtocol() {
		return statusProtocol;
	}
	public void setStatusProtocol(String statusProtocol) {
		this.statusProtocol = statusProtocol;
	}
	public String getStatusServer() {
		return statusServer;
	}
	public void setStatusServer(String statusServer) {
		this.statusServer = statusServer;
	}
	public int getStatusPort() {
		return statusPort;
	}
	public void setStatusPort(int statusPort) {
		this.statusPort = statusPort;
	}
	public String getIngestProtocol() {
		return ingestProtocol;
	}
	public void setIngestProtocol(String ingestProtocol) {
		this.ingestProtocol = ingestProtocol;
	}
	public String getIngestServer() {
		return ingestServer;
	}
	public void setIngestServer(String ingestServer) {
		this.ingestServer = ingestServer;
	}
	public int getIngestPort() {
		return ingestPort;
	}
	public void setIngestPort(int ingestPort) {
		this.ingestPort = ingestPort;
	}
	
	
}
