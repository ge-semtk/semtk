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


package com.ge.research.semtk.services.ontologyinfo.requests;

import com.ge.research.semtk.sparqlX.SparqlConnection;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * For service calls needing SPARQL connection and domain
 * 
 * WARNING/NOTE: Please use this for future endpoints:
 *               		springutillib.requests.SparqlConnectionRequest
 */

public class OntologyInfoRequestBody {
	@Schema(
			name = "jsonRenderedSparqlConnection",
			required = false,
			example = 	"Deprecated version of 'conn'"
			)	
	private String jsonRenderedSparqlConnection = "";
	@Schema(
			name = "conn",
			required = false,
			example = 	"\"{ \"name\":\"my-conn\",\"serverType\":\"virtuoso\",\"dataServerUrl\": ... }\""
			)	
	private String conn = "";

	// support old fashioned jsonRenderedSparqlConnection
	@Deprecated
	public void setJsonRenderedSparqlConnection(String jsonRenderedSparqlConnection) {
		this.jsonRenderedSparqlConnection = jsonRenderedSparqlConnection;
	}
	
	@Deprecated
	public String getJsonRenderedSparqlConnection() {
		return jsonRenderedSparqlConnection;
	}
	
	public String getConn() throws Exception {
		if (conn.length()==0) {
			if (jsonRenderedSparqlConnection.length()==0) {
				throw new Exception("Missing both conn and jsonRenderedSparqlConnection");
			} else {
				return jsonRenderedSparqlConnection;
			}
		} else {
			return conn;
		}
	}

	public void setConn(String conn) {
		this.conn = conn;
	}
	public SparqlConnection buildSparqlConnection() throws Exception{
		return (new SparqlConnection(this.getConn()));
	}
}
