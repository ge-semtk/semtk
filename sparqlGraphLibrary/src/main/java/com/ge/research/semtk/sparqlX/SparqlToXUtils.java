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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

public class SparqlToXUtils {
	static final Pattern PATTERN_BAD_FIRST_CHAR = Pattern.compile("#[^a-zA-Z0-9]");
	public static final String BLANK_NODE_PREFIX = "nodeID://";
	
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
				" ?x ?y ?z FILTER strstarts(str(?x), \"%s\")." +
				"}", 
				prefix);
	  
	  return sparql;
  }
  
  public static String generateDeleteBySubjectRegexQuery(SparqlEndpointInterface sei, String regex) {
	  // delete all triples containing any trace of the given prefix
	  String sparql = String.format(
				generateWithDeleteWhereClause(sei, "{ ?x ?y ?z. }") +
				" ?x ?y ?z FILTER regex(str(?x), \"%s\")." +
				"}", 
				regex);
	  
	  return sparql;
  }
  public static String generateClearGraphSparql(SparqlEndpointInterface sei) {
	  return "CLEAR GRAPH <" + sei.getGraph() + ">";
  }
  public static String generateCountTriplesSparql(SparqlEndpointInterface sei) {
	  return "SELECT (COUNT(*) as ?count) from <" + sei.getGraph() + "> WHERE { ?x ?y ?z. }";
  }
  
  public static String generateDropGraphSparql(SparqlEndpointInterface sei) {
	  return "DROP GRAPH <" + sei.getGraph() + ">";
  }
  
  public static String generateCreateGraphSparql(SparqlEndpointInterface sei) {
	  return "CREATE GRAPH <" + sei.getGraph() + ">";
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

	/**
	 * FROM or USING clause logic
	 * Generates clauses if this.conn has
	 *     - exactly 1 serverURL
	 */		
	public static String generateSparqlFromOrUsing(String tab, String fromOrUsing, SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		
		// do nothing if no conn
		if (conn == null) return "";
		if (conn.isOwlImportsEnabled() && oInfo == null) {
			throw new Exception("Internal error: Can't generate SPARQL for owlImport-enabled connection and no OntologyInfo.  Validate or inflate nodegroup first.");
		}
		
		// multiple ServerURLs is not implemented
		if (! conn.isSingleDataServerURL() ) {
			throw new Error("SPARQL generation across multiple data servers is not yet supported.");
		}
		
		// get graphs/datasets for first model server.  All others must be equal
		ArrayList<String> datasets = conn.getAllDatasetsForServer(conn.getDataInterface(0).getServerAndPort());
		
		// add graphs from owlImports
		if (oInfo != null) {
			ArrayList<String> owlImports = oInfo.getImportedGraphs();
			for (String g : owlImports) {
				datasets.add(g);
			}
		}
				
		StringBuilder sparql = new StringBuilder().append("\n");
		// No optimization: always "from" all datasets
		tab = tabIndent(tab);
		for (int i=0; i < datasets.size(); i++) {
			sparql.append(tab + fromOrUsing + " <" + datasets.get(i) + ">\n");
		}
		tab = tabOutdent(tab);
		
		return sparql.toString();
	}
	
	public static String tabIndent(String tab) {
		return tab.concat("\t");
	}
	
	public static String tabOutdent(String tab) {
		return tab.substring(0, tab.length()-1);
	}
	
	
	
	/**
	 * Generate query to retrieve ?s ?s_class ?p ?o ?o_class 
     * where the ?s_class ?p pair is in predicatePairs
	 * @param conn
	 * @param oInfo
	 * @param predicatePairs
	 * @param limitOverride
	 * @param offsetOverride
	 * @param countQuery
	 * @return
	 * @throws Exception
	 */
	public static String generateSelectInstanceDataPredicates(SparqlConnection conn, OntologyInfo oInfo, ArrayList<String[]> predicatePairs, int limitOverride, int offsetOverride, boolean countQuery) throws Exception {
		StringBuilder sparql = new StringBuilder();
		
		if (predicatePairs.size() == 0) {
			throw new Exception("[domainURI predicateURI] predicate pairs list is empty");
		}
		// Start the query
		if (countQuery) {
			sparql.append("SELECT (COUNT(*) as ?count) \n");
			sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
			sparql.append("{ \n");
		}
		// select FROM WHERE
		sparql.append("SELECT DISTINCT ?s ?s_class ?p ?o ?o_class \n");
		
		if (! countQuery) {
			sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		}
		
		sparql.append("WHERE {" + "\n");
		
		if (predicatePairs.size() > 1) {
			sparql.append("{\n");
		}
		
		for (int i=0; i < predicatePairs.size(); i++) {
			sparql.append("	BIND ( <" + predicatePairs.get(i)[1] + "> as ?p) ." +  "\n");
			sparql.append("	?s ?p ?o." + "\n");
			sparql.append("	BIND ( <" + predicatePairs.get(i)[0] + "> as ?s_class) ." +  "\n");
			sparql.append("	?s a ?s_class. " + "\n");  // optional class names
			sparql.append("	optional { ?o a ?o_class. }" + "\n");
			
			if (i < predicatePairs.size() - 1) {
				sparql.append("} UNION {\n");
			} else if (i != 0) {
				sparql.append("} \n");
			}
		}
			
		// finsh it up
		
		if (countQuery) {
			sparql.append("}}\n");
		} else {
			sparql.append("}\n");
		}
		
		// offset and limit
		if (limitOverride != -1) {
			sparql.append("ORDER BY ?s, ?p, ?o " + "\n");
			sparql.append("LIMIT " + String.valueOf(limitOverride) + "\n");
		}
		if (offsetOverride != -1) {
			sparql.append("OFFSET " + String.valueOf(offsetOverride) + "\n");
		}
		
		System.out.println(sparql.toString());
		return sparql.toString();
	}
	
	/**
	 * Generate query to return ?s ?s_class
	 * where ?s_class is in classValues
	 * @param conn
	 * @param oInfo
	 * @param classValues
	 * @param limitOverride
	 * @param offsetOverride
	 * @param countQuery
	 * @return
	 * @throws Exception
	 */
	public static String generateSelectInstanceDataSubjects(SparqlConnection conn, OntologyInfo oInfo, ArrayList<String> classValues, int limitOverride, int offsetOverride, boolean countQuery) throws Exception {
		StringBuilder sparql = new StringBuilder();
		
		String sClassValuesClause;
		
		if (classValues.size() == 0) {
			throw new Exception("class values list is empty");
		}
		
		sClassValuesClause =  ValueConstraint.buildValuesConstraint("?s_class", classValues, XSDSupportedType.NODE_URI) ;

		// Start the query
		if (countQuery) {
			sparql.append("SELECT (COUNT(*) as ?count) \n");
			sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
			sparql.append("{ \n");
		}
		// select FROM WHERE
		sparql.append("SELECT DISTINCT ?s ?s_class \n");
		
		if (! countQuery) {
			sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		}
		
		sparql.append("WHERE {" + "\n");
		sparql.append("	" + sClassValuesClause + ". \n");
		sparql.append("	?s a ?s_class." + "\n");
			
		// finsh it up
		
		if (countQuery) {
			sparql.append("}}\n");
		} else {
			sparql.append("}\n");
		}
		
		// offset and limit
		if (limitOverride != -1) {
			sparql.append("ORDER BY ?s " + "\n");
			sparql.append("LIMIT " + String.valueOf(limitOverride) + "\n");
		}
		if (offsetOverride != -1) {
			sparql.append("OFFSET " + String.valueOf(offsetOverride) + "\n");
		}
		
		System.out.println(sparql.toString());
		return sparql.toString();
	}
}
