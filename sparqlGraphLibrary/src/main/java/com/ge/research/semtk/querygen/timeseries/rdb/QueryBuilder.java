/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.querygen.timeseries.rdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.Query;
import com.ge.research.semtk.querygen.timeseries.TimeSeriesConstraint;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.TimeSeriesQueryFragmentBuilder;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Builds a single time series query
 */
public class QueryBuilder {
	
	private TimeSeriesQueryFragmentBuilder queryFragmentBuilder;
	
	private UUID uuid;
	private String database;
	private String tableName = "";
	private ArrayList<String> tagNames = null;
	private HashMap<String, String> tagNamesToVarNames;
	private String timeMarker;  // timestamp column
	private ArrayList<TimeSeriesConstraint> constraints = null;
	private String constraintsConjunction = null;  // AND or OR to apply between the non-time constraints
	private TimeSeriesConstraint timeConstraint = null;
	private JSONObject configInfo;
	
	private boolean omitAliasesFlag = false;	// if set, omits aliases for column names
	private boolean rawTimestampFlag = false;	// if set, returns timestamp column in raw format
	
	private ArrayList<String> extraSelectStrings = null;		// extra items to SELECT
	private ArrayList<String> extraConstraintStrings = null;	// extra items for WHERE clause
	private String groupByColumn = null;	
	private boolean orderBy = true;
	
	private String wrapperOpenStr;	// use to wrap the query in a custom outer query
	private String wrapperCloseStr;

	/**
	 * Constructor
	 */
	public QueryBuilder(TimeSeriesQueryFragmentBuilder queryFragmentBuilder, String database, String tablename, String timeMarker, UUID uuid) throws Exception{
		this.queryFragmentBuilder = queryFragmentBuilder;
		this.uuid = uuid;
		this.database = database;
		this.tableName = tablename;
		this.timeMarker = timeMarker;
		this.tagNames = new ArrayList<String>();
		this.tagNames.add(timeMarker);
		this.tagNamesToVarNames = new HashMap<String, String>();
		this.tagNamesToVarNames.put(timeMarker, timeMarker);
		this.extraSelectStrings = new ArrayList<String>();
		this.extraConstraintStrings = new ArrayList<String>();
	}

	/**
	 * Add extra items to select
	 */
	public void addExtraSelectString(String s) throws Exception{
		this.extraSelectStrings.add(s);
	}
	
	/**
	 * Add extra constraint fragments to the query (e.g. "tp_seq_num > 0", "year=2009")
	 * (Using String instead of TimeSeriesConstraint because the latter requires the constrained column to appear in tag-variable mapping)
	 */
	public void addExtraConstraintString(String constraintString) throws Exception{
		this.extraConstraintStrings.add(constraintString);
	}
	
	/**
	 * This will add a "group by" the given column 2) aggregate timestamp column using "minimum" 3) aggregate all other columns using "average"
	 * (e.g. group by tp_seq_num)
	 */
	public void setGroupByColumn_Average(String column) throws Exception{
		if(column != null && column.isEmpty()){
			throw new Exception("Can't group by empty column");
		}
		this.groupByColumn = column;	
	}
	
	/**
	 * True to include an ordering clause (by timestamp), false to not include an ordering clause.
	 */
	public void setOrderBy(boolean b){
		this.orderBy = b;
	}
	
	/**
	 * Omit aliases for column names
	 */
	public void setOmitAliasesFlag(boolean b){
		omitAliasesFlag = b;
	}
	
	/**
	 * Return timestamp column in raw format
	 */
	public void setRawTimestampFlag(boolean b){
		rawTimestampFlag = b;
	}
	
	public void addConfig(JSONObject info){
		this.configInfo = info;
	}
	
	public void addConstraints(ArrayList<TimeSeriesConstraint> constraintlist){
		this.constraints = constraintlist;		
	}
	
	public void addConstraintsConjunction(String s){
		this.constraintsConjunction = s;		
	}
	
	public void addTimeConstraint(TimeSeriesConstraint tsc){
		this.timeConstraint = tsc;
	}
	
	public void addTagName(String tagName){
		this.tagNames.add(tagName);
	}
	
	public void addTagNameToVarName(String tagName, String varName) {
		tagNamesToVarNames.put(tagName, varName);
	}
	
	/**
	 * Wrap the generated query in opener/closer strings (e.g. to apply an outer query)
	 */
	public void addWrapper(String wrapperOpen, String wrapperClose) throws Exception{
		if(wrapperOpen == null || wrapperClose == null){
			throw new Exception("Cannot use null string for query wrapper");
		}
		this.wrapperOpenStr = wrapperOpen;
		this.wrapperCloseStr = wrapperClose;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public String getTimeMarker(){
		return this.timeMarker;
	}
	
	public ArrayList<String> getTagNames(){
		return this.tagNames;
	}
	
	public HashMap<String, String> getTagAndVarNames(){
		return this.tagNamesToVarNames;
	}
	
	public String getTableName() {
		return tableName;
	}

	
	/**
	 * Get a Hive query.  Samples:
	 * 
	 * select cast(ts_time_utc AS timestamp) as `timestamp`, HX_101 AS HX_101, HX_102 AS HX_102 from dbase.simu_TEST_1400_T001 order by `timestamp`
	 * select cast(ts_time_utc AS timestamp) as `timestamp`, HX_101 AS HX_101, HX_102 AS HX_102 from dbase.simu_TEST_1400_T001 where tp_seq_num > 0 order by `timestamp`
	 * select cast(min(ts_time_utc) AS timestamp) as `timestamp`, avg(HX_100) AS HX_100, avg(HX_102) AS HX_102 from dbase.simu_TEST_1400_T001 where tp_seq_num > 0 group by tp_seq_num order by `timestamp`
	 */
	public Query getQuery() throws Exception{
		
		String TIMESTAMP_COL = "";						// Hive timestamp column (e.g. ts_time_utc)
		final String TIMESTAMP_ALIAS = "timestamp";  	// timestamp alias.  When running in Hive, need enclosing backticks (`) because timestamp appears to be a reserved word in AWS version of Hive
		boolean whereExists = false;
		
		String query = "";
		query = queryFragmentBuilder.getFragmentForSelect() + " ";  // "select "
		
		// add extra selects
		if(!extraSelectStrings.isEmpty()){
			for(String s : extraSelectStrings){
				query += s + ", ";
			}
		}
		
		// add regular tag selects
		int i = 0;
		for(String tag : tagNames){
			String s;
			if(!tag.equals(timeMarker)){	// this is a non-timestamp column				
				if(groupByColumn == null){
					s = queryFragmentBuilder.getFragmentForColumnName(tag); 
				}else{
					// same as above except average value for testpoint sequence number, e.g. avg(HX_48)
					s = queryFragmentBuilder.getFragmentForAverage(tag);
				}
				if(!omitAliasesFlag){
					// alias to variable name, e.g. HX_48 as `Pressure`
					s += " " + queryFragmentBuilder.getFragmentForAlias(tagNamesToVarNames.get(tag));	// alias to variable name
				}else{
					// alias to the original column name.  This is sometimes superfluous, and sometimes needed (e.g. if an average has been applied, need avg("HX_185") as "HX_185", otherwise it returns as something like _col1_)
					s += " " + queryFragmentBuilder.getFragmentForAlias(tag);
				}
			}else{							// this is the timestamp column
				TIMESTAMP_COL = tag;		// (e.g. ts_time_utc)
				if(groupByColumn == null){
					// format Unix timestamp as human-readable timestamp
					s = queryFragmentBuilder.getFragmentForColumnName(TIMESTAMP_COL); 
				}else{
					// same as above except need min timestamp for testpoint sequence number  
					s = queryFragmentBuilder.getFragmentForMinimum(TIMESTAMP_COL);
				}
				if(!rawTimestampFlag){
					s = queryFragmentBuilder.getFragmentForCastToTimestamp(s);				// cast to human readable timestamp
				}
				if(!omitAliasesFlag){
					s += " " + queryFragmentBuilder.getFragmentForAlias(TIMESTAMP_ALIAS);	// alias the timestamp
				}else{
					s += " " + queryFragmentBuilder.getFragmentForAlias(TIMESTAMP_COL);	// alias the timestamp to the original column name.  This is sometimes superfluous, and sometimes needed (e.g. to order by an aggregated/cast timestamp) 
				}
			}
			if(i != 0){ 
				query += queryFragmentBuilder.getFragmentForComma() + " " + s; 
			} else {
				query +=  s; 
			}
			i += 1;
		}
		query += " " + queryFragmentBuilder.getFragmentForFrom() + " ";
		// add the prefix, if any was given
		if(this.database != null){
			query += this.database + ".";
		}	
		query += tableName;	

		// ---- start constraints 
		if(this.constraints != null && this.constraints.size() > 0){
			if(groupByColumn != null){
				// allow a constraint on the groupBy column 		(includes/excludes entire groups)
				// throw exception if any other constraints present (includes/excludes individual points within a group, e.g. producing a misleading average)
				for(TimeSeriesConstraint tsc : this.constraints){
					if(!tsc.getVariableName().equalsIgnoreCase(groupByColumn)){
						throw new Exception("Cannot accept constraint on '" + tsc.getVariableName() + "' when grouping/averaging by '" + groupByColumn + "'");   // disallowed because may result in group/average over only a subset of values	
					}
				}
			}
			// spin through the list of the constraints and add the where clauses
			query += " " + queryFragmentBuilder.getFragmentForWhereClauseStart();
			whereExists = true;
			int j = 0;
			for(TimeSeriesConstraint tsc : this.constraints){
				// add the constraints
				query += tsc.getConstraintQueryFragment(tagNamesToVarNames, queryFragmentBuilder);
				j += 1;
				if(j != this.constraints.size() ){ query += " " + constraintsConjunction + " "; }		
			}
			query += queryFragmentBuilder.getFragmentForWhereClauseEnd();
		}				
		if(this.timeConstraint != null){
			if(groupByColumn != null){	
				throw new Exception("Cannot accept time constraints when grouping/averaging");	// disallowed because may result in group/average over only a subset of values	
			}
			if(!whereExists){
				query += " " + queryFragmentBuilder.getFragmentForWhere() + " ";
				whereExists = true;
			}else{
				query += " " + queryFragmentBuilder.getFragmentForAnd() + " ";
			}
			query += timeConstraint.getTimeConstraintQueryFragment(TIMESTAMP_COL, queryFragmentBuilder); // time constraint needs to use the original Hive column, not the alias
		}
		// ----- end constraints 
	
		// add the extra constraints  
		// this is kept separate from other TimeSeriesConstraints because 1) allow constraint on column that doesn't appear in tag-variable mapping 2) enforce AND conjunction
		if(!extraConstraintStrings.isEmpty()){
			if(!whereExists){
				query += " " + queryFragmentBuilder.getFragmentForWhere() + " ";	
				whereExists = true;
			}else{
				query += " " + queryFragmentBuilder.getFragmentForAnd() + " ";
			}
			for(int j = 0; j < extraConstraintStrings.size(); j++){
				if(j > 0){
					query += " " + queryFragmentBuilder.getFragmentForAnd() + " ";
				}
				query += extraConstraintStrings.get(j);
			}
		}
		
		// add a group by clause (if needed)
		if(groupByColumn != null){
			query += " " + queryFragmentBuilder.getFragmentForGroupBy(groupByColumn);  	// e.g. group by tp_seq_num
		}
		
		// add an order by clause (if needed)
		if(orderBy){
			if(!omitAliasesFlag){
				query += " " + queryFragmentBuilder.getFragmentForOrderBy(TIMESTAMP_ALIAS);	
			}else{
				query += " " + queryFragmentBuilder.getFragmentForOrderBy(TIMESTAMP_COL);	
			}
		}
		
		// wrap the query (if needed)
		if(wrapperOpenStr != null && wrapperCloseStr != null){
			query = wrapperOpenStr + query + wrapperCloseStr;
		}
		
		LocalLogger.logToStdOut("Generated query: " + query);
		
		return new Query(query);
	}

	public JSONObject getConfig() {
		return this.configInfo;
	}
	
}
