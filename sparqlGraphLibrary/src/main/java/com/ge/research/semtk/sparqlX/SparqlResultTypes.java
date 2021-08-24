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


package com.ge.research.semtk.sparqlX;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Enumeration indicating expected result type from a SPARQL query.
 */
public enum SparqlResultTypes {
	TABLE,			// expect tabular results (e.g. for select queries)
	GRAPH_JSONLD,  	// expect graph results (e.g. for construct queries)
	// GRAPH_TURTLE	// in the future may support multiple result types for a graph
	CONFIRM, 		// expect a confirmation message (e.g. "inserted 5 tuples", "cleared graph")
	RDF,            // 
	HTML;           // e.g. uploading owl to strange endpoint
	/**
	 * Determines if a query is a DROP GRAPH query or not.
	 * e.g. clear graph <http://com.ge.research/knowledge/graph>
	 */
	// TODO move, does not really belong here any more
	public static boolean isDropGraphQuery(String query){
		return containsRegexIgnoreCase(query, "drop\\s+graph\\s+\\<");
	}


	/**
	 * Determine if a string contains a regular expression
	 */
	// TODO move, does not really belong here any more
	public static boolean containsRegexIgnoreCase(String s, String regex){
		s = s.toLowerCase().trim();		
		Pattern whitespace = Pattern.compile(regex);    
		Matcher matcher = whitespace.matcher(s);
		if (matcher.find()) {
			return true;
		}
		return false;
	}		

}
