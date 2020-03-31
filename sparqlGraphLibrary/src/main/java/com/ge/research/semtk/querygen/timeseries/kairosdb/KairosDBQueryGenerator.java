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
package com.ge.research.semtk.querygen.timeseries.kairosdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONObject;
import org.kairosdb.client.builder.QueryBuilder;
import org.kairosdb.client.builder.TimeUnit;

import com.ge.research.semtk.edc.client.KairosDBClient;
import com.ge.research.semtk.querygen.Query;
import com.ge.research.semtk.querygen.QueryList;
import com.ge.research.semtk.querygen.timeseries.TimeSeriesConstraint;
import com.ge.research.semtk.querygen.timeseries.TimeSeriesQueryGenerator;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utilityge.Utility;



/**
 * Generate queries to retrieve time series data from KairosDB
 */
public class KairosDBQueryGenerator extends TimeSeriesQueryGenerator {

	// columns required to be present in the input table  
	public final static String[] REQUIRED_COLS = {Utility.COL_NAME_UUID, Utility.COL_NAME_DATABASE_SERVER, Utility.COL_NAME_TAG_NAME};
	// columns that may be present in the input table
	public final static String[] OPTIONAL_COLS = {Utility.COL_NAME_TAG_PREFIX};
	// columns required for building the configuration json for the query executor
	private final static String[] CONFIG_COLS = {Utility.COL_NAME_DATABASE_SERVER}; // e.g. "http://host.crd.ge.com:8080"

	private boolean prefixesProvided;  // true if prefix column is present, false if not

	/**
	 * Constructor
	 * @param locationAndValueInfo a table containing the required location columns
	 * @param constraintsJson a JSON object containing the constraints (both value and time)
	 * @throws Exception
	 */
	public KairosDBQueryGenerator(Table locationAndValueInfo, JSONObject constraintsJson) throws Exception {
		super(locationAndValueInfo, constraintsJson);

		// validate that input table has all of the required columns
		for(String reqCol : REQUIRED_COLS){
			if(!locationAndValueInfo.hasColumn(reqCol)){
				throw new Exception("Missing required column: " + reqCol);
			}
		}

		// see if optional column is present
		prefixesProvided = locationAndValueInfo.hasColumn(Utility.COL_NAME_TAG_PREFIX);
		
		// validate that table is populated
		if(locationAndValueInfo.getNumRows() == 0){
			throw new Exception("Cannot generate queries for empty table");
		}
	}

	/**
	 * Get the generated queries
	 * 
	 * @return a hashmap of UUID to QueryList
	 */
	@Override
	public HashMap<UUID, Object> getQueries() throws Exception {

		HashMap<UUID, Object> queryListMap = new HashMap<UUID,Object>();  // UUID to QueryList   

		// for each unique UUID
		String[] uuidsUnique = locationAndValueInfo.getColumnUniqueValues(Utility.COL_NAME_UUID);
		for(String uuid : uuidsUnique){

			queryListMap.put(UUID.fromString(uuid), new QueryList());	// instantiate an empty QueryList for this UUID

			Table dataForOneUUID;
			if(!prefixesProvided){
				dataForOneUUID = locationAndValueInfo.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid, REQUIRED_COLS);
			}else{
				dataForOneUUID = locationAndValueInfo.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid, (String[])ArrayUtils.addAll(REQUIRED_COLS, OPTIONAL_COLS));
			}
			Table configForOneUUID = locationAndValueInfo.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid, CONFIG_COLS);

			// get KairosDB database for this UUID (and confirm there is only 1)
			if(dataForOneUUID.getColumnUniqueValues(Utility.COL_NAME_DATABASE_SERVER).length > 1){
				throw new Exception("Cannot support querying more than one KairosDB database for a single UUID");
			}	

			// get config json (e.g. {"kairosDBUrl":"http://host.crd.ge.com:8080"})
			JSONObject config = this.getConfigForUUIDEntry(configForOneUUID);
			((QueryList)queryListMap.get(UUID.fromString(uuid))).addConfig(config);  // set config for QueryList

			// get Kairos metric names, prepending prefixes if provided
			HashSet<String> metrics = new HashSet<String>();
			for(int row = 0; row < dataForOneUUID.getNumRows(); row++){
				String tag = dataForOneUUID.getCell(row, dataForOneUUID.getColumnIndex(Utility.COL_NAME_TAG_NAME));
				if(!prefixesProvided){
					metrics.add(tag);						// metric name is the tag (no prefix)
				}else{
					String tagPrefix = dataForOneUUID.getCell(row, dataForOneUUID.getColumnIndex(Utility.COL_NAME_TAG_PREFIX)); 	
					metrics.add(tagPrefix + "." + tag); 	// metric name is prefix.tag
				}
			}

			// construct the query 
			String query = buildQuery(metrics, constraints, timeConstraint);
			((QueryList)queryListMap.get(UUID.fromString(uuid))).addQuery(new Query(query));	 // NOTE: for other query generators there may be more than 1 query per QueryList
		}

		return queryListMap;
	}


	/**
	 * Use the KairosDB Java API to construct the query.
	 * It is public for testing purposes only.
	 */
	public String buildQuery(HashSet<String> tagsWithPrefixes, ArrayList<TimeSeriesConstraint> constraints, TimeSeriesConstraint timeConstraint) throws Exception {
		try {

			// add constraints 
			if(this.constraints != null && this.constraints.size() > 0){
				throw new Exception("Cannot yet support constraints in KairosDB queries: " + this.constraints.get(0).toString()); 
				// TODO support these - use this.constraints and this.constraintsConjunction
			}
			if(this.timeConstraint != null){
				throw new Exception("Cannot yet support constraints in KairosDB queries: " + this.timeConstraint.toString()); 
				// TODO support these
			}

			QueryBuilder queryBuilder = QueryBuilder.getInstance();
			for(String s : tagsWithPrefixes){
				queryBuilder.setStart(10, TimeUnit.YEARS).addMetric(s); // start time is required - using 10 years to "get all".  
			}
			String queryJson = queryBuilder.build();
			LocalLogger.logToStdOut("KairosDBQueryGenerator built query: " + queryJson);
			return queryJson;

		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			throw new Exception("Cannot build KairosDB query: " + e.getMessage());
		}	

	}


	/**
	 * Generate a configJSON object that will be used to instantiate the query executor.
	 * 
	 * This config has one element (kairosDBUrl).  Key "kairosDBUrl", value "http://host.crd.ge.com:8080"
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject getConfigForUUIDEntry(Table configData) throws Exception {

		// all rows in the table must be the same - else can't generate a single config (note: this would already be caught by 1-database check above)
		if(!configData.allRowsMatch()){
			throw new Exception("Cannot produce configuration JSON for input with varying rows: " + configData.toCSVString());
		}

		JSONObject configJson = new JSONObject();		
		configJson.put(KairosDBClient.CONFIGJSONKEY_KAIROSDBURL, configData.getRows().get(0).get(configData.getColumnIndex(Utility.COL_NAME_DATABASE_SERVER)));
		return configJson;
	}	


}
