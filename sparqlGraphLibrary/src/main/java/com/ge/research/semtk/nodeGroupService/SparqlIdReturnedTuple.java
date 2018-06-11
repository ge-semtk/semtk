package com.ge.research.semtk.nodeGroupService;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.BelmontUtil;

public class SparqlIdReturnedTuple {
	private String sparqlId;
	private boolean returned;
	
	public SparqlIdReturnedTuple() {
		
	}
	public SparqlIdReturnedTuple(String sparqlId, boolean isReturned) {
		this.sparqlId = sparqlId;
		this.returned = isReturned;
	}
	public String getSparqlId() throws Exception {
		return BelmontUtil.formatSparqlId(sparqlId);
	}
	public void setSparqlID(String sparqlId) {
		this.sparqlId = sparqlId;
	}
	public boolean isReturned() {
		return returned;
	}
	public void setReturned(boolean bool) {
		this.returned = bool;
	}
	
	public JSONObject toJson() {
		JSONObject ret = new JSONObject();
		ret.put("sparqlId", this.sparqlId);
		ret.put("returned", this.returned);
		return ret;
	}
}
