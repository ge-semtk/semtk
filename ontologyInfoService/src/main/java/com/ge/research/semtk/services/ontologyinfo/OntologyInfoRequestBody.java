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


package com.ge.research.semtk.services.ontologyinfo;

import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.sparqlX.SparqlConnection;

import io.swagger.annotations.ApiModelProperty;

/**
 * For service calls needing SPARQL connection and domain
 */
public class OntologyInfoRequestBody {
	@ApiModelProperty(
			value = "jsonRenderedSparqlConnection",
			required = true,
			example = 	"{ \"name\":\"my-conn\",\"serverType\":\"virtuoso\",\"dataServerUrl\": ... }"
			           )	
	private String jsonRenderedSparqlConnection = "";
	
	public void setJsonRenderedSparqlConnection(String jsonRenderedSparqlConnection) {
		this.jsonRenderedSparqlConnection = jsonRenderedSparqlConnection;
	}
	
	public SparqlConnection getJsonRenderedSparqlConnection() throws Exception{
		return (new SparqlConnection(jsonRenderedSparqlConnection));
	}
    
    /**
     * Validate request contents.  Throws an exception if validation fails.
     */
    public void validate() throws Exception{
    	try{
    	SparqlConnection conn = new SparqlConnection(this.jsonRenderedSparqlConnection); 
    	}
    	catch(Exception e){
    		throw new Exception("unable to create ontology info: " + e.getMessage(), e);
    	}
    }
}
