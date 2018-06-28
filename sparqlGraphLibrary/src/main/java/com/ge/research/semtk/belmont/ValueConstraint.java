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

public class ValueConstraint {

	protected String constraint = "";
	
	public ValueConstraint() {}
	
	public ValueConstraint(String vc){
		this.constraint = vc;
	}
	
	public String getConstraint(){
		return this.constraint;
	}
	
	public void changeSparqlID(String oldID, String newID) {
		if (this.constraint != null && oldID != null && newID != null) {
			this.constraint = this.constraint.replaceAll("\\" + oldID + "\\b", newID);
		}
	}
	
	/** 
	 * to non-null string
	 */
	public String toString() {
		return this.constraint == null ? "" : this.constraint;
	}
	
	public static String buildValuesConstraint(Returnable item, ArrayList<String> valList) throws Exception {
		// build a value constraint for an "item" (see item interface comment)
		
		if (item.getSparqlID().isEmpty()) {
			throw new Error("Trying to build VALUES constraint for property with empty sparql ID");
		}

		StringBuffer ret = new StringBuffer();
		if (valList.size() > 0) {
			XSDSupportedType t = item.getValueType();
			ret.append("VALUES " + item.getSparqlID() + " {");
			
			for (int i=0; i < valList.size(); i++) {
				if (t == XSDSupportedType.NODE_URI) {
					ret.append(" <" + valList.get(i) + ">");
				} else {
					ret.append(" '" + BelmontUtil.sparqlSafe(valList.get(i)) + "'^^" + item.getValueType().getPrefixedName());
				}
			}
			
			ret.append(" } ");
		}
		return ret.toString();
	}
	
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
	
}
