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
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * For service calls with optional SPARQL connection information.
 */
public class SparqlEndpointOptionalRequestBody {
	
	// missing the deprecated "dataset"  So don't splice this into legacy code.
	
	@Schema(required = false,  example = "http://my_server:2420")
	@Pattern(regexp="https?://.*:[0-9]+", message="Triplestore URL with port is required")
	public String serverAndPort = null;  
	
	@Schema(required = false,  example = "virtuoso|fuseki|neptune")
	@Pattern(regexp="^(virtuoso|fuseki|neptune)$", message="valid server types: virtuoso|fuseki|neptune")
	public String serverType = null;		
	
	@Schema(required = false,  example = "http://graph/name")
	public String graph = null;	        
    
	@Schema(required = false)
	public String user = null;
    
	@Schema(required = false)
	public String password = null;
   
    
    public String getGraph() {
		return this.graph;
    }

	public String getServerAndPort() {
		return serverAndPort;
	}

	public String getServerType() {
		return serverType;
	}
	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
    
	/** 
	 * get Sei or null 
	 * @return
	 * @throws Exception
	 */
	public SparqlEndpointInterface buildSei() throws Exception {
		if (serverType == null || serverAndPort == null || graph == null) {
			return null;
		} else if (user == null) {
			return SparqlEndpointInterface.getInstance(serverType, serverAndPort, graph);
		} else {
			return SparqlEndpointInterface.getInstance(serverType, serverAndPort, graph, user, password);
		}
	}
}
