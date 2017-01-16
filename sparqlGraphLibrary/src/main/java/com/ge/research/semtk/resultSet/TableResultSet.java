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

import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.resultSet.GeneralResultSet;

/**
 * A result set containing a Table
 */
public class TableResultSet extends GeneralResultSet{
	
	public static final String RESULTS_BLOCK_NAME = "table";
	public final static String TABLE_JSONKEY = "@table";

	public TableResultSet(JSONObject encoded) throws EndpointNotFoundException {
		super();
		this.readJson(encoded);
	}
	
	public TableResultSet(Boolean succeeded) {
		super(succeeded);
	}

	public TableResultSet() {
		super();	
	}
	
	@Override
	public String getResultsBlockName() {
		return RESULTS_BLOCK_NAME;
	}
	
	@Override
	public Table getResults() throws Exception {
		Table table = Table.fromJson((JSONObject)this.resultsContents.get(TABLE_JSONKEY));
		return table;
	}
	
	/**
	 * Get the table
	 */
	public Table getTable() throws Exception {
		return this.getResults();
	}
	
	/**
	 * Get the table as a CSV string
	 */
	public String getTableCSVString() throws Exception {
		return this.getResults().toCSVString();
	}
	
	/**
	 * Add results as a Table object
	 */
	public void addResults(Table table) throws Exception {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(TABLE_JSONKEY,table.toJson()); 
		addResultsJSON(jsonObj);
	}

	protected void processConstructJson(JSONObject encoded) {	
		if(encoded.get(getResultsBlockName()) != null){
			this.resultsContents = (JSONObject) encoded.get(getResultsBlockName());
		}
	}

	/**
	 * Merge a set of TableResultSets into a single TableResultSet
	 * 
	 * (wanted to override an abstract method in GeneralResultSet, but cannot have static abstract method)
	 * TODO check for result set failures?
	 */
	public static TableResultSet merge(ArrayList<TableResultSet> tableResultSets) throws Exception {	
		ArrayList<Table> tables = new ArrayList<Table>();		
		for(TableResultSet tableResultSet : tableResultSets){
			tables.add(tableResultSet.getTable());	
		}
		Table mergedTable = Table.merge(tables);		
		TableResultSet ret = new TableResultSet(true);
		ret.addResults(mergedTable);
		return ret;
	}

}
