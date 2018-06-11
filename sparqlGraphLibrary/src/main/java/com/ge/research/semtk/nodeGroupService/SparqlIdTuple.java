package com.ge.research.semtk.nodeGroupService;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.BelmontUtil;

public class SparqlIdTuple {
	private String sparqlIdFrom;
	private String sparqlIdTo;
	
	public SparqlIdTuple() {
	}
	
	public SparqlIdTuple(String from, String to) {
		this.sparqlIdFrom = from;
		this.sparqlIdTo = to;
	}
	public String getSparqlIdFrom() throws Exception {
		return BelmontUtil.formatSparqlId(sparqlIdFrom);
	}
	public void setSparqlIdFrom(String sparqlIdFrom) {
		this.sparqlIdFrom = sparqlIdFrom;
	}
	public String getSparqlIdTo()throws Exception {
		return BelmontUtil.formatSparqlId(sparqlIdTo);
	}
	public void setSparqlIdTo(String sparqlIdTo) {
		this.sparqlIdTo = sparqlIdTo;
	}
	public JSONObject toJson() {
		JSONObject ret = new JSONObject();
		ret.put("sparqlIdFrom", this.sparqlIdFrom);
		ret.put("sparqlIdTo", this.sparqlIdTo);
		return ret;
	}
}
