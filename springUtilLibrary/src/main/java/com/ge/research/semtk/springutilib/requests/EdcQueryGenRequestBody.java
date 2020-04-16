/**
 ** Copyright 2020 General Electric Company
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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;
import com.ge.research.semtk.utility.Utility;


/**
 * For query generation service calls that need the table and constraints
 */
public class EdcQueryGenRequestBody extends EdcQueryGenBaseRequestBody {
		
	public String constraintsJsonStr;				// JSON object containing query constraints
    public String flagsJsonArrayStr;				// JSON array containing flags
	
    /**
     * Parse and return the constraint JSON object.
     * Throws exception if null or not parseable.
     */
    public JSONObject getConstraintsJson() throws Exception{

		if(constraintsJsonStr == null){
			return null;
		}
		try{
			return (JSONObject) (new JSONParser()).parse(constraintsJsonStr); 
		}catch(Exception e){
			throw new Exception("Cannot parse constraints json '" + constraintsJsonStr + "': " + e.getMessage());
		}
    }
    
    /**
     * Parse and return the flags JSON array.
     * Throws exception if null or not parseable.
     */
    public QueryFlags getQueryFlags() throws Exception{
    	if (flagsJsonArrayStr == null || flagsJsonArrayStr.isEmpty()) {
    		return null;
    	} else {
    		return new QueryFlags(Utility.getJsonArrayFromString(flagsJsonArrayStr));
    	}
    }
    
}
