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

package com.ge.research.semtk.services.nodeGroupService.requests;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;

import io.swagger.annotations.ApiModelProperty;

public class NodegroupRequest {

	@ApiModelProperty(
			value = "jsonRenderedNodeGroup",
			required = true,
			example = "{ complex json }")	
	private String jsonRenderedNodeGroup;
	
	/**
	 * Get just the nodegroup out of the json
	 * @return
	 * @throws Exception
	 */
	public NodeGroup getNodeGroup() throws Exception {
		return this.getSparqlGraphJson().getNodeGroup();
	}
	
	/**
	 * 
	 * @return SparqlGraphJson which might contain just a NodeGroup or a full SGJson
	 * @throws Exception
	 */
	public SparqlGraphJson getSparqlGraphJson() throws Exception {
		
		SparqlGraphJson ret = null;
			
		try {
			ret = new SparqlGraphJson(this.jsonRenderedNodeGroup);
		} catch (Exception e) {
			throw new Exception("Error parsing nodegroup json", e);
		}
		return ret;
	}

	public void setJsonRenderedNodeGroup(String jsonRenderedNodeGroup) {
		this.jsonRenderedNodeGroup = jsonRenderedNodeGroup;
	}
	
	public void validate() throws Exception {
		// all validation happens in getJsonNodeGroup
	}
}
