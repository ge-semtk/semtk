package com.ge.research.semtk.edc.services.hive;

import com.ge.research.semtk.edc.ServiceProperties;
import com.ge.research.semtk.query.rdb.HiveConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;

public class HiveQueryThread extends QueryThread {
	HiveConnector hiveConn = null;
	String hiveQuery;
	
	public HiveQueryThread(HiveConnector conn, String query, ServiceProperties statusProps, ServiceProperties resultsProps) throws Exception {
		super(statusProps, resultsProps);
		this.hiveConn = conn;
		this.hiveQuery = query;
	}

	@Override
	Table execQuery() throws Exception {
		try {
			return hiveConn.query(this.hiveQuery);
		} catch (Exception e) {
			throw new Exception("Hive query failed: ", e);
		}
	}

}
