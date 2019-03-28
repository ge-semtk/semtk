package com.ge.research.semtk.services.nodeGroupExecution.requests;

import static org.hamcrest.CoreMatchers.anything;

import java.util.ArrayList;
import java.util.Arrays;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.json.simple.JSONArray;
import org.mortbay.util.ajax.JSON;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.annotations.ApiModelProperty;

public class InstanceDataRequestBody extends SparqlConnectionRequest {
	
	@ApiModelProperty(
			value = "Maximum number of results to return.<br>Overrides LIMIT stored in nodegroup.",
			required = false,
			example = "-1")
	private int limitOverride = -1;
	
	@ApiModelProperty(
			value = "Query offset.<br>Overrides OFFSET stored in nodegroup.",
			required = false,
			example = "-1")
	private int offsetOverride = -1;
	
	@Pattern(regexp="^\\[\\\".\\\"\\]*$", message="expecting a string representation of an array of strings")
	@ApiModelProperty(
			value = "Class values",
			required = false,
			example = "-1")
	private String [] classValues = new String[0];
	
	@Pattern(regexp="^\\[\\\".\\\"\\]*$", message="expecting a string representation of an array of strings")
	@ApiModelProperty(
			value = "Predicate values",
			required = false,
			example = "-1")
	private String [] predicateValues = new String[0];
	
	@ApiModelProperty(
			value = "On",
			required = false,
			example = "false")
	private boolean countOnly = false;
	
	
	public int getLimitOverride() {
		return limitOverride;
	}

	public int getOffsetOverride() {
		return offsetOverride;
	}
	
	public boolean getCountOnly() {
		return countOnly;
	}

	public ArrayList<String> getClassValues() {
		return new ArrayList<String> ( Arrays.asList(this.classValues));
	}
	
	public ArrayList<String> getPredicateValues() {
		return new ArrayList<String> ( Arrays.asList(this.predicateValues));
	}
}
