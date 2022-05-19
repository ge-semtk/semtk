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

public class CardinalityReportRequest extends SparqlConnectionRequest {

    @Schema(
    		name = "maxRows",
    		description = "Return only this many violations (e.g. protect browser memory)",
            required = false,
            example = "1000")
    private int maxRows = 0;
 
    public int getMaxRows() {
		return this.maxRows;
	}

}
