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


import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.api.nodeGroupExecution.StitchingStep;

import io.swagger.v3.oas.annotations.media.Schema;

public class StitchByIdRequestBody extends SparqlConnRequestBody {
	@Schema(
			description = "JSON string describing steps.  Matches MATERIA config.js format.",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "{\"label\": \"myNg\", \"value\": [\"nodegroupid1\", \"nodegroupid2\"], \"keys\": [[\"--ignored--\"], [\"key1\", \"key2\"]], \"commonCol\": [\"col1\"] }"
			)
	private String steps;

	// for swagger only
	public String getSteps() {
		return steps;
	}

	/**
	 * Read a JSON format that is legacy MATERIA
	 *     label:  unused
	 *     value:  [ list of nodegroup ids to stitch ]
	 *     keys:   [ parallel array of [ array of column names keys for each step ] ]    // first one is ignored
	 *     commonCols: [ list of global column names that are merged instead of default logic ] 
	 * @return
	 * @throws ParseException
	 * @throws Exception
	 */
	public StitchingStep[] buildSteps() throws ParseException, Exception  {
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(this.steps);
		
		JSONArray values = (JSONArray) jObj.get("value");
		JSONArray keys = (JSONArray) jObj.get("keys");
		if (values == null) throw new Exception("steps JSON does not contain the field \"value\"");
		if (keys == null) throw new Exception("steps JSON does not contain the field \"keys\"");
		// commonCols might be empty (null) 
		
		// translate into an array of StitchingSteps
		StitchingStep ret[] = new StitchingStep[keys.size()];
		// first one
		ret[0] = new StitchingStep((String) values.get(0), null);
		
		for (int i=1; i < keys.size(); i++) {
			// translate keys array into an array of Strings
			JSONArray keyJArr = ((JSONArray) keys.get(i));
			String keyArr[] = new String[keyJArr.size()];
			for (int j=0; j < keyJArr.size(); j++ )
				keyArr[j] = (String) keyJArr.get(j);
			
			// build the next StitchingStep
			ret[i] = new StitchingStep((String) values.get(i), keyArr);
		}
		return ret;
	}
	
	/**
	 * Return commonCol list or []
	 * @return
	 * @throws ParseException
	 * @throws Exception
	 */
	public HashSet<String> buildCommonCol() throws ParseException, Exception  {
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(this.steps);

		JSONArray commonCols = (JSONArray) jObj.get("commonCol");
		HashSet<String> ret = new HashSet<String>();
		if (commonCols != null) {
			for (int i=0; i < commonCols.size(); i++) {
				ret.add((String) commonCols.get(i));
			}
		}
		return ret;
	}
	
}

