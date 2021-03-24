package com.ge.research.semtk.plotting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang.math.NumberUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.Utility;

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
	private static String JKEY_SPEC = "spec";
	private static String JKEY_DATA = "data";
	private static String JKEY_LAYOUT = "layout";
	private static String JKEY_CONFIG = "config";
	
	private static String PREFIX = "SEMTK_TABLE";     // x: "SEMTK_TABLE.col[col_name]"
	private static String CMD_COL = "col";
	
	JSONObject plotSpecJson = null;
	String plotSpecJsonStrTemp = null;	// temp string to use while creating a replacement plotSpecJson
	
	/**
	 * NOTES:
	 *     sparqlgraphjson might have:
	 *     	   .plots: [
	 *     		>>	{   type: plotly,
	 *	 	    >>      name: "line 1",
	 *          >>      spec: { data: [{},{}], layout: {}, config: {} }    
	 *          >>    },
	 *                { type: visjs,
	 *                  spec: { vsJsSpecJson }
	 *                },
	 *                { type: plotly,
	 *                  spec: { data, layout, config }
	 *              },
	 *              
	 */
	
	/**
	 * 
	 * @param plotSpecJson - JSON to specify a single plotly plot.
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
		
		JSONObject spec = (JSONObject) this.plotSpecJson.get(JKEY_SPEC);
		if (spec == null) throw new Exception("Plotly spec json needs a " + JKEY_SPEC + " element");
		JSONArray data = (JSONArray) spec.get(JKEY_DATA);
		if (data == null) throw new Exception("Plotly spec json needs a " + JKEY_DATA + " element");

		this.plotSpecJsonStrTemp = plotSpecJson.toJSONString();  // create a temp string in which to make substitutions (cannot iterate + modify JSON at the same time)
		walkToApplyTable(data, table);
		this.plotSpecJson = Utility.getJsonObjectFromString(plotSpecJsonStrTemp);
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
	 * @param jObj the containing JSON object 
	 * @param key the key of a known string field
	 * @param table replace the string field with data from this table
	 * @throws Exception
	 */
	private void applyTableToJsonStringField(JSONObject jObj, String key, Table table) throws Exception {
		
		String s = (String) jObj.get(key);	// e.g. SEMTK_TABLE.col[colA]
		if (!s.startsWith(PREFIX + ".")) return;
		
		// TODO if already replaced this field, then return
				
		String[] sSplit = s.replaceAll("\\s","").split("[\\.\\[\\]]");  // e.g. split SEMTK_TABLE.col[colA] into ["SEMTK_TABLE", "col", "colA"]
		if(sSplit[0].equals(PREFIX) && sSplit[1].equals(CMD_COL)){
			String colName = sSplit[2];
			ArrayList<String> list = new ArrayList<>(Arrays.asList(table.getColumn(colName)));
			String columnDataStr;
			if(NumberUtils.isNumber(table.getCell(0, colName))){
				columnDataStr = "[" + list.stream().collect(Collectors.joining(", ")) + "]";							// no quotes needed, e.g. [11.1,11.5]
			}else{
				columnDataStr = "[" + list.stream().map(t -> "\"" + t + "\"").collect(Collectors.joining(", ")) + "]";  // surround each entry in quotes (e.g. e.g. ["2020-01-24T00:00:00","2020-01-23T00:00:00"])
			}
			plotSpecJsonStrTemp = plotSpecJsonStrTemp.replace("\"" + s + "\"", columnDataStr);  	// replace the SEMTK_TABLE instruction with the data
		}else{
			throw new Exception("Unsupported data specification for plotting: " + s);
		}
	}
}
