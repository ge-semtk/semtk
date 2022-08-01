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
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.client.KairosDBClient;
import com.ge.research.semtk.edcquerygen.client.KairosDBQueryGenClient;
import com.ge.research.semtk.querygen.client.QueryGenClientConfig;
import com.ge.research.semtk.querygen.timeseries.kairosdb.KairosDBQueryGenerator;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utilityge.Utility;

public class KairosDBQueryGenClientTest_IT {

	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	private final String SERVICE_ENDPOINT_KAIROSDB = "edcQueryGeneration/kairosDB/generateQueries";

	private final String[] COL_TYPES = {"String","String","String"};
	private final String KAIROS_CONNECTION_URL_1 = "http://server:8080";

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();		
		SERVICE_PROTOCOL = IntegrationTestUtility.get("protocol");
		SERVICE_SERVER = IntegrationTestUtility.get("edcquerygenservice.server");
		SERVICE_PORT = IntegrationTestUtility.getInt("edcquerygenservice.port");
		
		assumeTrue("No edc query gen service server is configured for JUNIT", ! SERVICE_SERVER.isEmpty());

	}

	@Test
	public void testClient() throws Exception {
		QueryGenClientConfig conf = new QueryGenClientConfig(SERVICE_PROTOCOL,SERVICE_SERVER,SERVICE_PORT,SERVICE_ENDPOINT_KAIROSDB);
		KairosDBQueryGenClient client = new KairosDBQueryGenClient(conf);

		// create a table
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		UUID uuid1 = UUID.randomUUID();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"TAG1")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"TAG2")));
		Table locationAndValueInfoTable = new Table(KairosDBQueryGenerator.REQUIRED_COLS, COL_TYPES, rows);

		// execute
		TableResultSet resultSet = client.execute(locationAndValueInfoTable, null, null);
		Table resultTable = resultSet.getResults();

		// check results
		assertTrue(resultSet.getSuccess());
		assertEquals(resultTable.getNumRows(),1);
		assertEquals(resultTable.getNumColumns(),3);

		// check query - can't rely on internal order
		String queryJsonStr = resultTable.getCell(0, resultTable.getColumnIndex(Utility.COL_NAME_QUERY));
		assertTrue(queryJsonStr.contains("{\"name\":\"TAG1\",\"tags\":{},\"group_by\":[],\"aggregators\":[]"));
		assertTrue(queryJsonStr.contains("{\"name\":\"TAG2\",\"tags\":{},\"group_by\":[],\"aggregators\":[]"));

		// check configJson
		String configJsonStr = resultTable.getCell(0, resultTable.getColumnIndex(Utility.COL_NAME_CONFIGJSON));
		JSONObject configJson = com.ge.research.semtk.utility.Utility.getJsonObjectFromString(configJsonStr);
		assertTrue(configJson.keySet().contains(KairosDBClient.CONFIGJSONKEY_KAIROSDBURL));  // "kairosDBUrl"
		assertTrue(configJson.get(KairosDBClient.CONFIGJSONKEY_KAIROSDBURL).equals(KAIROS_CONNECTION_URL_1));
	}
	
	// TODO add test with constraints

}
