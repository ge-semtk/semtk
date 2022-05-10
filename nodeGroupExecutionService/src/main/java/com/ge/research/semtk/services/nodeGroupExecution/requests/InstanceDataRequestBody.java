package com.ge.research.semtk.services.nodeGroupExecution.requests;


import java.util.ArrayList;
import java.util.Arrays;

import io.swagger.v3.oas.annotations.media.Schema;

public class InstanceDataRequestBody extends SparqlConnectionLimitOffsetCountRequestBody {
	
	
	@Schema(
			name = "Class values",
			required = false,
			example = "[\"http:/namespace#class1\", \"http:/namespace#class2\"]")
	private String [] classValues = new String[0];
	
	@Schema(
			name = "Predicate values",
			required = false,
			example = "[\"http:/namespace#predicate1\", \"http:/namespace#predicate2\"]")
	private String [] predicateValues = new String[0];
	

	public ArrayList<String> getClassValues() {
		return new ArrayList<String> ( Arrays.asList(this.classValues));
	}
	
	public ArrayList<String> getPredicateValues() {
		return new ArrayList<String> ( Arrays.asList(this.predicateValues));
	}
	
}
