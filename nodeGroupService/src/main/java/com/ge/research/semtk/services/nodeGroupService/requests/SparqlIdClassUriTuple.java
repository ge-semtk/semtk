package com.ge.research.semtk.services.nodeGroupService.requests;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.BelmontUtil;

import io.swagger.v3.oas.annotations.media.Schema;

public class SparqlIdClassUriTuple {
	@Schema(
			description = "SparqlId of node",
            required = true)
	private String sparqlId;
	@Schema(
			description = "classURI of node",
            required = true)
	private String classURI;
	
	public SparqlIdClassUriTuple() {
		
	}
	public SparqlIdClassUriTuple(String sparqlId, String classURI) {
		this.sparqlId = sparqlId;
		this.classURI = classURI;
	}
	public String getSparqlId() throws Exception {
		return BelmontUtil.formatSparqlId(sparqlId);
	}
	public void setSparqlId(String sparqlId) {
		this.sparqlId = sparqlId;
	}
	
	public String getClassURI() {
		return classURI;
	}
	public void setClassURI(String classURI) {
		this.classURI = classURI;
	}
	
	public JSONObject toJson() {
		JSONObject ret = new JSONObject();
		ret.put("sparqlId", this.sparqlId);
		ret.put("classURI", this.classURI);
		return ret;
	}
}
