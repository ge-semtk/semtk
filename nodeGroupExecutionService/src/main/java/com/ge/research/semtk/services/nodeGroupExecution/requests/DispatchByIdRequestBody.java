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

package com.ge.research.semtk.services.nodeGroupExecution.requests;

import io.swagger.v3.oas.annotations.media.Schema;

public class DispatchByIdRequestBody extends DispatchRequestBody {
	
	@Schema(
			required = true,
			example = "demoNodegroup")
	private String nodeGroupId;
	
	@Schema(
			description = "Maximum number of results to return.<br>Overrides LIMIT stored in nodegroup.",
			required = false,
			example = "-1")
	private int limitOverride = -1;
	
	@Schema(
			description = "Query offset.<br>Overrides OFFSET stored in nodegroup.",
			required = false,
			example = "-1")
	private int offsetOverride = -1;
	
	// This is custom optional field only available through straight REST
	// for Saqib @ collins
	@Schema(
			description = "Prune select table return down to one column and uniquify.",
			required = false,
			example = "favorite_column")
	private String pruneToColumn = null;
	
	public int getLimitOverride() {
		return limitOverride;
	}
	public void setLimitOverride(int limitOverride) {
		this.limitOverride = limitOverride;
	}
	public int getOffsetOverride() {
		return offsetOverride;
	}
	public void setOffsetOverride(int offsetOverride) {
		this.offsetOverride = offsetOverride;
	}
	public void setNodeGroupId(String nodeGroupId){
		this.nodeGroupId = nodeGroupId;
	}
	public String getNodeGroupId(){
		return this.nodeGroupId;
	}	
	
	public void setPruneToColumn(String val) {
		this.pruneToColumn = val;
	}
	public String getPruneToColumn() {
		return this.pruneToColumn;
	}
	
	/**
	 * Validate request contents.  Throws an exception if validation fails.
	 */
	public void validate() throws Exception{
		super.validate();

		if(nodeGroupId == null || nodeGroupId.trim().isEmpty()){
			throw new Exception("Request is missing 'nodeGroupId'");
		}
	}
}
