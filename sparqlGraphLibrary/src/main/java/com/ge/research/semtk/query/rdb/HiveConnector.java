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


package com.ge.research.semtk.query.rdb;

import com.ge.research.semtk.query.rdb.Connector;


/**
 * Hive connector
 * 
 * If you get a ConnectException, start the Thrift server: on vesuvius05, run "hive --service hiveserver"
 */
public class HiveConnector extends Connector {

	private static String HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";	// TODO put in properties file?
	private static String HIVE_URL_PREFIX = "jdbc:hive2://";				// TODO put in properties file?
	private static String HIVE_TEST_QUERY = "show tables";
	
	/**
	 * Instantiate the connector.
	 * @throws Exception 
	 */
	public HiveConnector(String host, int port, String database, String username, String password) throws Exception{
		this.driver = HIVE_DRIVER;
		this.dbUrl = getDatabaseURL(host, port, database); // TODO have ODBCDataset use this too
		this.username = username;
		this.password = password;
		this.connectionTestQuery = HIVE_TEST_QUERY;
		testConnection();	
	}

	/**
	 * Get the Hive driver.
	 */
	public static String getDriver(){
		return HIVE_DRIVER;
	}
	
	/**
	 * Get the Hive database URL prefix
	 */
	public static String getDatabaseURLPrefix(){
		return HIVE_URL_PREFIX;
	}
	
	/**
	 * Construct an Hive database URL.
	 */
	public static String getDatabaseURL(String host, int port, String database){
		return HIVE_URL_PREFIX + host + ":" + port + "/" + database;
	}		

}
