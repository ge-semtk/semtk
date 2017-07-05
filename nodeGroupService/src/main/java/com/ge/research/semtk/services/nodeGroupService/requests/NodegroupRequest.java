package com.ge.research.semtk.services.nodeGroupService.requests;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class NodegroupRequest {

	private String jsonRenderedNodeGroup;
	
	public JSONObject getJsonNodeGroup(){
		
		JSONParser prsr = new JSONParser();
		JSONObject retval = null;
			
		try {
			retval = (JSONObject) prsr.parse(this.jsonRenderedNodeGroup);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}

	public void setJsonRenderedNodeGroup(String jsonRenderedNodeGroup) {
		this.jsonRenderedNodeGroup = jsonRenderedNodeGroup;
	}
	
}
