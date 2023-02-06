/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.services.nodeGroupExecution.requests;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

import io.swagger.v3.oas.annotations.media.Schema;

public class DispatchConstructToGraphByIdRequestBody extends DispatchByIdRequestBody {
	
	@Schema(
			description = "Send results as rdf this endpoint",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "{\"type\":\"fuseki\",\"url\":\"http://localhost:3030/EXAMPLE\",\"graph\":\"http://example\"}")
	private String resultsEndpoint;
	
	
	public String getResultsEndpoint() {
		return this.resultsEndpoint;
	}
	public SparqlEndpointInterface buildResultsSei() throws Exception {
		return SparqlEndpointInterface.getInstance((JSONObject)(new JSONParser().parse(this.resultsEndpoint)));
	}
	
	public void setResultsEndpoint(String s) {
		this.resultsEndpoint = s;
	}
	
	/**
	 * Validate request contents.  Deprecated with @Schema
	 */
	public void validate() throws Exception{
		super.validate();

	}
}
