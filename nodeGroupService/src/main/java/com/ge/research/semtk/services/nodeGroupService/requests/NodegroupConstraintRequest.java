/**
 ** Copyright 2018 General Electric Company
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

package com.ge.research.semtk.services.nodeGroupService.requests;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.sparqlX.SparqlConnection;

import io.swagger.v3.oas.annotations.media.Schema;


public class NodegroupConstraintRequest extends RuntimeConstraintRequest {

	// total copy-and-paste from NodegroupRequest because I can't figure out 
	// how to fake multiple inheritance and keep the annotations.
	@Schema(
			name = "jsonRenderedNodeGroup",
			required = true,
			example = 	"{ \"sparqlConn\": {...}, \"sNodeGroup\": {...}, \"importSpec\": {...} }\n" +
						"or\n"+
						"{\"version\": 3, \"limit\": 0, \"offset\": 0, \"sNodeList\": [...], \"orderBy\": []}"
			           )	
	private String jsonRenderedNodeGroup;
	
	/**
	 * Get just the nodegroup out of the json
	 * @return
	 * @throws Exception
	 */
	public NodeGroup buildNodeGroup() throws Exception {
		return this.buildSparqlGraphJson().getNodeGroup();
	}


	public SparqlConnection buildConnection() throws Exception {
		SparqlConnection conn = buildSparqlGraphJsonWithConn().getSparqlConn();
		if (conn == null) { 
			throw new Exception("Operation requires a connection, not a legacy plain nodegroup json");
		}
		return conn;
	}
	
	/**
	 * 
	 * @return SparqlGraphJson which might contain just a NodeGroup or a full SGJson
	 * @throws Exception
	 */
	public SparqlGraphJson buildSparqlGraphJson() throws Exception {
		
		SparqlGraphJson ret = null;
			
		try {
			ret = new SparqlGraphJson(this.jsonRenderedNodeGroup);
		} catch (Exception e) {
			throw new Exception("Error parsing jsonRenderedNodeGroup", e);
		}
		return ret;
	}
	
	/**
	 * Make sure jsonRenderedNodeGroup contains nodegroup and connection, and return
	 * @return
	 * @throws Exception
	 */
	public SparqlGraphJson buildSparqlGraphJsonWithConn() throws Exception {
		
		SparqlGraphJson ret = this.buildSparqlGraphJson();
		if (ret.getSparqlConn() == null) {
			throw new Exception("Missing sparql connection: jsonRenderedNodeGroup param has no sparqlConn");
		}
		
		if (ret.getNodeGroup() == null) {
			throw new Exception("Missing nodegroup: jsonRenderedNodeGroup param has no sNodeGroup");
		}
		return ret;
	}

	public void setJsonRenderedNodeGroup(String jsonRenderedNodeGroup) {
		this.jsonRenderedNodeGroup = jsonRenderedNodeGroup;
	}

}
