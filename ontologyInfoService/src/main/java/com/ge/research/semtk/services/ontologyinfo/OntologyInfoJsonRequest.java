package com.ge.research.semtk.services.ontologyinfo;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.ontologyTools.OntologyInfo;

public class OntologyInfoJsonRequest {

	private String jsonRenderedOInfo;
	private String base;
	
	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public OntologyInfo getOInfo() throws Exception{
		
		JSONParser prsr = new JSONParser();
		JSONObject json = null;
			
		try {
			json = (JSONObject) prsr.parse(this.jsonRenderedOInfo);
			return new OntologyInfo(json);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void setJsonRenderedNodeGroup(String jsonRenderedOInfo) {
		this.jsonRenderedOInfo = jsonRenderedOInfo;
	}
	
}