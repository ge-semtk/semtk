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
package com.ge.research.semtk.plotting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.jena.atlas.json.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.utility.Utility;

/**
 * Information for a single Plotly plot spec
 * 
 * Example:
 *     	{   type: "plotly",
 *          name: "line 1",
 *          spec: { data: [{},{}], layout: {}, config: {} }    
 *      }
 */
public class PlotlyPlotSpec extends PlotSpec {

	public static final String TYPE = "plotly";
	
	public static final String JKEY_DATA = "data";
	public static final String JKEY_LAYOUT = "layout";
	public static final String JKEY_CONFIG = "config";
	
	private static final String SEMTK_FOREACH = "SEMTK_FOREACH";
	private static final String JKEY_COLNAME = "colName";
	private static final String JKEY_FILL_WITH_VALUE = "fillWithValue";
	private static final String JKEY_FILL_BY_VALUE = "fillByValue";
	private static final String SEMTK_TRACE = "SEMTK_TRACE_";     

	private static final String PREFIX = "SEMTK_TABLE";     // x: "SEMTK_TABLE.col[col_name]"
	private static final String CMD_COL = "col";
			

	public PlotlyPlotSpec(JSONObject plotSpecJson) throws Exception {
		super(plotSpecJson);	
		if(!getType().equals(TYPE)){ throw new Exception("Cannot create PlotlyPlotSpec with type " + getType() + ": expected type " + TYPE); }	
	}
	
	/**
	 * Substitute data and info from a table into a data spec
	 */
	public void applyTable(Table table) throws Exception {
		
		JSONObject spec = (JSONObject) this.getSpec();
		if (spec == null) throw new Exception("Plotly spec json needs a " + JKEY_SPEC + " element");
		JSONArray data = (JSONArray) spec.get(JKEY_DATA);
		if (data == null) throw new Exception("Plotly spec json needs a " + JKEY_DATA + " element");

		while (walkToApplyTable(null, data, table)) {
			// repeat until false;
		}
	}
	
	/**
	 * Recursively walk json and apply table.  Make first change only (to avoid concurrent mod).
	 * Call repeatedly until you get "false"
	 * 
	 * Returns: boolean - were any changes made.
	 */
	private boolean walkToApplyTable(Object parent, Object json, Table table) throws Exception {
		boolean changeFlag = false;
		if (json instanceof JSONObject) {
			changeFlag = this.applyTableToObject(parent, (JSONObject) json, table);
			
			if (! changeFlag) {
			
				// walk members
				JSONObject jObj = (JSONObject) json;
				for (Object key : jObj.keySet()) {
					Object member = jObj.get(key);
					
					if (member instanceof JSONObject) {
						changeFlag = this.walkToApplyTable(json, member, table);
					
					} else if (member instanceof JSONArray) {
						changeFlag = this.walkToApplyTable(json, member, table);
					
					} else if (member instanceof String) {
						// found a string field: do the work
						changeFlag = this.applyTableToJsonStringField(jObj, (String)key, table);
					}
					// return after first change to avoid concurrent operations exception
					if (changeFlag) break;  
				}
			}
	
		} else if (json instanceof JSONArray) {
			// recursively apply to each member of array
			JSONArray jArr = (JSONArray) json;
			for (Object o : jArr) {
				changeFlag = this.walkToApplyTable(json, o, table);
				// return after first change to avoid concurrent operations exception
				if (changeFlag) break;
			}
		}
		
		return changeFlag;
	}
	
	private boolean applyTableToObject(Object parent, JSONObject json, Table table) throws Exception {
		return this.applyTableToObjectForeach(parent, json, table);
		    /// 
	}
	
	/**
	 * Apply SEMTK_FOREACH if warranted.
	 * Adds a copy for each unique value in the column colName
	 * Each copy also gets fillWithValue: unique  for each unique value in column colName
	 * @param parent - must be JSONArray
	 * @param json
	 * @param table
	 * @return boolean - were changes made
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private boolean applyTableToObjectForeach(Object parent, JSONObject json, Table table) throws Exception {
		JSONObject foreach = (JSONObject) ((JSONObject)json).get(SEMTK_FOREACH);
		if (foreach == null) return false;
				
		String colName = (String) foreach.get(JKEY_COLNAME);
		String fillWithValue = (String) foreach.get(JKEY_FILL_WITH_VALUE);
		JSONObject fillByVal = (JSONObject) foreach.get(JKEY_FILL_BY_VALUE);
		int semtkTrace = 1;
		
		// error check incoming json
		if (! (parent instanceof JSONArray)) throw new Exception ("SEMTK_FOREACH applied to object that is not in a JSONArray");
		if (colName == null) throw new Exception("SEMTK_FOREACH is missing 'colName' field");
		
		// remove this entry from parent array
		json.remove("SEMTK_FOREACH");
		((JSONArray) parent).remove(json);
		JSONArray newArr = new JSONArray();
		
		String jsonTemplateStr = json.toJSONString();

		String [] uniqueVals = table.getColumnUniqueValues(colName);
		for (String uVal : uniqueVals) {
		
			Table subTable = table.getSubsetWhereMatches(colName, uVal);
			JSONObject copyObj = (JSONObject) (new JSONParser()).parse(jsonTemplateStr);
			newArr.add(copyObj);
			
			// fill fillWithValue
			if (fillWithValue != null) {
				copyObj.put(fillWithValue, uVal);
			}
			
			// fillByVal
			if (fillByVal != null) {
				JSONObject addMe = (JSONObject) fillByVal.get(uVal);
				// if no specific trace was found, try next generic one
				if (addMe == null) {
					String traceKey = SEMTK_TRACE + String.valueOf(semtkTrace++);
					addMe = (JSONObject) fillByVal.get(traceKey);
				}
				// if found: add
				if (addMe != null) {
					for (Object key : addMe.keySet()) {
						copyObj.put(key, addMe.get(key));
					}
				} 
			}
			
			// no need to worry about concurrent stuff here.  object is not yet added to Json.
			while (this.walkToApplyTable(parent, copyObj, subTable))
				;
		}
		
		// now add everything to parent
		((JSONArray) parent).addAll(newArr);
		
		return true;
	}

	/**
	 * Process the given existing string field of a JSON object
	 * @param jObj the containing JSON object 
	 * @param key the key of a known string field
	 * @param table replace the string field with data from this table
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private boolean applyTableToJsonStringField(JSONObject jObj, String key, Table table) throws Exception {
		
		String s = (String) jObj.get(key);	// e.g. SEMTK_TABLE.col[colA]
		if (!s.startsWith(PREFIX + ".")) return false;
		
		// TODO if already replaced this field, then return
				
		String[] sSplit = s.replaceAll("\\s","").split("[\\.\\[\\]]");  // e.g. split SEMTK_TABLE.col[colA] into ["SEMTK_TABLE", "col", "colA"]
		if(sSplit[0].equals(PREFIX) && sSplit[1].equals(CMD_COL)){
			String colName = sSplit[2].trim();
			int colIndex = table.getColumnIndex(colName);
			if (colIndex == -1) {
				throw new Exception("Plot spec contains column which does not exist in table: '" + colName + "'");
			}
			
			JSONArray jArr = new JSONArray();
			
			if (table.getColumnXSDType(colName).isInt()) {
				for (int r=0; r < table.getNumRows(); r++) {
					jArr.add(table.getCellAsInt(r, colIndex));
				}
			} else if (table.getColumnXSDType(colName).isFloat()) {
				for (int r=0; r < table.getNumRows(); r++) {
					jArr.add(table.getCellAsFloat(r, colIndex));
				}
			} else  {
				for (int r=0; r < table.getNumRows(); r++) {
					jArr.add(table.getCellAsString(r, colIndex));
				}
			}
			
			jObj.put(key, jArr);
			return true;

		}else{
			throw new Exception("Unsupported data specification for plotting: " + s);
		}
		
	}
	
	/**
	 * Create a sample plot
	 */
	@SuppressWarnings("unchecked")
	public static PlotlyPlotSpec getSample(String name, String graphType, String[] columnNames) throws Exception{
		
		final String[] SUPPORTED_GRAPH_TYPES = {"scatter", "bar"};
		
		// validate
		if(name == null || name.trim().isEmpty()){
			throw new Exception("Cannot create sample plot with no name");
		}
		if(graphType == null || !Arrays.asList(SUPPORTED_GRAPH_TYPES).contains(graphType)){
			throw new Exception("Cannot create sample plot with graph type '" + graphType + "': supported types are " + Arrays.toString(SUPPORTED_GRAPH_TYPES));
		}
		if(columnNames == null || columnNames.length < 2){
			throw new Exception("Cannot create sample plot with fewer than 2 column names");
		}
		
		// create trace using first 2 columns	 TODO could add more traces
		JSONObject traceJson = new JSONObject();
		traceJson.put("type", graphType);
		traceJson.put("x", "SEMTK_TABLE.col[" + columnNames[0] + "]");
		traceJson.put("y", "SEMTK_TABLE.col[" + columnNames[1] + "]");
		
		// add trace(s) to data object 
		JSONArray dataJsonArr = new JSONArray();
		dataJsonArr.add(traceJson);
		
		// add data, config, layout to spec object
		JSONObject specJson = new JSONObject();
		specJson.put(JKEY_DATA, dataJsonArr);
		specJson.put(JKEY_CONFIG, Utility.getJsonObjectFromString("{ \"editable\": true }"));
		specJson.put(JKEY_LAYOUT, Utility.getJsonObjectFromString("{ \"title\": \"Sample Plot\" }"));
		
		// add type, name, spec to plotSpec object
		JSONObject plotSpecJson = new JSONObject();
		plotSpecJson.put(JKEY_TYPE, TYPE);
		plotSpecJson.put(JKEY_NAME, name);
		plotSpecJson.put(JKEY_SPEC, specJson);
		
		return new PlotlyPlotSpec(plotSpecJson);
	}

}
