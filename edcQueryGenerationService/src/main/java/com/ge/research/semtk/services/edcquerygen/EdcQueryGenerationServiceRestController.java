/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.services.edcquerygen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.AthenaQueryFragmentBuilder;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.HiveQueryFragmentBuilder;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.TimeSeriesQueryFragmentBuilder;
import com.ge.research.semtk.querygen.timeseries.kairosdb.KairosDBQueryGenerator;
import com.ge.research.semtk.querygen.timeseries.rdb.RDBTimeCoherentTimeSeriesQueryGenerator;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.dispatch.EdcDispatcher;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;
import com.ge.research.semtk.springutilib.requests.EdcQueryGenBaseRequestBody;
import com.ge.research.semtk.springutilib.requests.EdcQueryGenRequestBody;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utilityge.Utility;

/**
 * Query generator for EDC.  Support various external stores.
 */
@RestController
@RequestMapping("/edcQueryGeneration")
public class EdcQueryGenerationServiceRestController {			

	/**
	 * Generate Hive queries.
	 */
	@CrossOrigin
	@RequestMapping(value="/hive/generateTimeSeriesQueries", method= RequestMethod.POST)
	public JSONObject generateQueriesHive(@RequestBody EdcQueryGenRequestBody requestBody){	
		return generateRDBTimeSeriesQueries(requestBody, new HiveQueryFragmentBuilder());	
	}
	
	
	/**
	 * Generate Athena queries.
	 */
	@CrossOrigin
	@RequestMapping(value="/athena/generateTimeSeriesQueries", method= RequestMethod.POST)
	public JSONObject generateQueriesAthena(@RequestBody EdcQueryGenRequestBody requestBody){	
		return generateRDBTimeSeriesQueries(requestBody, new AthenaQueryFragmentBuilder());	
	}	

	/**
	 * Generate KairosDB queries.
	 */
	@CrossOrigin
	@RequestMapping(value="/kairosDB/generateQueries", method= RequestMethod.POST)
	public JSONObject generateQueries(@RequestBody EdcQueryGenRequestBody requestBody){	
		TableResultSet resultSet = null;
		try{
			Table locationAndValueInfoTable = requestBody.getLocationAndValueInfoTable();
			JSONObject constraintsJson = requestBody.getConstraintsJson();	// null if no constraints
			LocalLogger.logToStdOut("Generate KairosDB queries for \n" + locationAndValueInfoTable.toCSVString() + "\n" + (constraintsJson == null ? "" : constraintsJson.toJSONString()));
			
			KairosDBQueryGenerator g = new KairosDBQueryGenerator(locationAndValueInfoTable, constraintsJson);			
			HashMap<UUID, Object> queriesHash = g.getQueries();	
			Table retTable = EdcDispatcher.getTableForDispatcher(queriesHash);  // convert to table needed by Dispatcher

			// put the Table in a TableResultSet
			resultSet = new TableResultSet(true); 
			resultSet.addResults(retTable);

		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);	
			resultSet = new TableResultSet(false);
			resultSet.addRationaleMessage(e.getMessage());
		}
		return resultSet.toJson();	
	}



	/**
	 * Generate "queries" to the binary file service - a passthrough for URLs and filenames.
	 */
	@CrossOrigin
	@RequestMapping(value="/binaryFile/generateQueries", method= RequestMethod.POST)
	public JSONObject generateQueries(@RequestBody EdcQueryGenBaseRequestBody requestBody){	
		
		// columns required to be present in the location input table  
		final String[] REQUIRED_COLS = { Utility.COL_NAME_UUID, Utility.COL_NAME_URL, Utility.COL_NAME_FILENAME }; 
		final String DELIMITER = "###"; // delimiter for the "query" (e.g. "hdfs://test1-98231834.img###test1.img")

		TableResultSet resultSet = null;
		try{

			Table locationAndValueInfoTable = requestBody.getLocationAndValueInfoTable(); 
			LocalLogger.logToStdOut("Generate binary file queries for \n" + locationAndValueInfoTable.toCSVString());			
			
			// validate that required columns are present
			for(String reqCol : REQUIRED_COLS){
				if(!locationAndValueInfoTable.hasColumn(reqCol)){
					throw new Exception("Missing required column: " + reqCol);
				}
			}
			
			final int INDEX_UUID = locationAndValueInfoTable.getColumnIndex(Utility.COL_NAME_UUID); // a single UUID may be found in multiple rows
			final int INDEX_URL = locationAndValueInfoTable.getColumnIndex(Utility.COL_NAME_URL);
			final int INDEX_FILENAME = locationAndValueInfoTable.getColumnIndex(Utility.COL_NAME_FILENAME);

			// create the table to return
			// sample:
			// 		UUID,Query,ConfigJSON
			// 		a9248460-02f0-4ab1-8604-30df416d44d7,hdfs://test1-98231834.img###test1.img,{}
			// 		564f69b6-6444-4030-a16b-be8ef29a9a82,hdfs://test2-13523653.img###test2.img,{}
			Table retTable = new Table(EdcDispatcher.EDC_DISPATCHER_COL_NAMES, EdcDispatcher.EDC_DISPATCHER_COL_TYPES, null);
			String url;
			String filename;
			for(int i = 0; i < locationAndValueInfoTable.getNumRows(); i++){
				ArrayList<String> row = new ArrayList<String>();
				row.add(locationAndValueInfoTable.getCell(i, INDEX_UUID));			
				url = locationAndValueInfoTable.getCell(i, INDEX_URL).trim();
				filename = locationAndValueInfoTable.getCell(i, INDEX_FILENAME).trim();
				validate(url, "URL", DELIMITER);				// error if null or empty, or contains delimiter
				validate(filename, "File name", DELIMITER);		// error if null or empty, or contains delimiter
				row.add(url.trim() + DELIMITER + filename.trim()); // the "query" is a string: URL + delimiter + filename
				row.add("{}");  // empty configJson - nothing needed to instantiate executor client
				retTable.addRow(row);
			}

			LocalLogger.logToStdOut("Returning: \n" + retTable.toCSVString());

			// put the Table in a TableResultSet
			resultSet = new TableResultSet(true); 
			resultSet.addResults(retTable);

		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);	
			resultSet = new TableResultSet(false);
			resultSet.addRationaleMessage(e.getMessage());
		}
		return resultSet.toJson();		

	}
	
	
	
	/**
	 * Generate time-coherent (row-style) time series queries against RDB data store (e.g. Hive).
	 * Fulfill the request using the given query fragment builder.
	 */
	private JSONObject generateRDBTimeSeriesQueries(EdcQueryGenRequestBody requestBody, TimeSeriesQueryFragmentBuilder queryFragmentBuilder){
		TableResultSet resultSet = null;
		try{
			Table locationAndValueInfoTable = requestBody.getLocationAndValueInfoTable();
			JSONObject constraintsJson = requestBody.getConstraintsJson();	// null if no constraints
			QueryFlags flags = requestBody.getQueryFlags();		// null if no flags sent
			LocalLogger.logToStdOut("---Generate RDB queries for \n" + locationAndValueInfoTable.toCSVString() + (constraintsJson == null ? "" : constraintsJson.toJSONString() + "\n")  + (flags == null ? "" : flags.toJSONString()));
	
			RDBTimeCoherentTimeSeriesQueryGenerator g = new RDBTimeCoherentTimeSeriesQueryGenerator(queryFragmentBuilder, locationAndValueInfoTable, constraintsJson, flags);			
			HashMap<UUID, Object> queriesHash = g.getQueries();	
			Table retTable = EdcDispatcher.getTableForDispatcher(queriesHash);  // convert to table needed by Dispatcher
			
			// put the Table in a TableResultSet
			resultSet = new TableResultSet(true); 
			resultSet.addResults(retTable);
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);	
			resultSet = new TableResultSet(false);
			resultSet.addRationaleMessage(e.getMessage());
		}
		return resultSet.toJson();		
	}	


	/**
	 * Utility method
	 */
	private void validate(String s, String sDescription, String delimiter) throws Exception{
		if(s == null || s.trim().equals("null") || s.trim().isEmpty() || s.contains(delimiter)){
			throw new Exception(sDescription + " '" + s + "' is null, empty, or contains " + delimiter);
		}
	}

}
