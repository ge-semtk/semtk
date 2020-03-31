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
package com.ge.research.semtk.querygen.timeseries.fragmentbuilder;


import java.time.LocalDateTime;

import com.ge.research.semtk.utility.Utility;


/**
 * Creates Athena (SQL-ish queries over S3) query fragments.  
 */
public class AthenaQueryFragmentBuilder extends TimeSeriesQueryFragmentBuilder {
	
	@Override
	public String getFragmentForSelect(){
		return "SELECT";
	}
	
	@Override
	public String getFragmentForFrom(){
		return "FROM";
	}
	
	@Override
	public String getFragmentForAnd() throws Exception {
		return "AND";
	}

	@Override
	public String getFragmentForComma() throws Exception {
		return ",";
	}

	@Override
	public String getFragmentForGroupBy(String colName) throws Exception {
		return "GROUP BY \"" + colName + "\"";
	}
	
	@Override
	public String getFragmentForOrderBy(String colName) throws Exception {
		return "ORDER BY \"" + colName + "\"";
	}
	
	@Override
	public String getFragmentForWhere() throws Exception {
		return "WHERE";
	}
	
	@Override
	public String getFragmentForWhereClauseStart() throws Exception {
		return "WHERE (";
	}
	
	@Override
	public String getFragmentForWhereClauseEnd() throws Exception {
		return ")";
	}

	@Override
	public String getFragmentForCondition(String value1, String operator, String value2) throws Exception {
		return "\"" + value1 +  "\" " + operator + " " + value2;
	}
	
	@Override
	public String getFragmentForTimeCondition(String timeCol, String operator, LocalDateTime timeValue) throws Exception { 
		return "(from_unixtime(\"" + timeCol +  "\") " + operator + " timestamp '" + timeValue.format(Utility.DATETIME_FORMATTER_yyyyMMddHHmmss) +  "')";  // e.g. format as '2018-02-02 04:00:00'
	}
	
	@Override
	// e.g. to_iso8601(date_time) < '2017-09-16T13:37:04Z'
	public String getFragmentForTimeCondition_typeTimestamp(String timeCol, String operator, LocalDateTime dateTime) throws Exception {
		return "(to_iso8601(\"" + timeCol +  "\") " + operator + " '" + dateTime.format(Utility.DATETIME_FORMATTER_ISO8601) +  "')";
	}
	
	@Override
	public String getFragmentForColumnName(String colName){
		return "\"" + colName + "\"";
	}

	@Override
	public String getFragmentForAverage(String colName) {
		return "avg(\"" + colName + "\")"; 
	}
	
	@Override
	public String getFragmentForMinimum(String colName){
		return "min(\"" + colName + "\")";
	}
	
	@Override
	public String getFragmentForAlias(String alias) {
		return "AS \"" + alias + "\"";
	}
	
	@Override
	public String getFragmentForCastToTimestamp(String toCast) {
		return "from_unixtime(" + toCast + ")";
	}
	
	@Override
	public String encloseInParentheses(String s) throws Exception {
		return "(" + s + ")";
	}

}
