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

package com.ge.research.semtk.querygen.timeseries.rdb.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.querygen.QueryList;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.HiveQueryFragmentBuilder;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.AthenaQueryFragmentBuilder;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.TimeSeriesQueryFragmentBuilder;
import com.ge.research.semtk.querygen.timeseries.rdb.RDBTimeCoherentTimeSeriesQueryGenerator;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.Utility;


public class RDBRowQueryGeneratorTest {

	private final String[] COL_TYPES_7 = {"String","String","String","String","String","String","String"};

	private TimeSeriesQueryFragmentBuilder hiveQueryFragmentBuilder = new HiveQueryFragmentBuilder(); 
	private TimeSeriesQueryFragmentBuilder athenaQueryFragmentBuilder = new AthenaQueryFragmentBuilder();  
	RDBTimeCoherentTimeSeriesQueryGenerator generator;
	HashMap<UUID, Object> queriesHash;
	
	private static JSONObject getSampleConstraintJson() throws Exception{
		String s = "{\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"100\",\"@type\":\"number\"}},{\"@var\":\"TEMPERATURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"60\",\"@type\":\"number\"}}]},\"@timeConstraint\":" + IntegrationTestUtility.getSampleTimeSeriesConstraintJsonStr() + "}";
		return Utility.getJsonObjectFromString(s);	
	}
	
	@Test
	public void testSettingFlags() throws Exception{

		Table locationAndValueInfo = new Table(RDBTimeCoherentTimeSeriesQueryGenerator.REQUIRED_COLS, COL_TYPES_7);
		JSONObject constraintJson = Utility.getJsonObjectFromString("{}");

		// test valid flags
		QueryFlags flags = new QueryFlags();
		flags.set(RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_OMIT_ALIASES);
		flags.set(RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_RAW_TIMESTAMP);
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson, flags);
		assertTrue(generator.isFlagSet(RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_OMIT_ALIASES));
		assertTrue(generator.isFlagSet(RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_RAW_TIMESTAMP));
		assertFalse(generator.isFlagSet("NOT SET"));

		// test null flags
		flags = null;
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson, flags);
		assertFalse(generator.isFlagSet("NOT SET"));

		// test empty flags
		flags = new QueryFlags();
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson, flags);
		assertFalse(generator.isFlagSet("NOT SET"));
	}

	
	@Test
	public void testAndOrConstraints() throws Exception {

		// create a table
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		UUID uuid1 = UUID.randomUUID();
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,ts_time_utc,PRESSURE,col628"));
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,ts_time_utc,TEMPERATURE,col629"));
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,ts_time_utc,SPEED,col630"));					
		Table locationAndValueInfo = new Table(RDBTimeCoherentTimeSeriesQueryGenerator.REQUIRED_COLS, COL_TYPES_7, rows);

		String constraintJsonStr;
		JSONObject constraintJson;
		QueryList queryList;			

		// no constraint
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, null); 
		queryList = ((QueryList)(generator.getQueries()).get(uuid1));
		assertEquals(queryList.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col628` AS `PRESSURE`, `col629` AS `TEMPERATURE`, `col630` AS `SPEED` FROM dbase.table5544 ORDER BY `timestamp`"); // runnable on our Hive					

		// timestamp constraint only
		constraintJsonStr = "{\"@timeConstraint\":{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"10/08/2014 10:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"10/08/2014 11:00:00 AM\",\"@type\":\"datetime\"}}}";
		constraintJson = Utility.getJsonObjectFromString(constraintJsonStr);
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson);
		queryList = ((QueryList)(generator.getQueries()).get(uuid1));
		assertEquals(queryList.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col628` AS `PRESSURE`, `col629` AS `TEMPERATURE`, `col630` AS `SPEED` FROM dbase.table5544 WHERE ((unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') >= unix_timestamp('2014-10-08 10:00:00','yyyy-MM-dd HH:mm:ss')) AND (unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') <= unix_timestamp('2014-10-08 11:00:00','yyyy-MM-dd HH:mm:ss'))) ORDER BY `timestamp`");		

		// AND constraint
		constraintJsonStr = "{\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"80\",\"@type\":\"number\"}},{\"@var\":\"TEMPERATURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"40000\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"70000\",\"@type\":\"number\"}}]},\"@timeConstraint\":{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"10/08/2014 10:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"10/08/2014 11:00:00 AM\",\"@type\":\"datetime\"}}}";
		constraintJson = Utility.getJsonObjectFromString(constraintJsonStr);
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson);
		queryList = ((QueryList)(generator.getQueries()).get(uuid1));
		assertEquals(queryList.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col628` AS `PRESSURE`, `col629` AS `TEMPERATURE`, `col630` AS `SPEED` FROM dbase.table5544 WHERE ((`col628` > 80) AND (`col629` > 40000 AND `col629` < 70000)) AND ((unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') >= unix_timestamp('2014-10-08 10:00:00','yyyy-MM-dd HH:mm:ss')) AND (unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') <= unix_timestamp('2014-10-08 11:00:00','yyyy-MM-dd HH:mm:ss'))) ORDER BY `timestamp`");	

		// values constraints only
		constraintJsonStr = "{\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"80\",\"@type\":\"number\"}},{\"@var\":\"TEMPERATURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"40000\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"70000\",\"@type\":\"number\"}}]}}";
		constraintJson = Utility.getJsonObjectFromString(constraintJsonStr);
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson);
		queryList = ((QueryList)(generator.getQueries()).get(uuid1));
		assertEquals(queryList.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col628` AS `PRESSURE`, `col629` AS `TEMPERATURE`, `col630` AS `SPEED` FROM dbase.table5544 WHERE ((`col628` > 80) AND (`col629` > 40000 AND `col629` < 70000)) ORDER BY `timestamp`"); // runnable on our Hive		

		// OR constraint
		constraintJsonStr = "{\"@constraintSet\":{\"@op\":\"OR\",\"@constraints\":[{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"80\",\"@type\":\"number\"}},{\"@var\":\"TEMPERATURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"40000\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"70000\",\"@type\":\"number\"}}]},\"@timeConstraint\":{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"10/08/2014 10:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"10/08/2014 11:00:00 AM\",\"@type\":\"datetime\"}}}";
		constraintJson = Utility.getJsonObjectFromString(constraintJsonStr);
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson);
		queryList = ((QueryList)(generator.getQueries()).get(uuid1));
		assertEquals(queryList.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col628` AS `PRESSURE`, `col629` AS `TEMPERATURE`, `col630` AS `SPEED` FROM dbase.table5544 WHERE ((`col628` > 80) OR (`col629` > 40000 AND `col629` < 70000)) AND ((unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') >= unix_timestamp('2014-10-08 10:00:00','yyyy-MM-dd HH:mm:ss')) AND (unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') <= unix_timestamp('2014-10-08 11:00:00','yyyy-MM-dd HH:mm:ss'))) ORDER BY `timestamp`");

	}	


	@Test
	public void test() throws Exception {

		JSONObject constraintJson = getSampleConstraintJson();

		ArrayList<ArrayList<String>> rows;
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();
		Table locationAndValueInfo;
		QueryList queriesForUUID1;
		QueryList queriesForUUID2;

		//------------ Hive -----------

		// create a table
		rows = new ArrayList<ArrayList<String>>();
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,ts_time_utc,PRESSURE,col27"));
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,ts_time_utc,TEMPERATURE,col28"));
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,ts_time_utc,SPEED,col29"));
		rows.add(toArrayList(uuid2.toString() + ",jdbc:hive2://myserver:10000,dbase,table4444,ts_time_utc,PRESSURE,col41"));
		rows.add(toArrayList(uuid2.toString() + ",jdbc:hive2://myserver:10000,dbase,table4444,ts_time_utc,TEMPERATURE,col42"));
		rows.add(toArrayList(uuid2.toString() + ",jdbc:hive2://myserver:10000,dbase,table4444,ts_time_utc,SPEED,col43"));
		String[] COL_TYPES_7 = {"String","String","String","String","String","String","String"};
		locationAndValueInfo = new Table(RDBTimeCoherentTimeSeriesQueryGenerator.REQUIRED_COLS, COL_TYPES_7, rows);

		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson);
		assertEquals(generator.getConstraints().size(),2);
		assertEquals(generator.getConstraints().get(0).getVariableName(),"PRESSURE");
		assertEquals(generator.getConstraints().get(1).getVariableName(),"TEMPERATURE");
		assertEquals(generator.getTimeConstraint().getVariableName(),"_Time_");
		queriesHash = generator.getQueries();  // each Object is an QueryList
		assertEquals(queriesHash.size(),2);			
		queriesForUUID1 = (QueryList)queriesHash.get(uuid1);
		queriesForUUID2 = (QueryList)queriesHash.get(uuid2);	
		assertEquals(queriesForUUID1.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col27` AS `PRESSURE`, `col28` AS `TEMPERATURE`, `col29` AS `SPEED` FROM dbase.table5544 WHERE ((`col27` > 100) AND (`col28` > 50 AND `col28` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `timestamp`");
		assertEquals(queriesForUUID2.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col41` AS `PRESSURE`, `col42` AS `TEMPERATURE`, `col43` AS `SPEED` FROM dbase.table4444 WHERE ((`col41` > 100) AND (`col42` > 50 AND `col42` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `timestamp`");						
		assertEquals(queriesForUUID1.getConfig().toString(), "{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"myserver\"}");
		assertEquals(queriesForUUID2.getConfig().toString(), "{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"myserver\"}");

		// with flags
		QueryFlags flags = new QueryFlags();
		flags.set(RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_OMIT_ALIASES);
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson, flags);	
		assertEquals(((QueryList)(generator.getQueries().get(uuid1))).getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `ts_time_utc`, `col27` AS `col27`, `col28` AS `col28`, `col29` AS `col29` FROM dbase.table5544 WHERE ((`col27` > 100) AND (`col28` > 50 AND `col28` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `ts_time_utc`");
		flags = new QueryFlags();
		flags.set(RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_RAW_TIMESTAMP);
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson, flags);	
		assertEquals(((QueryList)(generator.getQueries().get(uuid1))).getQueries().get(0).getQuery(), "SELECT `ts_time_utc` AS `timestamp`, `col27` AS `PRESSURE`, `col28` AS `TEMPERATURE`, `col29` AS `SPEED` FROM dbase.table5544 WHERE ((`col27` > 100) AND (`col28` > 50 AND `col28` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `timestamp`");
		flags = new QueryFlags();
		flags.set(RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_OMIT_ALIASES);
		flags.set(RDBTimeCoherentTimeSeriesQueryGenerator.FLAG_RDB_QUERYGEN_RAW_TIMESTAMP);
		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson, flags);	
		assertEquals(((QueryList)(generator.getQueries().get(uuid1))).getQueries().get(0).getQuery(), "SELECT `ts_time_utc` AS `ts_time_utc`, `col27` AS `col27`, `col28` AS `col28`, `col29` AS `col29` FROM dbase.table5544 WHERE ((`col27` > 100) AND (`col28` > 50 AND `col28` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `ts_time_utc`");

		
		
		//------------ Athena -----------

		// create a table
		rows = new ArrayList<ArrayList<String>>();
		rows.add(toArrayList(uuid1.toString() + ",athena,dbase,table5544,ts_time_utc,PRESSURE,col27"));
		rows.add(toArrayList(uuid1.toString() + ",athena,dbase,table5544,ts_time_utc,TEMPERATURE,col28"));
		rows.add(toArrayList(uuid1.toString() + ",athena,dbase,table5544,ts_time_utc,SPEED,col29"));
		rows.add(toArrayList(uuid2.toString() + ",athena,dbase,table4444,ts_time_utc,PRESSURE,col41"));
		rows.add(toArrayList(uuid2.toString() + ",athena,dbase,table4444,ts_time_utc,TEMPERATURE,col42"));
		rows.add(toArrayList(uuid2.toString() + ",athena,dbase,table4444,ts_time_utc,SPEED,col43"));					
		locationAndValueInfo = new Table(RDBTimeCoherentTimeSeriesQueryGenerator.REQUIRED_COLS, COL_TYPES_7, rows);

		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(athenaQueryFragmentBuilder, locationAndValueInfo, getSampleConstraintJson());
		assertEquals(generator.getConstraints().size(),2);
		assertEquals(generator.getConstraints().get(0).getVariableName(),"PRESSURE");
		assertEquals(generator.getConstraints().get(1).getVariableName(),"TEMPERATURE");
		assertEquals(generator.getTimeConstraint().getVariableName(),"_Time_");
		queriesHash = generator.getQueries();  // each Object is an QueryList
		assertEquals(queriesHash.size(),2);			
		queriesForUUID1 = (QueryList)queriesHash.get(uuid1);
		queriesForUUID2 = (QueryList)queriesHash.get(uuid2);		
		assertEquals(queriesForUUID1.getQueries().get(0).getQuery(), "SELECT from_unixtime(\"ts_time_utc\") AS \"timestamp\", \"col27\" AS \"PRESSURE\", \"col28\" AS \"TEMPERATURE\", \"col29\" AS \"SPEED\" FROM dbase.table5544 WHERE ((\"col27\" > 100) AND (\"col28\" > 50 AND \"col28\" < 60)) AND ((from_unixtime(\"ts_time_utc\") >= timestamp '2016-04-07 02:00:00') AND (from_unixtime(\"ts_time_utc\") <= timestamp '2016-04-09 04:00:00')) ORDER BY \"timestamp\""); 	// query structure confirmed on Athena
		assertEquals(queriesForUUID2.getQueries().get(0).getQuery(), "SELECT from_unixtime(\"ts_time_utc\") AS \"timestamp\", \"col41\" AS \"PRESSURE\", \"col42\" AS \"TEMPERATURE\", \"col43\" AS \"SPEED\" FROM dbase.table4444 WHERE ((\"col41\" > 100) AND (\"col42\" > 50 AND \"col42\" < 60)) AND ((from_unixtime(\"ts_time_utc\") >= timestamp '2016-04-07 02:00:00') AND (from_unixtime(\"ts_time_utc\") <= timestamp '2016-04-09 04:00:00')) ORDER BY \"timestamp\"");		// query structure confirmed on Athena				
		assertEquals(queriesForUUID1.getConfig().toString(), "{\"database\":\"dbase\"}");  // no host and port
		assertEquals(queriesForUUID2.getConfig().toString(), "{\"database\":\"dbase\"}");	// no host and port
		
	}


	@Test
	public void test_Binning() throws Exception {

		// create a table
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();

		UUID uuid1 = UUID.randomUUID();
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver08:10000,dbase,table5544,ts_time_utc,PRESSURE,col31"));
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver08:10000,dbase,table5544,ts_time_utc,TEMPERATURE,col32"));
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver08:10000,dbase,table5544,ts_time_utc,SPEED,col33"));
		UUID uuid2 = UUID.randomUUID();
		rows.add(toArrayList(uuid2.toString() + ",jdbc:hive2://myserver08:10000,dbase,table4444,ts_time_utc,PRESSURE,col41"));
		rows.add(toArrayList(uuid2.toString() + ",jdbc:hive2://myserver08:10000,dbase,table4444,ts_time_utc,TEMPERATURE,col42"));
		rows.add(toArrayList(uuid2.toString() + ",jdbc:hive2://myserver08:10000,dbase,table4444,ts_time_utc,SPEED,col43"));	
		UUID uuid3 = UUID.randomUUID();
		rows.add(toArrayList(uuid3.toString() + ",jdbc:hive2://myserver10:10000,dbase,table4444,ts_time_utc,PRESSURE,col41"));
		rows.add(toArrayList(uuid3.toString() + ",jdbc:hive2://myserver10:10000,dbase,table4444,ts_time_utc,TEMPERATURE,col42"));
		rows.add(toArrayList(uuid3.toString() + ",jdbc:hive2://myserver10:10000,dbase,table4444,ts_time_utc,SPEED,col43"));
		UUID uuid4 = UUID.randomUUID();
		rows.add(toArrayList(uuid4.toString() + ",jdbc:hive2://myserver15:10000,dbase5,table4444,ts_time_utc,PRESSURE,col41"));
		rows.add(toArrayList(uuid4.toString() + ",jdbc:hive2://myserver15:10000,dbase5,table4444,ts_time_utc,TEMPERATURE,col42"));
		rows.add(toArrayList(uuid4.toString() + ",jdbc:hive2://myserver15:10000,dbase5,table4444,ts_time_utc,SPEED,col43"));						
		rows.add(toArrayList(uuid4.toString() + ",jdbc:hive2://myserver15:10000,dbase5,table8888,ts_time_utc,PRESSURE,col41"));
		rows.add(toArrayList(uuid4.toString() + ",jdbc:hive2://myserver15:10000,dbase5,table8888,ts_time_utc,TEMPERATURE,col42"));
		rows.add(toArrayList(uuid4.toString() + ",jdbc:hive2://myserver15:10000,dbase5,table8888,ts_time_utc,SPEED,col43"));			

		Table locationAndValueInfo = new Table(RDBTimeCoherentTimeSeriesQueryGenerator.REQUIRED_COLS, COL_TYPES_7, rows);
		JSONObject constraintJson = getSampleConstraintJson();

		generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson);

		// test that constraints are getting properly parsed
		assertEquals(generator.getConstraints().size(),2);
		assertEquals(generator.getConstraints().get(0).getVariableName(),"PRESSURE");
		assertEquals(generator.getConstraints().get(1).getVariableName(),"TEMPERATURE");
		assertEquals(generator.getTimeConstraint().getVariableName(),"_Time_");

		queriesHash = generator.getQueries();  // each Object is an QueryList

		assertEquals(queriesHash.size(),4);

		QueryList queriesForUUID1 = (QueryList)queriesHash.get(uuid1);
		QueryList queriesForUUID2 = (QueryList)queriesHash.get(uuid2);
		QueryList queriesForUUID3 = (QueryList)queriesHash.get(uuid3);
		QueryList queriesForUUID4 = (QueryList)queriesHash.get(uuid4);		
		assertEquals(queriesForUUID1.getQueries().size(),1);
		assertEquals(queriesForUUID2.getQueries().size(),1);
		assertEquals(queriesForUUID3.getQueries().size(),1);
		assertEquals(queriesForUUID4.getQueries().size(),2);  // this one has 2 queries
		assertEquals(queriesForUUID1.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col31` AS `PRESSURE`, `col32` AS `TEMPERATURE`, `col33` AS `SPEED` FROM dbase.table5544 WHERE ((`col31` > 100) AND (`col32` > 50 AND `col32` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `timestamp`");
		assertEquals(queriesForUUID2.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col41` AS `PRESSURE`, `col42` AS `TEMPERATURE`, `col43` AS `SPEED` FROM dbase.table4444 WHERE ((`col41` > 100) AND (`col42` > 50 AND `col42` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `timestamp`");			
		assertEquals(queriesForUUID3.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col41` AS `PRESSURE`, `col42` AS `TEMPERATURE`, `col43` AS `SPEED` FROM dbase.table4444 WHERE ((`col41` > 100) AND (`col42` > 50 AND `col42` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `timestamp`");			
		// TODO the below 2 should be order-agnostic
		assertEquals(queriesForUUID4.getQueries().get(0).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col41` AS `PRESSURE`, `col42` AS `TEMPERATURE`, `col43` AS `SPEED` FROM dbase5.table8888 WHERE ((`col41` > 100) AND (`col42` > 50 AND `col42` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `timestamp`");			
		assertEquals(queriesForUUID4.getQueries().get(1).getQuery(), "SELECT cast(`ts_time_utc` AS timestamp) AS `timestamp`, `col41` AS `PRESSURE`, `col42` AS `TEMPERATURE`, `col43` AS `SPEED` FROM dbase5.table4444 WHERE ((`col41` > 100) AND (`col42` > 50 AND `col42` < 60)) AND " + IntegrationTestUtility.getSampleTimeSeriesConstraintQueryFragment_Hive() + " ORDER BY `timestamp`");			

		// check configs
		assertEquals(queriesForUUID1.getConfig().toString(), "{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"myserver08\"}");
		assertEquals(queriesForUUID2.getConfig().toString(), "{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"myserver08\"}");
		assertEquals(queriesForUUID3.getConfig().toString(), "{\"database\":\"dbase\",\"port\":\"10000\",\"host\":\"myserver10\"}");
		assertEquals(queriesForUUID4.getConfig().toString(), "{\"database\":\"dbase5\",\"port\":\"10000\",\"host\":\"myserver15\"}");

	}
	
	/**
	 * Test that if we send a UUID with non-unique configuration info, that it gets rejected.
	 */
	@Test
	public void test_UUIDWithVaryingConfigInputs() {
		boolean exceptionThrown = false;
		try{

			// create a table
			ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();			
			UUID uuid = UUID.randomUUID();
			rows.add(toArrayList(uuid.toString() + ",jdbc:hive2://myserver10:10000,dbase2,table4444,ts_time_utc,PRESSURE,col41"));						
			rows.add(toArrayList(uuid.toString() + ",jdbc:hive2://myserver15:10000,dbase5,table4444,ts_time_utc,PRESSURE,col41"));			

			Table locationAndValueInfo = new Table(RDBTimeCoherentTimeSeriesQueryGenerator.REQUIRED_COLS, COL_TYPES_7, rows);
			generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, null);

			// this should fail because we can't generate a single config object for this UUID (e.g. different Hive servers)
			generator.getQueries();  

			fail(); // should not get here

		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot produce configuration JSON for input with varying rows"));
		}
		assertTrue(exceptionThrown);
	}

	@Test
	public void test_failWithBadConstraints() throws Exception {

		// create a table
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		UUID uuid1 = UUID.randomUUID();
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,datetime_utc,PRESSURE,col31"));
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,datetime_utc,TEMPERATURE,col32"));
		rows.add(toArrayList(uuid1.toString() + ",jdbc:hive2://myserver:10000,dbase,table5544,datetime_utc,SPEED,col33"));
		Table locationAndValueInfo = new Table(RDBTimeCoherentTimeSeriesQueryGenerator.REQUIRED_COLS, COL_TYPES_7, rows);

		boolean thrown;
		String constraintJsonStr;
		JSONObject constraintJson;

		// fail when find @xxxconstraintSet
		thrown = false;
		try{				
			constraintJsonStr = "{\"@xxxxconstraintSet\":{\"@op\":\"AND\",\"@constraints\":[{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"100\",\"@type\":\"number\"}},{\"@var\":\"TEMPERATURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"60\",\"@type\":\"number\"}}]},\"@timeConstraint\":" + IntegrationTestUtility.getSampleTimeSeriesConstraintJsonStr() + "}";
			constraintJson =  Utility.getJsonObjectFromString(constraintJsonStr);
			generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson);			
			generator.getQueries();
		}catch(Exception e){
			assertTrue(e.getMessage().contains("Constraint JSON may be malformed"));
			thrown = true;
		}
		assertTrue(thrown);

		// fail when no @op in @constraints
		thrown = false;
		try{				
			constraintJsonStr = "{\"@constraintSet\":{\"@constraints\":[{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"100\",\"@type\":\"number\"}},{\"@var\":\"TEMPERATURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"60\",\"@type\":\"number\"}}]},\"@timeConstraint\":{" + IntegrationTestUtility.getSampleTimeSeriesConstraintJsonStr() + "}";
			constraintJson =  Utility.getJsonObjectFromString(constraintJsonStr);
			generator = new RDBTimeCoherentTimeSeriesQueryGenerator(hiveQueryFragmentBuilder, locationAndValueInfo, constraintJson);			
			generator.getQueries();
		}catch(Exception e){
			assertTrue(e.getMessage().contains("Error parsing JSONObject from string"));
			thrown = true;
		}
		assertTrue(thrown);
	}

	// TODO ADD ATHENA EXAMPLE

	
	/**
	 * Returns an ArrayList of strings from a given string, using comma as a delimiter.
	 */
	private static ArrayList<String> toArrayList(String s){
		return new ArrayList<String>(Arrays.asList(s.split(",")));
	}

}
