package com.ge.research.semtk.querygen.timeseries.rdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.QueryList;
import com.ge.research.semtk.querygen.timeseries.TimeSeriesQueryGenerator;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.TimeSeriesQueryFragmentBuilder;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utilityge.Utility;



/**
 * Generate queries to retrieve time series data from a relational database, 
 * where the data is stored by row (one row represents a timestamp with all values).
 */
public class RDBTimeCoherentTimeSeriesQueryGenerator extends TimeSeriesQueryGenerator {
	
	// columns required to be present in the location input table  
	public final static String[] REQUIRED_COLS = {Utility.COL_NAME_UUID, Utility.COL_NAME_DATABASE_SERVER, Utility.COL_NAME_DATABASE, Utility.COL_NAME_TABLE_NAME, Utility.COL_NAME_TIMESTAMP_COLUMN, Utility.COL_NAME_VARIABLE_NAME, Utility.COL_NAME_TAG_NAME}; 
	// columns required for building the configuration json for the query executor
	private final static String[] CONFIG_COLS = {Utility.COL_NAME_DATABASE_SERVER, Utility.COL_NAME_DATABASE};
	
	protected TimeSeriesQueryFragmentBuilder queryFragmentBuilder; // class to generate appropriate query syntax (e.g. Hive, Athena)

	public final static String FLAG_RDB_QUERYGEN_OMIT_ALIASES = "RDB_QUERYGEN_OMIT_ALIASES";
	public final static String FLAG_RDB_QUERYGEN_RAW_TIMESTAMP = "RDB_QUERYGEN_RAW_TIMESTAMP";
	
	/**
	 * Constructor without flags  
	 */
	public RDBTimeCoherentTimeSeriesQueryGenerator(TimeSeriesQueryFragmentBuilder queryFragmentBuilder, Table locationAndValueInfo, JSONObject externalConstraintsJson) throws Exception {
		this(queryFragmentBuilder, locationAndValueInfo, externalConstraintsJson, null);
	}
	
	/**
	 * Constructor
	 * @param locationAndValueInfo a table containing the required location columns
	 * @param externalConstraintsJson a JSON object containing the constraints (both value and time)
	 * @param queryFragmentBuilder class to generate appropriate query syntax (e.g. Hive or SQL)
	 * @param flagsJsonArray a JSON array containing flags
	 * @throws Exception
	 */
	public RDBTimeCoherentTimeSeriesQueryGenerator(TimeSeriesQueryFragmentBuilder queryFragmentBuilder, Table locationAndValueInfo, JSONObject externalConstraintsJson, QueryFlags flags) throws Exception {
		super(locationAndValueInfo, externalConstraintsJson, flags);
		this.queryFragmentBuilder = queryFragmentBuilder;
		
		// validate that input table has all of the required columns
		for(String reqCol : REQUIRED_COLS){
			if(!locationAndValueInfo.hasColumn(reqCol)){
				throw new Exception("Missing required column: " + reqCol);
			}
		}
	}

	/**
	 * Get the generated queries
	 * 
	 * @return a hashmap of UUID to QueryList
	 */
	@Override
	public HashMap<UUID, Object> getQueries() throws Exception {

		HashMap<UUID, Object> ret = new HashMap<UUID,Object>();  // UUID to QueryList
	
		ArrayList<QueryBuilder> queryBuilders = createQueryBuilders();		
		for(QueryBuilder queryBuilder : queryBuilders){
			if(ret.get(queryBuilder.getUUID()) == null){
				ret.put(queryBuilder.getUUID(), new QueryList());  // add a query set if does not exist
			}
			((QueryList)ret.get(queryBuilder.getUUID())).addQuery(queryBuilder.getQuery()); // add the query
			((QueryList)ret.get(queryBuilder.getUUID())).addConfig(queryBuilder.getConfig()); // add the row-specific config.
		}
		return ret;
	}


	/**
	 * Create QueryBuilder objects
	 * 
	 * TODO dispatcher should ensure all tables have same variable names - we don't support disjoint variable name sets
	 */
	private ArrayList<QueryBuilder> createQueryBuilders() throws IOException{
		ArrayList<QueryBuilder> retval = new ArrayList<QueryBuilder>();
		try{

			// get list of unique UUIDs
			String[] uuidsUnique = locationAndValueInfo.getColumnUniqueValues(Utility.COL_NAME_UUID);
			
			// for each UUID, gather info and create QueryBuilder
			for(String uuid : uuidsUnique){

				Table dataForOneUUID = getDataForOneUUID(uuid);				
				Table configForOneUUID = locationAndValueInfo.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid, CONFIG_COLS);
				
				int tagNameIndex = dataForOneUUID.getColumnIndex(Utility.COL_NAME_TAG_NAME); 
				int varNameIndex = dataForOneUUID.getColumnIndex(Utility.COL_NAME_VARIABLE_NAME); 				
				
				// temporary hashmap of QueryBuilder objects for this UUID
				HashMap<String,QueryBuilder> queryBuildersForOneUUID = new HashMap<String,QueryBuilder>();
				
				// for each row in the uuid-specific table
				for(int i = 0; i < dataForOneUUID.getNumRows(); i++){
					
					ArrayList<String> row = dataForOneUUID.getRows().get(i);
					String databaseServer = row.get(dataForOneUUID.getColumnIndex(Utility.COL_NAME_DATABASE_SERVER));
					String database = row.get(dataForOneUUID.getColumnIndex(Utility.COL_NAME_DATABASE));
					String table = row.get(dataForOneUUID.getColumnIndex(Utility.COL_NAME_TABLE_NAME));	
					String timestampColumn = row.get(dataForOneUUID.getColumnIndex(Utility.COL_NAME_TIMESTAMP_COLUMN));
				
					// get the config for this section:
					JSONObject config = this.getConfigForUUIDEntry(configForOneUUID);
					
					// add QueryBuilder to temporary hashmap for this UUID, if not already there
					String key = databaseServer + database + table + timestampColumn;
					if(queryBuildersForOneUUID.get(key) == null){						
						QueryBuilder queryBuilder = new QueryBuilder(queryFragmentBuilder, database, table, timestampColumn, UUID.fromString(uuid));
						queryBuilder.addConfig(config);  // our config
						queryBuilder.setOmitAliasesFlag(isFlagSet(FLAG_RDB_QUERYGEN_OMIT_ALIASES));		// set flag
						queryBuilder.setRawTimestampFlag(isFlagSet(FLAG_RDB_QUERYGEN_RAW_TIMESTAMP));	// set flag

						// add constraints to QueryBuilder object
						if(this.constraints != null){
							queryBuilder.addConstraints(this.constraints);
							queryBuilder.addConstraintsConjunction(this.constraintsConjunction);
						}
						if(this.timeConstraint != null){
							queryBuilder.addTimeConstraint(this.timeConstraint);
						}			
											
						modifyQueryBuilder(queryBuilder, row, dataForOneUUID);
						
						// add to temporary hashmap
						queryBuildersForOneUUID.put(key,queryBuilder);						
					}
					
					// find the relevant QueryBuilder from our temporary hashmap, and add col/var info to it
					QueryBuilder queryBuilder = queryBuildersForOneUUID.get(key);
					queryBuilder.addTagName(row.get(tagNameIndex));  	
					queryBuilder.addTagNameToVarName(row.get(tagNameIndex), row.get(varNameIndex)); 				
				
				}
				
				// add QueryBuilder from temporary hashmap to the return list
				for(QueryBuilder queryBuilder : queryBuildersForOneUUID.values()){
					modifyQueryBuilder(queryBuilder);  // apply modifications
					retval.add(queryBuilder);
				}
			}

		}
		catch(Exception e){
			LocalLogger.printStackTrace(e);
			throw new IOException("Error building QueryBuilder objects: " + e.getMessage());
		}
		return retval;
	}

	/**
	 * Gets a table with data for a particular UUID.
	 * A subclass may override this to use its own REQUIRED_COLS
	 */
	protected Table getDataForOneUUID(String uuid) throws Exception{
		return locationAndValueInfo.getSubsetWhereMatches(Utility.COL_NAME_UUID, uuid, REQUIRED_COLS);
	}
	
	/**
	 * Method that subclasses may use to modify a QueryBuilder, based on the input data.
	 */
	protected void modifyQueryBuilder(QueryBuilder queryBuilder, ArrayList<String> row, Table dataForOneUUID) throws Exception{
	}
	
	/**
	 * Method that subclasses may use to modify a QueryBuilder.  
	 * (This is called after the QueryBuilder has been assembled, and thus is useful if caller needs to interrogate the QueryBuilder to produce the modifications.)
	 */
	protected void modifyQueryBuilder(QueryBuilder queryBuilder) throws Exception{
	}
	
	/**
	 * Generate a config JSON object that will be used to instantiate the query executor.
	 * 
	 * The database server is treated specially.  Sample inputs are "jdbc:hive2://serverXX:10000" for Hive, 
	 * and "athena" for Athena.  If a host and port is parseable from the database server string, then set them 
	 * in the config JSON.  If not, then ignore the database server. Note that "jdbc:hive2://" and "athena" 
	 * do not actually determine the query executor (rather, the EDC mnemonic configuration does)
	 * 
	 * All other fields are passed through to the config (e.g. database: "equipmentDb")
	 * 
	 * @param configData the data to use to generate a single config JSON object.  All rows must be the same to proceed.
	 * @return a config JSON object, e.g. {"database":"equipmentDb","port":"10000","host":"server01"} for Hive, {"database":"equipmentDb"} for Athena
	 * @exception
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject getConfigForUUIDEntry(Table configData) throws Exception {

		// all rows in the table must be the same - else can't generate a single config.  
		if(!configData.allRowsMatch()){
			throw new Exception("Cannot produce configuration JSON for input with varying rows: " + configData.toCSVString());
		}

		// go through the values and build a config
		JSONObject configJson = new JSONObject();
		
		for(String configCol : CONFIG_COLS){

			String s = configData.getRows().get(0).get(configData.getColumnIndex(configCol)); // use row 0, since all rows are the same

			if(configCol.equalsIgnoreCase(Utility.COL_NAME_DATABASE_SERVER)){	

				// if database server contains a colon (e.g. jdbc:hive2://serverXX:10000), then attempt to parse host/port.
				// if not, then not needed (e.g. "athena")
				if(s.indexOf(":") != -1){				
					try{
						String host, port;
						host = s.split(":")[2];
						if(host.startsWith("//")){
							host = host.substring(2);
						}
						port = s.split(":")[3];
						configJson.put("host", host);
						configJson.put("port", port);
					}catch(Exception e){
						throw new Exception("Error parsing host/port from " + s);  
					}
				}

			} else {
				configJson.put(configCol.toLowerCase(), s);
			}
		}

		return configJson;
	}	
	
}
