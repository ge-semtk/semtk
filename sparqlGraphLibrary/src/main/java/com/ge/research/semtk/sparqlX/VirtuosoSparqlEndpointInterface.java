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


package com.ge.research.semtk.sparqlX;

import org.json.simple.JSONObject;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Interface to Virtuoso SPARQL endpoint
 */
public class VirtuosoSparqlEndpointInterface extends SparqlEndpointInterface {
	
	/**
	 * Constructor
	 */
	public VirtuosoSparqlEndpointInterface(String server, String dataset)	throws Exception {
		super(server, dataset);
		// TODO Auto-generated constructor stub
	}
 
	/**
	 * Constructor used for authenticated connections... like insert/update/delete/clear
	 */
	public VirtuosoSparqlEndpointInterface(String server, String dataset, String user, String pass)	throws Exception {
		super(server, dataset, user, pass);
		// TODO Auto-generated constructor stub
	}
	
	public String getServerType() {
		return "virtuoso";
	}
	
	/**
	 * Build a GET URL
	 */
	public String getGetURL(){
		if(this.userName != null && this.userName.length() > 0 && this.password != null && this.password.length() > 0){
			return String.format("%s:%s/sparql-auth/?default-graph-uri=%s&format=json&query=", server, this.port, this.dataset);  
		}else{
			return String.format("%s:%s/sparql/?default-graph-uri=%s&format=json&query=", this.server, this.port, this.dataset); 
		}
	}
	
	/**
	 * Build a POST URL
	 */
	public String getPostURL(){
		if(this.userName != null && this.userName.length() > 0 && this.password != null && this.password.length() > 0){
			return String.format("%s:%s/sparql-auth", this.server, this.port);
		}else{
			return String.format("%s:%s/sparql", this.server, this.port);
		}
	}

	/**
	 * Build a upload URL
	 */
	public String getUploadURL() throws Exception{
		if(this.userName != null && this.userName.length() > 0 && this.password != null && this.password.length() > 0){
			return this.server + "/sparql-graph-crud-auth";
		}else{
			throw new Exception("Virtuoso requires authentication for file upload");	
		}
	}
	
	/**
	 * Handle an empty response
	 * (if a Virtuoso response is empty, then something is wrong)
	 * @throws Exception 
	 */
	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Virtuoso returning empty response (could be wrong username/password)");	
	}

	@Override
	public JSONObject executeTestQuery() throws Exception {
		final String sparql = "select ?Concept where {[] a ?Concept} LIMIT 1";
		try {
			return executeQuery(sparql, SparqlResultTypes.TABLE);
		} catch (Exception e) {
			throw new Exception("Failure executing test query.  Authentication might have failed.", e);
		}
	}

	@Override
	public SparqlEndpointInterface copy() throws Exception {
		VirtuosoSparqlEndpointInterface retval = null;
		
		retval = new VirtuosoSparqlEndpointInterface(this.getServerAndPort(), this.dataset, this.userName, this.password);
		
		return (SparqlEndpointInterface) retval;
	}
}
