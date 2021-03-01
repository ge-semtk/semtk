package com.ge.research.semtk.propertygraph;

import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.Utility;

/**
 * A table that can be loaded to a Neptune property graph.
 * TODO add tests
 */
public class NeptuneGremlinTable extends Table {

	/**
	 * Constructor
	 */
	public NeptuneGremlinTable(Table table) throws Exception {
		super(table.getColumnNames(), table.getColumnTypes(), table.copy().getRows());
	}
	
	/**
	 * Create a vertex table with Neptune Gremlin CSV headers ~id (required), ~label (optional)
	 * @param idPattern 	columns/constants to create the ~id column
	 * @param delimiter 	delimiter to join the columns/constants	(if null, defaults to "_")
	 * @param label 		constant for the ~label column (if null, skips label)
	 * @return 				the table with all original columns, plus ~id and ~label columns
	 */
	public void toVertexTable(String[] idPattern, String delimiter, String label) throws Exception{
		if(idPattern == null || idPattern.length == 0){
			throw new Exception("Cannot create vertex table: must provide table, pattern for ~id");
		}
		delimiter = (delimiter != null) ? delimiter : "_";
		this.appendJoinedColumn("~id", "String", idPattern, delimiter);
		if(label != null){
			this.appendColumn("~label", "String", label);
		}
	}
	
	/**
	 * Create an edge table with Neptune Gremlin CSV headers ~from, ~to, ~id (all required), and ~label (optional)
	 * @param fromPattern 	columns/constants to create the ~from column
	 * @param toPattern 	columns/constants to create the ~to column
	 * @param delimiter 	delimiter to join the columns/constants (if null, defaults to "_")
	 * @param label 		constant for the ~label column (if null, skips label)
	 * @return 				a table with columns ~from, ~to, ~id, ~label
	 */
	public void toEdgeTable(String[] fromPattern, String[] toPattern, String delimiter, String label) throws Exception{
		if(fromPattern == null || fromPattern.length == 0 || toPattern == null || toPattern.length == 0){
			throw new Exception("Cannot create edge table: must provide table, patterns for ~from and ~to");
		}
		delimiter = (delimiter != null) ? delimiter : "_";
		this.appendJoinedColumn("~from", "String", fromPattern, delimiter);
		this.appendJoinedColumn("~to", "String", toPattern, delimiter);
		this.appendJoinedColumn("~id", "String", (String[])ArrayUtils.addAll(fromPattern, toPattern), delimiter);  // create the id as from-to
		if(label != null){
			this.appendColumn("~label", "String", label); 
		}
		this.toSubTable(new String[]{"~from","~to","~id","~label"});
	}	
	
	
	/**
	 * Write the table to Neptune Gremlin CSV load format
	 * The table should already include columns ~id (for vertices and edges), ~from and ~to (for edges only), and ~label (optional for vertices and edges)
	 */
	public String toGremlinCSVString() throws Exception, IOException{
		
		if(this.getColumnNames().length == 0){
			throw new Exception("Table is null or empty");
		}
		
		// error if input does not include a ~id column - this is required for both vertices and edges
		if(!this.hasColumn("~id")){
			throw new Exception("A Neptune Gremlin CSV table must include a column '~id'");
		}
		
		// don't want to change the existing table
		Table clone = this.copy();
		
		String [] colNames = clone.getColumnNames(); 
		String [] colTypes = clone.getColumnTypes();
		
		// format datetime (where needed)
		// Gremlin supports the following formats: yyyy-MM-dd, yyyy-MM-ddTHH:mm, yyyy-MM-ddTHH:mm:ss, yyyy-MM-ddTHH:mm:ssZ (https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format-gremlin.html)
		for(int colIndex = 0; colIndex < colTypes.length;  colIndex++){
			if(getGremlinDataType(colTypes[colIndex]).equals("Date")){
				for(int rowIndex = 0; rowIndex < clone.getNumRows(); rowIndex++){
					String dateTimeOrig = clone.getCell(rowIndex, colIndex);
					String dateTimeStrFormatted;
					if(dateTimeOrig == null){
						continue;
					}else if(dateTimeOrig.length() >= 19){  			
						if(dateTimeOrig.length() > 19){
							dateTimeOrig = dateTimeOrig.substring(0, dateTimeOrig.indexOf("."));  // Gremlin does not support microseconds
						}
						dateTimeStrFormatted = Utility.formatDateTime(dateTimeOrig, Utility.DATETIME_FORMATTER_yyyyMMddHHmmss, Utility.DATETIME_FORMATTER_ISO8601);  
						clone.setCell(rowIndex, colIndex, dateTimeStrFormatted);
					}else if(dateTimeOrig.length() != 10 && dateTimeOrig.length() != 15 && dateTimeOrig.length() != 18 && dateTimeOrig.length() != 20){
						throw new Exception("Unrecognized date format: " + dateTimeOrig);
					}				
				}
			}
		}
		
		// write the modified clone to buffer as CSV
		StringBuffer buf = new StringBuffer();
		
		// write column names
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

		// write data rows
		for(int i = 0; i < clone.getNumRows(); i++){	
			buf.append(clone.getRowAsCSVString(i));			
			buf.append("\n");
		}		
		
		return buf.toString();
	}
	
	
	/**
	 * Apply a partition to a table (add partition_key column, prepend ~id/~from/~to with partition key
	 * Note: it is not sufficient to just add the partition_key column, because id's must also be partition-specific
	 */
	public void setPartition(String partitionKey) throws Exception{
		
		if(partitionKey == null || partitionKey.trim().isEmpty()){
			throw new Exception("Partition key may not be null or empty");
		}
		
		this.appendColumn("partition_key", "String", partitionKey);  // append column for partition key
		
		// prepend all ~id with partition key
		this.renameColumn("~id","~idBeforePartition");
		this.appendJoinedColumn("~id", "String", new String[]{"partition_key", "~idBeforePartition"}, "_");
		this.removeColumn("~idBeforePartition");
		
		// prepend all ~from with partition key
		if(this.hasColumn("~from")){
			this.renameColumn("~from","~fromBeforePartition");
			this.appendJoinedColumn("~from", "String", new String[]{"partition_key", "~fromBeforePartition"}, "_");
			this.removeColumn("~fromBeforePartition");
		}
		
		// prepend all ~to with partition key
		if(this.hasColumn("~to")){
			this.renameColumn("~to","~toBeforePartition");
			this.appendJoinedColumn("~to", "String", new String[]{"partition_key", "~toBeforePartition"}, "_");
			this.removeColumn("~toBeforePartition");
		}
	}
	
	
	/**
	 * Translate a data type to Neptune Gremlin type
	 * https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format-gremlin.html#bulk-load-tutorial-format-gremlin-datatypes
	 */
	private static String getGremlinDataType(String type) throws Exception {
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
