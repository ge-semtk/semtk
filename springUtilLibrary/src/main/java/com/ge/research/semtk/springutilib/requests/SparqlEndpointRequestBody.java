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
public class SparqlEndpointRequestBody {
	
	// missing the deprecated "dataset"  So don't splice this into legacy code.
	
	@Schema(required = true,  example = "http://my_server:2420")
	@Pattern(regexp="https?://.*:[0-9]+", message="Triplestore URL with port is required")
	public String serverAndPort;  
	
	@Schema(required = true,  example = "virtuoso|fuseki|neptune")
	@Pattern(regexp="^(virtuoso|fuseki|neptune)$", message="valid server types: virtuoso|fuseki|neptune")
	public String serverType;		
	
	@Schema(required = true,  example = "http://graph/name")
	public String graph;	        
    
	@Schema(required = false)
	public String user;
    
	@Schema(required = false)
	public String password;
    /**
     * Print request info to console
     */
    public void printInfo(){
		LocalLogger.logToStdOut("Connect to " + serverAndPort + " (" + serverType + "), graph " + this.getGraph());
    }
    
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
    
	public SparqlEndpointInterface buildSei() throws Exception {
		return SparqlEndpointInterface.getInstance(serverType, serverAndPort, graph, user, password);
	}
}
