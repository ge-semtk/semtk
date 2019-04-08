package com.ge.research.semtk.services.nodeGroupExecution.requests;


import io.swagger.annotations.ApiModelProperty;

public class InstanceDataPredicate  {
	
	@ApiModelProperty(
			value = "domain URI",
			required = true,
			example = "http://my/domain#className")
	private String domainURI;
	
	@ApiModelProperty(
			value = "Predicate URI",
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
