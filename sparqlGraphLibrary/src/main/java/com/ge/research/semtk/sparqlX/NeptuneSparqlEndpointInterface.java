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

import org.apache.http.client.methods.HttpPost;
import org.json.simple.JSONObject;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Interface to Virtuoso SPARQL endpoint
 */
public class NeptuneSparqlEndpointInterface extends SparqlEndpointInterface {
	
	/**
	 * Constructor
	 */
	public NeptuneSparqlEndpointInterface(String server, String graph)	throws Exception {
		super(server, graph);
		// TODO Auto-generated constructor stub
	}
 
	/**
	 * Constructor used for authenticated connections... like insert/update/delete/clear
	 */
	public NeptuneSparqlEndpointInterface(String server, String graph, String user, String pass)	throws Exception {
		super(server, graph, user, pass);
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
			return String.format("%s:%s/sparql-auth/?default-graph-uri=%s&format=json&query=", server, this.port, this.graph);  
		}else{
			return String.format("%s:%s/sparql/?default-graph-uri=%s&format=json&query=", this.server, this.port, this.graph); 
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
			return this.server + "/sparql-graph-crud-auth";
	}
	
	public JSONObject executeUpload(byte[] owl) throws Exception {
		throw new Exception("Unimplmenented");
	}

	/**
	 * Handle an empty response
	 * (if a Virtuoso response is empty, then something is wrong)
	 * @throws Exception 
	 */
	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Neptune returned empty response");	
	}

	@Override
	protected void addHeaders(HttpPost httppost, String resultsFormat) {
		
		httppost.addHeader("Accept",resultsFormat);
	}
	
	@Override
	public SparqlEndpointInterface copy() throws Exception {
		NeptuneSparqlEndpointInterface retval = null;
		
		retval = new NeptuneSparqlEndpointInterface(this.getServerAndPort(), this.graph, this.userName, this.password);
		
		return (SparqlEndpointInterface) retval;
	}
	
	@Override
	protected String getResposeTextExplanation(String responseTxt) {
		if ( responseTxt.contains("Some known error from Neptune")) {
			return "SemTk says: Some known error from Neptune.\n";
		} else {
			return "Non-JSON error was returned from Neptune.\n";
		}
	}
}
