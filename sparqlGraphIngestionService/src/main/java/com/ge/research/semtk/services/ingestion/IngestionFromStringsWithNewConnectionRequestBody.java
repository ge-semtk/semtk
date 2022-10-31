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

package com.ge.research.semtk.services.ingestion;

import javax.validation.constraints.NotNull;

import com.ge.research.semtk.springutilib.requests.IngestionFromStringsRequestBody;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * For requests that provide a SPARQL connection to override the connection in the template.
 */
public class IngestionFromStringsWithNewConnectionRequestBody extends IngestionFromStringsRequestBody {

	@NotNull
	@Schema(
	   name = "connectionOverride",
	   required = true,
	   example = "{ connection json }")
	private String connectionOverride;
	
	@Schema(
			   required = false,
			   example = "true")
	private boolean skipPrecheck = false;
	@Schema(
			   required = false,
			   example = "true")
	private boolean skipIngest = false;
	
	
	public boolean getSkipPrecheck() { return this.skipPrecheck; }
	public boolean getSkipIngest() { return this.skipIngest; }
	
	public String getConnectionOverride(){
		return this.connectionOverride;
	}
	public void setConnectionOverride(String connectionOverride){
		this.connectionOverride = connectionOverride;
	}

}
