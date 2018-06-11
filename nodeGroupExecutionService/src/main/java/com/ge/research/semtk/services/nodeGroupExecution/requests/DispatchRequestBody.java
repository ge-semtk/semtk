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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.Utility;

public class DispatchRequestBody extends SparqlConnRequestBody {
	
	private String externalDataConnectionConstraints;
	private String flags; // json array
	private String runtimeConstraints;

	
	public JSONObject getExternalDataConnectionConstraintsJson() throws Exception {
		if(this.externalDataConnectionConstraints == null || this.externalDataConnectionConstraints.trim().isEmpty()){
			return null;
		}
		return Utility.getJsonObjectFromString(this.externalDataConnectionConstraints);
	}
	public String getExternalDataConnectionConstraints() {
		return externalDataConnectionConstraints;
	}
	public void setExternalDataConnectionConstraints(String externalDataConnectionConstraints) {
		this.externalDataConnectionConstraints = externalDataConnectionConstraints;
	}
	
	
	public JSONArray getRuntimeConstraintsJson() throws Exception {
		if(this.runtimeConstraints == null || this.runtimeConstraints.trim().isEmpty()){
			return null;
		}
		return Utility.getJsonArrayFromString(this.runtimeConstraints);
	}
	public String getRuntimeConstraints(){
		return(this.runtimeConstraints);
	}
	public void setRuntimeConstraints(String runtimeConstraints){
		this.runtimeConstraints = runtimeConstraints;
	}
	
	
	public JSONArray getFlagsJson() throws Exception {
		if(this.flags == null || this.flags.trim().isEmpty()){
			return null;
		}
		return Utility.getJsonArrayFromString(this.flags);
	}
	public String getFlags() {
		return flags;
	}
	public void setFlags(String flags) {
		this.flags = flags;
	}
	
	/**
	 * Validate request contents.  Throws an exception if validation fails.
	 */
	public void validate() throws Exception{
		super.validate();
		// all in this class are optional
	}

}

