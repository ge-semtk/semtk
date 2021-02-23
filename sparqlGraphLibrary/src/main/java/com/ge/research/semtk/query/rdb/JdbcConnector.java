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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import com.ge.research.semtk.propertygraph.SQLPropertyGraphUtils;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * A JDBC connector to access an RDB database (e.g. Hive)
 */
public abstract class JdbcConnector extends Connector {
	
	// keys for connection properties
	protected final static String PROPERTY_KEY_USERNAME = "user";
	protected final static String PROPERTY_KEY_PASSWORD = "password";
	
	// these are the items needed to create a connection
	private String driver;  										// the database driver class
	private int loginTimeoutSec = 30;								// the login timeout, defaulting to 30 sec
	private String dbUrl;											// the database url, e.g. prefix@//host:port/sid
	private Properties connectionProperties = new Properties();  	// connection properties (e.g. username, password)
	
	/**
	 * Set the driver for this connection
	 */
	protected void setDriver(String driver){
		this.driver = driver;
	}
	
	protected void setLoginTimeout(int loginTimeoutSec){
		this.loginTimeoutSec = loginTimeoutSec;
	}
	
	/**
	 * Set the database URL for this connection
	 */
	protected void setDatabaseUrl(String dbUrl){
		this.dbUrl = dbUrl;
	}
	
	/**
	 * Set a property for this connection.  
	 * Common properties are username and password, but different connectors may require other properties.
	 */
	protected void setConnectionProperty(String key, String value){
		if(key != null && value != null){
			this.connectionProperties.setProperty(key, value);		
		}
	}
	
	/**
	 * Get a property that has already been set for this connection.  .
	 */
	protected String getConnectionProperty(String key){
		return this.connectionProperties.getProperty(key);		
	}
	
	/**
	 * Confirm that a property is non-empty for this connection.
	 * @throws Exception if the property is null or empty
	 */
	protected void validateProperty(String key) throws Exception{
		if(getConnectionProperty(key) == null || getConnectionProperty(key).trim().isEmpty()){
			throw new Exception("Connection requires a `" + key + "`");
		}		
	}
	
	/**
	 * Check that required fields are are available.
	 * Extend in subclasses to add validation for required properties.
	 */
	protected void validate() throws Exception{
		if(driver == null || driver.trim().isEmpty()){
			throw new Exception("Connection requires a driver");
		}
		if(dbUrl == null || dbUrl.trim().isEmpty()){
			throw new Exception("Connection requires a connection string");
		}
	}
	
	/**
	 * Get a java.sql.Connection object
	 */
	private Connection getConnection() throws Exception{
		Class.forName(driver);
		DriverManager.setLoginTimeout(loginTimeoutSec);
		LocalLogger.logToStdOut("JdbcConnector.getConnection() login timeout = " + DriverManager.getLoginTimeout() + " sec"); // TODO REMOVE
		return DriverManager.getConnection(dbUrl, connectionProperties);
	}
	
	/**
	 * Test the connection.
	 * @param a simple query for testing the connection, e.g. ("show tables");
	 * @throw Exception if fails
	 */
	public void testConnection(String testQuery) throws Exception{
		
		if(testQuery == null || testQuery.trim().isEmpty()){
			throw new Exception("Connection requires a test query");
		}
	
		Connection conn = null;
		Statement stmt = null;
		try{
			// open connection and execute the test query
			conn = getConnection(); 
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(testQuery);
			rs.getMetaData();

			// clean up
			rs.close();
			stmt.close();
			stmt = null;
			conn.close();
			conn = null;
		}catch(Exception e){
			throw e;
		}finally{
			if(stmt!=null){	stmt.close(); }
			if(conn!=null){ conn.close(); }
		}		
	}
	
	/**
	 * Query for column_name, data_type
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public Table queryColumnInfo(String tableName) throws Exception {
		return this.query(SQLPropertyGraphUtils.genColumnInfoSQL(tableName));
	}
	
	public Table queryAll(String tableName, int limit, int offset) throws Exception {
		return this.query(SQLPropertyGraphUtils.genSelectStarSQL(tableName, limit, offset));
	}
	
	/**
	 * Runs a query and retrieves results 
	 * @throws Exception 
	 */
	public Table query(String query) throws Exception {
		
		// validate query
		query = query.trim();
		if(query == null || query.trim().isEmpty()){
			throw new Exception("Must specify a query");
		}
		
		// limit queries
		if((!query.toLowerCase().startsWith("select"))
		&&(!query.toLowerCase().startsWith("set"))
		&&(!query.toLowerCase().startsWith("call"))){
			throw new Exception("Query is not supported");
		}

		ArrayList<ArrayList<String>> recs = new ArrayList<ArrayList<String>>();
		ArrayList<String> tmp;

		Connection conn = null;
		Statement stmt = null;
		try{
			// register JDBC driver, open connection
			conn = getConnection();
			stmt = conn.createStatement();
			String[] queries = query.split(";");
			ResultSet rs = null;
			ResultSetMetaData rsmeta = null;
			for (int i = 0; i < queries.length; i++) {
				if (queries[i].trim().toLowerCase().startsWith("set")) {
					stmt.execute(queries[i].trim());
				} else {
					rs = stmt.executeQuery(queries[i].trim());
					rsmeta = rs.getMetaData();
				}
			}

			// extract column list
			String[] cols = new String[rsmeta.getColumnCount()];
			String[] colTypes = new String[rsmeta.getColumnCount()];
			for(int i = 0; i < rsmeta.getColumnCount(); i++){ 
				cols[i] = rsmeta.getColumnLabel(i+1).toLowerCase();  // use lower case for column headers
				colTypes[i] = rsmeta.getColumnTypeName(i+1);
			}			
			
			// extract data from result set
			while(rs.next()){
				tmp = new ArrayList<String>();
				for(int i = 0; i < rsmeta.getColumnCount(); i++){ 
					tmp.add(rs.getString(i+1));   // use lower case for column headers
					//LocalLogger.logToStdOut("put " + rsmeta.getColumnLabel(i+1).toLowerCase() + ", " + rs.getString(i+1));
				}
				recs.add(tmp);				
			}

			// clean up
			rs.close();
			stmt.close();
			stmt = null;
			conn.close();
			conn = null;
			
			return new Table(cols, colTypes, recs);

		}catch(Exception e){
			//LocalLogger.printStackTrace(e);
			throw e;
		}finally{
			if(stmt!=null){	stmt.close(); }
			if(conn!=null){ conn.close(); }
		}

	}		
	
}
