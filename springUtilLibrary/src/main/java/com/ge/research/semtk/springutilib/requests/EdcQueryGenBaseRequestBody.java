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

import com.ge.research.semtk.resultSet.Table;

/**
 * For query generation service calls that only need the table
 */
public class EdcQueryGenBaseRequestBody {
	
	public String locationAndValueInfoTableJsonStr; // JSON-serialized Table object containing location and value info 	
	
    /**
     * Parse and return the table.
     * Throws exception if null or not parseable.
     */
    public Table getLocationAndValueInfoTable() throws Exception{
		
    	if(locationAndValueInfoTableJsonStr == null){
			throw new Exception("Request body is missing table json");
		}
    	try{
    		return Table.fromJson((JSONObject)(new JSONParser()).parse(locationAndValueInfoTableJsonStr)); 
    	}catch(Exception e){
			throw new Exception("Cannot parse json containing location/value table: " + e.getMessage());
    	}
    }
    
}
