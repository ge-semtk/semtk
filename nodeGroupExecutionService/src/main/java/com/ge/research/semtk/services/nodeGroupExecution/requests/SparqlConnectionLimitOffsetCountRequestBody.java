package com.ge.research.semtk.services.nodeGroupExecution.requests;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;

public class SparqlConnectionLimitOffsetCountRequestBody extends SparqlConnectionRequest {
	
	@Schema(
			description = "Maximum number of results to return.<br>Overrides LIMIT stored in nodegroup.",
			required = false,
			example = "-1")
	private int limitOverride = -1;
	
	@Schema(
			description = "Query offset.<br>Overrides OFFSET stored in nodegroup.",
			required = false,
			example = "-1")
	private int offsetOverride = -1;
	
	@Schema(
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
