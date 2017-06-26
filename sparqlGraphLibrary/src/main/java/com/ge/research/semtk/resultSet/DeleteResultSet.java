package com.ge.research.semtk.resultSet;

import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;

public class DeleteResultSet extends GeneralResultSet {

	public static final String RESULTS_BLOCK_NAME = "deleteresults";  
	public static final String DELETE_JSONKEY = "@deletedTriples";

	public DeleteResultSet(JSONObject encoded) throws EndpointNotFoundException {
		super();
		this.readJson(encoded);
	}
	
	public DeleteResultSet(Boolean succeeded) {
		super(succeeded);
	}

	public DeleteResultSet() {
		super();	
	}
	
	@Override
	public String getResultsBlockName() {
		return RESULTS_BLOCK_NAME;
	}
	
	@Override
	public Integer getResults() throws Exception {
		
		Integer retval = (Integer)this.resultsContents.get(DELETE_JSONKEY);
		
		return retval;
	}
	
	public void addResults(Integer triplesDeleted) throws Exception {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(DELETE_JSONKEY, triplesDeleted); 
		addResultsJSON(jsonObj);
	}


}
