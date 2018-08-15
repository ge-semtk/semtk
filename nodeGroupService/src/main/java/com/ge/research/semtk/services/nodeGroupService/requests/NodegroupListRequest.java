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

package com.ge.research.semtk.services.nodeGroupService.requests;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;

import io.swagger.annotations.ApiModelProperty;

public class NodegroupListRequest {

	@ApiModelProperty(
			value = "jsonRenderedNodeGroupArray[]",
			required = true,
			example = "[{ complex json }, { another }]")	
	private String[] jsonRenderedNodeGroupArray;
	
	/**
	 * Get just the nodegroup out of the json
	 * @return
	 * 
	 * @throws Exception
	 */
	public NodeGroup[] getNodeGroupList() throws Exception {
		SparqlGraphJson [] sgjsonArray = this.getSparqlGraphJsonArray();
		NodeGroup [] ret = new NodeGroup[sgjsonArray.length];
		
		for (int i=0; i < ret.length; i++) {
			ret[i] = sgjsonArray[i].getNodeGroup();
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @return SparqlGraphJsonArray which might contain just a NodeGroups or a full SGJsons
	 * @throws Exception
	 */
	public SparqlGraphJson [] getSparqlGraphJsonArray() throws Exception {
		
		SparqlGraphJson [] ret = new SparqlGraphJson[this.jsonRenderedNodeGroupArray.length];
			
		try {
			for (int i=0; i < this.jsonRenderedNodeGroupArray.length; i++) {
				ret[i] = new SparqlGraphJson(this.jsonRenderedNodeGroupArray[i]);
			}
		} catch (Exception e) {
			throw new Exception("Error parsing nodegroup json", e);
		}
		return ret;
	}

	public void setJsonRenderedNodeGroupArray(String [] jsonRenderedNodeGroupArr) {
		this.jsonRenderedNodeGroupArray = jsonRenderedNodeGroupArr;
	}
	
}
