package com.ge.research.semtk.services.nodeGroupExecution.requests;


import io.swagger.v3.oas.annotations.media.Schema;

public class InstanceDataPredicate  {
	
	@Schema(
			name = "domain URI",
			required = true,
			example = "http://my/domain#className")
	private String domainURI;
	
	@Schema(
			name = "Predicate URI",
			required = false,
			example = "http://my/domain#predicate")
	private String predicateURI;

	public String getDomainURI() {
		return domainURI;
	}

	public String getPredicateURI() {
		return predicateURI;
	}

}
