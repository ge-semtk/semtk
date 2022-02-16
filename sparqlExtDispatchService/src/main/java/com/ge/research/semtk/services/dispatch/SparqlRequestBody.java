/**
 ** Copyright 2017 General Electric Company
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

package com.ge.research.semtk.services.dispatch;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

public class SparqlRequestBody{

	private String sparqlConnectionJson;
	private String rawSparqlQuery;
	private SparqlResultTypes resultType = SparqlResultTypes.TABLE;
	
	public String getRawSparqlQuery() {
		return rawSparqlQuery;
	}
	public void setRawSparqlQuery(String rawSparqlQuery) {
		this.rawSparqlQuery = rawSparqlQuery;
	}
	public String getSparqlConnectionJson() {
		return sparqlConnectionJson;
	}
	public void setSparqlConnectionJson(String sparqlConnectionJson) {
		this.sparqlConnectionJson = sparqlConnectionJson;
	}
	public void setResultType(SparqlResultTypes rt) {
		this.resultType = rt;
	}
	public SparqlResultTypes getResultType() {
		return this.resultType;
	}

	
	public SparqlConnection getConnection() throws Exception{
		return (new SparqlConnection(sparqlConnectionJson));		
	}
	
	
}
