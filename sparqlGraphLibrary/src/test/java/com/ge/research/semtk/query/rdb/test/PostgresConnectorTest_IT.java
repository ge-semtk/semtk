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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.query.rdb.PostgresConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;

public class PostgresConnectorTest_IT {
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();	
	}
	
	@Test
	public void testGetDatabaseURL() {
		assertEquals(PostgresConnector.getDatabaseURL("1.2.3.4", 5000, "db"), "jdbc:postgresql://1.2.3.4:5000/db");
	}

//	@Test
//	public void testQueryResults() throws Exception {			
//		String query = "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'";			
//		PostgresConnector oc = new PostgresConnector("server", 5532, "postgres", "database", "password");
//		Table results = oc.query(query);
//		System.out.println(results.toCSVString());
//	}	
	
}
