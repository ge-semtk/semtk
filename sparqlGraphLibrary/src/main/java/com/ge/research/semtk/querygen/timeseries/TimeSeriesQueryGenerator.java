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


import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.querygen.QueryGenerator;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;


/**
 * Generate queries to retrieve time series data from a relational database.
 */
public abstract class TimeSeriesQueryGenerator extends QueryGenerator {
	
	protected Table locationAndValueInfo;  // db, table, tscol, vars (from EDC value), UUID (for semantic binning)
	protected ArrayList<TimeSeriesConstraint> constraints;	// constraints on values
	protected String constraintsConjunction; 				// AND or OR to apply amongst the non-time constraints
	protected TimeSeriesConstraint timeConstraint;			// constraint on timestamp (this will get ANDed with the other constraints)
	
	
	/**
	 * Constructor with no flags
	 */
	public TimeSeriesQueryGenerator(Table locationAndValueInfo, JSONObject externalConstraintsJson) throws Exception{
		this(locationAndValueInfo, externalConstraintsJson, null);
	}
	
	/**
	 * Constructor with flags
	 */
	public TimeSeriesQueryGenerator(Table locationAndValueInfo, JSONObject externalConstraintsJson, QueryFlags flags) throws Exception{
		this.locationAndValueInfo = locationAndValueInfo;
		parseConstraints(externalConstraintsJson);  // if constraintsJson null, will do nothing
		this.flags = flags;
	}
	
	public ArrayList<TimeSeriesConstraint> getConstraints(){
		return constraints;
	}
	
	public TimeSeriesConstraint getTimeConstraint(){
		return timeConstraint;
	}
	
	/**
	 * Parse constraint JSON into TimeSeriesConstraint objects
	 * @param constrainJson
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void parseConstraints(JSONObject constraintJson) throws Exception{
		
		try{
			if(constraintJson != null){		
				
				// validate JSON 
				for(String k : (String[])(constraintJson.keySet().toArray(new String[constraintJson.keySet().size()]))){
					if(!k.equals("@constraintSet") && !k.equals("@timeConstraint")){
						throw new Exception("Constraint JSON may be malformed: contains entries other than @constraintSet or @timeConstraint");
					}
				}
								
				// split out the "@constraintSet" and "@timeConstraint" from the block
				JSONObject constraintSetJson  = (JSONObject) constraintJson.get("@constraintSet");				
				JSONObject timeConstraintJson = (JSONObject) constraintJson.get("@timeConstraint");
				
				// parse value constraints
				constraints = new ArrayList<TimeSeriesConstraint>();
				if(constraintSetJson != null){
				
					// get the "@op" which defines how the constraints are combined					
					String op = (String) constraintSetJson.get("@op"); 
					if(op == null){
						throw new Exception("Constraint JSON has no @op entry");
					}
					if(!(op.equalsIgnoreCase("and") || op.equalsIgnoreCase("or"))){
						throw new Exception("Constraint JSON has invalid @op entry: must be AND or OR (case insensitive)");
					}
					constraintsConjunction = op;
					
					// get the "@constraints" array from the "@constraintSet"
					JSONArray constraintArr = (JSONArray) constraintSetJson.get("@constraints");					
					// build constraint objects
					if(constraintArr != null){
						for(int i = 0; i < constraintArr.size(); i += 1){
							TimeSeriesConstraint curr = new TimeSeriesConstraint((JSONObject) constraintArr.get(i));
							constraints.add(curr);
						}
					}
				}				
				// TODO validate the constraints, e.g. column-level dependency between time series tuple results
				
				// parse time constraint
				if(timeConstraintJson != null){
					timeConstraint = new TimeSeriesConstraint(timeConstraintJson);	
				}				
				
			}
		}
		catch(Exception e){
			throw new Exception("Error processing constraints: " + e);			
		}
		
	}

}

