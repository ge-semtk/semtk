/**
 ** Copyright 2018 General Electric Company
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

import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;

public class ConnectionUriRequest extends SparqlConnectionRequest {

    @Schema(
    		name = "sparqlID",
            required = false,
            example = "myVarName")
    private String sparqlID;
    
    @Schema(
    		name = "uri",
            required = true,
            example = "http://path#this")
    private String uri;

 
    public String getUri() {
		return uri;
	}

	public String getSparqlID() {
        return sparqlID;
    }

}
