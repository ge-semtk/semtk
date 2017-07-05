package com.ge.research.semtk.services.nodeGroupExecution;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DispatchRawSparqlRequestBody {
	private String sparqlConnection;
	private String sparql;
	
	public String getSparqlConnection() {
		return sparqlConnection;
	}
	public void setSparqlConnection(String sparqlConnection) {
		this.sparqlConnection = sparqlConnection;
	}

	public String getSparql() {
		return sparql;
	}

	public void setSparql(String sparql) {
		this.sparql = sparql;
	}

	
}
