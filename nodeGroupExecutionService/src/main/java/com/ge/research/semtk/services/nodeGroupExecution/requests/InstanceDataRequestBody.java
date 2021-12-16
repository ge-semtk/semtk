package com.ge.research.semtk.services.nodeGroupExecution.requests;

import static org.hamcrest.CoreMatchers.anything;

import java.util.ArrayList;
import java.util.Arrays;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.json.simple.JSONArray;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.annotations.ApiModelProperty;

public class InstanceDataRequestBody extends SparqlConnectionLimitOffsetCountRequestBody {
	
	
	@ApiModelProperty(
			value = "Class values",
			required = false,
			example = "[\"http:/namespace#class1\", \"http:/namespace#class2\"]")
	private String [] classValues = new String[0];
	
	@ApiModelProperty(
			value = "Predicate values",
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
