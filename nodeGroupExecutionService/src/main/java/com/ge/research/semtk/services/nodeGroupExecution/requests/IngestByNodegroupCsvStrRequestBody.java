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

package com.ge.research.semtk.services.nodeGroupExecution.requests;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.sparqlX.SparqlConnection;

import io.swagger.v3.oas.annotations.media.Schema;

public class IngestByNodegroupCsvStrRequestBody {

	@Schema(
			required = true,
			example = "{ semtk-generated nodegroup json string }")
	private String jsonRenderedNodeGroup = "";

	@Schema(
			required = false,
			example = "{ semtk-generated connection json string }")
	private String sparqlConnection = "";
	@Schema(
			required = true,
			example = "header1,header2\nval1,val2\nval11,val22\n")
	private String csvContent = "";
	
	@Schema(
			required = false,
			example = "true")
	public Boolean trackFlag = false;

	@Schema(
			required = false,
			example = "$TRACK_KEY  or  http://freds/data")
	public String overrideBaseURI = null;
	
	public String getJsonRenderedNodeGroup() {
		return jsonRenderedNodeGroup;
	}

	public void setJsonRenderedNodeGroup(String jsonRenderedNodeGroup) {
		this.jsonRenderedNodeGroup = jsonRenderedNodeGroup;
	}
	public SparqlGraphJson buildSparqlGraphJson() throws Exception {
		return new SparqlGraphJson(this.jsonRenderedNodeGroup);
	}

	public SparqlConnection buildSparqlConnection() throws Exception {
		return new SparqlConnection(sparqlConnection);
	}
	public void setSparqlConnection(String sparqlConnection) {
		this.sparqlConnection = sparqlConnection;
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
