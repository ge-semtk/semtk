package com.ge.research.semtk.query.rdb;

import com.ge.research.semtk.resultSet.Table;

/**
 * A connector to a database 
 */
public abstract class Connector {

	protected abstract Table query(String query) throws Exception;
	
}
