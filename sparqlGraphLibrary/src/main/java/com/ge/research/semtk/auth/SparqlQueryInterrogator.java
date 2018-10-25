package com.ge.research.semtk.auth;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ge.research.semtk.utility.Utility;

/**
 * Very simple SPARQL "parser" used to figure out what kind of query is being executed
 * And to what graphs.
 * @author 200001934
 *
 */
public class SparqlQueryInterrogator {
	private static Pattern PATTERN_INSERT = 	Pattern.compile("\\Winsert\\W", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_DELETE = 	Pattern.compile("\\Wdelete\\W", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_CLEAR = 		Pattern.compile("\\Wclear\\W", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_DROP = 		Pattern.compile("\\Wdrop\\W", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_CREATE = 	Pattern.compile("\\Wcreate\\W", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_SELECT = 	Pattern.compile("\\Wselect\\W", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_CONSTRUCT = 	Pattern.compile("\\Wconstruct\\W", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_ASK = 		Pattern.compile("\\Wask\\W", Pattern.CASE_INSENSITIVE);
	private static Pattern PATTERN_SERVICE = 	Pattern.compile("\\Wservice\\W", Pattern.CASE_INSENSITIVE);
	
	// a keyword that's supposed to be followed by <graph> but has something else (like a variable) afterwards
	private static Pattern PATTERN_ILLEGAL_GRAPH = Pattern.compile("\\W(from|into|graph)\\s*[^<]", Pattern.CASE_INSENSITIVE);

	private static Pattern PATTERN_GRAPH_CAPTURE = Pattern.compile("\\W(?:from|into|graph)\\s*<([^>]+)>", Pattern.CASE_INSENSITIVE);
	
	private String query = null;
	private String origQuery = null;
	
	public SparqlQueryInterrogator(String query) throws AuthorizationException {
		
		// initialize by removing all sub strings from the query
		// and surrounding by spaces to make the word boundaries more efficient (not looking for ^ or $)
		this.origQuery = query;
		this.query = " " + Utility.removeQuotedSubstrings(query, "\"REPLACED\"") + " ";
		
		// check for non-starter keywords we can't authorize
		if (this.containsService()) {
			throw new AuthorizationException("Can not authorize query containing SERVICE keyword: \n" + this.origQuery);
		}
	}
	
	/**
	 * Does this query only read graphs
	 * @return
	 */
	public boolean isReadOnly() throws AuthorizationException {
		boolean hasWriteKeyword = 
				this.containsClear() ||
				this.containsCreate() ||
				this.containsDrop() ||
				this.containsInsert() ||
				this.containsDelete()
				;
		
		boolean hasReadKeyword =
				this.containsSelect() ||
				this.containsConstruct() ||
				this.containsAsk();
		
		if (!hasWriteKeyword && !hasReadKeyword) {
			throw new AuthorizationException("Can not authorize query containing none of CLEAR, CREATE, DROP, DELETE, SELECT, CONSTRUCT, ASK: \n" + this.origQuery);
		}
		
		return hasReadKeyword && !hasWriteKeyword;
	}
	
	public ArrayList<String> getGraphNames() throws AuthorizationException {
		ArrayList<String> ret = new ArrayList<String>();
		
		Matcher m = PATTERN_GRAPH_CAPTURE.matcher(this.query);
		while (m.find()) {
			String graph = m.group(1);
			if (graph.contains("?")) {
				throw new AuthorizationException("Can not authorize query with un-parsable graph: " + m.group(0));
			}
			ret.add(graph);
		}
		return ret;
	}
	
	private boolean containsInsert() {
		return PATTERN_INSERT.matcher(this.query).find();
	}
	private boolean containsDelete() {
		return PATTERN_DELETE.matcher(this.query).find();
	}
	private boolean containsClear() {
		return PATTERN_CLEAR.matcher(this.query).find();
	}
	private boolean containsDrop() {
		return PATTERN_DROP.matcher(this.query).find();
	}
	private boolean containsCreate() {
		return PATTERN_CREATE.matcher(this.query).find();
	}
	private boolean containsSelect() {
		return PATTERN_SELECT.matcher(this.query).find();
	}
	private boolean containsConstruct() {
		return PATTERN_CONSTRUCT.matcher(this.query).find();
	}
	private boolean containsAsk() {
		return PATTERN_ASK.matcher(this.query).find();
	}
	private boolean containsService() {
		return PATTERN_SERVICE.matcher(this.query).find();
	}
	
	
	
}
