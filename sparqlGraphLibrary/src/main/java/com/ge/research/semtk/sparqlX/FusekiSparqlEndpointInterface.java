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

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Interface to Fuseki SPARQL endpoint
 * TODO NOT TESTED!!!
 */
public class FusekiSparqlEndpointInterface extends SparqlEndpointInterface {

	public FusekiSparqlEndpointInterface(String server, String dataset)	throws Exception {
		super(server, dataset);
		// TODO Auto-generated constructor stub
	}

	public FusekiSparqlEndpointInterface(String server, String dataset, String user, String pass)	throws Exception {
		super(server, dataset, user, pass);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Build a GET URL
	 */
	public String getGetURL(){
		return String.format("%s:%s/%s/query?output=json&query=", this.server, this.port, this.dataset);
	}


	/**
	 * Build a POST URL
	 */
	public String getPostURL(){
		return String.format("%s:%s/%s/update?update=", this.server, this.port, this.dataset);	
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
	
	
}
