/**
 ** Copyright 2019 General Electric Company
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

import java.util.ArrayList;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.entity.BaseDocument;
import com.ge.research.semtk.resultSet.Table;

/**
 * ArangoDB connector
 */
public class ArangoDbConnector extends Connector {
	
	private final String HOST;  										
	private final int PORT;
	private final String DATABASE;
	private final String USER;
	private final String PASSWORD;
	private final ArangoDB arangoDB;
	
	/**
	 * Constructor
	 */
	public ArangoDbConnector(String host, int port, String database, String user, String password){
		this.HOST = host;
		this.PORT = port;
		this.DATABASE = database;
		this.USER = user;
		this.PASSWORD = password;

		arangoDB = new ArangoDB.Builder().host(HOST, PORT).user(USER).password(PASSWORD).build();
	}

	/**
	 * Execute an AQL query
	 */
	@Override
	public Table query(String query) throws Exception {

		Table table = null;
		ArangoCursor<BaseDocument> cursor = null;

		try{
			cursor = arangoDB.db(DATABASE).query(query, null, null, BaseDocument.class);
			for(BaseDocument doc : cursor.asListRemaining()){

				// instantiate the table
				if(table == null){										
					String[] colNames = doc.getProperties().keySet().toArray(new String[0]); 
					String[] colTypes = new String[colNames.length];	
					for (int i = 0; i < colNames.length; ++i) {
						colTypes[i] = "String";		// I don't think we have access to the real data types
					}
					table = new Table(colNames, colTypes);			
				}

				// each document must have the same properties - if not then error
				if(doc.getProperties().keySet().size() != table.getNumColumns()){
					throw new Exception("Documents do not have the same set of properties, cannot create Table"); // guessing this cannot happen (mismatches seem to come back with value=null), but just in case
				}	

				// add table row
				ArrayList<String> row = new ArrayList<String>();
				for(String colName : table.getColumnNames()){
					Object value = doc.getProperties().get(colName);
					if(value != null){
						row.add(value.toString());		
					}else{
						row.add("null");  // represent this as a string "null" in the table, to closely match the AQL results (e.g. properties={a=Foo9, c=null} )
					}
				}
				table.addRow(row);
			}
		}catch(Exception e){
			throw e;
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}
		return table;
	}

}
