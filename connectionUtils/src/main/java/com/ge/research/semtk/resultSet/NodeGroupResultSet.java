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

import com.ge.research.semtk.connutil.EndpointNotFoundException;

/**
 * Only exists for backwards compatibility with MATERIA
 * @author 200001934
 *
 */
public class NodeGroupResultSet extends GeneralResultSet{
	
	public static final String RESULTS_BLOCK_NAME = "NodeGroup";  
	
	public NodeGroupResultSet() {
		super();		
	}	
	
	public NodeGroupResultSet(JSONObject encoded) throws EndpointNotFoundException {
		super();
		this.readJson(encoded);
	}
	
	public NodeGroupResultSet(Boolean succeeded) {
		super(succeeded);
	}

	public NodeGroupResultSet(Boolean succeeded, String rationale) {
		super(succeeded);
		addRationaleMessage(rationale);
	}	
	
	@Override
	public String getResultsBlockName() {
		return RESULTS_BLOCK_NAME;
	}

	@Override
	public JSONObject getResults() throws Exception {
		// TODO Auto-generated method stub
		return this.resultsContents;
	}
	
}
