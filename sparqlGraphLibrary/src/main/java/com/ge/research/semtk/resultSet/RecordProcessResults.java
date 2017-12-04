/**
 ** Copyright 2016 General Electric Company
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
import com.ge.research.semtk.resultSet.GeneralResultSet;

public class RecordProcessResults extends GeneralResultSet{

	public static final String RESULTS_BLOCK_NAME = "recordProcessResults";
	
	int recordsProcessed = 0;
	int failuresEncountered = 0;
	
	public RecordProcessResults(Boolean succeeded) {
		super(succeeded);
	}

	public RecordProcessResults() {
		super();	
	}
	
	public RecordProcessResults(JSONObject encoded) throws EndpointNotFoundException{
		super();
		this.readJson(encoded);
		JSONObject unwrapped = (JSONObject) encoded.get(RESULTS_BLOCK_NAME);
		
		// copy failuresEncountered and recordsProcessed up to the top level
		if(unwrapped.containsKey("failuresEncountered")){
			this.failuresEncountered = ((Long) unwrapped.get("failuresEncountered")).intValue();}
		if(unwrapped.containsKey("recordsProcessed")){
			this.recordsProcessed = ((Long) unwrapped.get("recordsProcessed")).intValue();}
	}
	
	@Override
	public String getResultsBlockName() {
		return RESULTS_BLOCK_NAME;
	}

	public int getRecordsProcessed() {
		return this.recordsProcessed;
	}
	
	public void setRecordsProcessed(int recordsProcessed) {
		this.recordsProcessed = recordsProcessed;
	}
	
	public int getFailuresEncountered() {
		return this.failuresEncountered;
	}
	
	public void setFailuresEncountered(int failuresEncountered){
		this.failuresEncountered = failuresEncountered;
	}
	
	@Override
	public void addResultsJSON(JSONObject actualResults) {
		// TODO Auto-generated method stub
		this.resultsContents = this.addPreamble(actualResults);
	}	
	
	public void addResults(Table table) throws Exception {
		if(table.getRows().size() > 0){
			this.resultsContents = this.addPreamble(table.toJson());
			this.addRationaleMessage("at least one error occurred. please check embedded error table for details");
		}
		else{
			this.resultsContents = this.addPreamble(null);
		}
	}

	private JSONObject addPreamble(JSONObject resultsWithoutPreamble){
		JSONObject retval = new JSONObject();
		
		retval.put("recordsProcessed", this.recordsProcessed);
		retval.put("failuresEncountered", this.failuresEncountered);
		if(resultsWithoutPreamble != null){
			retval.put("errorTable", resultsWithoutPreamble);
		}
		return retval;
	}

	@Override
	public Object getResults() throws Exception {
		// TODO IMPLEMENT and change return type from Object to something more useful
		throw new Exception("Not implemented yet");
	}
	
	protected void processConstructJson(JSONObject encoded) {	
		if(encoded.get(getResultsBlockName()) != null){
			this.resultsContents = (JSONObject) encoded.get(getResultsBlockName());
		}
	}


}
