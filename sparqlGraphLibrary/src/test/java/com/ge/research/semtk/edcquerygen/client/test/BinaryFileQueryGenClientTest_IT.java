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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edcquerygen.client.BinaryFileQueryGenClient;
import com.ge.research.semtk.querygen.client.QueryGenClientConfig;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utilityge.Utility;

public class BinaryFileQueryGenClientTest_IT {

	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	private static final String SERVICE_ENDPOINT = "edcQueryGeneration/binaryFile/generateQueries";

	private final String[] COLS = {Utility.COL_NAME_UUID, Utility.COL_NAME_URL, Utility.COL_NAME_FILENAME}; // TODO deduplicate - also appears in the RestController
	private final String[] COL_TYPES = {"String","String","String"};
	private final String[] COLS_TO_CHECK = {Utility.COL_NAME_QUERY, Utility.COL_NAME_CONFIGJSON};

	private static QueryGenClientConfig conf;
	private static BinaryFileQueryGenClient client;
	ArrayList<ArrayList<String>> rows;
	private Table locationAndValueInfoTable;
	private TableResultSet resultSet;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();	
		SERVICE_PROTOCOL = IntegrationTestUtility.get("protocol");
		SERVICE_SERVER = IntegrationTestUtility.get("edcquerygenservice.server");
		SERVICE_PORT = IntegrationTestUtility.getInt("edcquerygenservice.port");

		conf = new QueryGenClientConfig(SERVICE_PROTOCOL,SERVICE_SERVER,SERVICE_PORT,SERVICE_ENDPOINT);
		client = new BinaryFileQueryGenClient(conf);
	}

	@Test
	public void testClient() throws Exception {

		// create a table
		rows = new ArrayList<ArrayList<String>>();
		UUID uuid1 = UUID.randomUUID();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"hdfs://test1-98231834.img","test1.img")));
		UUID uuid2 = UUID.randomUUID();
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),"hdfs://test2-13523653.img","test2.img")));			
		locationAndValueInfoTable = new Table(COLS, COL_TYPES, rows);

		// execute
		resultSet = client.execute(locationAndValueInfoTable, null, null); 
		Table resultTable = resultSet.getResults();

		// check results
		assertTrue(resultSet.getSuccess());
		assertEquals(resultTable.getNumRows(),2);
		assertEquals(resultTable.getNumColumns(),3);
		Table queriesForUUID1 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid1.toString(), COLS_TO_CHECK);			
		assertEquals(queriesForUUID1.getNumRows(),1);
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_QUERY)), "hdfs://test1-98231834.img###test1.img");
		assertEquals(queriesForUUID1.getRow(0).get(queriesForUUID1.getColumnIndex(Utility.COL_NAME_CONFIGJSON)),"{}");
		Table queriesForUUID2 = resultTable.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid2.toString(), COLS_TO_CHECK);
		assertEquals(queriesForUUID2.getNumRows(),1);
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_QUERY)),"hdfs://test2-13523653.img###test2.img");
		assertEquals(queriesForUUID2.getRow(0).get(queriesForUUID2.getColumnIndex(Utility.COL_NAME_CONFIGJSON)),"{}");
	}

	@Test
	public void testBadInput() throws Exception {
		
		// test url null
		rows = new ArrayList<ArrayList<String>>();
		UUID uuid1 = UUID.randomUUID();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),null,"test1.img")));			
		locationAndValueInfoTable = new Table(COLS, COL_TYPES, rows);
		resultSet = client.execute(locationAndValueInfoTable, null, null); 
		assertFalse(resultSet.getSuccess());
		assertTrue(resultSet.getRationaleAsString("").contains("URL 'null' is null, empty, or contains ###"));
		
		// test filename null
		rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"hdfs://test1-98231834.img",null)));			
		locationAndValueInfoTable = new Table(COLS, COL_TYPES, rows);
		resultSet = client.execute(locationAndValueInfoTable, null, null); 
		assertFalse(resultSet.getSuccess());
		assertTrue(resultSet.getRationaleAsString("").contains("File name 'null' is null, empty, or contains ###"));
		
		// test url empty
		rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString()," ","test1.img")));			
		locationAndValueInfoTable = new Table(COLS, COL_TYPES, rows);
		resultSet = client.execute(locationAndValueInfoTable, null, null); 
		assertFalse(resultSet.getSuccess());
		assertTrue(resultSet.getRationaleAsString("").contains("is null, empty, or contains ###"));
		
		// test filename empty
		rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"hdfs://test1-98231834.img","")));			
		locationAndValueInfoTable = new Table(COLS, COL_TYPES, rows);
		resultSet = client.execute(locationAndValueInfoTable, null, null); 
		assertFalse(resultSet.getSuccess());
		assertTrue(resultSet.getRationaleAsString("").contains("is null, empty, or contains ###"));
		
		// test if filename contains delimiter
		rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),"hdfs://test1-98231834.img","filename###.jpg")));			
		locationAndValueInfoTable = new Table(COLS, COL_TYPES, rows);
		resultSet = client.execute(locationAndValueInfoTable, null, null); 
		assertFalse(resultSet.getSuccess());
		assertTrue(resultSet.getRationaleAsString("").contains("is null, empty, or contains ###"));
	
	}


}
