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

import com.ge.research.semtk.sparqlX.SparqlConnection;

import io.swagger.annotations.ApiModelProperty;

public class IngestByIdCsvStrRequestBody extends SparqlConnRequestBody {

	private String templateId = "";
	private String csvContent = "";
	
	@ApiModelProperty(
			value = "trackFlag",
			required = false,
			example = "true")
	public Boolean trackFlag = false;

	@ApiModelProperty(
			value = "overrideBaseURI",
			required = false,
			example = "http://$TRACK_KEY/data")
	public String overrideBaseURI = null;
	
	public String getTemplateId() {
		return templateId;
	}
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}
	public String getCsvContent() {
		return csvContent;
	}
	public void setCsvContent(String csvContent) {
		this.csvContent = csvContent;
	}
	public Boolean getTrackFlag() {
		return trackFlag;
	}

	public void setTrackFlag(Boolean trackFlag) {
		this.trackFlag = trackFlag;
	}

	public String getOverrideBaseURI() {
		return overrideBaseURI;
	}

	public void setOverrideBaseURI(String overrideBaseURI) {
		this.overrideBaseURI = overrideBaseURI;
	}
}
