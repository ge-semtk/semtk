/**
 ** Copyright 2016-2018 General Electric Company
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

package com.ge.research.semtk.load.utility;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.transform.Transform;

/**
 * One item in an ImportMapping (column or text)
 * This is name-alike to the javascript but NOT a port. 
 * It is just a data structure.
 * Reading from JSON and smarts are left to ImportSpecHandler, so it can do optimization.
 *  
 * @author 200001934
 */
public class MappingItem {
	int columnIndex = -1;
	String textVal = null;
	Transform transformList[] = null;
	
	/**
	 * Create from JSON with lots of help from hash tables to make this super-efficient
	 * @param mapItemJson
	 * @param colNameHash   col id -> name
	 * @param colIndexHash  col name -> col index
	 * @param textHash      text id -> text value
	 * @param transformHash transform id -> transform obj
	 * @throws Exception
	 */
	public void fromJson(JSONObject mapItemJson, HashMap<String, String> colNameHash, HashMap<String, Integer> colIndexHash, HashMap<String,String> textHash, HashMap<String,Transform> transformHash) throws Exception {
		if (mapItemJson.containsKey("textId")) {       
			
			String id = mapItemJson.get("textId").toString();
			
			// look up text
			try {
				this.textVal = textHash.get(id);
			} catch (Exception e) {
				throw new Exception("Failed to look up textId: " + id);
			}
			
		} else if (mapItemJson.containsKey("colId")) { 
			
			String id = mapItemJson.get("colId").toString();
			String colName = null;
			
			// column name
			try{
				colName = colNameHash.get(id);
			}
			catch(Exception e){
				throw new Exception("Failed to look up columnId: " + id);
			}
			
			// change into columnIndex
			try{
				this.columnIndex = colIndexHash.get(colName);  
			}
			catch(Exception e){
				throw new Exception("Failed to look up column position: " + colName);
			}
			
			// transforms
			JSONArray transformJsonArr = (JSONArray) mapItemJson.get("transformList");
			if (transformJsonArr != null) {
				this.transformList = new Transform[transformJsonArr.size()];
				for(int i=0; i < transformJsonArr.size(); i++) {
					this.transformList[i] = transformHash.get( (String)transformJsonArr.get(i) );
				}
			}
			
		} else {
			throw new Exception("importSpec mapping item has no known type: " + mapItemJson.toString());
		}
		
	}

	public boolean isColumnMapping() {
		return this.textVal == null && this.columnIndex > -1;
	}
	
	public String buildString(ArrayList<String> record) {
		
		if (this.textVal != null) {
			return this.textVal;
			
		} else {
			String ret = record.get(this.columnIndex);
			if (this.transformList != null) {
				for(int i=0; i < this.transformList.length; i++) {
					ret = this.transformList[i].applyTransform(ret);
				}
			}
			return ret;
		}
	}
}
