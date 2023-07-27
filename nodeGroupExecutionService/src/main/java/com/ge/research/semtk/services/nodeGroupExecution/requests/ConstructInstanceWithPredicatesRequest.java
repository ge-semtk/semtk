/**
 ** Copyright 2023 General Electric Company
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
import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import java.util.HashSet;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public class ConstructInstanceWithPredicatesRequest extends SparqlConnectionRequest {
    
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
            example = "http://path#12345")
    private String instanceUri;
    
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
            example = "http://path#Class")
    private String instanceClassUri = null;
    
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,  
    		description = "list of predicates to return",
    		example = "['uri://ontology#MyPred', 'uri://ontology#YourPred']")
	public List<String> predicateList = null;

	public String getInstanceUri() {
		return instanceUri;
	}
    
    public String getInstanceClassUri() {
		return instanceClassUri;
	}
	
	public HashSet<String> getPredicateList() {
		if (this.predicateList == null)
			return null;
		else {
			HashSet<String> ret = new HashSet<String>();
			ret.addAll(this.predicateList);
			return ret;
		}
	}

}
