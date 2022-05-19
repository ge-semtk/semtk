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


package com.ge.research.semtk.services.ingestion;

import javax.validation.constraints.NotNull;

import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.v3.oas.annotations.media.Schema;

/* contains no template */

public abstract class FromStringsRequestBody {

	@NotNull
	@Schema(
			name = "data",
			required = true,
			example = "csv,file\n1,2\n")
	public String data;

	@Schema(
			name = "trackFlag",
			required = false,
			example = "true")
	public Boolean trackFlag = false;

	@Schema(
			name = "overrideBaseURI",
			required = false,
			example = "$TRACK_KEY  or  http://johns/data")
	public String overrideBaseURI = null;
	
	public String getData() {
		try {	
			return data;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LocalLogger.printStackTrace(e);
			return "";
		}
	}
	
	public void setData(String data) {
		this.data = data;
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
