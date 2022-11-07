package com.ge.research.semtk.springutilib.requests;

import io.swagger.v3.oas.annotations.media.Schema;

public class GraphNameRequestBody {

	@Schema(required = true,
			example = "http://localhost:3030/DEFAULT")
	public String serverAndPort;  	// e.g. http://localhost:2420
	
	@Schema(required = true,
			example = "fuseki")
	public String serverType;		// e.g. virtuoso
	
	@Schema(required = false)
	public boolean skipSemtkGraphs = false;
	
	@Schema(required = false)
	public boolean graphNamesOnly = true;

	public String getServerAndPort() {
		return serverAndPort;
	}

	public String getServerType() {
		return serverType;
	}

	public boolean getSkipSemtkGraphs() {
		return this.skipSemtkGraphs;
	}

	public boolean getGraphNamesOnly() {
		return this.graphNamesOnly;
	}
}
