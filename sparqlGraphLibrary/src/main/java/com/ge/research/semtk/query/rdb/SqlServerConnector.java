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


package com.ge.research.semtk.query.rdb;

import com.ge.research.semtk.query.rdb.JdbcConnector;

/**
 * SQL Server connector
 */
public class SqlServerConnector extends JdbcConnector {

	private static final String SQLSERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";			
	private static final String SQLSERVER_URL_PREFIX = "jdbc:sqlserver://";			
	public static final String SQLSERVER_TEST_QUERY = "SELECT name FROM master.sys.databases";
	
	/**
	 * Instantiate the connector.
	 */
	public SqlServerConnector(String host, int port, String database, String username, String password) throws Exception{
		setDriver(SQLSERVER_DRIVER);
		setDatabaseUrl(getDatabaseURL(host, port)); 
		setConnectionProperty(PROPERTY_KEY_USERNAME, username);
		setConnectionProperty(PROPERTY_KEY_PASSWORD, password);
		validate();
		testConnection(SQLSERVER_TEST_QUERY);
	}
	
	/**
	 * Utility method to get the SQL Server  driver.
	 */
	public static String getDriver(){
		return SQLSERVER_DRIVER;
	}
	
	/**
	 * Utility method to construct a SQL Server database URL
	 */
	public static String getDatabaseURL(String host, int port){
		return SQLSERVER_URL_PREFIX + host + ":" + port;
	}	

	/**
	 * Check for required connection information
	 */
	protected void validate() throws Exception{	
		super.validate();
		validateProperty(PROPERTY_KEY_USERNAME);
		validateProperty(PROPERTY_KEY_PASSWORD);
	}
	
}
