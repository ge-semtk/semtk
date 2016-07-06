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


package com.ge.research.semtk.load;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.load.utility.Utility;

public class TemplateUtil {

	public static ArrayList<String> getColumnNames(JSONObject template) throws ImproperTemplateException{
		ArrayList<String> retval = new ArrayList<String>();
		
		SparqlGraphJson sgJson = new SparqlGraphJson(template);
		JSONObject importSpec = sgJson.getImportSpecJson();
		if(importSpec == null){
			throw new ImproperTemplateException("ImportSpec not found. this does not appear to be a template.");
			
		}
		JSONArray columns = (JSONArray) importSpec.get("columns");
		if(columns == null){
			throw new ImproperTemplateException("columns array not found in template.");
		}
		
		// get the columns. we don't really care order but it is likely preserved.
		
		
		
		return retval;
	}
	
}
