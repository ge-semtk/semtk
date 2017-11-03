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

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.resultSet.GeneralResultSet;

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


	public static JSONObject getJsonLdResultsMetaData(JSONObject jsonLD) throws Exception{
		JSONObject retval = new JSONObject();
		
		try{
			System.err.println("incoming nodegroup json was:");
			System.err.println(jsonLD.toJSONString());
			
			String JSON_TYPE = "type";
			String JSON_NODE_COUNT = "node_count";
			
			
			// convert the jsonLD to a nodegroup.
			// note: this assumes that the results of the construct json can be transformed into a nodegroup.
			// this will break in the event that type info is omited, for instance. because this is intended to be
			// used with the dispatcher, it is a reasonable assumption for now.
			NodeGroup ngTemp = new NodeGroup();
			int nodeGroupNodeCount = 0;
			
			if(jsonLD.containsKey("@graph")){
				ngTemp.fromConstructJSON(jsonLD);
				nodeGroupNodeCount = ngTemp.getNodeCount();
			}
						
			retval.put(JSON_TYPE, "JSON-LD");
			retval.put(JSON_NODE_COUNT, nodeGroupNodeCount);
			
		return retval;
		}
		catch(Exception e){
			throw new Exception("Error assembling JSON header information for JSON-LD results: " + e.getMessage());
		}
	}
}
