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

package com.ge.research.semtk.services.nodeGroupExecution;

public class DispatchByIdRequestBody extends DispatchRequestBody {
	
	private String nodeGroupId;
	private int limitOverride = -1;
	private int offsetOverride = -1;
	
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
