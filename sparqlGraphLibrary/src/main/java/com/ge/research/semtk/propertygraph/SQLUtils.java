package com.ge.research.semtk.propertygraph;


/**
 * SQL query utilities
 */
public class SQLUtils {
	
	/**
	 * Generate a query to get column metadata for a given table.
	 */
	public static String genColumnInfoSQL(String tableName) {
		return String.format(
				"SELECT column_name, data_type " +
				" FROM information_schema.columns " +
				" WHERE table_name = '%s'",
				tableName);
	}

	/**
	 * Generate a "select all" query
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

}
