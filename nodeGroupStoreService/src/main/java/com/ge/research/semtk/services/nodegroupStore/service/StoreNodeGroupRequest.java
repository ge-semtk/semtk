package com.ge.research.semtk.services.nodegroupStore.service;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class StoreNodeGroupRequest {

	private String jsonRenderedNodeGroup;
	private String name;
	private String comments;
		
	public JSONObject getJsonNodeGroup(){
	
		JSONParser prsr = new JSONParser();
		JSONObject retval = null;
		
		System.err.println("----------START MESSAGE-------------");
		
		System.err.println("incoming node group string for conversion: ");
		System.err.println(jsonRenderedNodeGroup);
		
		System.err.println("----------END MESSAGE---------------");
		
		
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
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getComments() {
		return comments;
	}
	
	public void setComments(String comments) {
		this.comments = comments;
	}
	
}
