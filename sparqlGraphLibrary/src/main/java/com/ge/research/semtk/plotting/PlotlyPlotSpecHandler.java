package com.ge.research.semtk.plotting;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;

/**
 ** Copyright 2021 General Electric Company
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

public class PlotlyPlotSpecHandler {
	private static String JKEY_DATA = "data";
	private static String JKEY_LAYOUT = "layout";
	private static String JKEY_CONFIG = "config";
	private static String JKEY_SEMTK = "semtk";
	
	private static String PREFIX = "SEMTK_TABLE.";
	
	JSONObject plotSpecJson = null;
	
	/**
	 * NOTES:
	 *     sparqlgraphjson might have:
	 *         plotly : [ { plotSpecJson }, {plotSpecJson} ],
	 */
	
	/**
	 * 
	 * @param plotSpecJson - JSON to specify a single plotly plot.  {  "data": [{},{}], "layout": {}, "config": {}, "semtk":{} }
	 */
	public PlotlyPlotSpecHandler(JSONObject plotSpecJson) {
		this.plotSpecJson = plotSpecJson;
	}
	
	/**
	 * Get json, could be null
	 * @return
	 */
	public JSONObject getJSON() {
		return this.plotSpecJson;
	}
	
	/**
	 * Substitute data and info from a table into a data spec
	 * @param table
	 * @throws Exception
	 */
	public void applyTable(Table table) throws Exception {
		JSONObject data = (JSONObject) this.plotSpecJson.get(JKEY_DATA);
		if (data == null) return;
		
		walkToApplyTable(data, table);
	}
	
	/**
	 * Recursively walk json and apply table
	 * @param json
	 */
	private void walkToApplyTable(Object json, Table table) throws Exception {
		if (json instanceof JSONObject) {
			// walk members
			JSONObject jObj = (JSONObject) json;
			for (Object key : jObj.keySet()) {
				Object member = jObj.get(key);
				
				if (member instanceof JSONObject || member instanceof JSONArray) {
					// if member is more json, then recurse
					this.walkToApplyTable(member, table);
				
				} else if (member instanceof String) {
					// found a string field: do the work
					this.applyTableToJsonStringField(jObj, (String)key, table);
				}
			}
			
		} else if (json instanceof JSONArray) {
			// recursively apply to each member of array
			JSONArray jArr = (JSONArray) json;
			for (Object o : jArr) {
				this.walkToApplyTable(o, table);
			}
		}
	}
	
	/**
	 * Process the given existing string field of a JSON object
	 * @param jObj - json 
	 * @param key - key of a known existing string in json
	 * @param table - table to apply
	 * @throws Exception
	 */
	private void applyTableToJsonStringField(JSONObject jObj, String key, Table table) throws Exception {
		String field = (String) jObj.get(key);
		if (!field.startsWith(PREFIX)) return;
		
		// PEC HERE TODO do the work.  jObj.put(key, some_transformed)
	}
}
