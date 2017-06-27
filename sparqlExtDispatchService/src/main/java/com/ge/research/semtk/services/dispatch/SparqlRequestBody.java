package com.ge.research.semtk.services.dispatch;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.sparqlX.SparqlConnection;

public class SparqlRequestBody{

	private String sparqlConnectionJson;
	private String rawSparqlQuery;
	
	public String getRawSparqlQuery() {
		return rawSparqlQuery;
	}
	public void setRawSparqlQuery(String rawSparqlQuery) {
		this.rawSparqlQuery = rawSparqlQuery;
	}
	public String getSparqlConnectionJson() {
		return sparqlConnectionJson;
	}
	public void setSparqlConnectionJson(String sparqlConnectionJson) {
		this.sparqlConnectionJson = sparqlConnectionJson;
	}

	
	public SparqlConnection getConnection() throws Exception{
		return (new SparqlConnection(sparqlConnectionJson));		
	}
	
	
}
