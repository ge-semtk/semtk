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

import com.ge.research.semtk.sparqlX.SparqlResultTypes;

/**
 * Types of auto-generated queries
 */
public enum AutoGeneratedQueryTypes {
	SELECT_DISTINCT,  
	FILTER_CONSTRAINT,
	COUNT,
	CONSTRUCT,
	ASK,
	DELETE;
		
	/**
	 * More lenient lookup: adds QUERY_ and uppercases.
	 * (Couldn't figure out how to override valueOf()
	 * @param s
	 * @return
	 * @throws Exception
	 */
	public static AutoGeneratedQueryTypes getMatchingValue(String s) throws Exception {
		if (s == null) 
			throw new Exception("Query type is null.");
		
		String lookup = s.toUpperCase();
		
		return AutoGeneratedQueryTypes.valueOf(lookup);
	}
	
	public SparqlResultTypes getDefaultResultType() {
		switch (this) {
		case SELECT_DISTINCT:
		case FILTER_CONSTRAINT:  
		case COUNT:  
		case ASK:  
			return SparqlResultTypes.TABLE;
		case DELETE:
			return SparqlResultTypes.CONFIRM;
		case CONSTRUCT:  
			return SparqlResultTypes.GRAPH_JSONLD;
		default:
			return SparqlResultTypes.TABLE;
		}
	}
	
	public void throwExceptionIfIncompatible(SparqlResultTypes rt) throws Exception {
		switch (this) {
		case SELECT_DISTINCT:
		case FILTER_CONSTRAINT:  
		case COUNT:  
		case ASK:  
			if (rt != SparqlResultTypes.TABLE) 
				throw new Exception ("Only return type TABLE is available for query type: " + this.toString());
			break;
		case DELETE:
			if (rt != SparqlResultTypes.CONFIRM) 
				throw new Exception ("Only return type CONFIRM is available for query type: " + this.toString());
			break;
		case CONSTRUCT:  
			if (rt != SparqlResultTypes.GRAPH_JSONLD && rt != SparqlResultTypes.RDF) 
				throw new Exception ("Only return types GRAPH_JSONLD and RDF are available for query type: " + this.toString());
			break;
		}
	}
	
//	public static String[] allOf() {
//		
//		String [] ret = null;
//		EnumSet.allOf(AutoGeneratedQueryTypes.class).toArray(ret);
//		return ret;
//		
//	}
}

// where is DELETE ?