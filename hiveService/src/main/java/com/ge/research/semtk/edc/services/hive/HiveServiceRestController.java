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


package com.ge.research.semtk.edc.services.hive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

import org.json.simple.JSONObject;

import com.ge.research.semtk.query.rdb.HiveConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.springutilib.requests.DatabaseQueryRequest;
import com.ge.research.semtk.springutilib.requests.DatabaseRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.edc.services.hive.HiveProperties;
import com.ge.research.semtk.lib.resultSet.TableOrJobIdResultSet;

/**
 * Service to execute queries on Hive
 */
@CrossOrigin
@RestController
@RequestMapping("/hiveService")
public class HiveServiceRestController {
 	private static final String SERVICE_NAME = "HiveService";
 	
	private static String setStmt = "set hive.exec.stagingdir=/tmp/hive-staging;";

	private boolean ASYNC = true;	// true for asynchronous mode (returns job id), false for synchronous mode (returns query results)
	
	@Autowired 
	private ApplicationContext appContext;
	@Autowired
	HiveProperties props;
	@Autowired
	ResultsServiceProperties resultsProps;
	@Autowired
	StatusServiceProperties statusProps;
	
	@PostConstruct
    public void init() {		
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		props.validateWithExit();
		resultsProps.validateWithExit();
		statusProps.validateWithExit();
	}
	
	/**
	 * Execute arbitrary query in Hive.  
	 * Returns a job id (running in asynchronous mode)
	 */
	@CrossOrigin
	@RequestMapping(value="/queryHive", method= RequestMethod.POST)
	public JSONObject queryHive(@RequestBody DatabaseQueryRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers); // add security to async job
		String query = requestBody.query;
		JSONObject ret = runQuery(requestBody, query);
		LocalLogger.logToStdOut(SERVICE_NAME + " queryHive initiated asynchronous query " + query);
		return ret;
	}
	
	/**
	 * Execute arbitrary query in Hive.  
	 * Returns the query results (not a job id).
	 */
	@CrossOrigin
	@RequestMapping(value="/queryHiveSync", method= RequestMethod.POST)
	public JSONObject queryHiveSync(@RequestBody DatabaseQueryRequest requestBody){
		long startTimeMillis = System.currentTimeMillis();
		String query = requestBody.query;
		JSONObject ret =  runQuery(requestBody, query, false);
		LocalLogger.logToStdOut(SERVICE_NAME + " queryHiveSync completed in " + Utility.getSecondsSince(startTimeMillis) + " sec for query " + query);
		return ret;
	}

	/**
	 * Execute count of each unique element on a specific column in Hive
	 */
	@CrossOrigin
	@RequestMapping(value="/count", method= RequestMethod.POST)
	public JSONObject count(@RequestBody HiveServiceCountRequestBody requestBody){
		String col = requestBody.column;
		String query = "select " + col + ", count(" + col + ") as " + col + "_count from " + requestBody.table + " group by " + col;
		return runQuery (requestBody, query);
	}

	/**
	 * Execute count number of rows of a specific table in Hive
	 */
	@CrossOrigin
	@RequestMapping(value="/countRows", method= RequestMethod.POST)
	public JSONObject countRows(@RequestBody HiveServiceCountRowsRequestBody requestBody){
		String query = "select count(*) as " + requestBody.table + "_count from " + requestBody.table;
		return runQuery (requestBody, query);
	}

	/**
	 * Return random sampling of rows from a specific table in Hive
	 */
	@CrossOrigin
	@RequestMapping(value="/randomSampling", method= RequestMethod.POST)
	public JSONObject randomSampling(@RequestBody HiveServiceRandomSamplingRequestBody requestBody){
		String query = "select * from " + requestBody.table + " distribute by rand() sort by rand() limit " + requestBody.numRows;
		return runQuery (requestBody, query);
	}

	/**
	 * Get full row of data where a specific column has min or max value in Hive
	 */
	@CrossOrigin
	@RequestMapping(value="/minmaxRow", method= RequestMethod.POST)
	public JSONObject minmaxRow(@RequestBody HiveServiceMinMaxRowRequestBody requestBody){
		String sortOrder = null;
		String op = requestBody.operation;
		if (op == null)
			op = "";
		op = op.toLowerCase ();
		switch (op) {
			case "min":	sortOrder = "asc";
					break;
			case "max":	sortOrder = "desc";
					break;
			default:	TableResultSet tableResultSet = new TableResultSet();
					tableResultSet.setSuccess(false);
					tableResultSet.addRationaleMessage("Unknown operation: " + requestBody.operation + ".\n  Valid options are: min or max.");
					return tableResultSet.toJson();
		}

		String query = "select * from (SELECT *, rank() over (order by cast(" + requestBody.column + " as DOUBLE) " + sortOrder + ") as rank FROM " + requestBody.table + ") S where S.rank = 1";

		return runQuery (requestBody, query);
	}

	/**
	 * Execute row filter query on one or more columns in Hive
	 */
	@CrossOrigin
	@RequestMapping(value="/rowFilter", method= RequestMethod.POST)
	public JSONObject rowFilter(@RequestBody HiveServiceRowFilterRequestBody requestBody){

		String whereClause = "";
		for (ColumnFilter co : requestBody.columnFilters) {
			String operation = null;
			String op = co.operation;
			if (op == null)
				op = "";
			op = op.toLowerCase ();
			switch (op) {
				case "gt":	operation = ">";
						break;
				case "lt":	operation = "<";
						break;
				case "gte":	operation = ">=";
						break;
				case "lte":	operation = "<=";
						break;
				case "eq": operation = "=";
						break;
				default:	TableResultSet tableResultSet = new TableResultSet();
						tableResultSet.setSuccess(false);
						tableResultSet.addRationaleMessage("Unknown operation: " + co.operation + ".\n  Valid options are: min, max, avg, sum, variance, and stddev.");
						return tableResultSet.toJson();
			}
			if (!whereClause.equals (""))
				whereClause += " and ";
			String col = co.column;
			if (!co.type.toLowerCase().equalsIgnoreCase("string"))
				col = "cast(" + col + " as " + co.type + ")";
			whereClause += col + " " + operation + " " + co.value;
		}

		String query = "select S.* from " + requestBody.table + " S  where " + whereClause;
		return runQuery (requestBody, query);
	}

	/**
	 * Execute stats query on one or more columns in Hive
	 */
	@CrossOrigin
	@RequestMapping(value="/stat", method= RequestMethod.POST)
	public JSONObject stat(@RequestBody HiveServiceStatRequestBody requestBody){

		String query = "";
		for (ColumnOperation co : requestBody.columnOperations) {
			String operation = null;
			String op = co.operation;
			if (op == null)
				op = "";
			op = op.toLowerCase ();
			switch (op) {
				case "min":	operation = "min";
						break;
				case "max":	operation = "max";
						break;
				case "avg":	operation = "avg";
						break;
				case "sum":	operation = "sum";
						break;
				case "variance": operation = "variance";
						break;
				case "stddev":	operation = "stddev_pop";
						break;
				default:	TableResultSet tableResultSet = new TableResultSet();
						tableResultSet.setSuccess(false);
						tableResultSet.addRationaleMessage("Unknown operation: " + co.operation + ".\n  Valid options are: min, max, avg, sum, variance, and stddev.");
						return tableResultSet.toJson();
			}
			if (!query.equals (""))
				query += ", ";
			query += operation + "(cast(" + co.column + " as DOUBLE)) as " + co.operation + "_" + co.column;
		}

		query = "select " + query + " from " + requestBody.table;
		return runQuery (requestBody, query);
	}

	/**
	 * Execute histogram query in Hive
	 */
	@CrossOrigin
	@RequestMapping(value="/histogram", method= RequestMethod.POST)
	public JSONObject histogram(@RequestBody HiveServiceHistogramRequestBody requestBody){
		String query = "";
		for (String column : requestBody.columns) {
			if (!query.equals (""))
				query += ", ";
			query += "histogram_numeric(cast(" + column + " as DOUBLE), " + requestBody.numBuckets + ") as histogram_" + column;
		}

		query = "select " + query + " from " + requestBody.table;
		return runQuery (requestBody, query);
	}

	/**
	 * Execute percentile query in Hive
	 */
	@CrossOrigin
	@RequestMapping(value="/percentile", method= RequestMethod.POST)
	public JSONObject percentile(@RequestBody HiveServicePercentileRequestBody requestBody){
		String percentiles = "";
		for (String percentile : requestBody.percentiles) {
			if (!percentiles.equals (""))
				percentiles += ", ";
			if (percentile.startsWith("."))
				percentiles += "0";
			percentiles += percentile;
		}

		String query = "";
		for (String column : requestBody.columns) {
			if (!query.equals (""))
				query += ", ";
			query += "percentile_approx(cast(" + column + " as DOUBLE), array(" + percentiles + ")) as percentile_" + column;
		}

		query = "select " + query + " from " + requestBody.table;
		return runQuery (requestBody, query);
	}

	/**
	 * Execute query in Hive
	 */
	private JSONObject runQuery (DatabaseRequest requestBody, String query) {
		return runQuery(requestBody, query, ASYNC);
	}
		
	/**
	 * Execute query in Hive
	 */
	private JSONObject runQuery (DatabaseRequest requestBody, String query, boolean async) {
		
		// prepend statement to set execution engine (should be mr/tez/spark - Hive itself will give a nice error if not)
		String setStmtExecutionEngine = "";  
		if(props.getExecutionEngine() != null && !props.getExecutionEngine().trim().isEmpty()){ // if property missing or blank, then don't include this clause
			setStmtExecutionEngine = "set hive.execution.engine=" + props.getExecutionEngine().trim() + ";";
		}
		query = setStmt + " " + setStmtExecutionEngine + " " + query;
		
		TableOrJobIdResultSet resultSet = new TableOrJobIdResultSet();

		// get credentials from property file
		String username = props.getUsername();
		String password = props.getPassword();

		try {
			LocalLogger.logToStdOut("Connecting to: " + HiveConnector.getDatabaseURL(requestBody.host, Integer.valueOf(requestBody.port), requestBody.database));
			HiveConnector oc;
			if(props.getLoginTimeoutSec() == null){
				oc = new HiveConnector(requestBody.host, Integer.valueOf(requestBody.port), requestBody.database, username, password); // use default timeout
			}else{
				oc = new HiveConnector(requestBody.host, Integer.valueOf(requestBody.port), requestBody.database, username, password, props.getLoginTimeoutSec().intValue()); 
			}
				
			if (async) {
				LocalLogger.logToStdOut("Async Hive query: " + query);
				HiveQueryThread queryThread = new HiveQueryThread(oc, query, statusProps, resultsProps);
				queryThread.start();
				resultSet.addResults(queryThread.getJobId());
			} else {
				LocalLogger.logToStdOut("Hive query: " + query);
				Table table = oc.query(query);
				LocalLogger.logToStdOut("Returning num rows: " + (table != null ? String.valueOf(table.getNumRows()) : "<null>"));
				resultSet.addResults(table);
			}
			resultSet.setSuccess(true);

		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage("Failed executing query " + query + ": " + e.getMessage());
		}
		
		return resultSet.toJson();
	}

}
