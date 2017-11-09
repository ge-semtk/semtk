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


package com.ge.research.semtk.load.dataset;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.utility.LocalLogger;


/*
 * Read data via an ODBC connection.
 * Must call close() when done.
 */
public class ODBCDataset extends Dataset{

	private Connection conn;	// the database connection
	private Statement stmt;		// the database statement
	private ResultSet rs;		// the result set
	private ArrayList<String> columnNamesInOrder; // column names returned by query, in order


	/**
	 * Instantiate a dataset from a database.
	 * @param driver the driver class
	 * @param url the database URL
	 * @param username the username
	 * @param password the password
	 * @param query the query
	 * @throws Exception
	 */
	public ODBCDataset(String driver, String url, String username, String password, String query) throws Exception{
		LocalLogger.logToStdOut("Database: " + url);
		LocalLogger.logToStdOut("Query: " + query);
		initialize(driver, url, username, password, query);
	}


	/**
	 * Called when instantiating the object using JSON.
	 */
	protected void fromJSON(JSONObject config) throws Exception{
		// TODO read the 5 inputs from JSON, and then call initialize()
		throw new Exception("Method not implemented yet");
	}


	/**
	 * Initialize.
	 * @param driver the driver class
	 * @param url the database URL
	 * @param username the username
	 * @param password the password
	 * @param query the query
	 * @throws Exception 
	 */
	private void initialize(String driver, String url, String username, String password, String query) throws Exception{
		// validate inputs
		if(driver == null || driver.trim().isEmpty()){
			throw new Exception("Must specify a driver");
		}
		if(url == null || url.trim().isEmpty()){
			throw new Exception("Must specify a connection string");
		}
		if(username == null || username.trim().isEmpty()){
			throw new Exception("Must specify a username");
		}
		if(password == null || password.trim().isEmpty()){
			throw new Exception("Must specify a password");
		}
		query = query.trim();
		if(query == null || query.trim().isEmpty()){
			throw new Exception("Must specify a query");
		}
		if(!query.toLowerCase().startsWith("select ")){
			throw new Exception("Must use a SELECT query");
		}
		if(query.toLowerCase().startsWith("select *")){
			throw new Exception("Queries with * are not supported"); // for now require user to explicitly name columns, to prevent huge returns
		}

		// get the result set to iterate through when records are requested
		try{			
			// get a database connection and execute the query
			Class.forName(driver);
			conn = DriverManager.getConnection(url, username, password);							
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);  // enable beforeFirst()
			// execute the query
			rs = stmt.executeQuery(query);
		}catch(Exception e){
			throw e;
		}

		// get the column names returned by the query (by retrieving one row of data)
		setColumnNamesInOrder(query);
	}


	@Override
	/*
	 * Read the next set of records from the database
	 * Returns null if all records have been read.
	 */
	public ArrayList<ArrayList<String>> getNextRecords(int numRecords) throws Exception {

		// if the connection has been closed, return null
		if(conn.isClosed()){
			return null;
		}

		ArrayList<ArrayList<String>> recs = new ArrayList<ArrayList<String>>();
		ArrayList<String> tmp;

		for(int i = 0; i < numRecords; i++){
			if(rs.next()){
				//LocalLogger.logToStdOut(rs.getString(1) + "..." + rs.getString(2)+ "..." + rs.getString(3)+ "..." + rs.getString(4));
				tmp = new ArrayList<String>();
				for(int j = 1; j <= rs.getMetaData().getColumnCount(); j++){ 
					tmp.add(rs.getString(j));
				}
				recs.add(tmp);
			}else{ 	  	    	  
				break;
			}
		}

		return recs;
	}


	/**
	 * Get the column names in order
	 * @return an arraylist of column names
	 * @throws Exception 
	 */
	@Override
	public ArrayList<String> getColumnNamesinOrder() throws Exception{
		return columnNamesInOrder;
	}

	/**
	 * Reset the dataset
	 */
	@Override
	public void reset() throws Exception {
		rs.beforeFirst();
	}		
	
	
	/**
	 * Close the dataset
	 * @throws Exception  
	 */
	@Override
	public void close() throws Exception {
		if(rs != null){ rs.close(); }
		if(stmt != null){ stmt.close(); }
		if(conn != null){ conn.close(); LocalLogger.logToStdOut("CLOSING ODBC CONNECTION"); }	 
	}

	/**
	 * Retrieve the column names by executing the query to retrieve one row.
	 * @throws Exception
	 */
	private void setColumnNamesInOrder(String query) throws Exception{

		columnNamesInOrder = new ArrayList<String>();
		Statement stmt2 = null;
		ResultSet rs2 = null;
		ResultSetMetaData rsMetadata = null;
		try{
			stmt2 = conn.createStatement();
			String queryModified = query.toLowerCase();
			if(queryModified.indexOf("order by") > -1){
				// strip off "order by" before appending "where rownum = 1"
				queryModified = queryModified.substring(0,queryModified.indexOf("order by")); 
			}
			queryModified += " where rownum = 1";  // only need one result to get the column names
			rs2 = stmt2.executeQuery(query);
			// use metadata to get column names
			rsMetadata = rs2.getMetaData();
			int numCols = rsMetadata.getColumnCount();
			for(int i = 1; i <= numCols; i++){
				columnNamesInOrder.add(rsMetadata.getColumnName(i).toLowerCase());
			}
		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			if(rs2 != null){ rs2.close(); }
			if(stmt2 != null){ stmt2.close(); }		
			if(conn != null){ conn.close(); LocalLogger.logToStdOut("CLOSING ODBC CONNECTION"); }		
			throw new Exception("Cannot retrieve column names: " + e.getMessage());
		}finally{
			if(rs2 != null){ rs2.close(); }
			if(stmt2 != null){ stmt2.close(); }
		}
	}

}
