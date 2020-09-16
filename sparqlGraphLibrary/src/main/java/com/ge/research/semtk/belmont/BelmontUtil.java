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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class BelmontUtil {
	private final static String ILLEGAL_CHAR = "[^A-Za-z_0-9]";
	
	public static String buildSparqlDate(Date d) {
		DateFormat xsdFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return xsdFormat.format(d);
	}
	public static String generateSparqlID(String proposedName, HashSet<String> reservedNameHash){
		// accepts a suggested sparqlID and outputs either that ID or one based off it.
		String retval = proposedName; 	// assume the proposed name is fine. 
		
		if(retval.startsWith("?")){
			retval = retval.substring(1);	// remove that first character.
		}
		// remove known prefixes. this is done for backward compatibility's sake 
		// and should not be encountered often in practice. 
		if(retval.startsWith("has")){
			retval = retval.substring(3);
		}
		if(retval.startsWith("is")){
			retval = retval.substring(2);
		}
		// remove leading underscore, if any
		if(retval.startsWith("_")){
			retval = retval.substring(1);
		}
		
		// create what we see as a legal sparql ID
		retval = legalizeSparqlID(retval);
		
		// check that the name is not already used. 
		if(reservedNameHash != null && reservedNameHash.contains(retval)){
			// remove any number from the end of the name. 
			retval = retval.replace("_\\d+$", "");
			
			// check for a better option.
			int i = 0;
			while(reservedNameHash.contains(retval + "_" + i )){
				i += 1;
			}
			retval = retval + "_" + i;
		}
		
		return retval;
	}
	
	/**
	 * Make sure a sparqlID is legal and add "?" if needed
	 * @param proposedName
	 * @return sparqlID guaranteed to start with "?" and have no illegal characters
	 * @throws Exception - contains illegal characters
	 */
	public static String formatSparqlId(String proposedName) throws Exception {
		String noPrefixName;
		if (proposedName.startsWith("?")) {
			noPrefixName = proposedName.substring(1);
		} else {
			noPrefixName = proposedName;
		}
		
		Pattern p = Pattern.compile(ILLEGAL_CHAR);
		Matcher m = p.matcher(noPrefixName);
		if (m.find()) {
			throw new Exception("SparqlId \"" + proposedName + "\" contains unsupported character: " + m.group(0));
		}
		
		return "?" + noPrefixName;
		
	}
	
	public static String legalizeSparqlID(String proposedName){
		// removes illeagal characters from the sparqlID and then 
		// adds a proper "?" as a prefix. 
		
		// remove ? if any
		String retval = proposedName.startsWith("?") ? proposedName.substring(1) : proposedName;
		// clean
		retval = retval.replaceAll(ILLEGAL_CHAR, "_");
		// put ? (back) on
		return "?" + retval;
		
	}

	public static JSONObject updateSparqlIdsForJSON(JSONObject jobj, String IndexName, HashMap<String, String> changedHash, HashSet<String> tempNameHash){
		// updates the names used in the json object given. this had to be divided into separate methods
		JSONObject retval = jobj;
		
		String ID = retval.get(IndexName).toString();
		if(changedHash.keySet().contains(ID)){
			// no op
		}
		else{
			String newId = BelmontUtil.generateSparqlID(ID, tempNameHash);
			if(!newId.equals(ID)){
				changedHash.put(ID, newId);  // when we come across the key ID when processing the new JSON, we will replace it with newId
				tempNameHash.add(newId);
				retval.remove(IndexName);
				retval.put(IndexName, newId);
			}
		}
		
		return retval;
	}

	public static JSONArray updateSparqlIdsForJSON(JSONArray jobj, int IndexName, HashMap<String, String> changedHash, HashSet<String> tempNameHash){
			// updates the names used in the json array given. this had to be divided into separate methods
			JSONArray retval = jobj;
			
			String ID = retval.get(IndexName).toString();
			if(changedHash.keySet().contains(ID)){
				// no op
			}
			else{
				String newId = BelmontUtil.generateSparqlID(ID, tempNameHash);
				if(!newId.equals(ID)){
					changedHash.put(ID, newId);  // when we come across the key ID when processing the new JSON, we will replace it with newId
					tempNameHash.add(newId);
					retval.set(IndexName, newId);
				}
			}
			
			return retval;
	}

	public static String prefixQuery(String query) throws Exception {
			
		String prefixes = "";
		
		String retval = "";
		
		// shuffle the query lines into two piles: prefixes & query1
		// depending on whether or not the line contains "prefix" (also keeps
		// "Error" lines in prefixes)
		// this will move prefixes to the front of the query and stop them from
		// being re-prefixed
		String[] lines = query.split("\n");
		
		Pattern prefixPattern = Pattern.compile("^prefix");
		
		for (String line : lines) {
			
			Matcher match = prefixPattern.matcher(line);
			
			if (match.find()) {
				prefixes += line;
				prefixes += '\n';
			}
			else {
				retval += line;
			}
		}
		
		//String pattern = "<(([^#<]+)\\/([-_A-Za-z0-9]+))#([^<>]+)>";
		
		// explain pattern:  <prefix#fragment>
		//   prefix is (anything except #<) / (alphanum or - or _)
		//   fragment can be anything except <>%
		//      note that % is an escaped character code that the algorithm can't seem to prefix properly, so leave them alone.
		Pattern pattern = Pattern.compile("<(([^#<]+)\\/([-_A-Za-z0-9]+))#([^<>%]+)>");
		
		Matcher m = null;
		
		HashMap<String,Integer> prefixHash = new HashMap<String,Integer>();
		
		while ((m = pattern.matcher(retval)) != null && m.find()) {
		
			String prefixAll = m.group(1);
			String prefix0 = m.group(2);
			String prefix1 = m.group(3);
			String prefixName = null;
			// count how many times this prefix has occurred
			if (prefixHash.containsKey(prefix1)) {
				prefixHash.put(prefix1, prefixHash.get(prefix1) + 1);
				prefixName = prefix1 + prefixHash.get(prefix1);
			} else {
				prefixHash.put(prefix1, 0);
				prefixName = prefix1;
			}
			
			prefixes = prefixes + "prefix " + prefixName + ":<" + prefix0 + "/"
					+ prefix1 + "#>\n";
			
			String pattern2 = "<" + prefixAll + "#([^>]+)>";
			
			String replace = prefixName + ":$1";
			
			String last = retval;
			
			retval = retval.replaceAll(pattern2, replace);
			
			if (last.equals(retval)) {
				throw new Exception("internal error in prefixQuery.  Failed to replace string in query.  \nString:" + pattern2 + "\nQuery: " + last);
			}
		}
		
		return prefixes + "\n" + retval;
		
	}
	
	/**
	 * Make a string safe for use as part of a SPARQL statement
	 * @param str
	 * @return
	 */
	static String sparqlSafe(String str) {
		return str.replace("\"", "\\\"").replace("\'", "\\\'").replace("\n", "\\n");
	}
}
