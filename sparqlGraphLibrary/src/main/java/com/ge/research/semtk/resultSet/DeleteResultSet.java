/**
 ** Copyright 2017 General Electric Company
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
