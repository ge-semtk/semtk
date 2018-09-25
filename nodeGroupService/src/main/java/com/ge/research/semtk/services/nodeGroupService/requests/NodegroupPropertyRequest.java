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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModelProperty;

public class NodegroupPropertyRequest extends NodegroupRequest {
	@NotNull
	@ApiModelProperty(
			value = "nodeSparqlId",
			required = true,
			example = "?MyClass")	
	private String nodeSparqlId;
	
	@NotNull
	@Pattern(regexp="^[^{}[]:;\"'#]+#[^{}[]:;\\\"'#]+$", message="uri is ill-formed")
	@Size(min=8, max=256, message="uri must be 8-256 characters in length")
	@ApiModelProperty(
				value = "propertyUri",
				required = true,
				example = "http://sample/uri#property")	
	private String propertyUri;
	
	@NotNull
	@ApiModelProperty(
			value = "newPropSparqlId",
			required = true,
			example = "?MyProp")	
	private String newPropSparqlId;
	
	@NotNull
	@ApiModelProperty(
			value = "isReturned",
			required = true,
			example = "true")	
	Boolean isReturned;

	public void setNodeSparqlId(String nodeSparqlId) {
		this.nodeSparqlId = nodeSparqlId;
	}
	public void setPropertyUri(String propertyUri) {
		this.propertyUri = propertyUri;
	}
	public void setNewPropSparqlId(String newPropSparqlId) {
		this.newPropSparqlId = newPropSparqlId;
	}
	public void setIsReturned(Boolean isReturned) {
		this.isReturned = isReturned;
	}
	public boolean isReturned() {
		return isReturned;
	}
	public String getNewPropSparqlId() {
		return newPropSparqlId;
	}
	public String getNodeSparqlId() {
		return nodeSparqlId;
	}
	public String getPropertyUri() {
		return propertyUri;
	}

	
}
