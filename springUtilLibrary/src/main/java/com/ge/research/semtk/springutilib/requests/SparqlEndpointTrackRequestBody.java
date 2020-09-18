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


package com.ge.research.semtk.springutilib.requests;


import io.swagger.annotations.ApiModelProperty;


public class SparqlEndpointTrackRequestBody extends SparqlEndpointRequestBody {
	
	@ApiModelProperty(
	   value = "trackFlag",
	   required = false,
	   example = "true")
	public Boolean trackFlag = false;

	public Boolean getTrackFlag() {
		return trackFlag;
	}

	public void setTrackFlag(Boolean trackFlag) {
		this.trackFlag = trackFlag;
	}
	
}
