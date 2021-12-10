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


package com.ge.research.semtk.services.ingestion;

import javax.validation.constraints.NotNull;

import com.ge.research.semtk.sparqlX.SparqlConnection;

import io.swagger.annotations.ApiModelProperty;


public class IngestionFromStringsAndClassRequestBody extends FromStringsRequestBody {
	@NotNull
	@ApiModelProperty(
			value = "classURI",
			required = true,
			example = "http://myprefix#className")
	public String classURI;
	
	@NotNull
	@ApiModelProperty(
			value = "connection",
			required = true,
			example = "{ connection json }")
	private String connection;

	@ApiModelProperty(
			value = "idRegex",
			required = false,
			example = "identifier")
	private String idRegex = "identifier";
	
	public String getClassURI() {
		return classURI;
	}

	public String getConnection() {
		return this.connection;
	}
	
	public SparqlConnection buildConnection() throws Exception {
		return new SparqlConnection(this.connection);
	}
	
	public String getIdRegex() {
		return idRegex;
	}
}
