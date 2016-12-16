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
 * Postgres connector
 */
public class PostgresConnector extends Connector {

	private static String POSTGRES_DRIVER = "org.postgresql.Driver";	// assume invariant for now
	private static String POSTGRES_URL_PREFIX = "jdbc:postgresql://";			// assume invariant for now
	private static String POSTGRES_TEST_QUERY = "SELECT * from clock_timestamp();";
	
	// ADD CONSTRUCTOR THAT ALLOWS PASSING IN DRIVER?
	
	/**
	 * Instantiate the connector.
	 * @throws Exception 
	 */
	public PostgresConnector(String host, int port, String database, String username, String password) throws Exception{
		this.driver = POSTGRES_DRIVER;
		this.dbUrl = getDatabaseURL(host, port, database); 
		this.username = username;
		this.password = password;
		this.connectionTestQuery = POSTGRES_TEST_QUERY;
		testConnection();  
	}
	
	/**
	 * Utility method to get the POSTGRES driver.
	 */
	public static String getDriver(){
		return POSTGRES_DRIVER;
	}
	
	/**
	 * Utility method to get the POSTGRES database URL prefix
	 */
	public static String getDatabaseURLPrefix(){
		return POSTGRES_URL_PREFIX;
	}
	
	/**
	 * Utility method to construct an POSTGRES database URL (works for SID or service name)
	 */
	public static String getDatabaseURL(String host, int port, String database){
		return POSTGRES_URL_PREFIX + host + ":" + port + "/" + database;
	}	

}
