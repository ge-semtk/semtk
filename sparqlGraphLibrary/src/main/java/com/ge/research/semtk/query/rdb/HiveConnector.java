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

import com.ge.research.semtk.query.rdb.JdbcConnector;


/**
 * Hive connector
 */
public class HiveConnector extends JdbcConnector {

	private static String HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";	
	private static String HIVE_URL_PREFIX = "jdbc:hive2://";			
	private static String HIVE_TEST_QUERY = "show tables";
	
	/**
	 * Instantiate the connector.
	 * @throws Exception 
	 */
	public HiveConnector(String host, int port, String database, String username, String password) throws Exception{
		setDriver(HIVE_DRIVER);
		setDatabaseUrl(getDatabaseURL(host, port, database));
		setConnectionProperty(PROPERTY_KEY_USERNAME, username);
		setConnectionProperty(PROPERTY_KEY_PASSWORD, password);
		validate();
		testConnection(HIVE_TEST_QUERY);
	}

	/**
	 * Get the Hive driver.
	 */
	public static String getDriver(){
		return HIVE_DRIVER;
	}
	
	/**
	 * Construct an Hive database URL.
	 */
	public static String getDatabaseURL(String host, int port, String database){
		return HIVE_URL_PREFIX + host + ":" + port + "/" + database;
	}		

	/**
	 * Check for required connection information
	 */
	protected void validate() throws Exception{	
		super.validate();
		validateProperty(PROPERTY_KEY_USERNAME);
	}
	
}
