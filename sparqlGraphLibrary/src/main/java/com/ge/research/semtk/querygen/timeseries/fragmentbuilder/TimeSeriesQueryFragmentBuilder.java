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

import com.ge.research.semtk.querygen.QueryFragmentBuilder;

/**
 * Creates query fragments.  Extend to implement different query languages.
 */
public abstract class TimeSeriesQueryFragmentBuilder extends QueryFragmentBuilder {
	
	/**
	 * Get the fragment for "select" (or equivalent)
	 */
	public abstract String getFragmentForSelect() throws Exception;
	
	/**
	 * Get the fragment for "from" (or equivalent)
	 */
	public abstract String getFragmentForFrom() throws Exception;
	
	/**
	 * Get the fragment for "and" (or equivalent)
	 */
	public abstract String getFragmentForAnd() throws Exception;
	
	/**
	 * Get the fragment for a comma (or equivalent)
	 */
	public abstract String getFragmentForComma() throws Exception;
	
	/**
	 * Get the fragment for group by (or equivalent)
	 */
	public abstract String getFragmentForGroupBy(String colName) throws Exception;
	
	/**
	 * Get the fragment for order by (or equivalent)
	 */
	public abstract String getFragmentForOrderBy(String colName) throws Exception;
	
	/**
	 * Get the fragment for where clause start 
	 */
	public abstract String getFragmentForWhere() throws Exception;
	
	/**
	 * Get the fragment for where clause start 
	 */
	public abstract String getFragmentForWhereClauseStart() throws Exception;
	
	/**
	 * Get the fragment for where clause end
	 */
	public abstract String getFragmentForWhereClauseEnd() throws Exception;
	
	/**
	 * Get fragment for a condition (to be used in a where clause)
	 */
	public abstract String getFragmentForCondition(String value1, String operator, String value2) throws Exception;

	/**
	 * Get a fragment for a time condition (to be used in a where clause).
	 * Assumes that timeCol contains type double.
	 */
	public abstract String getFragmentForTimeCondition(String timeCol, String operator, LocalDateTime dateTime) throws Exception;

	/**
	 * Get a fragment for a time condition (to be used in a where clause).
	 * Assumes that timeCol contains type timestamp.
	 */
	public abstract String getFragmentForTimeCondition_typeTimestamp(String timeCol, String operator, LocalDateTime dateTime) throws Exception;
	
	/**
	 * Get a fragment for a column name
	 */
	public abstract String getFragmentForColumnName(String colName) throws Exception;
	
	/**
	 * Get a fragment to return the average of a column 
	 */
	public abstract String getFragmentForAverage(String colName) throws Exception;
	
	/**
	 * Get a fragment to return the minimum of a column
	 */
	public abstract String getFragmentForMinimum(String colName) throws Exception;
	
	/**
	 * Get a fragment for aliasing a column
	 */
	public abstract String getFragmentForAlias(String alias);
	
	/**
	 * Get a fragment to cast to timestamp
	 */
	public abstract String getFragmentForCastToTimestamp(String toCast);

	/**
	 * Enclose the given fragment in parentheses (or equivalent)
	 */
	public abstract String encloseInParentheses(String s) throws Exception;
	
}
