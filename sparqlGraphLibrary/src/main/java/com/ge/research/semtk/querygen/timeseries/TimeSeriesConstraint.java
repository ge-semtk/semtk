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
package com.ge.research.semtk.querygen.timeseries;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.EdcConstraint;
import com.ge.research.semtk.querygen.QueryFragmentBuilder;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.TimeSeriesQueryFragmentBuilder;
import com.ge.research.semtk.utility.Utility;


/**
 * A constraint on a single variable, to be used in a time series query.
 * 
 * Examples: 
 * 		variable1 > 50 and variable1 < 60
 * 		timestamp > 04/07/2016 2:00:00 AM and timestamp < 04/09/2016 4:00:00 AM  (this is a "time constraint")
 * 
 * TODO currently all constraints related to a single variable are logically "AND"ed (&&) together - may want to make this more flexible
 */
public class TimeSeriesConstraint extends EdcConstraint{
	
	private String variableName;
	private ArrayList<String> operators;		// currently only 2 are supported but we may extend this.
	private ArrayList<TimeSeriesConstraintValue> values;	

	public TimeSeriesConstraint(JSONObject jobj) throws Exception {
		super(jobj);
	}
	
	public String getVariableName() { 
		return this.variableName;
	}
	public int getNumOperators(){
		return operators.size();
	}
	public boolean hasLowerBound(){
		return operators.contains(">") || operators.contains(">=");
	}
	public boolean hasUpperBound(){
		return operators.contains("<") || operators.contains("<=");
	}
	
	/**
	 * Parse a string containing a timestamp into a LocalDateTime object.
	 * Accepts 2 formats: 1) 04/07/2016 2:00:00 AM and 2) 2016-04-07 02:00:00
	 */
	public static LocalDateTime parseDateTime(String s) throws Exception{
		try{
			return LocalDateTime.parse(s, Utility.DATETIME_FORMATTER_MMddyyyyhmmssa);  
		}catch(Exception e){
		}
		try{
			return LocalDateTime.parse(s, Utility.DATETIME_FORMATTER_yyyyMMddHHmmss);  
		}catch(Exception e){
		}
		throw new Exception("Cannot parse timestamp '" + s + "' using formatter " + Utility.DATETIME_FORMATTER_MMddyyyyhmmssa + " or " + Utility.DATETIME_FORMATTER_yyyyMMddHHmmss);
	}
	
	/**
	 * Get a query fragment for a time constraint, where the time column is a timestamp type (vs a double)
	 */
	public String getTimeConstraintQueryFragment_typeTimestamp(String timeLabel, TimeSeriesQueryFragmentBuilder queryFragmentBuilder) throws Exception{
		return getTimeConstraintQueryFragment(timeLabel, queryFragmentBuilder, false);
	}
	
	/**
	 * Get a query fragment for a time constraint, where the time column is a double (vs a timestamp)
	 */
	public String getTimeConstraintQueryFragment(String timeLabel, TimeSeriesQueryFragmentBuilder queryFragmentBuilder) throws Exception{
		return getTimeConstraintQueryFragment(timeLabel, queryFragmentBuilder, true);
	}
	
	/**
	 * Get a query fragment for a time constraint
	 * e.g. ((unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd hh:mm:ss') >= unix_timestamp('04/07/2016 2:00:00 AM','MM/dd/yyyy hh:mm:ss a')) AND (unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd hh:mm:ss') <= unix_timestamp('04/09/2016 4:00:00 AM','MM/dd/yyyy hh:mm:ss a')))
	 */
	public String getTimeConstraintQueryFragment(String timeLabel, TimeSeriesQueryFragmentBuilder queryFragmentBuilder, boolean typeDouble) throws Exception{
		String constraint = "";
		int pos = 0;
		for(TimeSeriesConstraintValue v : values){
			if(pos != 0) { 
				constraint += " " + queryFragmentBuilder.getFragmentForAnd() + " ";
			}
			LocalDateTime dateTime = parseDateTime(v.getValue()); 
			if(typeDouble){
				// time column is type double
				constraint += queryFragmentBuilder.getFragmentForTimeCondition(timeLabel, this.operators.get(pos), dateTime);
			}else{
				// time column is type timestamp
				constraint += queryFragmentBuilder.getFragmentForTimeCondition_typeTimestamp(timeLabel, this.operators.get(pos), dateTime);
			}
			pos += 1;
		}
		constraint = queryFragmentBuilder.encloseInParentheses(constraint);
		return constraint;
	}
	

	/**
	 * If this is a time constraint, get the duration in seconds if possible:
	 * 	 - if single equals operator, return 0 seconds 
	 *   - if lower/upper-bound (min/max) timestamps exist, then compute the seconds between them
	 *   - otherwise, throw an exception
	 */
	public long getTimeConstraintDurationSecs() throws Exception{
		
		// if single equals operator, return 0 seconds 
		if(this.getNumOperators() == 1 && this.operators.get(0).equals("=")){
			return 0;
		}
		
		// if lower/upper-bound (min/max) timestamps exist, then compute the seconds between them
		if(this.getNumOperators() != 2 || !this.hasLowerBound() || !this.hasUpperBound()){		
			throw new Exception("Cannot compute time constraint duration - requires a single equals operator or a pair of min/max operators");
		}
		// parse the timestamps
		LocalDateTime dateTime0, dateTime1;
		try{
			dateTime0 = parseDateTime(values.get(0).getValue());   
		}catch(Exception e){
			throw new Exception("Cannot parse as timestamp: " + values.get(0).getValue());
		}
		try{
			dateTime1 = parseDateTime(values.get(1).getValue());
		}catch(Exception e){
			throw new Exception("Cannot parse as timestamp: " + values.get(1).getValue());
		}
		// return the difference in seconds between the two timestamps
		return Math.abs(dateTime1.toEpochSecond(ZoneOffset.UTC) - dateTime0.toEpochSecond(ZoneOffset.UTC));
	}
	
	
	/**
	 * If this is a time constraint with a fixed duration, then get a list of the dates covered (e.g. 20141030, 20141031, 20141101)
	 */
	public ArrayList<String> getTimeConstraintDates() throws Exception{
		
		final DateTimeFormatter FORMATTER = Utility.DATE_FORMATTER_yyyyMMdd;
		ArrayList<String> ret = new ArrayList<String>();
		long durationSecs = getTimeConstraintDurationSecs();  // throws exception if this is not a valid time constraint with a fixed duration
		
		// single equals operator, return single date
		if(durationSecs == 0){
			ret.add(parseDateTime(values.get(0).getValue()).toLocalDate().format(FORMATTER));
			return ret;
		}
		
		// min/max operators
		
		LocalDate date0 = parseDateTime(values.get(0).getValue()).toLocalDate();
		LocalDate date1 = parseDateTime(values.get(1).getValue()).toLocalDate();
		
		// determine which is start/end
		LocalDate start, end;
		if(date0.isBefore(date1)){
			start = date0;
			end = date1;
		}else{
			start = date1;
			end = date0;
		}
		
		while (!start.isAfter(end)) {
			ret.add(start.format(FORMATTER));
		    start = start.plusDays(1);
		}
		return ret;
	}
	
	
	/**
	 * Get a query fragment for a non-time constraint
 	 * e.g. (`emnot2` > 50 AND `emnot2` < 60)
	 */
	public String getConstraintQueryFragment(HashMap<String, String> colNames, QueryFragmentBuilder queryFragmentBuilder) throws Exception{
		
		if(!(queryFragmentBuilder instanceof TimeSeriesQueryFragmentBuilder)){
			throw new Exception("An instance of TimeSeriesQueryFragmentBuilder is required");
		}
		
		// return the actual constraint based on the structure passed
		String constraint = "";
		
		String columnNameToUse = "";
		// get the column Name
		Set<String> keys = colNames.keySet();
		for(String k : keys){
			// find the correct value
			if(colNames.get(k).equalsIgnoreCase(this.variableName)){ 
				columnNameToUse = k;
				break;
			}
		}
		
		// if no column name found (e.g. this variable is not mapped), then use variable name (previously not checking for this, created invalid query with empty column name)
		if(columnNameToUse.isEmpty()){
			columnNameToUse = this.variableName;
		}
		
		int pos = 0;
		for(TimeSeriesConstraintValue v : values){
			if(pos != 0) { 
				constraint += " " + ((TimeSeriesQueryFragmentBuilder)queryFragmentBuilder).getFragmentForAnd() + " "; 
			}
			// check the type of the value. we have a specific action if it is a column. 
			String valueType = v.getType();
			String value     = v.getValue();
			if(valueType.equalsIgnoreCase("COLUMN")){
				// we must now get the real column name for this value. 
				for(String k : keys){
					// find the correct value
					if(colNames.get(k).equalsIgnoreCase(value)){ 
						value = k;
						break;
					}
				}
			}
			constraint += ((TimeSeriesQueryFragmentBuilder)queryFragmentBuilder).getFragmentForCondition(columnNameToUse, this.operators.get(pos), value); 
			pos += 1;
		}
		
		constraint = ((TimeSeriesQueryFragmentBuilder)queryFragmentBuilder).encloseInParentheses(constraint);
		return constraint;
	}
	
	
	public ArrayList<String> getConstrained(HashMap<String, String> colNames){
		ArrayList<String> columnNamesToUse = new ArrayList<String>();
		// get the column Name
		Set<String> keys = colNames.keySet();
		for(String k : keys){
			// find the correct value
			if(colNames.get(k).equalsIgnoreCase(this.variableName)){ 
				columnNamesToUse.add(k);
			}
		}
		return columnNamesToUse;
	}
	
	/**
	 * Populate a TimeSeriesConstraint object from json.
	 */
	@Override
	protected void processJson(JSONObject jobj) throws Exception {
		
		// make sure these are instantiated (will not hit instantiations above if using TimeSeriesConstraint(JSONObject) constructor)
		operators = new ArrayList<String>();
		values = new ArrayList<TimeSeriesConstraintValue>();
		
		// get the variable name
		this.variableName = (String) jobj.get("@var");
		
		// add the operators
		String op1 = (String) jobj.get("@operator1");
		String op2 = (String) jobj.get("@operator2");
		
		/* 
		 * 	TODO: EVENTUALLY, OPERATR CONSISTENCY SHOULD BE CHECKED. A SET LIKE "<VAR> > 16 AND <VAR> LIKE '122'"
		 * 	MAKES SOME SENSE BUT IS NOT A LOGICAL OR TYPICAL CONSTRAINT. WE MAY WANT TO SPECIFICALLY DIABLE SUCH COMBINATIONS.
		 */		 
		if(op1 != null){ 
			// add operator validity check
			this.operators.add(op1); }
		if(op2 != null){ 
			// add operator validity check
			this.operators.add(op2); }
		
		// get the operands
		JSONObject operand0 = (JSONObject) jobj.get("@value1");
		JSONObject operand1 = (JSONObject) jobj.get("@value2");
		
		if(operand0 != null){
			TimeSeriesConstraintValue tscv = new TimeSeriesConstraintValue((String) operand0.get("@value"), (String) operand0.get("@type"));
			this.values.add(tscv);
		}
		if(operand1 != null){
			TimeSeriesConstraintValue tscv = new TimeSeriesConstraintValue((String) operand1.get("@value"), (String) operand1.get("@type"));
			this.values.add(tscv);	
		}
		
	}

	/**
	 * To human readable string (for debug only)
	 */
	public String toString(){
		String s = "";
		for(int i = 0; i < operators.size(); i++){
			s +=  this.variableName + " " + operators.get(i) + " " + values.get(i).getValue() + " (" + values.get(i).getType() + "), ";
		}
		return s;
	}
	
}
