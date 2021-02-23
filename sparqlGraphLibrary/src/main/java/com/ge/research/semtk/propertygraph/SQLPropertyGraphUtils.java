package com.ge.research.semtk.propertygraph;

public class SQLPropertyGraphUtils {
	
	public static String genColumnInfoSQL(String tableName) {
		return String.format(
				"SELECT column_name, data_type " +
				" FROM information_schema.columns " +
				" WHERE table_name = '%s'",
				tableName);
	}

	public static String genSelectStarSQL(String tableName, int limit, int offset) {
		return String.format(
				"SELECT * " +
				" FROM %s " +
				" LIMIT %d OFFSET %d",
				tableName, limit, offset);
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
