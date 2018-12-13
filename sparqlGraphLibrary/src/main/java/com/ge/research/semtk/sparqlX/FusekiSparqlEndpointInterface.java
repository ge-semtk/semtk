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

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Interface to Fuseki SPARQL endpoint
 * TODO NOT TESTED!!!
 */
public class FusekiSparqlEndpointInterface extends SparqlEndpointInterface {

	public FusekiSparqlEndpointInterface(String server, String graph)	throws Exception {
		super(server, graph);
		// TODO Auto-generated constructor stub
	}

	public FusekiSparqlEndpointInterface(String server, String graph, String user, String pass)	throws Exception {
		super(server, graph, user, pass);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Build a GET URL
	 */
	public String getGetURL(){
		return String.format("%s:%s/%s/query?output=json&query=", this.server, this.port, this.graph);
	}


	/**
	 * Build a POST URL
	 */
	public String getPostURL(){
		return String.format("%s:%s/%s/update?update=", this.server, this.port, this.graph);	
	}	
	
	public JSONObject executeUpload(byte[] owl) throws AuthorizationException, Exception {
		this.authorizeUpload();
		throw new Exception("Unimplmenented");
	}
	
	/**
	 * Handle an empty response
	 * @throws Exception 
	 */
	@Override
	public void handleEmptyResponse() throws Exception {
		// do nothing for now	
	}

	@Override
	public String getUploadURL() throws Exception {
		// TODO Auto-generated method stub
		throw new Exception("Uploading owl is not yet implemented for Fuseki.");
		
	}

	@Override
	public String getServerType() {
		return null;
	}
	
	@Override
	public SparqlEndpointInterface copy() throws Exception {
		FusekiSparqlEndpointInterface retval = null;
		
		retval = new FusekiSparqlEndpointInterface(this.getServerAndPort(), this.graph, this.userName, this.password);
		
		return (SparqlEndpointInterface) retval;
	}
	
}
