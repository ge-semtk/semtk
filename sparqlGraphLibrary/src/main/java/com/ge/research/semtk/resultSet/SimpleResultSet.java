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


package com.ge.research.semtk.resultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;


public class SimpleResultSet extends GeneralResultSet{
	
	public static final String RESULTS_BLOCK_NAME = "simpleresults";  
	public static final String MESSAGE_JSONKEY = "@message";
 	public static final String JOB_ID_RESULT_KEY = "JobId";
 	public static final String STATUS_RESULT_KEY = "status";
 	public static final String STATUS_MESSAGE_RESULT_KEY = "statusMessage";
 	public static final String PERCENT_COMPLETE_RESULT_KEY = "percentComplete";
	
	public SimpleResultSet() {
		super();		
	}	
	
	public SimpleResultSet(JSONObject encoded) throws EndpointNotFoundException {
		super();
		this.readJson(encoded);
	}
	
	public SimpleResultSet(Boolean succeeded) {
		super(succeeded);
	}

	public SimpleResultSet(Boolean succeeded, String rationale) {
		super(succeeded);
		addRationaleMessage(rationale);
	}	
	
	@Override
	public String getResultsBlockName() {
		return RESULTS_BLOCK_NAME;
	}
	
	public String getMessage() throws Exception {
		return this.getResult(SimpleResultSet.MESSAGE_JSONKEY);		
	}
	
	public void setMessage(String msg) {
		this.addResult(SimpleResultSet.MESSAGE_JSONKEY, msg);
	}
	
	public String getJobId() throws Exception {
		return this.getResult(JOB_ID_RESULT_KEY);
	}
	/**
	 * Return results in a hashmap
	 */
	@Override
	public HashMap<String,Object> getResults() throws Exception {
		HashMap<String,Object> ret = new HashMap<String,Object>();;
		String key;
		Object value;
		@SuppressWarnings("unchecked")
		Iterator<String> iter = this.resultsContents.keySet().iterator();
		while(iter.hasNext()){
			key = (String)iter.next();
			value = this.resultsContents.get(key);
			ret.put(key, value);
		}
		return ret;
	}
	
	public ArrayList<String> getResultsKeys() {
		ArrayList<String> ret = new ArrayList<String>();
		if (this.resultsContents != null) {
			ret.addAll(this.resultsContents.keySet());
		}
		return ret;
	}
	
	// TODO this should extend an abstract method in GeneralResultSet
	@SuppressWarnings("unchecked")
	public void addResult(String name, String value) {
		if (this.resultsContents == null) {
			this.resultsContents = new JSONObject();
		}
		this.resultsContents.put(name, value);
	}
	
	@SuppressWarnings("unchecked")
	public void addResult(String name, int value) {
		if (this.resultsContents == null) {
			this.resultsContents = new JSONObject();
		}
		this.resultsContents.put(name, value);
	}
	
	@SuppressWarnings("unchecked")
	public void addResult(String name, JSONObject jObj) {
		if (this.resultsContents == null) {
			this.resultsContents = new JSONObject();
		}
		this.resultsContents.put(name, jObj);
	}
	
	public void addResult(String name, String [] value) {
		this.addResultStringArray(name, value);
	}
	
	/**
	 * 
	 * @param name
	 * @param value - string array.  null will be treated as empty array
	 */
	@SuppressWarnings("unchecked")
	public void addResultStringArray(String name, String [] value) {
		JSONArray arr = new JSONArray();

		if (value != null) {
			for (int i=0; i < value.length; i++) {
				arr.add(value[i]);
			}
		}
		if (this.resultsContents == null) {
			this.resultsContents = new JSONObject();
		}
		this.resultsContents.put(name, arr);
	}
	
	public int getResultInt(String name) throws Exception {
		String str = getResult(name);  
		try {
			return Integer.parseInt(str);
		}
		catch (Exception e) {
			throw new Exception(String.format("Error parsing find integer result '%s': %s", name, str));
		}
	}
	
	public String getResult(String name) throws Exception {
		if (this.resultsContents.containsKey(name)) {
			return this.resultsContents.get(name).toString();
		} else {
			throw new Exception(String.format("Can't find result field '%s'", name));
		}
	}
	
	public JSONObject getResultJSON(String name) throws Exception {
		if (this.resultsContents.containsKey(name)) {
			return (JSONObject) this.resultsContents.get(name);
		} else {
			throw new Exception(String.format("Can't find result field '%s'", name));
		}
	}
	
	public String [] getResultStringArray(String name) throws Exception {
		JSONArray arr = null;
		if (this.resultsContents.containsKey(name)) {
			arr = (JSONArray) this.resultsContents.get(name);
		} else {
			throw new Exception(String.format("Can't find result field '%s'", name));
		}
		String [] ret = new String[arr.size()];
		for (int i=0; i < arr.size(); i++) {
			ret[i] = (String) arr.get(i);
		}
		return ret;
	}
	
	@Override
	public void readJson(JSONObject jsonObj) {
		super.readJson(jsonObj);
	}
	
	public static SimpleResultSet fromJson(JSONObject jsonObj) throws Exception {
		SimpleResultSet ret = new SimpleResultSet();
		ret.readJson(jsonObj);
		return ret;
	}



}
