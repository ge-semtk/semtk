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

package com.ge.research.semtk.springutilib.requests;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public class SparqlIdOptionalRequest  {

	@Pattern(regexp="^[a-zA-Z][a-zA-Z0-9_-]+$", message="sparqlId is ill-formed")
	@Size(min=4, max=64, message="sparqlId must be 4-64 characters in length")
	@Schema(
	   name = "sparqlId",
	   required = true,
	   example = "Var_Name")
	private String sparqlId;

	public String getSparqlId() {
		return sparqlId;
	}
	
}
