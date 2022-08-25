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

package com.ge.research.semtk.services.ontologyinfo.requests;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;

public class InstanceDictionaryRequest extends SparqlConnectionRequest {

    @Schema(
    		description = "Labels assigned to URIs may not have more than this many words",
            required = false,
            example = "2")
    private int maxWords = 2;
    
    @Schema(
    		description = "Only find labels that associate with at most this many URIs",
            required = false,
            example = "1")
    private int specificityLimit = 1;
 
    public int getMaxWords() {
		return this.maxWords;
	}
    public int getSpecificityLimit() {
		return this.specificityLimit;
	}

}
