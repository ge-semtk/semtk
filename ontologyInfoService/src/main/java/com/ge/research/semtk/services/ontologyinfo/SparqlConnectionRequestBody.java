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


package com.ge.research.semtk.services.ontologyinfo;

import com.ge.research.semtk.sparqlX.SparqlConnection;

public class SparqlConnectionRequestBody {

	private String sparqlConnectionJson = "";
	
	public String getSparqlConnectionJson() {
		return sparqlConnectionJson;
	}
	public void setSparqlConnectionJson(String sparqlConnection) {
		this.sparqlConnectionJson = sparqlConnection;
	}
	
	public SparqlConnection getSparqlConnection() throws Exception {
		return new SparqlConnection(this.sparqlConnectionJson);
	}

}
