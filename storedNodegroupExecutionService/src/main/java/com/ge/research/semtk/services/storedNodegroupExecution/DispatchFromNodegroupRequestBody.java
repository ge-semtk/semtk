package com.ge.research.semtk.services.storedNodegroupExecution;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DispatchFromNodegroupRequestBody {
	private String jsonRenderedNodeGroup;
	private String sparqlConnection;
	private String externalDataConnectionConstraints;
	private String runtimeConstraints;
	
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
			e.printStackTrace();
		}
		return retval;
	}

	public String getSparqlConnection() {
		return sparqlConnection;
	}
	public void setSparqlConnection(String sparqlConnection) {
		this.sparqlConnection = sparqlConnection;
	}
	public String getExternalDataConnectionConstraints() {
		return externalDataConnectionConstraints;
	}
	public void setExternalDataConnectionConstraints(String externalDataConnectionConstraints) {
		this.externalDataConnectionConstraints = externalDataConnectionConstraints;
	}
	public String getRuntimeConstraints(){
		return(this.runtimeConstraints);
	}
	public void setRuntimeConstraints(String runtimeConstraints){
		this.runtimeConstraints = runtimeConstraints;
	}
	
}
