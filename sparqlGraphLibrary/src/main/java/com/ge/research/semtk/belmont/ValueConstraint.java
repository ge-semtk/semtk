/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.belmont;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValueConstraint {
	// list of strings representing constraint clauses.
	protected String constraint = null;
	
	public ValueConstraint() {}
	
	public ValueConstraint(String vc){
		this.constraint = vc;
	}
	
	/**
	 * Search and replace sparqlId
	 * @param oldID
	 * @param newID
	 */
	public void changeSparqlID(String oldID, String newID) {
		if (this.constraint != null && oldID != null && newID != null) {
			
				this.constraint = this.constraint.replaceAll("\\" + oldID + "\\b", newID);
		}
	}
	
	/**
	 * Brute force removal of any constraint that contains a given variable name
	 * @param id
	 */
	public void removeReferencesToVar(String id) {
		if (constraint == null)
			return;
		
		String regex = "";
		final String ending = "[^-_A-Za-z0-9]";
		
		if (id.startsWith("?")) {
			regex = "\\" +  id + ending;
		} else {
			regex = "\\?" + id + ending;
		}
		
		Matcher m = Pattern.compile(regex).matcher(this.constraint);
		if (m.find()) {
			this.constraint = null;
		}
	}
	
	/** 
	 * to non-null string, possibly multiple clauses separated by "."
	 */
	public String toString() {
		return this.constraint == null ? "" : this.constraint;
	}
	
	public void addConstraint(String vc) {
		if (this.constraint != null) {
			this.constraint += (" . " + vc);
		}
	}
	
	//
	//  Original ValueConstraint functions ported from javascript for use with NodegGroup items
	//
	//
	
	public static String buildValuesConstraint(Returnable item, String val) throws Exception {
		ArrayList<String> valList = new ArrayList<String>();
		valList.add(val);
		return buildValuesConstraint(item, valList);
	}

	/**
	 * Build VALUES clause
	 * @param item
	 * @param valList - list of items does not get "parsed"
	 * @return
	 * @throws Exception
	 */
	public static String buildValuesConstraint(Returnable item, ArrayList<String> valList) throws Exception {
		// build a value constraint for an "item" (see item interface comment)
		
		String sparqlID = item.getSparqlID();
		if (sparqlID.isEmpty()) {
			throw new Error("Trying to build VALUES constraint for property with empty sparql ID");
		}

		return buildValuesConstraint(sparqlID, valList, item.getValueType());
	}
	
	/**
	 * Build a FILTER clause with one operation
	 * @param item
	 * @param oper
	 * @param pred
	 * @return
	 * @throws Exception
	 */
	public static String buildFilterConstraint(Returnable item, String oper, String pred)  throws Exception {
		String ret;
		
		if (!(	oper.equals("=") || 
				oper.equals("!=") ||
				oper.equals(">") ||
				oper.equals(">=") ||
				oper.equals("<") ||
				oper.equals("<=")) ) {
			throw new Exception("Unknown operator for constraint: " + oper);
		}
		
		XSDSupportedType t = item.getValueType();
		

		String v = BelmontUtil.sparqlSafe(pred);
		if (t.dateOperationAvailable()) {
			// date
			ret = String.format("FILTER(%s %s '%s'%s)", item.getSparqlID(), oper, v, t.getXsdSparqlTrailer());
		} else if (t.regexIsAvailable()) {
			// string
			ret = String.format("FILTER(%s %s \"%s\"%s)", item.getSparqlID(), oper, v, t.getXsdSparqlTrailer());
		} else if (t == XSDSupportedType.NODE_URI) {
			// URI
			ret = String.format("FILTER(%s %s <%s>)", item.getSparqlID(), oper, v);
		} else 	{
			// leftovers
			ret = String.format("FILTER(%s %s %s%s)", item.getSparqlID(), oper, v, t.getXsdSparqlTrailer());
		} 
		return ret;
	}
	
	public static String buildFilterConstraintWithVariable(Returnable item, String oper, String variable)  throws Exception {
		String ret;
		
		if (!(	oper.equals("=") || 
				oper.equals("!=") ||
				oper.equals(">") ||
				oper.equals(">=") ||
				oper.equals("<") ||
				oper.equals("<=")) ) {
			throw new Exception("Unknown operator for constraint: " + oper);
		}
		
		XSDSupportedType t = item.getValueType();
		
		return String.format("FILTER(%s %s %s)", item.getSparqlID(), oper, (variable.startsWith("?") ? "" : "?") + variable);
	}
	
	/**
	 * Build filter clause for dates only
	 * @param item
	 * @param oper
	 * @param pred
	 * @return
	 * @throws Exception
	 */
	public static String buildFilterConstraint(Returnable item, String oper, Date pred)  throws Exception {
		String ret;
		
		if (!(	oper.equals("=") || 
				oper.equals("!=") ||
				oper.equals(">") ||
				oper.equals(">=") ||
				oper.equals("<") ||
				oper.equals("<=")) ) {
			throw new Exception("Unknown operator for constraint: " + oper);
		}
		
		XSDSupportedType t = item.getValueType();
		

		String dateStr = BelmontUtil.buildSparqlDate(pred);
		if (t.dateOperationAvailable()) {
			// date
			ret = String.format("FILTER(%s %s '%s'%s)", item.getSparqlID(), oper, dateStr, t.getXsdSparqlTrailer());
		} else {
			throw new Exception("Can't apply date value constraint to non-date item: " + item.getSparqlID());
		} 
		return ret;
	}
	
	//
	// Originally only RuntimeConstraint functions
	// These build value constraints using sparqlID and valType instead of a Returnable
	// These could be reconciled with above, but at some risk and little value. -PEC 7/2018
	// 
	
	/**
	 * Build a VALUES clause
	 * @param sparqlId
	 * @param valList
	 * @param valType
	 * @return
	 * @throws Exception
	 */
	public static String buildValuesConstraint(String sparqlId, ArrayList<String> valList, XSDSupportedType valType) throws Exception{
		
		
		// VALUES ?trNum { '1278'^^<http://www.w3.org/2001/XMLSchema#int> '1279'^^<http://www.w3.org/2001/XMLSchema#int> } 
	
		StringBuffer retval = new StringBuffer();
		retval.append("VALUES " + sparqlId + " { ");
		// go through each passed value and add them.
		for(String v : valList){
			v = BelmontUtil.sparqlSafe(v);
			valType.validate(v);
			retval.append(valType.buildRDF11ValueString(v) + " ");
			
			// for strings only:  SemTK ingestion backwards compatibility:  search for "string" and "string"^^XMLSchema:string
			if (valType == XSDSupportedType.STRING) {
				retval.append(valType.buildTypedValueString(v) + " ");   
			}
		}
		
		retval.append(" }");
		
		return retval.toString();
	}
	
	/**
	 * Build a FILTER REGEX
	 * @param sparqlId
	 * @param regexp
	 * @param valType
	 * @return
	 * @throws Exception
	 */
	public static String buildRegexConstraint(String sparqlId, String regexp, XSDSupportedType valType) throws Exception{
		
		String retval = "";
		
		valType.validate(regexp);
		
		if(valType.regexIsAvailable()){
			retval = "FILTER regex(" + sparqlId + " ,\"" + regexp + "\")";
		}
		else{
			// regex is not considered supported here.
			throw new Exception("requested type (" + valType + ") for the sparqlId (" + sparqlId + ") does not support regex constraints");
		}
		return retval;
	}
	
	/**
	 * Build a FILTER greater than
	 * @param sparqlId
	 * @param val
	 * @param valType
	 * @param greaterOrEqual
	 * @return
	 * @throws Exception
	 */
	public static String buildGreaterThanConstraint(String sparqlId, String val, XSDSupportedType valType, boolean greaterOrEqual) throws Exception{
		valType.validate(val);
		String retval = "";
		
		if(valType.rangeOperationsAvailable()) {
		
			if(!greaterOrEqual){
				retval = "FILTER (" + sparqlId + " > " + valType.buildRDF11ValueString(val) + ")";
			}
			else{
				retval = "FILTER (" + sparqlId + " >= " + valType.buildRDF11ValueString(val) + ")";
			}
		}
		else{
			throw new Exception("requested type (" + valType + ") for the sparqlId (" + sparqlId + ") does not support range operation constraints");
		}
		return retval;
	}
	
	/** 
	 * Build a FILTER less than
	 * @param sparqlId
	 * @param val
	 * @param valType
	 * @param lessOrEqual
	 * @return
	 * @throws Exception
	 */
	public static String buildLessThanConstraint(String sparqlId, String val, XSDSupportedType valType, boolean lessOrEqual) throws Exception{
		valType.validate(val);

		String retval = "";
		
		if(valType.rangeOperationsAvailable()) {
			if(!lessOrEqual){
				retval = "FILTER (" + sparqlId + " < " + valType.buildRDF11ValueString(val) + ")";
			}
			else{
				retval = "FILTER (" + sparqlId + " <= " + valType.buildRDF11ValueString(val) + ")";
			}
		}
		else{
			throw new Exception("requested type (" + valType + ") for the sparqlId (" + sparqlId + ") does not support range operation constraints");
		}
		return retval;
	}
	
	/**
	 * Build a FILTER y1 < ?x < y2
	 * @param sparqlId
	 * @param valLow
	 * @param valHigh
	 * @param valType
	 * @param greaterOrEqual - inclusive flag for >
	 * @param lessThanOrEqual - inclusive flag for <
	 * @return
	 * @throws Exception
	 */
	public static String buildRangeConstraint(String sparqlId, String valLow, String valHigh, XSDSupportedType valType, boolean greaterOrEqual, boolean lessThanOrEqual) throws Exception{
		valType.validate(valHigh);
		valType.validate(valLow);

		String retval = "";
		
		if(valType.rangeOperationsAvailable()) {
			
			StringBuilder ret = new StringBuilder("FILTER (");
			
			if(greaterOrEqual){
				ret.append(" " + sparqlId + " >= " + valType.buildRDF11ValueString(valLow) + " ");
			}
			else{
				ret.append(" " + sparqlId + " > " + valType.buildRDF11ValueString(valLow) + " ");
			}
			
			// add a conjunction
			ret.append(" && ");
			
			if(lessThanOrEqual){
				ret.append(" " + sparqlId + " <= " + valType.buildRDF11ValueString(valHigh) + " ");
			}
			else{
				ret.append(" " + sparqlId + " < " + valType.buildRDF11ValueString(valHigh) + " ");
			}
			ret.append(")");
			retval = ret.toString();
		}
		else{
			throw new Exception("requested type (" + valType + ") for the sparqlId (" + sparqlId + ") does not support range operation constraints");
		}
		return retval;
	}
}
