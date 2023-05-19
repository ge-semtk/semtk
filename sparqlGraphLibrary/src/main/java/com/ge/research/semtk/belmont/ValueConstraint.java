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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.sparql.lang.SPARQLParserFactory;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.utility.Utility;
import com.github.jsonldjava.shaded.com.google.common.base.Strings;

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
	
	
	
	public static String buildBestListConstraint(Returnable item, String val, SparqlEndpointInterface sei) throws Exception {
		ArrayList<String> valList = new ArrayList<String>();
		valList.add(val);
		return buildBestListConstraint(item, valList, sei);
	}
	
	/**
	 * Return the most performant list-matching constraint.  Based on Jan 2021 Testing
	 * @param item
	 * @param valList
	 * @param sei
	 * @return
	 * @throws Exception
	 */
	public static String buildBestListConstraint(Returnable item, ArrayList<String> valList, SparqlEndpointInterface sei) throws Exception {
		switch (sei.getServerType()) {
		case SparqlEndpointInterface.NEPTUNE_SERVER:
			return buildFilterInConstraint(item, valList, sei);
			
		case SparqlEndpointInterface.BLAZEGRAPH_SERVER:
		case SparqlEndpointInterface.FUSEKI_SERVER:
		case SparqlEndpointInterface.VIRTUOSO_SERVER:
		default:
			return buildValuesConstraint(item, valList, sei);
		}
	}
	
	public static String buildBestListConstraint(String sparqlId, AbstractCollection<String> valList, AbstractCollection<XSDSupportedType> valTypes, SparqlEndpointInterface sei) throws Exception {
		switch (sei.getServerType()) {
		case SparqlEndpointInterface.NEPTUNE_SERVER:
			return buildFilterInConstraint(sparqlId, valList, valTypes, sei);
			
		case SparqlEndpointInterface.BLAZEGRAPH_SERVER:
		case SparqlEndpointInterface.FUSEKI_SERVER:
		case SparqlEndpointInterface.VIRTUOSO_SERVER:
		default:
			return buildValuesConstraint(sparqlId, valList, valTypes, sei);
		}
	}
	
	/**
	 * Special case of best list.  Blazegraph like subclassof* better than VALUES { }
	 * @param sparqlId
	 * @param className
	 * @param subclassList
	 * @param sei
	 * @return
	 * @throws Exception
	 */
	public static String buildBestSubclassConstraint(String sparqlId, String className, ArrayList<String> subclassList, SparqlEndpointInterface sei) throws Exception {
		switch (sei.getServerType()) {
		case SparqlEndpointInterface.BLAZEGRAPH_SERVER:
			return sparqlId + " rdfs:subClassOf* " +  className;
					
		case SparqlEndpointInterface.NEPTUNE_SERVER:
		case SparqlEndpointInterface.FUSEKI_SERVER:
		case SparqlEndpointInterface.VIRTUOSO_SERVER:
		default:
			ArrayList<String> classList = new ArrayList<String>();
			classList.add(className);
			classList.addAll(subclassList);
			HashSet<XSDSupportedType> valTypes = new HashSet<XSDSupportedType>();
			valTypes.add(XSDSupportedType.NODE_URI);
			return buildValuesConstraint(sparqlId, classList, valTypes, sei);
		}
	}
	
	//
	//  Original ValueConstraint functions ported from javascript for use with NodegGroup items
	//
	//
	
	//  Use sei version to get optimization (avoid de-optimized virtuoso version)
	@Deprecated
	public static String buildValuesConstraint(Returnable item, String val) throws Exception {
		return buildValuesConstraint(item, val, null);
	}
	public static String buildValuesConstraint(Returnable item, String val, SparqlEndpointInterface sei) throws Exception {
		ArrayList<String> valList = new ArrayList<String>();
		valList.add(val);
		return buildValuesConstraint(item, valList, sei);
	}

	/**
	 * Build VALUES clause
	 * @param item
	 * @param valList - list of items does not get "parsed"
	 * @return
	 * @throws Exception
	 */

	//  Use sei version to get optimization (avoid de-optimized virtuoso version)
	@Deprecated
	public static String buildValuesConstraint(Returnable item, ArrayList<String> valList) throws Exception {
		return buildValuesConstraint(item, valList, null);
	}
	public static String buildValuesConstraint(Returnable item, ArrayList<String> valList, SparqlEndpointInterface sei) throws Exception {

		// build a value constraint for an "item" (see item interface comment)
		
		String sparqlID = item.getSparqlID();
		if (sparqlID.isEmpty()) {
			throw new Error("Trying to build VALUES constraint for property with empty sparql ID");
		}

		return buildValuesConstraint(sparqlID, valList, item.getValueTypes(), sei);
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
		String ret = "";
		
		if (!(	oper.equals("=") || 
				oper.equals("!=") ||
				oper.equals(">") ||
				oper.equals(">=") ||
				oper.equals("<") ||
				oper.equals("<=")) ) {
			throw new Exception("Unknown operator for constraint: " + oper);
		}
				
		// return first thing that works
		for (XSDSupportedType t : item.getValueTypes()) {
			String v = t.buildRDF11ValueString(pred);
			return String.format("FILTER(%s %s %s)", item.getSparqlID(), oper, v);
			
		}
		throw new Exception("Internal error: item has no valueType: " + item.getBindingOrSparqlID());

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
		
		
		String dateStr = BelmontUtil.buildSparqlDate(pred);
		for (XSDSupportedType t : item.getValueTypes()) {
			if (t.dateOperationAvailable()) {
				// date
				return String.format("FILTER(%s %s '%s'%s)", item.getSparqlID(), oper, dateStr, t.getXsdSparqlTrailer());
			} 
		}
		
		throw new Exception("Can't apply date value constraint to non-date item: " + item.getSparqlID());
		
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
	//  Use sei version to get optimization (avoid de-optimized virtuoso version)
	@Deprecated
	public static String buildValuesConstraint(String sparqlId, ArrayList<String> valList, HashSet<XSDSupportedType> valTypes) throws Exception{
		return buildValuesConstraint(sparqlId, valList, valTypes, null);
	}

	public static String buildValuesConstraint(String sparqlId, AbstractCollection<String> valList, AbstractCollection<XSDSupportedType> valTypes, SparqlEndpointInterface sei) throws Exception{
		
		sparqlId = BelmontUtil.legalizeSparqlID(sparqlId);
		
		// VALUES ?trNum { '1278'^^<http://www.w3.org/2001/XMLSchema#int> '1279'^^<http://www.w3.org/2001/XMLSchema#int> } 
	
		StringBuffer retval = new StringBuffer();
		retval.append("VALUES " + sparqlId + " { ");
		// go through each passed value and add them.
		for(String v : valList){
			for (XSDSupportedType valType : valTypes) {
				try {
					valType.validate(v);
					retval.append(valType.buildRDF11ValueString(v) + " ");
					
					// for strings and numbers:  
					// SemTK ingestion backwards compatibility:  search for "string" and "string"^^XMLSchema:string
					if (sei == null || sei.getServerType() == SparqlEndpointInterface.VIRTUOSO_SERVER ) {
						if (valType == XSDSupportedType.STRING ||
							valType.numericOperationAvailable()) {
							retval.append(valType.buildTypedValueString(v) + " ");   
						}
					}
					// break after first matching type
					break;
				} catch (Exception e) {
					// try next type
				}
			}
		}
		
		retval.append(" }");
		
		return retval.toString();
	}
	
	/**
	 * This version takes all values at face value: no typing is done
	 * @param sparqlId
	 * @param valList
	 * @return
	 * @throws Exception
	 */
	public static String buildValuesConstraint(String sparqlId, ArrayList<String> valList) throws Exception{
		
		sparqlId = BelmontUtil.legalizeSparqlID(sparqlId);
		
		// VALUES ?trNum { '1278'^^<http://www.w3.org/2001/XMLSchema#int> '1279'^^<http://www.w3.org/2001/XMLSchema#int> } 
	
		StringBuffer retval = new StringBuffer();
		retval.append("VALUES " + sparqlId + " { ");
		for(String v : valList){
			retval.append(v + " ");
		}
		
		retval.append(" }");
		
		return retval.toString();
	}
	
	/**
	 * Build a FILTER IN clause
	 * @param sparqlId
	 * @param valList
	 * @param valType
	 * @return
	 * @throws Exception
	 */
	//  Use sei version to get optimization (avoid de-optimized virtuoso version)
	public static String buildFilterInConstraint(String sparqlId, ArrayList<String> valList, HashSet<XSDSupportedType> valTypes) throws Exception{
		return buildFilterInConstraint(sparqlId, valList, valTypes, null);
	}

	public static String buildFilterInConstraint(String sparqlId, AbstractCollection<String> valList, AbstractCollection<XSDSupportedType> valTypes, SparqlEndpointInterface sei) throws Exception{
		sparqlId = BelmontUtil.legalizeSparqlID(sparqlId);
		
		
		// FILTER (?trNum IN ( 1278, 1279 )) 
		
		ArrayList<String> list = new ArrayList<String>();
		// go through each passed value and add them.
		for(String v : valList){
			for (XSDSupportedType valType : valTypes) {
				try {
					valType.validate(v);
					list.add(valType.buildRDF11ValueString(v));
					
					// for strings only:  
					// SemTK ingestion backwards compatibility:  search for "string" and "string"^^XMLSchema:string
					// TODO: remove this some day
					if (sei == null || sei.getServerType() == SparqlEndpointInterface.VIRTUOSO_SERVER ) {
						if (valType == XSDSupportedType.STRING ) {
							list.add(valType.buildTypedValueString(v));   
						}
					}
					// break after first success
					break;
				} catch (Exception e) {
					// try next type
				}
			}
		}
		
		return "FILTER (" + sparqlId + " IN ( " + String.join(", ", list) + ")) ";
	}

	
	/**
	 * Build a FILTER IN clause
	 * @param sparqlId
	 * @param valList
	 * @param valType
	 * @return
	 * @throws Exception
	 */
	//  Use sei version to get optimization (avoid de-optimized virtuoso version)
	@Deprecated
	public static String buildFilterInConstraint(String sparqlId, String val, HashSet<XSDSupportedType> valTypes) throws Exception{
		return buildFilterInConstraint(sparqlId, val, valTypes, null);
	}
	public static String buildFilterInConstraint(String sparqlId, String val, HashSet<XSDSupportedType> valTypes, SparqlEndpointInterface sei) throws Exception{
		
		
		// FILTER (?trNum IN ( 1278 )) 
		
		ArrayList<String> list = new ArrayList<String>();
		list.add(val);
		return ValueConstraint.buildFilterInConstraint(sparqlId, list, valTypes, sei);
		
	}
	
	//  Use sei version to get optimization (avoid de-optimized virtuoso version)
	@Deprecated
	public static String buildFilterInConstraint(Returnable item, ArrayList<String> valList) throws Exception{
		return buildFilterInConstraint(item, valList, null);
	}
	
	public static String buildFilterInConstraint(Returnable item, ArrayList<String> valList, SparqlEndpointInterface sei) throws Exception{

		return ValueConstraint.buildFilterInConstraint(item.getSparqlID(), valList, item.getValueTypes(), sei);

	}
	
	//  Use sei version to get optimization (avoid de-optimized virtuoso version)
	public static String buildFilterInConstraint(Returnable item, String val) throws Exception{
		return buildFilterInConstraint(item, val, null);
	}
	
	public static String buildFilterInConstraint(Returnable item, String val, SparqlEndpointInterface sei) throws Exception{

		ArrayList<String> list = new ArrayList<String>();
		list.add(val);
		return ValueConstraint.buildFilterInConstraint(item.getSparqlID(), list, item.getValueTypes(), sei);

	}
	
	public static String buildFilterNotInConstraint(String sparqlId, ArrayList<String> valList, HashSet<XSDSupportedType> valTypes) throws Exception{
		sparqlId = BelmontUtil.legalizeSparqlID(sparqlId);
				
		ArrayList<String> list = new ArrayList<String>();
		// go through each passed value and add them.
		for(String v : valList){		
			for (XSDSupportedType valType : valTypes) {
				try {
					valType.validate(v);
					list.add(valType.buildRDF11ValueString(v));
					break;
				} catch (Exception e) {
					// try next one
				}
			}
		}
		
		return "FILTER (" + sparqlId + " NOT IN ( " + String.join(", ", list) + ")) ";
	}
	
	/**
	 * Build a FILTER REGEX
	 * @param sparqlId
	 * @param regexp
	 * @param valType
	 * @return
	 * @throws Exception
	 */
	public static String buildRegexConstraint(String sparqlId, String regexp, HashSet<XSDSupportedType> valTypes) throws Exception{
		sparqlId = BelmontUtil.legalizeSparqlID(sparqlId);
		
		String retval = "";
		
		for (XSDSupportedType t :valTypes) {
			try {
				t.validate(regexp);
		
				if(t.regexIsAvailable()){
					return "FILTER regex(" + sparqlId + " ,\"" + regexp + "\")";
				}
			} catch (Exception e) {
				// try next one
			}
		}
		
		// regex is not considered supported here.
		throw new Exception("requested type (" + XSDSupportedType.buildTypeListString(valTypes) + ") for the sparqlId (" + sparqlId + ") does not support regex constraints");
		
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
	public static String buildGreaterThanConstraint(String sparqlId, String val, HashSet<XSDSupportedType> valTypes, boolean greaterOrEqual) throws Exception{
		sparqlId = BelmontUtil.legalizeSparqlID(sparqlId);
		for (XSDSupportedType valType :valTypes) {
			try {
				valType.validate(val);
			
		
				if (valType.rangeOperationsAvailable()) {
				
					if(!greaterOrEqual){
						return "FILTER (" + sparqlId + " > " + valType.buildRDF11ValueString(val) + ")";
					}
					else{
						return "FILTER (" + sparqlId + " >= " + valType.buildRDF11ValueString(val) + ")";
					}
				}
			} catch (Exception e) {
				// try next one
			}
		}
		
		throw new Exception("requested type (" + XSDSupportedType.buildTypeListString(valTypes) + ") for the sparqlId (" + sparqlId + ") does not support range operation constraints");
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
	public static String buildLessThanConstraint(String sparqlId, String val, HashSet<XSDSupportedType> valTypes, boolean lessOrEqual) throws Exception{
		sparqlId = BelmontUtil.legalizeSparqlID(sparqlId);
		
		for (XSDSupportedType valType :valTypes) {
			try {
				valType.validate(val);
			
				if (valType.rangeOperationsAvailable()) {
					if(!lessOrEqual){
						return "FILTER (" + sparqlId + " < " + valType.buildRDF11ValueString(val) + ")";
					}
					else{
						return "FILTER (" + sparqlId + " <= " + valType.buildRDF11ValueString(val) + ")";
					}
				}
			} catch (Exception e) {
				// try next one
			}
		}
				
		throw new Exception("requested type (" + XSDSupportedType.buildTypeListString(valTypes) + ") for the sparqlId (" + sparqlId + ") does not support range operation constraints");
		
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
	public static String buildRangeConstraint(String sparqlId, String valLow, String valHigh, HashSet<XSDSupportedType> valTypes, boolean greaterOrEqual, boolean lessThanOrEqual) throws Exception{
		sparqlId = BelmontUtil.legalizeSparqlID(sparqlId);
		String retval = "";
		
		for (XSDSupportedType valType : valTypes) {
			
			try {
				valType.validate(valHigh);
				valType.validate(valLow);
		
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
					return ret.toString();
				}
			} catch (Exception e) {
				// try next type
			}
		}
		
		throw new Exception("requested type (" + XSDSupportedType.buildTypeListString(valTypes) + ") for the sparqlId (" + sparqlId + ") does not support range operation constraints");
		
	}
	
	
}
