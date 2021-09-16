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


package com.ge.research.semtk.query.rdb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.query.rdb.HiveConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import static org.junit.Assume.*;

public class HiveConnectorTest_IT {

	private static String HOST;
	private static int PORT;
	private static String DATABASE;
	private static String USER;  
	private static String PASSWORD;	
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();	
		HOST = IntegrationTestUtility.get("hive.server");
		PORT = IntegrationTestUtility.getInt("hive.port");
		DATABASE = IntegrationTestUtility.get("hive.database");
		USER = IntegrationTestUtility.get("hive.username");
		PASSWORD = IntegrationTestUtility.get("hive.password");
		
		// if needed variables are not configured, then skip test(s)
		Assume.assumeTrue("No Hive server configured in environment", HOST != null && !HOST.trim().isEmpty());
		
		try {
			
			new HiveConnector(HOST, PORT, DATABASE, USER, PASSWORD);
		} catch ( SQLException e ) {
			e.printStackTrace();
			assumeFalse("Can not connect to hive server for testing", e.getMessage().contains("Connection reset"));
		}
	}
	
	@Test
	public void testGetDatabaseURL() {
		assertEquals(HiveConnector.getDatabaseURL("server", 10000, "database"), "jdbc:hive2://server:10000/database");
	}

	
	/**
	 * Test that we get a meaningful message when the user is null or empty
	 */
	@Test
	public void testMissingUser() {
		
		// null user
		boolean thrown = false;
		try {
			new HiveConnector("nonexistenthost", 152, "nonexistentdatabase", null, "pw"); // null user
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().indexOf("user") != -1);	// confirm useful error message
		}
		assertTrue(thrown);	
		
		// empty user
		thrown = false;
		try {
			new HiveConnector("nonexistenthost", 152, "nonexistentdatabase", " ", "pw"); // empty user
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().indexOf("user") != -1);	// confirm useful error message
		}
		assertTrue(thrown);	
	}
	
	@Test
	public void testBadConnection() {
		boolean thrown = false;
		try {
			new HiveConnector("nonexistenthost", 152, "nonexistentdatabase", "user", "pw");
		} catch (Exception e) {
			thrown = true;
		}
		assertTrue(thrown);	
	}
	
	
	@Test
	public void testGoodQuery() throws Exception{
		HiveConnector oc = new HiveConnector(HOST, PORT, DATABASE, USER, PASSWORD);
		Table results = oc.query(HiveConnector.HIVE_TEST_QUERY);
		assertEquals(results.getNumColumns(), 1);
		assertTrue(results.getColumnNames()[0].equals("tstamp"));
		assertEquals(results.getRows().size(), 1);
		assertEquals(results.getCell(0, 0).length(), 10);  // a 10-digit timestamp
	}
	
	
	@Test
	public void testBadQuery() {
		
		boolean thrown = false;
		try {
			HiveConnector oc = new HiveConnector(HOST, PORT, DATABASE, USER, PASSWORD);
			Table results = oc.query("give me some data");
		} catch (Exception e) {
			thrown = true;
		}
		assertTrue(thrown);	
	}	
	
}
