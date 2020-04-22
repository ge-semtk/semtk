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

package com.ge.research.semtk.query.rdb.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.query.rdb.SqlServerConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;

public class SqlServerConnectorTest_IT {
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();	
	}
	
	@Test
	public void testGetDatabaseURL() {
		assertEquals(SqlServerConnector.getDatabaseURL("1.2.3.4", 5000), "jdbc:sqlserver://1.2.3.4:5000");
	}
	
//	@Test
//	public void test() throws Exception{
//		SqlServerConnector conn = new SqlServerConnector("server.com", 1433, "database", "user", "pw"); // replace values to test
//		Table results = conn.query(SqlServerConnector.SQLSERVER_TEST_QUERY);
//		assert(Arrays.asList(results.getColumnUniqueValues("name")).contains("master"));
//		assert(Arrays.asList(results.getColumnUniqueValues("name")).contains("tempdb"));
//	}	
	
}
