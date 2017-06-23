package com.ge.research.semtk.services.storedNodegroupExecution;

public class FilterDispatchFromNodeGroupRequestBody extends DispatchFromNodegroupRequestBody {
	private String targetObjectSparqlId;

	public String getTargetObjectSparqlId() {
		return targetObjectSparqlId;
	}

	public void setTargetObjectSparqlId(String targetObjectSparqlId) {
		this.targetObjectSparqlId = targetObjectSparqlId;
	}
	
	
}
