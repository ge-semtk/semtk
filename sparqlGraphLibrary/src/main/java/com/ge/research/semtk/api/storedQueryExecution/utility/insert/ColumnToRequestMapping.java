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

package com.ge.research.semtk.api.storedQueryExecution.utility.insert;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ColumnToRequestMapping {

	/*
		the Column to Request Mapping is a simple JSON Array of JSON Objects which each have just two fields:
		<source column> and <field name in obkect>. it ends up looking like this:
		
		[
			{ 
				"DestinationColumn" : <name of the source column in the csv for ingestion to build> ,
			  	"SourceField" : <name of the field in the request object from which to retrieve this value>
			 },

		]		
	
	*/
	JSONArray mapping = null;
	HashMap<String, String> columnsToFields = null;
	GenericInsertionRequestBody reqBody = null;
	
	public ColumnToRequestMapping(JSONArray mapArray, GenericInsertionRequestBody request){
		this.columnsToFields = new HashMap<String, String>();
		this.mapping = mapArray;
		this.reqBody = request;
		this.fromJSONMapping();
	}
	
	public ColumnToRequestMapping(String mappingAsString, GenericInsertionRequestBody request) throws ParseException{
		this.columnsToFields = new HashMap<String, String>();
				
		// parse the JSONString 
		JSONParser jParse = new JSONParser();
		JSONArray mapArray = (JSONArray)jParse.parse(mappingAsString);
		this.mapping = mapArray;
		this.reqBody = request;
		this.fromJSONMapping();
	}
	
	// get a csv based on the incoming request data and the mapping.
	public String getCsvString() throws Exception{
		String retval = "";
		
		if(this.columnsToFields == null || this.columnsToFields.isEmpty()) {
			throw new Exception("getCsvString -- mapping had no entries. unable to create outpur csv records");
		}
		if(this.reqBody == null){
			throw new Exception("getCsvString -- requestbody was null. unable to create csv records");
		}
		
		// the mapping and the requestbody both exist. time to make the in-memory csv
		String[] columnHeaders = new String[this.columnsToFields.keySet().size()];
		int colnum = 0;
		for(String k : this.columnsToFields.keySet()){
			// add each key to the header row. this is why we use the destination column as the key.
			columnHeaders[colnum] = k;
			
			// add the column headers to the csv we will output.
			retval += k;
			colnum += 1;
			// if the column list is not exhausted, add a comma
			if(colnum < columnHeaders.length){
				retval += ",";
			}
		}
		// add a new line before the first line.
		retval += "\n";
		
		// get the values from the requestbody and place them in the csv structure
		colnum = 0; 	// reset the column number
		for (String colname : columnHeaders){
			// get the field name from the map of ColumnsToFields
			String srcName = this.columnsToFields.get(colname);
			String valOfInterest = this.reqBody.getValueByName(srcName);
			retval += valOfInterest;
			
			colnum += 1;
			// conditionally add a comma
			if(colnum < columnHeaders.length){
				retval += ",";
			}
		}
	
		return retval;
	}
	
	
	// from JSON to actually get the fields we want. 
	private void fromJSONMapping(){
		
		if(this.mapping == null) { return; }
		else{
			// actually map the correct inputs and outputs. 
			// it is assumed that the destination column name is the key and the source name is the value. 
			// this lets us reuse a source field multiple times.
			
			for(Object curr : this.mapping){
				JSONObject currEntry = (JSONObject) curr;
				// get the source and destination
				String destination = (String) currEntry.get("DestinationColumn");
				String source = (String) currEntry.get("SourceField");
				
				this.columnsToFields.put(destination, source);
			}
		}
	}
	
	// toJSON method... it might come up. 
	public JSONArray toJSON(){
		JSONArray retval = new JSONArray();
		
		if(this.columnsToFields == null || this.columnsToFields.isEmpty()){ 
			// do nothing and return empty
		}
		else{
			// actually build the json
			for(String k : this.columnsToFields.keySet()){
				JSONObject currEntry = new JSONObject();
				String dest = k;
				String src  = this.columnsToFields.get(k);
				
				currEntry.put("SourceField", src);
				currEntry.put("DestinationColumn", dest);
				
				retval.add(currEntry);
			}
		}
		
		return retval;
	}
}
