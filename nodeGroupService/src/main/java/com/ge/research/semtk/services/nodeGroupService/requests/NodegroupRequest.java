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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.belmont.NodeGroup;

public class NodegroupRequest {

	private String jsonRenderedNodeGroup;
	
	public NodeGroup getNodeGroup() throws Exception {
		return NodeGroup.getInstanceFromJson(this.getJsonNodeGroup());
	}
	
	public JSONObject getJsonNodeGroup() throws Exception {
		
		JSONParser prsr = new JSONParser();
		JSONObject retval = null;
			
		try {
			retval = (JSONObject) prsr.parse(this.jsonRenderedNodeGroup);
		} catch (ParseException e) {
			throw new Exception("Error parsing nodegroup json", e);
		}
		return retval;
	}

	public void setJsonRenderedNodeGroup(String jsonRenderedNodeGroup) {
		this.jsonRenderedNodeGroup = jsonRenderedNodeGroup;
	}
	
	public void validate() throws Exception {
		// all validation happens in getJsonNodeGroup
	}
}
