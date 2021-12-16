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

package com.ge.research.semtk.springutilib.requests;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;

import io.swagger.annotations.ApiModelProperty;

public class SNodeGroupRequest {

	@ApiModelProperty(
			value = "nodeGroup",
			required = true,
			example = "{\"version\": 3, \"limit\": 0, \"offset\": 0, \"sNodeList\": [...], \"orderBy\": []}"
	)	
	private String nodeGroup;
	
	/**
	 * Get just the nodegroup out of the json
	 * @return
	 * @throws Exception
	 */
	public NodeGroup getNodeGroup() throws Exception {
		return NodeGroup.getInstanceFromJson((JSONObject) new JSONParser().parse(this.nodeGroup));
	}
}
