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

import com.ge.research.semtk.resultSet.Table;

/**
 * A connector to access an RDB database (e.g. Oracle, Hive)
 */
public abstract class Connector {
	
	protected String driver;  	// the database driver class
	protected String dbUrl;		// the database url, e.g. prefix@//host:port/sid
	protected String username;
	protected String password;
	protected String connectionTestQuery; // a query to test the connection
	
	
	/**
	 * Validate the connection
	 * @throw Exception if fails
	 */
	public void testConnection() throws Exception{
		
		// validate connection parameters
		if(driver == null || driver.trim().isEmpty()){
			throw new Exception("Must specify a driver");
		}
		if(dbUrl == null || dbUrl.trim().isEmpty()){
			throw new Exception("Must specify a connection string");
		}
		if(username == null || username.trim().isEmpty()){
			throw new Exception("Must specify a username");
		}
//		if(password == null || password.trim().isEmpty()){    // leaving this commented out because sometimes password is blank
//			throw new Exception("Must specify a password");
//		}
		if(connectionTestQuery == null || connectionTestQuery.trim().isEmpty()){
			throw new Exception("Cannot test connection - no connection test query");
		}
		
		Connection conn = null;
		Statement stmt = null;
		try{
			// open connection and execute the test query
			Class.forName(driver);
			conn = DriverManager.getConnection(dbUrl, username, password); 
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(connectionTestQuery);
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
	 * Runs a query and retrieves results 
	 * @throws Exception 
	 */
	public Table query(String query) throws Exception {

		// validate query
		query = query.trim();
		if(query == null || query.trim().isEmpty()){
			throw new Exception("Must specify a query");
		}
		
		// limit to SELECT queries for now
		if((!query.toLowerCase().startsWith("select"))
		&&(!query.toLowerCase().startsWith("set"))){
			throw new Exception("Only SELECT or SET queries are currently supported");
		}

		ArrayList<ArrayList<String>> recs = new ArrayList<ArrayList<String>>();
		ArrayList<String> tmp;

		Connection conn = null;
		Statement stmt = null;
		try{
			// register JDBC driver, open connection
			Class.forName(driver);
			conn = DriverManager.getConnection(dbUrl, username, password); 
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
					//System.out.println("put " + rsmeta.getColumnLabel(i+1).toLowerCase() + ", " + rs.getString(i+1));
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
			//e.printStackTrace();
			throw e;
		}finally{
			if(stmt!=null){	stmt.close(); }
			if(conn!=null){ conn.close(); }
		}

	}		
	
}
