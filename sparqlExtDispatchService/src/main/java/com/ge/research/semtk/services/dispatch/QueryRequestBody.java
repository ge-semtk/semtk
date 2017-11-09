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

package com.ge.research.semtk.services.dispatch;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.utility.LocalLogger;

public class QueryRequestBody extends NodegroupRequestBody {

	// revisit this later to determine the best way to insert user information in case we want to
	// to bolt security to it . 
	
	private String constraintSet;

	public void setConstraintSet(String constraintSet){
		this.constraintSet = constraintSet;
	}
	
	public String getConstraintSet(){
		return this.constraintSet;
	}
	
	public JSONObject getConstraintSetJson(){
		JSONParser prsr = new JSONParser();
		JSONObject retval = null;
		if(this.constraintSet != null && this.constraintSet.length() != 0 && !this.constraintSet.isEmpty()){
			try {
				retval = (JSONObject) prsr.parse(this.constraintSet);
			} catch (ParseException e) {
				LocalLogger.printStackTrace(e);
			}
		}
		return retval;
	}
}
