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

package com.ge.research.semtk.services.nodeGroupExecution.requests;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.sparqlX.SparqlConnection;

import io.swagger.v3.oas.annotations.media.Schema;

// TODO: note there is a newer SparqlConnectionRequest in the springutil that has conflicting names.
public class SparqlConnRequestBody {
	
	@Schema(
			description = "Connection json or \"NODEGROUP_DEFAULT\" flag",
			required = true,
			example = "NODEGROUP_DEFAULT")
	private String sparqlConnection;
	
	public String getSparqlConnection() {
		return this.sparqlConnection;
	}
	public SparqlConnection buildSparqlConnection() throws Exception {
		if (sparqlConnection.equals(NodeGroupExecutor.USE_NODEGROUP_CONN_STR_SHORT)) {
			return NodeGroupExecutor.get_USE_NODEGROUP_CONN();
		} else {
			return new SparqlConnection(this.sparqlConnection);
		}
	}
	
	public void setSparqlConnection(String sparqlConnection) {
		this.sparqlConnection = sparqlConnection;
	}
	
	/**
	 * Validate request contents.  Throws an exception if validation fails.
	 */
	public void validate() throws Exception{
		if(sparqlConnection == null || sparqlConnection.trim().isEmpty()){
			throw new Exception("Request is missing 'sparqlConnection'");
		}
	}
}

