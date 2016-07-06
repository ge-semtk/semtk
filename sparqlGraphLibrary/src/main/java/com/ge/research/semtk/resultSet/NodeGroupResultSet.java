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

import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.belmont.NodeGroup;

/**
 * A result set containing a NodeGroup (graph)
 */
public class NodeGroupResultSet extends GeneralResultSet{
	
	public static final String RESULTS_BLOCK_NAME = "NodeGroup";
	
	public NodeGroupResultSet(Boolean succeeded) {
		super(succeeded);
	}
	
	public NodeGroupResultSet() {
		super();	
	}
	
	@Override
	public String getResultsBlockName() {
		return RESULTS_BLOCK_NAME;
	}
	
	@Override
	public NodeGroup getResults() throws Exception {
		NodeGroup ng = NodeGroup.fromConstructJSON(resultsContents);
		return ng;
	}
	
	/**
	 * Add results as a NodeGroup
	 * @throws Exception 
	 */
	public void addResults(NodeGroup ng) throws Exception{
		addResultsJSON(ng.toJson());
	}

	protected void processConstructJson(JSONObject encoded) {		
		if(encoded.get(getResultsBlockName()) != null){
			this.resultsContents = (JSONObject) encoded.get(getResultsBlockName());
		}
	}


}
