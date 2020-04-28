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


package com.ge.research.semtk.services.ontologyinfo.requests;

import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.annotations.ApiModelProperty;

public class OntologyInfoClassRequestBody extends SparqlConnectionRequest {
	@ApiModelProperty(
			value = "className",
			required = true,
			example = 	"http://my/model#Object17"
			           )	
	private String className = "";
	
	public void setClassName(String c) {
		this.className = c;
	}
	
	public String getClassName() throws Exception{
		return this.className;
	}
	
	public OntologyClass buildClass() throws Exception {
		return new OntologyClass(this.className);
	}
   
}
