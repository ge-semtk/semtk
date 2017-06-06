package com.ge.research.semtk.services.dispatch;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.ge.research.semtk.belmont.NodeGroup;

public class QueryRequestBody extends NodegroupRequestBody {

	// revisit this later to determine the best way to insert user information in case we want to
	// to bolt security to it . 
	
	private String constraintSet;

	public void setConstraintSet(String constraintSet){
		this.constraintSet = constraintSet;
	}
	
	public String getConstraintSet(){
		return this.constraintSet;
	}
	
	public JSONObject getConstraintSetJson(){
		JSONParser prsr = new JSONParser();
		JSONObject retval = null;
		try {
			retval = (JSONObject) prsr.parse(this.constraintSet);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		
		return retval;
	}
}
