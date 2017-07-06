package com.ge.research.semtk.services.nodeGroupExecution;

public class FilterDispatchByIdRequestBody extends DispatchByIdRequestBody{
	private String targetObjectSparqlId;

	public String getTargetObjectSparqlId() {
		return targetObjectSparqlId;
	}

	public void setTargetObjectSparqlId(String targetObjectSparqlId) {
		this.targetObjectSparqlId = targetObjectSparqlId;
	}
	
	
}
