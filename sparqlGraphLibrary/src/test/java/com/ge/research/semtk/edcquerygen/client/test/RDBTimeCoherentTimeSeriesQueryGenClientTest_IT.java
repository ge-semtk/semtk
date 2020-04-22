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

package com.ge.research.semtk.edcquerygen.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edcquerygen.client.RDBTimeCoherentTimeSeriesQueryGenClient;
import com.ge.research.semtk.querygen.client.QueryGenClientConfig;
import com.ge.research.semtk.querygen.timeseries.rdb.RDBTimeCoherentTimeSeriesQueryGenerator;
import com.ge.research.semtk.querygen.timeseries.test.TimeSeriesConstraintTest;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utilityge.Utility;

public class RDBTimeCoherentTimeSeriesQueryGenClientTest_IT {
	
	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	private final String SERVICE_ENDPOINT_HIVE = "edcQueryGeneration/hive/generateTimeSeriesQueries";
	private final String SERVICE_ENDPOINT_ATHENA = "edcQueryGeneration/athena/generateTimeSeriesQueries";

	private final String[] COLS = RDBTimeCoherentTimeSeriesQueryGenerator.REQUIRED_COLS;
	private final String[] COL_TYPES = {"String","String","String","String","String","String","String"};
	private final String[] COLS_TO_CHECK = {Utility.COL_NAME_QUERY, Utility.COL_NAME_CONFIGJSON};
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();		
		SERVICE_PROTOCOL = IntegrationTestUtility.get("protocol");
		SERVICE_SERVER = IntegrationTestUtility.get("edcquerygenservice.server");
		SERVICE_PORT = IntegrationTestUtility.getInt("edcquerygenservice.port");
	}
	
	@Test
	public void testClient_Hive() throws Exception {

		QueryGenClientConfig conf = new QueryGenClientConfig(SERVICE_PROTOCOL,SERVICE_SERVER,SERVICE_PORT,SERVICE_ENDPOINT_HIVE);
		RDBTimeCoherentTimeSeriesQueryGenClient client = new RDBTimeCoherentTimeSeriesQueryGenClient(conf);
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();
		Table locationAndValueInfoTable = getTestTable(uuid1, uuid2);

		String edcConstraintJsonStr = "{\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"100\",\"@type\":\"number\"}},{\"@var\":\"TEMPERATURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"60\",\"@type\":\"number\"}}]},\"@timeConstraint\":" + TimeSeriesConstraintTest.getSampleTimeSeriesConstraintJsonStr() + "}";
		JSONObject edcConstraintJson = com.ge.research.semtk.utility.Utility.getJsonObjectFromString(edcConstraintJsonStr);
		// execute
		TableResultSet resultSet = client.execute(locationAndValueInfoTable, edcConstraintJson, null);
		Table resultTable = resultSet.getResults();

		assertTrue(resultSet.getSuccess());
		assertEquals(resultTable.getNumRows(),2);
		assertEquals(resultTable.getNumColumns(),3);
		Table queriesForUUID1 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid1.toString(), COLS_TO_CHECK);			
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_QUERY)),"SELECT cast(`datetime_utc` AS timestamp) AS `timestamp`, `col31` AS `PRESSURE`, `col32` AS `TEMPERATURE`, `ca_33` AS `SPEED` FROM dbase.table3333 WHERE ((`col31` > 100) AND (`col32` > 50 AND `col32` < 60)) AND ((unix_timestamp(to_utc_timestamp(`datetime_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') >= unix_timestamp('2016-04-07 02:00:00','yyyy-MM-dd HH:mm:ss')) AND (unix_timestamp(to_utc_timestamp(`datetime_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') <= unix_timestamp('2016-04-09 04:00:00','yyyy-MM-dd HH:mm:ss'))) ORDER BY `timestamp`");
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_CONFIGJSON)),"{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"server\"}");
		Table queriesForUUID2 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid2.toString(), COLS_TO_CHECK);	
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_QUERY)),"SELECT cast(`datetime_utc` AS timestamp) AS `timestamp`, `col41` AS `PRESSURE`, `col42` AS `TEMPERATURE`, `col43` AS `SPEED` FROM dbase.table4444 WHERE ((`col41` > 100) AND (`col42` > 50 AND `col42` < 60)) AND ((unix_timestamp(to_utc_timestamp(`datetime_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') >= unix_timestamp('2016-04-07 02:00:00','yyyy-MM-dd HH:mm:ss')) AND (unix_timestamp(to_utc_timestamp(`datetime_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') <= unix_timestamp('2016-04-09 04:00:00','yyyy-MM-dd HH:mm:ss'))) ORDER BY `timestamp`");
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_CONFIGJSON)),"{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"server\"}");

	}
	
	
	@Test
	public void testClient_Athena() throws Exception {

		QueryGenClientConfig conf = new QueryGenClientConfig(SERVICE_PROTOCOL,SERVICE_SERVER,SERVICE_PORT,SERVICE_ENDPOINT_ATHENA);
		RDBTimeCoherentTimeSeriesQueryGenClient client = new RDBTimeCoherentTimeSeriesQueryGenClient(conf);

		// create a table
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		UUID uuid1 = UUID.randomUUID();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"athena","dbase","table3333","ts_time_utc","PRESSURE","col31")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"athena","dbase","table3333","ts_time_utc","TEMPERATURE","col32")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"athena","dbase","table3333","ts_time_utc","SPEED","ca_33")));
		UUID uuid2 = UUID.randomUUID();
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),"athena","dbase","table4444","ts_time_utc","PRESSURE","col41")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),"athena","dbase","table4444","ts_time_utc","TEMPERATURE","col42")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),"athena","dbase","table4444","ts_time_utc","SPEED","col43")));					
		Table locationAndValueInfoTable = new Table(COLS, COL_TYPES, rows);

		String edcConstraintJsonStr = "{\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"100\",\"@type\":\"number\"}},{\"@var\":\"TEMPERATURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"60\",\"@type\":\"number\"}}]},\"@timeConstraint\":" + TimeSeriesConstraintTest.getSampleTimeSeriesConstraintJsonStr() + "}";
		JSONObject edcConstraintJson = com.ge.research.semtk.utility.Utility.getJsonObjectFromString(edcConstraintJsonStr);
		// execute
		TableResultSet resultSet = client.execute(locationAndValueInfoTable, edcConstraintJson, null);
		Table resultTable = resultSet.getResults();

		assertTrue(resultSet.getSuccess());
		assertEquals(resultTable.getNumRows(),2);
		assertEquals(resultTable.getNumColumns(),3);
		Table queriesForUUID1 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid1.toString(), COLS_TO_CHECK);
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_QUERY)),"SELECT from_unixtime(\"ts_time_utc\") AS \"timestamp\", \"col31\" AS \"PRESSURE\", \"col32\" AS \"TEMPERATURE\", \"ca_33\" AS \"SPEED\" FROM dbase.table3333 WHERE ((\"col31\" > 100) AND (\"col32\" > 50 AND \"col32\" < 60)) AND ((from_unixtime(\"ts_time_utc\") >= timestamp '2016-04-07 02:00:00') AND (from_unixtime(\"ts_time_utc\") <= timestamp '2016-04-09 04:00:00')) ORDER BY \"timestamp\"");
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_CONFIGJSON)),"{\"database\":\"dbase\"}");   // no host/port
		Table queriesForUUID2 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid2.toString(), COLS_TO_CHECK);	
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_QUERY)),"SELECT from_unixtime(\"ts_time_utc\") AS \"timestamp\", \"col41\" AS \"PRESSURE\", \"col42\" AS \"TEMPERATURE\", \"col43\" AS \"SPEED\" FROM dbase.table4444 WHERE ((\"col41\" > 100) AND (\"col42\" > 50 AND \"col42\" < 60)) AND ((from_unixtime(\"ts_time_utc\") >= timestamp '2016-04-07 02:00:00') AND (from_unixtime(\"ts_time_utc\") <= timestamp '2016-04-09 04:00:00')) ORDER BY \"timestamp\"");
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_CONFIGJSON)),"{\"database\":\"dbase\"}");	// no host/port

	}
	
	
	@Test
	public void testClient_NullConstraints() throws Exception {

		QueryGenClientConfig conf = new QueryGenClientConfig(SERVICE_PROTOCOL,SERVICE_SERVER,SERVICE_PORT,SERVICE_ENDPOINT_HIVE);
		RDBTimeCoherentTimeSeriesQueryGenClient client = new RDBTimeCoherentTimeSeriesQueryGenClient(conf);			
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();
		Table locationAndValueInfoTable = getTestTable(uuid1, uuid2);

		// execute
		TableResultSet resultSet = client.execute(locationAndValueInfoTable, null, null); // passing in null constraints
		Table resultTable = resultSet.getResults();

		assertTrue(resultSet.getSuccess());
		assertEquals(resultTable.getNumRows(),2);
		assertEquals(resultTable.getNumColumns(),3);
		Table queriesForUUID1 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid1.toString(), COLS_TO_CHECK);			
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_QUERY)),"SELECT cast(`datetime_utc` AS timestamp) AS `timestamp`, `col31` AS `PRESSURE`, `col32` AS `TEMPERATURE`, `ca_33` AS `SPEED` FROM dbase.table3333 ORDER BY `timestamp`");
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_CONFIGJSON)),"{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"server\"}");
		Table queriesForUUID2 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid2.toString(), COLS_TO_CHECK);	
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_QUERY)),"SELECT cast(`datetime_utc` AS timestamp) AS `timestamp`, `col41` AS `PRESSURE`, `col42` AS `TEMPERATURE`, `col43` AS `SPEED` FROM dbase.table4444 ORDER BY `timestamp`");
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_CONFIGJSON)),"{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"server\"}");				
	}
	
	
	@Test
	public void testClient_Flags() throws Exception {

		QueryGenClientConfig conf = new QueryGenClientConfig(SERVICE_PROTOCOL,SERVICE_SERVER,SERVICE_PORT,SERVICE_ENDPOINT_HIVE);
		RDBTimeCoherentTimeSeriesQueryGenClient client = new RDBTimeCoherentTimeSeriesQueryGenClient(conf);
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();
		Table locationAndValueInfoTable = getTestTable(uuid1, uuid2);

		// set 2 flags
		QueryFlags flags = new QueryFlags(new String[] {RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_OMIT_ALIASES, RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_RAW_TIMESTAMP} );

		// execute
		TableResultSet resultSet = client.execute(locationAndValueInfoTable, null, flags); // null constraints
		Table resultTable = resultSet.getResults();

		assertTrue(resultSet.getSuccess());
		assertEquals(resultTable.getNumRows(),2);
		assertEquals(resultTable.getNumColumns(),3);
		Table queriesForUUID1 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid1.toString(), COLS_TO_CHECK);			
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_QUERY)),"SELECT `datetime_utc` AS `datetime_utc`, `col31` AS `col31`, `col32` AS `col32`, `ca_33` AS `ca_33` FROM dbase.table3333 ORDER BY `datetime_utc`");
		Table queriesForUUID2 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid2.toString(), COLS_TO_CHECK);	
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_QUERY)),"SELECT `datetime_utc` AS `datetime_utc`, `col41` AS `col41`, `col42` AS `col42`, `col43` AS `col43` FROM dbase.table4444 ORDER BY `datetime_utc`");			

	}
	
	
	/**
	 * Table used in multiple tests
	 */
	private Table getTestTable(UUID uuid1, UUID uuid2) throws Exception{
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"jdbc:hive2://server:10000","dbase","table3333","datetime_utc","PRESSURE","col31")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"jdbc:hive2://server:10000","dbase","table3333","datetime_utc","TEMPERATURE","col32")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"jdbc:hive2://server:10000","dbase","table3333","datetime_utc","SPEED","ca_33")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),"jdbc:hive2://server:10000","dbase","table4444","datetime_utc","PRESSURE","col41")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),"jdbc:hive2://server:10000","dbase","table4444","datetime_utc","TEMPERATURE","col42")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),"jdbc:hive2://server:10000","dbase","table4444","datetime_utc","SPEED","col43")));					
		return new Table(COLS, COL_TYPES, rows);
	}
}
