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


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.api.nodeGroupExecution.StitchingStep;

import io.swagger.v3.oas.annotations.media.Schema;

public class StitchByIdRequestBody extends SparqlConnRequestBody {
	@Schema(
			description = "Array of stiching steps with: nodegroupId and keyColumns array",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "[{\"nodegroupId\": \"myNg\", \"keyColumns\": [\"col1\", \"col2\"]}]"
			)
	private String steps;

	// for swagger only
	public String getSteps() {
		return steps;
	}
	// use string to be compatable with all the other fields
	public StitchingStep[] buildSteps() throws ParseException, Exception  {
		JSONArray jArr = (JSONArray) (new JSONParser()).parse(this.steps);
		StitchingStep ret[] = new StitchingStep[jArr.size()];
		for (int i=0; i < jArr.size(); i++) {
			ret[i] = new StitchingStep((JSONObject) jArr.get(i));
		}
		return ret;
	}

	
}

