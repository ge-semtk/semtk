package com.ge.research.semtk.services.dispatch;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.belmont.NodeGroup;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		
		return retval;
	}
	
}
