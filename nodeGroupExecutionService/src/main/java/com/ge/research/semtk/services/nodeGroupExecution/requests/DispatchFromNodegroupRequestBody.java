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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.v3.oas.annotations.media.Schema;

public class DispatchFromNodegroupRequestBody extends DispatchRequestBody {

	@Schema(
	   name = "jsonRenderedNodegroup",
	   required = true,
	   example = "\"{ \"version\": 2,\"sparqlConn\":{...},\"sNodeGroup\": {}... }\""
	   )
	private String jsonRenderedNodeGroup;
	
	public void setJsonRenderedNodeGroup(String jsonRenderedNodeGroup) {
		this.jsonRenderedNodeGroup = jsonRenderedNodeGroup;
	}
	
	public JSONObject getJsonNodeGroup(){
		JSONParser prsr = new JSONParser();
		JSONObject retval = null;
		try {
			retval = (JSONObject) prsr.parse(this.jsonRenderedNodeGroup);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			LocalLogger.printStackTrace(e);
		}
		return retval;
	}

}
