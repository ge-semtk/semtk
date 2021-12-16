package com.ge.research.semtk.services.nodeGroupExecution.requests;

import static org.hamcrest.CoreMatchers.anything;

import java.util.ArrayList;
import java.util.Arrays;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.json.simple.JSONArray;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.annotations.ApiModelProperty;

public class SparqlConnectionLimitOffsetCountRequestBody extends SparqlConnectionRequest {
	
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

}
