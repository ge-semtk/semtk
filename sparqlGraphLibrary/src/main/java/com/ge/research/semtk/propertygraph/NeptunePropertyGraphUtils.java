package com.ge.research.semtk.propertygraph;

import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.Utility;

/**
 * Utilities for using the Neptune property graph.
 * TODO add tests
 */
public class NeptunePropertyGraphUtils {
	
	/**
	 * Create a vertex table with Neptune Gremlin CSV headers ~id (required), ~label (optional)
	 * @param table 		a table containing the elements to load
	 * @param idPattern 	columns/constants to create the ~id column
	 * @param delimiter 	delimiter to join the columns/constants	(if null, defaults to "_")
	 * @param label 		constant for the ~label column (if null, skips label)
	 * @return 				a table with all original columns, plus ~id and ~label columns
	 */
	public static Table getVertexTable(Table table, String[] idPattern, String delimiter, String label) throws Exception{
		if(table == null || idPattern == null || idPattern.length == 0){
			throw new Exception("Cannot create vertex table: must provide table, pattern for ~id");
		}
		delimiter = (delimiter != null) ? delimiter : "_";
		table.appendJoinedColumn("~id", "String", idPattern, delimiter);
		if(label != null){
			table.appendColumn("~label", "String", label);
		}
		return table;
	}	
	
	/**
	 * Create an edge table with Neptune Gremlin CSV headers ~from, ~to, ~id (all required), and ~label (optional)
	 * @param table 		a table containing the elements needed to create ~from and ~to columns
	 * @param fromPattern 	columns/constants to create the ~from column
	 * @param toPattern 	columns/constants to create the ~to column
	 * @param delimiter 	delimiter to join the columns/constants (if null, defaults to "_")
	 * @param label 		constant for the ~label column (if null, skips label)
	 * @return 				a table with columns ~from, ~to, ~id, ~label
	 */
	public static Table getEdgeTable(Table table, String[] fromPattern, String[] toPattern, String delimiter, String label) throws Exception{
		if(table == null || fromPattern == null || fromPattern.length == 0 || toPattern == null || toPattern.length == 0){
			throw new Exception("Cannot create edge table: must provide table, patterns for ~from and ~to");
		}
		delimiter = (delimiter != null) ? delimiter : "_";
		table.appendJoinedColumn("~from", "String", fromPattern, delimiter);
		table.appendJoinedColumn("~to", "String", toPattern, delimiter);
		table.appendJoinedColumn("~id", "String", (String[])ArrayUtils.addAll(fromPattern, toPattern), delimiter);  // create the id as from-to
		if(label != null){
			table.appendColumn("~label", "String", label); 
		}
		return table.getSubTable(new String[]{"~from","~to","~id","~label"});
	}	
	
	
	/**
	 * Convert a table into the Neptune Gremlin CSV load format
	 * The table should already include columns ~id (for vertices and edges), ~from and ~to (for edges only), and ~label (optional for vertices and edges)
	 */
	public static String getGremlinCSVString(Table table) throws Exception, IOException{
		
		if(table == null || table.getColumnNames().length == 0){
			throw new Exception("Table is null or empty");
		}
		
		// error if input does not include a ~id column - this is required for both vertices and edges
		if(!table.hasColumn("~id")){
			throw new Exception("A Neptune Gremlin CSV table must include a column '~id'");
		}
	
		String [] colNames = table.getColumnNames(); 
		String [] colTypes = table.getColumnTypes();
		
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
			if (colNames[i].charAt(0) == '~') {
				// leave alone any column starting with ~ (e.g. ~id, ~from, ~to, ~label)
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
			case "String":
				return "String";			
			case "text":
				return "String";
			case "timestamp":
				return "Date";
			case "date":
				return "Date";
			case "numeric":
				return "Double";
		default:
			throw new Exception("Can't find corresponding Neptune Gremlin type for: " + type);
		}
	}
	
}
