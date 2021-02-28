package com.ge.research.semtk.propertygraph;

import java.io.IOException;
import java.util.Arrays;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.Utility;

/**
 * Utilities for using the Neptune property graph.
 */
public class NeptunePropertyGraphUtils {

	/**
	 * Convert a table into the Neptune Gremlin CSV load format
	 * @param the table
	 */
	public static String getGremlinCSVString(Table table) throws Exception, IOException{
		return getGremlinCSVString(table, null, null, null, null);
	}

	/**
	 * Convert a table into the Neptune Gremlin CSV load format
	 * @param the table
	 */

	public static String getGremlinCSVString(Table table, String idColumn, String labelColumn) throws Exception, IOException{
		return getGremlinCSVString(table, idColumn, labelColumn, null, null);
	}

	/**
	 * Convert a table into the Neptune Gremlin CSV load format
	 * @param the table
	 * @param idColumn  	column to rename "~id"
	 * @param labelColumn	column to rename "~label"
	 * @param fromColumn  	column to rename "~from"
	 * @param toColumn	column to rename "~to"
	 */
	public static String getGremlinCSVString(Table table, String idColumn, String labelColumn, String fromColumn, String toColumn) throws Exception, IOException{
		
		if(table.getColumnNames().length == 0){
			return "";
		}
	
		String [] colNames = table.getColumnNames(); 
		String [] colTypes = table.getColumnTypes();
		
		// make sure special column names exist
		for (String col : new String [] {idColumn, labelColumn, fromColumn, toColumn} ) {		
			if (col != null && !Arrays.stream(colNames).anyMatch(col::equals)) {
				throw new Exception("Column does not exist: " + col);
			}
		}
		
		// format datetime (where needed)
		// Gremlin supports the following formats: yyyy-MM-dd, yyyy-MM-ddTHH:mm, yyyy-MM-ddTHH:mm:ss, yyyy-MM-ddTHH:mm:ssZ (https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format-gremlin.html)
		for(int colIndex = 0; colIndex < colTypes.length;  colIndex++){
			if(getGremlinDataType(colTypes[colIndex]).equals("Date")){
				for(int rowIndex = 0; rowIndex < table.getNumRows(); rowIndex++){
					String dateTimeOrig = table.getCell(rowIndex, colIndex);
					String dateTimeStrFormatted;
					if(dateTimeOrig == null){
						continue;
					}else if(dateTimeOrig.length() >= 19){  			
						if(dateTimeOrig.length() > 19){
							dateTimeOrig = dateTimeOrig.substring(0, dateTimeOrig.indexOf("."));  // Gremlin does not support microseconds
						}
						dateTimeStrFormatted = Utility.formatDateTime(dateTimeOrig, Utility.DATETIME_FORMATTER_yyyyMMddHHmmss, Utility.DATETIME_FORMATTER_ISO8601);  
						table.setCell(rowIndex, colIndex, dateTimeStrFormatted);
					}else if(dateTimeOrig.length() != 10 && dateTimeOrig.length() != 15 && dateTimeOrig.length() != 18 && dateTimeOrig.length() != 20){
						throw new Exception("Unrecognized date format: " + dateTimeOrig);
					}				
				}
			}
		}
		
		StringBuffer buf = new StringBuffer();
		
		// create column names
		for (int i=0; i < colNames.length; i++) {
			if (i>0) buf.append(",");			
			if (colNames[i].equals(idColumn!=null?idColumn:"")) {
				// change idColumn to ~id
				buf.append("~id");
			} else if (colNames[i].equals(labelColumn!=null?labelColumn:"")) {
				// change labelColumn to ~label
				buf.append("~label");
			} else if (colNames[i].equals(fromColumn!=null?fromColumn:"")) {
				// change fromColumn to ~label
				buf.append("~from");
			} else if (colNames[i].equals(toColumn!=null?toColumn:"")) {
				// change toColumn to ~label
				buf.append("~to");
			} else if (colNames[i].charAt(0) == '~') {
				// leave alone any column starting with ~
				buf.append(colNames[i]);
			} else {
				// append :gremlin-type to any other column
				buf.append(colNames[i] + ":" + getGremlinDataType(colTypes[i]));
			}
		}
		buf.append("\n");

		// get CSV for data rows
		for(int i = 0; i < table.getNumRows(); i++){	
			buf.append(table.getRowAsCSVString(i));			
			buf.append("\n");
		}		
		
		return buf.toString();
	}
	
	/**
	 * Translate a data type to Neptune Gremlin type
	 * https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format-gremlin.html#bulk-load-tutorial-format-gremlin-datatypes
	 */
	public static String getGremlinDataType(String type) throws Exception {

		switch (type) {
		case "text":
			return "String";

		case "timestamp":
			return "Date";

		case "date":
			return "Date";

		case "numeric":
			return "Double";

		default:
			throw new Exception("Can't translate to gremlin an unknown SQL type: " + type);
		}
	}
	
}
