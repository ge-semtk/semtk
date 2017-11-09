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

package com.ge.research.semtk.services.dispatch;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.utility.LocalLogger;

public class NodegroupRequestBody {
	private String jsonRenderedNodeGroup;
	 
	public void setjsonRenderedNodeGroup(String jsonRenderedNodeGroup){
		this.jsonRenderedNodeGroup = jsonRenderedNodeGroup;
	}
	
	public String getJsonRenderedNodeGroup(){
		return this.jsonRenderedNodeGroup;
	}
	
	public NodeGroup getNodeGroup() throws Exception{
		JSONParser prsr = new JSONParser();
		JSONObject jNodeGroup = (JSONObject) prsr.parse(this.jsonRenderedNodeGroup);
		return NodeGroup.getInstanceFromJson(jNodeGroup);		
	}
	
	public JSONObject getJsonNodeGroup(){
		JSONParser prsr = new JSONParser();
		JSONObject retval = null;
		try {
			retval = (JSONObject) prsr.parse(this.jsonRenderedNodeGroup);
		} catch (ParseException e) {
			LocalLogger.printStackTrace(e);	
		}
		
		return retval;
	}
	
}
