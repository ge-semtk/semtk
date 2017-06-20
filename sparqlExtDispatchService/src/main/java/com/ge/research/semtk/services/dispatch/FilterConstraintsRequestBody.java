package com.ge.research.semtk.services.dispatch;

public class FilterConstraintsRequestBody extends QueryRequestBody {
	private String targetObjectSparqlID;

	public String getTargetObjectSparqlID() {
		return targetObjectSparqlID;
	}

	public void setTargetObjectSparqlID(String targetObjectSparqlID) {
		this.targetObjectSparqlID = targetObjectSparqlID;
	}
}
