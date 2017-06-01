package com.ge.research.semtk.services.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="dispatch", ignoreUnknownFields = true)
public class DispatchProperties {

	private String sparqlServiceServer;
	private int    sparqlServicePort;
	private String sparqlServiceProtocol;
	private String sparqlServiceEndpoint;
	
	private String dispatchSparqlServerAndPort;
	private String dispatchSparqlServerType;
	private String dispatchSparqlDataset;
	
	private String edcServiceServer;
	private int edcServicePort;
	private String edcServiceProtocol;
	private String edcServiceEndpoint;
	private String edcServiceDataSet;
	private String edcSparqlServerAndPort;
	private String edcSparqlServerDataset;
	private String edcSparqlServerType;
	

	private String resultsServiceProtocol;
	private String resultsServiceServer;
	private int resultsServicePort;

	private String statusServiceProtocol;
	private String statusServiceServer;
	private int statusServicePort;
	
	private String dispatcherClassName;
	
	
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
	public String getDispatchSparqlServerAndPort() {
		return dispatchSparqlServerAndPort;
	}
	public String getDispatchSparqlServerType() {
		return dispatchSparqlServerType;
	}
	public String getDispatchSparqlDataset() {
		return dispatchSparqlDataset;
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
	public String getEdcServiceServer() {
		return edcServiceServer;
	}
	public void setEdcServiceServer(String edcServiceServer) {
		this.edcServiceServer = edcServiceServer;
	}
	public int getEdcServicePort() {
		return edcServicePort;
	}
	public void setEdcServicePort(int edcServicePort) {
		this.edcServicePort = edcServicePort;
	}
	public String getEdcServiceProtocol() {
		return edcServiceProtocol;
	}
	public void setEdcServiceProtocol(String edcServiceProtocol) {
		this.edcServiceProtocol = edcServiceProtocol;
	}
	public String getEdcServiceDataSet() {
		return edcServiceDataSet;
	}
	public void setEdcServiceDataSet(String edcServiceDataSet) {
		this.edcServiceDataSet = edcServiceDataSet;
	}
	public String getEdcServiceEndpoint() {
		return edcServiceEndpoint;
	}
	public void setEdcServiceEndpoint(String edcServiceEndpoint) {
		this.edcServiceEndpoint = edcServiceEndpoint;
	}
	public String getEdcSparqlServerAndPort() {
		return edcSparqlServerAndPort;
	}
	public void setEdcSparqlServerAndPort(String edcSparqlServerAndPort) {
		this.edcSparqlServerAndPort = edcSparqlServerAndPort;
	}
	public String getEdcSparqlServerDataset() {
		return edcSparqlServerDataset;
	}
	public void setEdcSparqlServerDataset(String edcSparqlServerDataset) {
		this.edcSparqlServerDataset = edcSparqlServerDataset;
	}
	public String getEdcSparqlServerType() {
		return edcSparqlServerType;
	}
	public void setEdcSparqlServerType(String edcSparqlServerType) {
		this.edcSparqlServerType = edcSparqlServerType;
	}
	public String getResultsServiceProtocol() {
		return resultsServiceProtocol;
	}
	public void setResultsServiceProtocol(String resultsServiceProtocol) {
		this.resultsServiceProtocol = resultsServiceProtocol;
	}
	public String getResultsServiceServer() {
		return resultsServiceServer;
	}
	public void setResultsServiceServer(String resultsServiceServer) {
		this.resultsServiceServer = resultsServiceServer;
	}
	public int getResultsServicePort() {
		return resultsServicePort;
	}
	public void setResultsServicePort(int resultsServicePort) {
		this.resultsServicePort = resultsServicePort;
	}
	public String getStatusServiceServer() {
		return statusServiceServer;
	}
	public void setStatusServiceServer(String statusServiceServer) {
		this.statusServiceServer = statusServiceServer;
	}
	public String getStatusServiceProtocol() {
		return statusServiceProtocol;
	}
	public void setStatusServiceProtocol(String statusServiceProtocol) {
		this.statusServiceProtocol = statusServiceProtocol;
	}
	public int getStatusServicePort() {
		return statusServicePort;
	}
	public void setStatusServicePort(int statusServicePort) {
		this.statusServicePort = statusServicePort;
	}
	public String getDispatcherClassName() {
		return dispatcherClassName;
	}
	public void setDispatcherClassName(String dispatcherClassName) {
		this.dispatcherClassName = dispatcherClassName;
	}
}
