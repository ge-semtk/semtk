package com.ge.research.semtk.services.results.requests;

import com.ge.research.semtk.springutilib.requests.IdRequest;
import com.ge.research.semtk.springutilib.requests.JobIdRequest;

public class JsonBlobRequestBody extends JobIdRequest {

	private String jsonBlobString;
	               
	public String getJsonBlobString() {
		return jsonBlobString;
	}

	public void setJsonBlobString(String jsonBlobString) {
		this.jsonBlobString = jsonBlobString;
	}
	
	
}
