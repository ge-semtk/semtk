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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ge.research.semtk.utility.LocalLogger;

public class SparqlToXUtils {
	static final Pattern PATTERN_BAD_FIRST_CHAR = Pattern.compile("#[^a-zA-Z0-9]");
	public static final String BLANK_NODE_PREFIX = "nodeID://";
	public static final String UNRESERVED_URI_CHARS = "0123456789-_ABCDEFJHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.~";
	public static final String BLANK_NODE_REGEX = "^(nodeID://|_:)";
	public static final Pattern BLANK_NODE_PATTERN = Pattern.compile(BLANK_NODE_REGEX);
	
	
	/**
	 * Determine if a given node string represents a blank node
	 */
	public static boolean isBlankNode(String s) {
		return SparqlToXUtils.BLANK_NODE_PATTERN.matcher(s).find();
	}
	
	// check that the passed Sparql query references the appropriate manadate columns in the return.
	public static void validateSparqlQuery(String query, String[] requiredCols) throws IOException {
		
		for (String i : requiredCols){
			if (!query.contains('?'+i)){
				// throw an exception
				throw new IOException("Incoming query missing required field " + i);
			}
		}
	}
	
	// check that the passed SPARQL query does not contain blocked words (e.g. to disallow freeform DELETE queries)
	public static void validateSparqlInsertQuery(String query, String[] blockedWords) throws IOException{
		for (String i : blockedWords){
			if (query.contains(i)){
				// throw an exception
				throw new IOException("Incoming query contains blocked word " + i);
			}
		}
	}
	
  /**
   * Check to see if a URI is of a given type.  True if matches, false if does not
   * @throws Exception 
   */
  public static boolean checkURIType(String uri, String type, String sparqlServerUrl, String sparqlDataset, String serverTypeString, String user, String pass) throws Exception{
    String checkTypeQuery = "select * where { " +  
        "VALUES ?x {<" + uri + ">} . " +
        "?x rdf:type <" + type + ">" +
        "}";
    // execute query
    SparqlEndpointInterface endpoint = SparqlEndpointInterface.executeQuery(sparqlServerUrl, sparqlDataset, serverTypeString, checkTypeQuery, user, pass, SparqlResultTypes.TABLE);
    String[] results = endpoint.getStringResultsColumn("x");
    
    if(results != null && results.length > 0){
      return true; 
    }
    return false;
  } 	
	
  /**
   * Generate: select ?p ?o where  subject ?p ?o
   * @param subject - "<http://something>"
   * @throws IOException
   */
  public static String generateSelectBySubjectQuery(SparqlEndpointInterface sei, String subject) throws IOException{
    return generateSelectFromWhereClause(sei, "?p ?o") + subject + " ?p ?o  } ";
  }
  
  public static String generateInsertTripleQuery(SparqlEndpointInterface sei, String sub, String pred, String obj) {
	  return "INSERT DATA { GRAPH <" + sei.getGraph() + "> { " + sub + " " + pred + " " + obj + "} }";
  }
	/**
	 * Generate a DELETE query for a specific URI.
	 * To prevent a catastrophic delete, disallows a URI starting with ?
	 * @throws IOException
	 */
  public static String generateDeleteURIQuery(SparqlEndpointInterface sei, String uri) throws IOException{

    // check that URI does not start with ?   
    if(uri.startsWith("?")){
      LocalLogger.logToStdOut("URI to delete may not start with '?'...not deleting");
      throw new IOException("URI to delete may not start with '?'");
    }
    
    // delete anywhere where the URI is a subject or object
    String query = generateDeleteURISubjectQuery(sei, uri) + generateDeleteURIObjectQuery(sei, uri);
    return query;

  }	
  
 
  /**
   * Generate a DELETE query where the subject is a specific URI.
   * @throws IOException
   */
  public static String generateDeleteURISubjectQuery(SparqlEndpointInterface sei, String uri) throws IOException{
    return generateDeleteURISubjectQuery(sei, uri, null);
  }
  
  /**
   * Generate a DELETE query where the subject is a specific URI, and the predicate is given.
   * Expects that predicate will NOT be enclosed by < >
   * @throws IOException
   */
  public static String generateDeleteURISubjectQuery(SparqlEndpointInterface sei, String uri, String predicate) throws IOException {
   
    // avoid catastrophic delete by checking that URI does not start with ?  
    if(uri.startsWith("?")){
      LocalLogger.logToStdOut("URI to delete may not start with '?'...not deleting");
      throw new IOException("URI to delete may not start with '?'");
    }else if(uri.trim().equals("")){
      LocalLogger.logToStdOut("URI to delete may not be empty...not deleting");
      throw new IOException("URI to delete may not be empty");
    }
    
    // predicate must be null or a valid string - if it's empty then something is wrong
    if(predicate != null && predicate.trim().equals("")){
      LocalLogger.logToStdOut("Predicate to delete may not be empty...not deleting");
      throw new IOException("Predicate to delete may not be empty");
    }
    
    if(predicate == null){
      predicate = "?y1";  // no predicate specified - do not restrict it
    }else{
      predicate = "<" + predicate + ">";  // predicate specified - use it
    }
    
    String query = generateWithDeleteWhereClause(sei, "") + " <" +  uri + "> " + predicate + " ?z1 . } ";  // uri is subject
    return query;
  }   
  
  /**
   * Generate a DELETE query where the object is a specific URI.
   * @throws IOException
   */
  public static String generateDeleteURIObjectQuery(SparqlEndpointInterface sei, String uri) throws IOException{

    // avoid catastrophic delete by checking that URI does not start with ?   
    if(uri.startsWith("?")){
      LocalLogger.logToStdOut("URI to delete may not start with '?'...not deleting");
      throw new IOException("URI to delete may not start with '?'");
    }else if(uri.trim().equals("")){
      LocalLogger.logToStdOut("URI to delete may not be empty...not deleting");
      throw new IOException("URI to delete may not be empty");
    }
    
    String query = generateWithDeleteWhereClause(sei, "") + " ?x2 ?y2 <" + uri + "> . } ";  // uri is object
    return query;

  }  
  
  /**
   * 
   * @param sei
   * @param prefix
   * @return
   */
  public static String generateDeletePrefixQuery(SparqlEndpointInterface sei, String prefix) {
	  // delete all triples containing any trace of the given prefix
	  String sparql = String.format(
				generateWithDeleteWhereClause(sei, "{ ?x ?y ?z. }") +
				" ?x ?y ?z  FILTER ( strstarts(str(?x), \"%s\") || strstarts(str(?y), \"%s\") || strstarts(str(?z), \"%s\") )." +
				"}", 
				prefix, prefix, prefix);
	  
	  return sparql;
  }
  
  public static String generateDeleteBySubjectPrefixQuery(SparqlEndpointInterface sei, String prefix) {
	  // delete all triples containing any trace of the given prefix
	  String sparql = String.format(
			  generateWithDeleteWhereClause(sei, "{ ?x ?y ?z. }") +
				" ?x ?y ?z . FILTER strstarts(str(?x), \"%s\")." +
				"}", 
				prefix);
	  
	  return sparql;
  }
  
  public static String generateDeleteBySubjectRegexQuery(SparqlEndpointInterface sei, String regex) {
	  // delete all triples containing any trace of the given prefix
	  String sparql = String.format(
				generateWithDeleteWhereClause(sei, "{ ?x ?y ?z. }") +
				" ?x ?y ?z . FILTER regex(str(?x), \"%s\")." +
				"}", 
				regex);
	  
	  return sparql;
  }
  public static String generateDeleteAllQuery(SparqlEndpointInterface sei) {
	  // delete all triples containing any trace of the given prefix
	  String sparql =
				generateWithDeleteWhereClause(sei, "{ ?x ?y ?z. }") +
				" ?x ?y ?z " +
				"}";
	  
	  return sparql;
  }
  public static String generateClearGraphSparql(SparqlEndpointInterface sei) {
	  return "CLEAR GRAPH <" + sei.getGraph() + ">";
  }
  public static String generateCountTriplesSparql(SparqlEndpointInterface sei) {
	  return "SELECT (COUNT(*) as ?count) from <" + sei.getGraph() + "> WHERE { ?x ?y ?z }";
  }
  public static String generateSelectTriplesSparql(SparqlEndpointInterface sei, int limit) {
	  return "SELECT ?x ?y ?z from <" + sei.getGraph() + "> WHERE { ?x ?y ?z } LIMIT " + limit;
  }
  
  public static String generateCountBySubjectRegexQuery(SparqlEndpointInterface sei, String subjectRegex) {
	  return String.format(
			  "SELECT (COUNT(*) as ?count) from <" + sei.getGraph() + "> WHERE { ?x ?y ?z . FILTER regex(str(?x), \"%s\").}", 
			  subjectRegex);
  }
  
  /*
   * Will return nothing if ontology isn't loaded.
   * Row with ontology and possibly empty version if ontology IS loaded.
   */
  public static String generateGetVersionOfOntology(SparqlEndpointInterface sei, String base) {
	  return String.format(
			  "SELECT ?subject ?version from <" + sei.getGraph() + "> WHERE { \n" 
			  + "VALUES ?subject { <%s> } \n"
			  + "?subject a <http://www.w3.org/2002/07/owl#Ontology> . \n"
			  + "optional { ?subject <http://www.w3.org/2002/07/owl#versionInfo> ?version . } \n"
			  + "} ", 
			  base);
  }
  
  public static String generateDropGraphSparql(SparqlEndpointInterface sei) {
	  return "DROP GRAPH <" + sei.getGraph() + ">";
  }
  
  public static String generateCreateGraphSparql(SparqlEndpointInterface sei) {
	  return "CREATE GRAPH <" + sei.getGraph() + ">";
  }
  
  public static String generateSelectSPOSparql(SparqlEndpointInterface sei, String clause) {
	  return "SELECT ?s ?p ?o FROM <" + sei.getGraph() + "> WHERE { ?s ?p ?o. " + (clause == null ? "" : clause) + "}";
  }
  
  public static String generateConstructSPOSparql(SparqlEndpointInterface sei, String clause) {
	  return "CONSTRUCT { ?s ?p ?o } FROM <" + sei.getGraph() + "> WHERE { ?s ?p ?o. " + (clause == null ? "" : clause) + "}";
  }
  
  /**
   * Delete all model triples given a list of prefixes.   Also deletes blank nodes.
   * @param prefixes
   * @return
   */
  public static String generateDeleteModelTriplesQuery(SparqlEndpointInterface sei, ArrayList<String> prefixes, Boolean deleteBlankNodes) {
	  // init regex
	  StringBuilder regex = new StringBuilder("^(");
	  
	  // add all prefixes
	  for (String p : prefixes) {
		  if (regex.toString().length() > 2) {
			  regex.append("|");
		  }
		  regex.append(p);
	  }
	  
	  // add blank node prefix
	  if (deleteBlankNodes) {
		  regex.append("|" + SparqlToXUtils.BLANK_NODE_PREFIX);
	  }
	  regex.append(")");
	  
	  // delete all triples w
	  return SparqlToXUtils.generateDeleteBySubjectRegexQuery(sei, regex.toString());
	  
  }
  

	
	// check that all required columns are found in the return values from the Sparql endpoint. 
	public static void validateSparqlResults(SparqlEndpointInterface sei, String[] requiredCols) throws IOException {
		for (String col : requiredCols){
			try {
				sei.checkResultsCol(col);
			} catch (Exception e) {
				throw new IOException("Semantic query did not return column: " + col);
			}
		}
	}
	// check to see if a required parameter is null/missing
	public static void genExceptionIfNull(Object o, String msg, String where) {
		if (o == null) {
		    if (where != null) 
		    	msg = msg + " at " + where;
			throw new IllegalArgumentException(msg);
		}
	}
	
	/**
	 * Build a safe variable name starting with ?
	 * @param s
	 * @return
	 */
	public static String safeSparqlVar(String s) {
		return "?" + s.replaceAll("[^a-zA-Z0-9]+", "_");
	}
	
	// legacy
	// this makes some changes to the string to try to make a safe and reasonable sparql INSERT.
	// Example flaw:  round trip "hi\\nthere" (slash n) and "hi\nthere" (return) each comes back as "hi\nthere" (return)
	// it is retained for backwards compatibility.  It seems to work with nodegroup json storage.
	//
	// Use this function when you can't control how/when strings will be queried back out.
	//
	// If you can control the query exit point, then use:
	// 	  escapeForSparql() before INSERT
	//    unescapeFromSparql() after the SELECT back out
	public static String safeSparqlString(String s) {
		
		StringBuilder out = new StringBuilder();
	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);
	        
	        // lowercase
	        if      (c == '\"')             { out.append("\\\""); }   // replaces " with \"  
	        else if (c == '\'')             { out.append("\\'"); }    // replaces ' with \'
	        else if (c == '\n')             { out.append("\\n"); } 
	        else if (c == '\r')             { out.append("\\r"); } 
	        else if (c == '\\') {
	        	if (i+1 < s.length()) {                              // backslash requires look-ahead
	        		char c2 = s.charAt(i+1);
	        		if (c2 == 'n' || c2 == 't') {                    // preserve backslash if followed by these
	        			out.append(c);
	        		}
	        		else {
	        			out.append("\\\\");                          // unknown next char: change \ to \\
	        		}
	        	} else {
	        		out.append("\\\\");                              // last char in string is \: change to \\
	        	}
	        }  

	        // rest
	        else                          { out.append(c); }
	    }
	    return out.toString();
		
	}
	
	/**
	 * Escape a string so it won't mess up a sparql query.
	 * Querying back out will require unescapeFromSparql()
	 * @param s
	 * @return
	 */
	public static String escapeForSparql(String s) {

		String ret = s.replaceAll("&", "&#38;")
				.replaceAll("[\n\r]+", "&#10;")
				.replace("\\", "&#92;")
				.replace("\"", "&#34;")
				;
					
		return ret;
		
	}
	
	/**
	 * Undo the escapeForSparql()
	 * @param s
	 * @return
	 */
	public static String unescapeFromSparql(String s) {
		String ret = s.replace("&#38;", "&")
				.replace("&#10;", "\n")
				.replace("&#92;", "\\")
				.replace("&#34;", "\"")
				;
		return ret;
	}
	
	public static boolean isLegalURI(String uri) {
		if (uri == null) return false;
		
		// special case: virtuoso silently chokes on these
		Matcher m = SparqlToXUtils.PATTERN_BAD_FIRST_CHAR.matcher(uri);
		if (m.find()) {
			return false;
		}
		
		try	{
			new URI(uri);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * 
	 * @param sei
	 * @param deletePhrase - e.g. ""  or "{ ?x ?y ?z. }"
	 * @return
	 */
	private static String generateWithDeleteWhereClause(SparqlEndpointInterface sei, String deletePhrase) {
		return "WITH <" + sei.getGraph() + "> DELETE " + deletePhrase + " WHERE {";
	}
	
	private static String generateSelectFromWhereClause(SparqlEndpointInterface sei, String returnsPhrase) {
		return "SELECT DISTINCT " + returnsPhrase + " FROM <"+ sei.getGraph() + ">  WHERE {";
	}

	public static String tabIndent(String tab) {
		return tab.concat("\t");
	}
	
	public static String tabOutdent(String tab) {
		if (tab.length() < 1) {
			return tab;
		} else {
			return tab.substring(0, tab.length()-1);
		}
	}

	/**
	 * Returns a query to get graph names.
	 * @return the query
	 */
	public static String generateSelectGraphNames() {
		return "SELECT ?g WHERE { GRAPH ?g { }} ORDER BY ?g";
	}

	/**
	 * Returns a query to get graph info (names and triple counts)
	 * @param sei the SPARQL endpoint interface
	 * @param defaultGraphOnly true to only return info about the default graph
	 * @return the query
	 */
	public static String generateSelectGraphInfo(SparqlEndpointInterface sei, boolean defaultGraphOnly) {
		if(!defaultGraphOnly) {
			return "SELECT DISTINCT ?graph (COUNT(?s) AS ?triples) { GRAPH ?graph { ?s ?p ?o } } GROUP BY ?graph ORDER BY ?graph";
		}else {
			return "SELECT (\"" + sei.getDefaultGraphName() + "\" AS ?graph) (COUNT(?s) AS ?triples) {GRAPH <" + sei.getLocalDefaultGraphName() + "> { ?s ?p ?o } }";
		}
	}

}
