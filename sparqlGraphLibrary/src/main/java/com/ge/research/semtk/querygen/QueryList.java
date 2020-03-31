/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.querygen;

import java.util.ArrayList;

import org.json.simple.JSONObject;

/**
 * A list of EDC queries
 */
public class QueryList {

	private ArrayList<Query> queries;
	private JSONObject queryConfig;
	
	public QueryList(){		
		queries = new ArrayList<Query>();
	}
	
	public void addQuery(Query query){
		queries.add(query);
	}
	
	public ArrayList<Query> getQueries(){
		return queries;
	}
	
	/**
	 * Replaces any existing queries with the given set.
	 */
	public void setQueries(ArrayList<Query> queries){
		this.queries = queries;
	}
	
	public void addConfig(JSONObject config){
		this.queryConfig = config;
	}
	
	public JSONObject getConfig(){
		return this.queryConfig;
	}
}
