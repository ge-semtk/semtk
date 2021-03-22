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

package com.ge.research.semtk.services.utility;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class ProcessPlotSpecRequest {

	private String plotSpecJsonStr;
	private String tableJsonStr;
		
	/**
	 * Validate the request
	 */
	public void validate() throws Exception {
		if(plotSpecJsonStr == null || plotSpecJsonStr.trim().isEmpty()){
			throw new Exception("Plot spec json is missing or empty");
		}
		if(tableJsonStr == null || tableJsonStr.trim().isEmpty()){
			throw new Exception("Table json is missing or empty");
		}
	}
	
	public void setPlotSpecJson(String plotSpecJsonStr) {
		this.plotSpecJsonStr = plotSpecJsonStr;
	}
	
	public void setTableJson(String tableJsonStr) {
		this.tableJsonStr = tableJsonStr;
	}
	
	public JSONObject getPlotSpecJson() throws Exception{
		try {
			return (JSONObject) ((new JSONParser()).parse(this.plotSpecJsonStr));
		} catch (ParseException e) {
			throw new Exception("Cannot parse plot spec json");
		}
	}
	
	public JSONObject getTableJson() throws Exception{
		try {
			return (JSONObject) ((new JSONParser()).parse(this.tableJsonStr));
		} catch (ParseException e) {
			throw new Exception("Cannot parse table json");
		}
	}
	
}
