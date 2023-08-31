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


package com.ge.research.semtk.springutilib.requests;

import javax.validation.constraints.Pattern;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * For service calls needing SPARQL connection information.
 */
public class SparqlEndpointsRequestBody {
	
	// missing the deprecated "dataset"  So don't splice this into legacy code.
	
	@Schema(required = true,  example = "http://my_server:2420")
	@Pattern(regexp="https?://.*:[0-9]+", message="Triplestore URL with port is required")
	public String fromServerAndPort;  
	
	@Schema(required = true,  example = "virtuoso|fuseki|neptune")
	@Pattern(regexp="^(virtuoso|fuseki|neptune)$", message="valid server types: virtuoso|fuseki|neptune")
	public String fromServerType;		
	
	@Schema(required = true,  example = "http://graph/name")
	public String fromGraph;
	
	@Schema(required = true,  example = "http://my_server:2420")
	@Pattern(regexp="https?://.*:[0-9]+", message="Triplestore URL with port is required")
	public String toServerAndPort;  
	
	@Schema(required = true,  example = "virtuoso|fuseki|neptune")
	@Pattern(regexp="^(virtuoso|fuseki|neptune)$", message="valid server types: virtuoso|fuseki|neptune")
	public String toServerType;		
	
	@Schema(required = true,  example = "http://graph/name")
	public String toGraph;
    
	@Schema(required = false)
	public String toUser = null;
    
	@Schema(required = false)
	public String toPassword = null;
    
    
	public String getFromServerAndPort() {
		return fromServerAndPort;
	}


	public String getFromServerType() {
		return fromServerType;
	}


	public String getFromGraph() {
		return fromGraph;
	}


	public String getToServerAndPort() {
		return toServerAndPort;
	}


	public String getToServerType() {
		return toServerType;
	}


	public String getToGraph() {
		return toGraph;
	}


	public String getToUser() {
		return toUser;
	}


	public String getToPassword() {
		return toPassword;
	}


	public SparqlEndpointInterface buildFromSei() throws Exception {
		return SparqlEndpointInterface.getInstance(fromServerType, fromServerAndPort, fromGraph);
	}
	
	public SparqlEndpointInterface buildToSei() throws Exception {
		return SparqlEndpointInterface.getInstance(toServerType, toServerAndPort, toGraph, toUser, toPassword);
	}
}
