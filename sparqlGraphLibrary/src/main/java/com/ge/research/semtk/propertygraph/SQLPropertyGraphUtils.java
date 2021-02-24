package com.ge.research.semtk.propertygraph;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

public class SQLPropertyGraphUtils {
	
	public static String genColumnInfoSQL(String tableName) {
		return String.format(
				"SELECT column_name, data_type " +
				" FROM information_schema.columns " +
				" WHERE table_name = '%s'",
				tableName);
	}

	/**
	 * 
	 * @param tableName
	 * @param whereClause or null
	 * @param orderByColumns or null
	 * @param limit or null
	 * @param offset or null
	 * @return
	 */
	public static String genSelectStarSQL(String tableName, String whereClause, String [] orderByColumns, Integer limit, Integer offset) {
		
		// create order string or ""
		String order = String.join(",",  orderByColumns==null ? new String [] {} : orderByColumns);
		
		return 	"SELECT * " +
				" FROM " + tableName +
				(whereClause==null ? ""   : (" WHERE " + whereClause)) +
				(order.length() == 0 ? "" : (" ORDER BY " + order)) +
				(limit == null ? ""       : (" LIMIT " + limit)) +
				(offset == null ? ""      : (" offset " + offset)) ;
	}

	/**
	 * Translate a list of SQL types to Gremlin types
	 * @param columnTypes
	 * @return
	 * @throws Exception
	 */
	public static String SQLtoGremlinType(String sqlType) throws Exception {

		// Gremlin types:
		// https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format-gremlin.html#bulk-load-tutorial-format-gremlin-datatypes

		switch (sqlType) {
		case "text":
			return "String";

		case "timestamp":
			return "Date";

		case "date":
			return "Date";

		case "numeric":
			return "Double";

		default:
			throw new Exception("Can't translate to gremlin an unknown SQL type: " + sqlType);
		}

	}
}
