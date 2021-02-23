package com.ge.research.semtk.sparqlX;

import org.json.simple.JSONObject;

public abstract class SparqlAndPropGraphEndpointInterface extends SparqlEndpointInterface {
	
	public SparqlAndPropGraphEndpointInterface(String serverAndPort, String graph) throws Exception {
		super (serverAndPort, graph);
	}
	
	public SparqlAndPropGraphEndpointInterface(String serverAndPort, String graph, String user, String pass) throws Exception {
		super (serverAndPort, graph, user, pass);
	}
	
	public abstract JSONObject executeUploadAPI(byte[] data, String format) throws Exception;
	
	public JSONObject executeUploadGremlinCSV(byte[] csv) throws Exception {
		return this.executeUploadAPI(csv, "csv");
	}
	
	public JSONObject executeUploadGremlinCSV(String csv) throws Exception {
		return this.executeUploadAPI(csv.getBytes(), "csv");
	}
	
}
