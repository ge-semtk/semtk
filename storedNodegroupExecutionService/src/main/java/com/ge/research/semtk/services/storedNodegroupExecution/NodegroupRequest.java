package com.ge.research.semtk.services.storedNodegroupExecution;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class NodegroupRequest {

	private String jsonRenderedNodeGroup;
	
	public JSONObject getJsonNodeGroup() throws Exception{
		
		JSONParser prsr = new JSONParser();
		JSONObject retval = null;
			
		try {
			retval = (JSONObject) prsr.parse(this.jsonRenderedNodeGroup);
		} catch (ParseException e) {
			throw new Exception("Incoming Nodegroup not in a valid JSON structure");
		}
		return retval;
	}

	public void setJsonRenderedNodeGroup(String jsonRenderedNodeGroup) {
		this.jsonRenderedNodeGroup = jsonRenderedNodeGroup;
	}
	
}
