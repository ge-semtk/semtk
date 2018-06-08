package com.ge.research.semtk.services.nodeGroupService.requests;

public class SparqlIdReturnedTuple {
	private String sparqlId;
	private boolean returned;
	
	public String getSparqlId() {
		return sparqlId;
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
}
