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
			String t = item.getValueType();
			ret.append("VALUES " + item.getSparqlID() + " {");
			
			for (int i=0; i < valList.size(); i++) {
				if (XSDSupportedTypes.getMatchingName(t).equals("NODE_URI")) {
					ret.append(" <" + valList.get(i) + ">");
				} else {
					ret.append(" '" + valList.get(i) + "'^^" + XSDSupportUtil.getPrefixedName(item.getValueType()));
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
		
		String t = item.getValueType();
		
		if (!XSDSupportUtil.supportedType(t)) {
			throw new Exception("Unknown type for constraint: " + t);
		}
		
		if (XSDSupportUtil.dateOperationAvailable(t)) {
			ret = String.format("FILTER(%s %s '%s'%s)", item.getSparqlID(), oper, pred, XSDSupportUtil.getXsdSparqlTrailer(t));
		} else if (XSDSupportUtil.regexIsAvailable(t)) {
			ret = String.format("FILTER(%s %s \"%s\"%s)", item.getSparqlID(), oper, pred, XSDSupportUtil.getXsdSparqlTrailer(t));
		} else if (XSDSupportedTypes.getMatchingName(t).equals("NODE_URI")) {
			ret = String.format("FILTER(%s %s <%s>)", item.getSparqlID(), oper, pred);
		} else 	{
			ret = String.format("FILTER(%s %s %s%s)", item.getSparqlID(), oper, pred, XSDSupportUtil.getXsdSparqlTrailer(t));
		} 
		return ret;
	}
	
}
