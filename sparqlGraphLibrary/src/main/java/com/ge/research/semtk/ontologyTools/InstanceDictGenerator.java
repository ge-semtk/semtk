package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * A utility class to generate a dictionary of instance URIs/types/identifiers
 * 
 * Dictionary columns:
 *   instance_uri - the instance URI
 *   class_uris - instance belongs to one or more classes
 *   label - label (or name) associated with the instance.  NOT UNIQUE: see label_specificity
 *   label_specificity - how many uris have this label
 *   property - what prop was used to associate label with instance_uri
 * 
 * Sample output:
 * 		instance_uri,class_uris,label,label_specificity,property
 * 		uri://semtk#f7bc522e,http://kdl.ge.com/batterydemo#Battery,battA,1,http://kdl.ge.com/batterydemo#name
 * 		uri://semtk#b42a5f42,http://kdl.ge.com/batterydemo#Cell,cell402,1,http://kdl.ge.com/batterydemo#cellId
 * 		uri://semtk#584b6722,http://kdl.ge.com/batterydemo#Cell,thing1,2,http://kdl.ge.com/batterydemo#cellId
 * 		uri://semtk#0ffa7f02,http://kdl.ge.com/batterydemo#Battery,thing1,2,http://kdl.ge.com/batterydemo#name
 * 		...
 */
public class InstanceDictGenerator {

	SparqlConnection conn;
	OntologyInfo oInfo;
	int specificityLimit = 0;
	String wordRegex;
	
	/**
	 * Constructor
	 * @param conn
	 * @param oInfo
	 * @param maxWords - strings with more than this many words are not considered
	 * @param specificityLimit - don't find a name/label if it identifies more than this many URI instances
	 */
	public InstanceDictGenerator(SparqlConnection conn, OntologyInfo oInfo, int maxWords, int specificityLimit) {
		this.conn = conn;
		this.oInfo = oInfo;
		this.specificityLimit = specificityLimit;
		
		// regex that will eliminate strings with more than maxWords words
		this.wordRegex = "\\\\w+\\\\s+";
		for (int i=1; i < maxWords; i++) {
			this.wordRegex += "\\\\w+\\\\s+";
		}
	}

	/**
	 * Generate a tabular report
	 */
//	public Table generateOLD() throws Exception{
//		
//		Table table = null;
//		
//		// only match strings that are unique ?o in the world of ?s ?p ?o
//
//		ArrayList<String> propNames = this.oInfo.getPropertyNames();
//		for (String propUri : propNames) {
//			OntologyProperty oProp = oInfo.getProperty(propUri);
//			Set<String> domains = oProp.getRangeDomains();
//			for (String domainUri : domains) {
//				OntologyRange oRange = oProp.getExactRange(domainUri); 
//				if (oRange.containsUri("http://www.w3.org/2001/XMLSchema#string")) {
//					// found oProp which can have a range of String when domain is domainUri
//					
//					// selects:
//					//    - ?sub  a subject URI
//					//    - concatenation of types
//					//    - ?str string that could be identifier
//					//    - ?str_count how many things this string might be an identifier for
//					//    - filters out ?str if it has more than two words:  see twoOrFewerWordRegex
//					String query = String.format(
//							  "select distinct (?sub as ?instance_uri) (GROUP_CONCAT(DISTINCT ?t) as ?class_uris) (?str as ?label) (COUNT(distinct ?sub2) as ?label_specificity) \n"
//							+ "		%s \n "
//							+ " where {\n"
//							+ "	\n"
//							+ "	?sub <%s> ?str.\n"
//							+ " ?sub a ?t .\n"
//							+ "	?t <http://www.w3.org/2000/01/rdf-schema#subClassOf>* <%s> .\n"
//							+ " filter ( ! regex (?str, \"%s\")) .\n"
//							+ " ?sub2 ?pred2 ?str ."
//							+ "} \n"
//							+ "GROUP BY ?sub ?str "
//							+ "HAVING (COUNT(distinct ?sub2) < %d)", 
//							SparqlToXLibUtil.generateSparqlFromOrUsing("", "FROM", conn, this.oInfo), 
//							propUri, 
//							domainUri, 
//							this.wordRegex, 
//							this.specificityLimit + 1 );
//					
//					Table tab = conn.getDefaultQueryInterface().executeToTable(query);
//					tab.appendColumn("property", "literal", propUri);
//					if (table == null)
//						table = tab;
//					else
//						table.append(tab);	
//				}
//			}
//		}
//		return table;
//	}
	// @TODO Different in that it will discount a string if it names members of different classes
	// @TODO Above queries by subjects of each (super)class
	public Table generate() throws Exception{
		
		HashSet<String> stringSet = new HashSet<String>();
				
		// get all strings (objects of a string predicate)
		ArrayList<String> propNames = this.oInfo.getPropertyNames();
		for (String propUri : propNames) {
			if(propUri.equals("http://sadl.org/sadlimplicitmodel#script")) {
				// one-off predicate to skip.  It has objects that we want to ignore that slip through the regex filter, e.g. "-- unique(...): uri(""com.ge.research.sadl.jena.reasoner.builtin#unique"")"
				// TODO a more generic solution would be better
				continue;
			}
			OntologyProperty oProp = oInfo.getProperty(propUri);		
			Set<String> domains = oProp.getRangeDomains();
			for (String domainUri : domains) {
				OntologyRange oRange = oProp.getExactRange(domainUri); 
				if (oRange.containsUri("http://www.w3.org/2001/XMLSchema#string")) {
					// query to get string objects, omitting strings that have too many words
					String query = String.format(									
							"select distinct ?str \n"
									+ "		%s \n "
									+ " where {\n"
									+ "	\n"
									+ "	?u <%s> ?str.\n"
									+ " filter ( ! regex (?str, \"%s\")) .\n"  // omit strings with too many words
									+ "} \n"
									, 
									SparqlToXLibUtil.generateSparqlFromOrUsing("", "FROM", conn, this.oInfo), 
									propUri, 
									this.wordRegex);
					LocalLogger.logToStdOut("query " + propUri + " domain: " + domainUri);
					Table tab = conn.getDefaultQueryInterface().executeToTable(query);
					LocalLogger.logToStdOut(tab.toCSVString());
					for (String s : tab.getColumn(0)) {
						stringSet.add(s);
					}
				}
			}
		}
		
		Table ret = new Table(new String[] {"instance_uri", "class_uris", "label", "label_specificity", "property"});
		
		for (String s : stringSet) {
			String query = String.format(
					  "select distinct ?sub (GROUP_CONCAT(DISTINCT ?t) as ?class_uris) ?label ?prop \n"
					+ "		%s \n "
					+ " where {\n"
					+ " BIND (\"%s\" as ?label) . \n"
					+ "	?sub ?prop ?label.\n"
					+ " ?sub a ?t .\n"
					+ "} \n"
					+ "GROUP BY ?sub ?label ?prop \n"
					+ "LIMIT %d", 
					SparqlToXLibUtil.generateSparqlFromOrUsing("", "FROM", conn, this.oInfo), 
					s,
					this.specificityLimit + 1);
			Table tab = conn.getDefaultQueryInterface().executeToTable(query);
			if (tab.getNumRows() <= this.specificityLimit) {
				for (int r=0; r < tab.getNumRows(); r++) {
					ret.addRow(new String[] {tab.getCell(r, 0), tab.getCell(r,1), s, String.valueOf(tab.getNumRows()), tab.getCell(r,3)});
				}
			}
		}
	
		return ret;
	}
}
