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


package com.ge.research.semtk.sparqlToXLib;


import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.Triple;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.sparqlX.XSDSupportedType;


/*
 * Split from SparqlToXUtils - functions requiring Ontology and other advance sparqlGraphLibrary imports
 */
public class SparqlToXLibUtil {
	public static final String TYPE_PROP = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	
	/**
	 * FROM or USING clause logic
	 * Generates clauses if this.conn has
	 *     - exactly 1 serverURL
	 */		
	
	/**
	 * FROM or USING clause logic
	 * Generates clauses if this.conn has
	 *     - exactly 1 serverURL
	 * 
	 * @param tab
	 * @param fromOrUsing
	 * @param conn
	 * @param oInfo - may be null if owlImports is false
	 * @return
	 * @throws Exception
	 */
	public static String generateSparqlFromOrUsing(String tab, String fromOrUsing, SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		
		// do nothing if no conn
		if (conn == null) return "";
		
		// check if entire connection is the default graph on one server
		if (conn.isSingleServerURL()) {
			boolean onlyDefault = true;
		
			for (SparqlEndpointInterface sei : conn.getAllInterfaces()) {
				if (! sei.isDefaultGraph()) {
					onlyDefault = false;
					break;
				}
			}
			if (onlyDefault) {
				return "";   // no FROM or USING if everything points to default graph
			}
		}
		
		
		if (conn.isOwlImportsEnabled() && oInfo == null) {
			throw new Exception("Internal error: Can't generate SPARQL for owlImport-enabled connection and no OntologyInfo.  Validate or inflate nodegroup first.");
		}
		
		// multiple ServerURLs is not implemented
		conn.confirmSingleServerURL();
		
		// get graphs/datasets for first model server.  All others must be equal
		ArrayList<String> graphs = conn.getAllGraphsForServer(conn.getDataInterface(0).getServerAndPort());
		
		// add graphs from owlImports
		if (oInfo != null) {
			ArrayList<String> owlImports = oInfo.getImportedGraphs();
			for (String g : owlImports) {
				graphs.add(g);
			}
		}
				
		StringBuilder sparql = new StringBuilder().append("\n");
		// No optimization: always "from" all datasets
		tab = SparqlToXUtils.tabIndent(tab);
		for (int i=0; i < graphs.size(); i++) {
			sparql.append(tab + fromOrUsing + " <" + graphs.get(i) + ">\n");
		}
		tab = SparqlToXUtils.tabOutdent(tab);
		
		return sparql.toString();
	}
	
	/**
	 * 
	 * @param conn - model and data   (model often contains enumerated instances)
	 * @param oInfo
	 * @return
	 * @throws Exception
	 */
	public static String generatePredicateStatsQuery(SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		
		String sparql = String.format(
				"select ?s_class ?p ?o_class (COUNT(*) as ?count)\n"
				+ "%s\n"
				+ "WHERE {\n"
				+ "	   ?s a ?s_class.\n"
				+ "	   ?s ?p ?o .\n"
				// filter these out of the model connection.
				// Another approach might be to remove these filters from the data connection
				// and send a separate query of only enumerated data from the model
				+ "	   FILTER (  !regex(str(?s_class), 'www.w3.org')) . "
				+ "	   FILTER (  !regex(str(?p), 'rdf-schema')) . "
				// --------------------
				+ "    optional {\n"
				+ "	      ?o a ?o_class.\n"
				+ "    }\n"
				+ "} group by ?s_class ?p ?o_class\n"
				+ "order by ?p ?s_class ?o_class",
				generateSparqlFromOrUsing("", "from", conn, oInfo));
		return sparql;
	}
	
	public static String generatePathInstanceCountQuery(OntologyPath path, SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		
		String sparql = 
				"select (COUNT(*) as ?count)\n"
				+ generateSparqlFromOrUsing("", "from", conn, oInfo) + "\n"
				+ "WHERE {\n"
				;
		OntologyPath tempPath = new OntologyPath(path.getStartClassName());
		Triple t0 = path.getTriple(0);
		sparql += String.format(
				"	   ?s_0 a <%s>.\n" +
				"      ?s_0 <%s> ?o_0.\n " +
				"	   ?o_0 a <%s>.\n",
				t0.getSubject(),
				t0.getPredicate(),
				t0.getObject()
				);
		tempPath.addTriple(t0.getSubject(),	t0.getPredicate(), t0.getObject());
		
		for (int i=1; i < path.getLength(); i++) {
			Triple t = path.getTriple(i);
			
			// add next triple forward or backward
			if (t.getSubject().equals(tempPath.getEndClassName())) {
				sparql += String.format(
						"      ?o_%d <%s> ?o_%d.\n " +
						"	   ?o_%d a <%s> .\n", 
						i-1, t.getPredicate(), i,
						i, t.getObject());
			} else {
				sparql += String.format(
						"      ?o_%d <%s> ?o_%d.\n " +
						"	   ?o_%d a <%s>.\n", 
						i, t.getPredicate(), i-1,
						i, t.getSubject());
			}
			
			tempPath.addTriple(t.getSubject(),	t.getPredicate(), t.getObject());
		}
		sparql += "}";
		return sparql;
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
			sparql.append("ORDER BY ?s ?p ?o " + "\n");
			sparql.append("LIMIT " + String.valueOf(limitOverride) + "\n");
		}
		if (offsetOverride != -1) {
			sparql.append("OFFSET " + String.valueOf(offsetOverride) + "\n");
		}
		
		return sparql.toString();
	}
	
	/**
	 * Generate query to get values of ?class for an instanceUri
	 * @param conn
	 * @param oInfo
	 * @param instanceUri
	 * @return
	 * @throws Exception
	 */
	public static String generateGetInstanceClass(SparqlConnection conn, OntologyInfo oInfo, String instanceUri) throws Exception {
		StringBuilder sparql = new StringBuilder();
		
		// select FROM WHERE
		sparql.append("SELECT DISTINCT ?class \n");
		sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		sparql.append(String.format("WHERE { <%s> a ?class . } \n", instanceUri));

		return sparql.toString();
	}
	
	/**
	 * Generate query to get values of ?class for an instanceUri
	 * @param conn
	 * @param oInfo
	 * @param instanceUri
	 * @return
	 * @throws Exception
	 */
	public static String generateAskInstanceExists(SparqlConnection conn, OntologyInfo oInfo, String instanceUri) throws Exception {
		StringBuilder sparql = new StringBuilder();
		
		// select FROM WHERE
		sparql.append("ASK \n");
		sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		sparql.append(String.format("WHERE { <%s> a ?class . } \n", instanceUri));

		return sparql.toString();
	}
	
	/**
	 * Generate query to get values of ?uri and group_concat of types
	 * 
	 * Generating the value: use as-is if it is enclosed in "" or <> 
	 * 		or contains ^^
	 * 		or if the property has a complicated range
	 *      or if property is unknown
	 * else make an RDF1.1 string 
	 * @param conn
	 * @param oInfo
	 * @param propValHash - hash property to value.  
	 * @return
	 * @throws Exception
	 */
	public static String generateSelectInstance(SparqlConnection conn, OntologyInfo oInfo, Hashtable<String,String> propValHash) throws Exception {
		StringBuilder sparql = new StringBuilder();
		
		// select FROM WHERE
		sparql.append("SELECT ?uri  (GROUP_CONCAT(?t) as ?type_list) \n");
		sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		sparql.append("WHERE { \n");
		
		for (String prop : propValHash.keySet()) {
			String val = (String) propValHash.get(prop);
			
			if (!val.startsWith("\"") && !val.startsWith("<") && !val.contains("^^")) {
				OntologyProperty oProp = oInfo.getProperty(prop);
				if (oProp != null) {
					HashSet<String> rangeUriSet = oProp.getAllRangeUris();
					if (rangeUriSet.size() == 1) {
						val = XSDSupportedType.getMatchingValue((String)(rangeUriSet.toArray()[0])).buildRDF11ValueString(val);
					}
				} else if (prop.endsWith("#type")) {
					// treat #type like it is "known"
					val = XSDSupportedType.URI.buildRDF11ValueString(val);
				} 
			}
			sparql.append(String.format("  ?uri %s %s . \n", XSDSupportedType.URI.buildRDF11ValueString(prop), val));
			
			
		}
		sparql.append("   optional { ?uri a ?t } . \n");
		sparql.append("} GROUP BY ?uri \n");

		return sparql.toString();
	}

	/**
	 * Get all props that are shared by subject1 and subject2
	 * @param conn
	 * @param oInfo
	 * @param subject1
	 * @param subject2
	 * @return
	 * @throws Exception
	 */
	public static String generateSelectDuplicateProps(SparqlConnection conn, OntologyInfo oInfo, String subject1, String subject2) throws Exception {
		StringBuilder sparql = new StringBuilder();
		
		// select FROM WHERE
		sparql.append("SELECT ?prop \n");
		sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		sparql.append("WHERE { \n");
		sparql.append(String.format("    %s ?prop ?o1 . \n", XSDSupportedType.URI.buildRDF11ValueString(subject1)));
		sparql.append(String.format("    %s ?prop ?o2 . \n", XSDSupportedType.URI.buildRDF11ValueString(subject2)));
		sparql.append("}");
		
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
		
		HashSet<XSDSupportedType> uriSet = new HashSet<XSDSupportedType>();
		uriSet.add(XSDSupportedType.NODE_URI);
		sClassValuesClause =  ValueConstraint.buildFilterInConstraint("?s_class", classValues, uriSet) ;

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
		
		return sparql.toString();
	}

	/**
	 * Generate query to get connected items.  Get type relationship for any new instances returned.
	 * @param conn
	 * @param oInfo
	 * @param instance - the target instance
	 * @param instanceType - type of the target instance
	 * @param limit - max number of triples to return
	 * @param classList - list of classes to blacklist or whitelist from returned data
	 * @param listIsWhite - boolean : is classList a whitelist (else it must be blacklist)
	 * @param listIsSuperclasses - boolean : does classList represent superclasses (else class must match exactly)
	 * @return string : sparql query
	 * @throws Exception
	 */
	public static String generateConstructConnected(SparqlConnection conn, OntologyInfo oInfo, String instance, XSDSupportedType instanceType, int limit, AbstractCollection<String> classList, boolean listIsWhite, boolean listIsSuperclasses, AbstractCollection<String> extraPredicatesList) throws Exception {
		
		// three triples per line, try to get limit close.  3X limit will come back.  Make sure > LIMIT is returned if it is activated.
		String limitClause = (limit > 0) ? String.format(" LIMIT %d", (int) Math.floor((limit+2)/3.0)) : "";
		String typeFilter = "";
		
		String oTypeClause = "";
		String sTypeClause = "";
		
		/*** extra predicates ***/
		String extraSClause = null;
		String extraOClause = null;
		if (extraPredicatesList != null && extraPredicatesList.size() > 0) {
			ArrayList<String> aList = new ArrayList<String>();
			aList.addAll(extraPredicatesList);
			String xpConstraint = ValueConstraint.buildValuesConstraint("?xp", aList, XSDSupportedType.asSet(XSDSupportedType.URI));
			extraSClause = String.format("OPTIONAL { ?s ?xp ?xs. %s } .\n", xpConstraint);
			extraOClause = String.format("OPTIONAL { ?o ?xp ?xo. %s } .\n", xpConstraint);
		}

		/*** set up the triple clauses ***/
		String oTripleClause = "";
		String sTripleClause = "";
		if (instanceType == null) {
            /**** val is piece of data of unknown type ****/
			// try to guess whether type matters (only if it is a date
			ArrayList<String> rdf11vals = XSDSupportedType.buildPossibleRDF11Values(instance);

			String values = ValueConstraint.buildValuesConstraint("?o", rdf11vals);
			oTripleClause = values + ". ?s ?p ?o . \n" + ((extraSClause != null) ? extraSClause : "");
			sTripleClause = "INTERNAL ERROR";
			
		} else {
			/**** val is a URI ****/
			String val = instanceType.buildRDF11ValueString(instance);

			sTripleClause = "BIND ( " + val + " as ?s) . ?s ?p ?o .\n" + ((extraOClause != null) ? extraOClause : "");
			oTripleClause = "BIND ( " + val + " as ?o) . ?s ?p ?o .\n" + ((extraSClause != null) ? extraSClause : "");
		} 
		
		/*** Add filters by classList, using the triple clauses above ****/
		if (classList == null || classList.size() == 0) {
			sTypeClause = oTripleClause + " OPTIONAL { ?s a ?st } . ";
			oTypeClause = sTripleClause + " OPTIONAL { ?o a ?ot } . ";
		} else {
			
			// expand class list if listIsSuperclasses
			ArrayList<String> fullList = new ArrayList<String>();
			
			if (listIsSuperclasses) {
				HashSet<String> classSet = new HashSet<String>();
				classSet.addAll(classList);
				for (String c : classList) {
					classSet.addAll(oInfo.getSubclassNames(c));
				}
				fullList.addAll(classSet);
			} else {
				fullList.addAll(classList);
			}
			
			
			// build class list constraint
			String sFilter = "";
			String oFilter = "";
			if (listIsWhite) {
				sFilter = ValueConstraint.buildFilterInConstraint("?st", fullList, XSDSupportedType.asSet(XSDSupportedType.URI));
				oFilter = ValueConstraint.buildFilterInConstraint("?ot", fullList, XSDSupportedType.asSet(XSDSupportedType.URI));
			} else {
				sFilter = ValueConstraint.buildFilterNotInConstraint("?st", fullList, XSDSupportedType.asSet(XSDSupportedType.URI));
				oFilter = ValueConstraint.buildFilterNotInConstraint("?ot", fullList, XSDSupportedType.asSet(XSDSupportedType.URI));
			}

			sTypeClause = String.format("{ %s FILTER NOT EXISTS { ?s a ?throwawayST }} UNION { %s ?s a ?st . %s } ", oTripleClause, oTripleClause, sFilter);
			oTypeClause = String.format("{ %s FILTER NOT EXISTS { ?o a ?throwawayOT }} UNION { %s ?o a ?ot . %s } ", sTripleClause, sTripleClause, oFilter);

		}
		
		if (instanceType == null) {
            /**** val is piece of data of unknown type ****/

			return  "CONSTRUCT { ?s ?p ?o.  ?s a ?st. ?s ?xp ?xs. } \n" +
					generateSparqlFromOrUsing("", "FROM", conn, oInfo) +
					"WHERE { " + 
						sTypeClause + " \n" +
					"} " + limitClause;
			
		} else if (instanceType.isURI()) {
			/**** val is a URI ****/

			return  "CONSTRUCT { ?s ?p ?o.  ?s a ?st. ?o a ?ot. ?s ?xp ?xs. ?o ?xp ?xo. } \n" +
					generateSparqlFromOrUsing("", "FROM", conn, oInfo) +
					"WHERE { {  \n" + 
							 oTypeClause + " \n" +
						"} UNION {\n" +
							 sTypeClause + " \n" +
						"} }" + limitClause;
		} else {
			/**** val is piece of data with known type ****/
			
			return  "CONSTRUCT { ?s ?p ?o.  ?s a ?st.  ?s ?xp ?xs. } \n" +
					generateSparqlFromOrUsing("", "FROM", conn, oInfo) +
					"WHERE { " + 
						sTypeClause + " \n" +
					"} " + limitClause;
		} 
	}

	/**
	 * Get cardinality restrictions from every graph in the conn.
	 * Table ?class, ?property ?restriction ?limit
	 * where ?class is optional
	 *       ?property is the property
	 *       ?restriction contains "ardinality"
	 *       ?limit is the integer cardinality limit
	 * @param conn
	 * @param oInfo
	 * @return
	 * @throws Exception
	 */
	public static String generateGetCardinalityRestrictions(SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		String ret = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
				+ "PREFIX owl:<http://www.w3.org/2002/07/owl#>  \n"
				+ " \n"
				+ "SELECT DISTINCT ?class ?property ?restriction ?limit   \n"
				+ " \n"
				+ generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n"
				+ " \n"
				+ "WHERE {  \n"
				+ "  ?r  a <http://www.w3.org/2002/07/owl#Restriction>.  \n"
				+ "  ?r <http://www.w3.org/2002/07/owl#onProperty> ?property.   \n"
				+ "  OPTIONAL {  \n"
				+ "      { \n"
				+ "           ?class <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?r.  \n"
				+             genBlankNodeFilterStatement("?class", false) + "\n"
				+ "       } union { \n"
				+ "            ?un <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?r . \n"
				+ "            ?un owl:unionOf ?list . \n"
				+ "            ?list rdf:rest* ?item . \n"
				+ "            ?item rdf:first ?class. \n"
				+ "       } \n"
				+ "   }   \n"
				+ "  ?r  ?restriction ?limit.  \n"
				+ "  FILTER REGEX (str(?restriction), \"ardinality\" ).  \n"
				+ "} ORDER BY ?class ?property ?restriction  \n"
				;
		return ret;
	}
	
	/**
	 * Query all distinct ?dataType ?equivType ?r_pred ?r_obj
	 * Note: does not handle difference due to Domain, since we could not make SADL generate this
	 * @param graphName 
	 * @param domain
	 * @return
	 */
	public static String generateGetDatatypeRestriction(String graphName, String domain, SparqlConnection conn, OntologyInfo oInfo) throws Exception {

		// SADL makes datatypes either owl:onDatatype or a union of objects w/o the onDatatype predicate (curious)
		String retval = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                		"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> \n" +
						"PREFIX owl:<http://www.w3.org/2002/07/owl#> \n" +
						"SELECT DISTINCT ?dataType ?equivType ?r_pred ?r_obj \n" +
						generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n" +
						"WHERE { \n" +
						"	?dataType rdf:type rdfs:Datatype . \n" +
						genDomainFilterStatement("dataType", domain, "") + "\n" +
						"   ?dataType owl:equivalentClass* ?e . \n" +
						"   { \n " +
						"       ?e owl:onDatatype ?equivType \n " +
						"   } UNION { \n " +
						"       ?e owl:unionOf ?u . \n" +
						"	    ?u rdf:rest* ?r . \n" +
						"	    ?r rdf:first ?equivType . \n" +
						"	} \n" +
						"   optional {  \n" +
						"     ?e owl:withRestrictions ?rlist . \n" +
						"     ?rlist rdf:rest* ?r2 . \n" +
						"     ?r2 rdf:first ?restriction . \n" +
						"     ?restriction ?r_pred ?r_obj . \n" +
						"   } \n" +
						"} ";
		return retval;
	}
	
	/**
	 * Generate the domain filter clause.  If none (the new normal) filter out blank nodes.
	 * @param varName
	 * @param domain
	 * @param clause
	 * @return
	 */
	public static String genDomainFilterStatement(String varName, String domain, String clause) {
		if (domain == null || domain.isEmpty()) {
			// remove blank nodes
			String ret = "filter (!regex(str(?" + varName + "),'" + SparqlToXUtils.BLANK_NODE_REGEX + "') " + clause + ") ";
			return ret;
			
		} else {
			// old-fashioned domain filter
			String ret = "filter (regex(str(?" + varName + "),'^" + domain + "') " + clause + ") . " + genBlankNodeFilterStatement(varName, false) + " ";
			return ret;
			
		}
	}
	
	public static String genBlankNodeFilterStatement(String varName, boolean flag) {	
		String var = !varName.startsWith("?") ? ("?" + varName) : varName;
		String ret = "filter (" + (flag?"":"!") + "regex(str(" + var + "),'" + SparqlToXUtils.BLANK_NODE_REGEX + "')) ";
		return ret;
	}
	
	/**
	 * 
	 * @param conn - data connections
	 * @param oInfo
	 * @param className - class or null
	 * @param predName - predicate to check
	 * @param op - operator for the check
	 * @param limit - limit for the check
	 * @return - sparql that will  return table ?subject ?object_COUNT  for any that match the subject className, predicate, op, and limit
	 * @throws Exception
	 */
	public static String generateCheckCardinalityRestrictions(SparqlConnection conn, OntologyInfo oInfo, String className, String predName, String op, int limit) throws Exception {
		StringBuffer sparql = new StringBuffer();
		sparql.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> \n");
		sparql.append("SELECT DISTINCT ?subject (COUNT(?object) AS ?object_COUNT)  \n");
		sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		sparql.append("WHERE { \n");
		
		if (! className.isEmpty()) {
			sparql.append("  ?subject a ?subject_class . ");
			ArrayList<String> classNames = new ArrayList<String>();
			classNames.addAll(oInfo.getSubclassNames(className));
			
			sparql.append("  " + ValueConstraint.buildBestSubclassConstraint(
					"?subject_class", 
					className,
					classNames, 
					conn.getDefaultQueryInterface()) + " .\n");
		}
		
		sparql.append(" OPTIONAL { ?subject <" + predName + "> ?object } . \n");
		sparql.append("} GROUP BY ?subject \n");
		sparql.append("  HAVING ( ?object_COUNT " + op + " " + Integer.toString(limit) + " ) \n");
		sparql.append(" ORDER BY ?subject \n");
		return sparql.toString();
	}
	
	/**
	 * Count instances of given class (including subclasses)
	 * @param conn - data connections
	 * @param oInfo
	 * @param className - class or null
	 * @return - sparql that will count instances
	 * @throws Exception
	 */
	public static String generateCountInstances(SparqlConnection conn, OntologyInfo oInfo, String className) throws Exception {
		StringBuffer sparql = new StringBuffer();
		sparql.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>  \n"); 
			
		sparql.append("SELECT DISTINCT (COUNT(?subject) AS ?subject_COUNT)  \n");
		sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		sparql.append("WHERE { \n");
		
		sparql.append("  ?subject a ?subject_class . ");
		ArrayList<String> classNames = new ArrayList<String>();
		classNames.addAll(oInfo.getSubclassNames(className));
		
		sparql.append("  " + ValueConstraint.buildBestSubclassConstraint(
				"?subject_class", 
				className,
				classNames, 
				conn.getDefaultQueryInterface()) + " .\n");
		
		sparql.append("} \n");
		return sparql.toString();
	}
	
	/**
	 * Add all incoming and outgoing triples of duplicateURI to targetURI
	 * @param conn
	 * @param targetURI
	 * @param duplicateURI
	 * @return
	 */
	public static String generateCombineEntitiesInsertOutgoing(SparqlConnection conn, String targetURI, String duplicateURI) throws Exception {
		StringBuilder ret = new StringBuilder();
		ret.append("INSERT { \n");
		ret.append(generateGraphClause(conn.getInsertInterface(), "    "));
		ret.append("    {\n");
		ret.append(String.format("        <%s> ?outgoingPred ?outgoingObj . \n", targetURI));
		ret.append("    }\n");
		ret.append("} \n");
		ret.append(generateUsingDatagraphsClause(conn, ""));
		ret.append("WHERE { \n");
		ret.append(String.format("        <%s> ?outgoingPred ?outgoingObj . \n", duplicateURI));
		ret.append("} \n");
		return ret.toString();
	}
	
	/**
	 * Add all incoming and outgoing triples of duplicateURI to targetURI
	 * @param conn
	 * @param targetURI
	 * @param duplicateURI
	 * @return
	 */
	public static String generateCombineEntitiesInsertIncoming(SparqlConnection conn, String targetURI, String duplicateURI) throws Exception {
		StringBuilder ret = new StringBuilder();
		ret.append("INSERT { \n");
		ret.append(generateGraphClause(conn.getInsertInterface(), "    "));
		ret.append("    {\n");
		ret.append(String.format("        ?incomingSub ?incomingPred <%s> . \n", targetURI));
		ret.append("    }\n");
		ret.append("} \n");
		ret.append(generateUsingDatagraphsClause(conn, ""));
		ret.append("WHERE { \n");
		ret.append(String.format("        ?incomingSub ?incomingPred <%s> . \n", duplicateURI));
		ret.append("} \n");
		return ret.toString();
	}
	
	/**
	 * Add all incoming and outgoing triples of duplicateURI to targetURI
	 * @param conn
	 * @param targetURI
	 * @param duplicateURI
	 * @return
	 */
	public static String generateDeleteExactProps(SparqlConnection conn, String itemURI, ArrayList<String> exactPropURIs) throws Exception {
		StringBuilder ret = new StringBuilder();
		ret.append("DELETE { \n");
		ret.append(generateGraphClause(conn.getInsertInterface(), "    "));
		ret.append("    {\n");
		ret.append(String.format("        <%s> ?pred ?obj . \n", itemURI));
		ret.append("    }\n");
		ret.append("} \n");
		ret.append(generateUsingDatagraphsClause(conn, ""));
		ret.append("WHERE { \n");
		ret.append(String.format("        <%s> ?pred ?obj . \n", itemURI));
		ret.append(String.format("        %s . \n", 
				ValueConstraint.buildBestListConstraint("?pred", exactPropURIs, XSDSupportedType.asSet(XSDSupportedType.URI), conn.getDeleteInterface()))
				);
		ret.append("} \n");
		return ret.toString();
	}
	
	public static String generateDeleteUri(SparqlConnection conn, String itemURI) throws Exception {
		ArrayList<String> uriAsList = new ArrayList<String>();
		uriAsList.add(itemURI);
		return generateDeleteUris(conn, uriAsList);
	}

	public static String generateDeleteUris(SparqlConnection conn, AbstractCollection<String> itemURIs) throws Exception {
		
		StringBuilder ret = new StringBuilder();
		ret.append("DELETE { \n");
		ret.append(generateGraphClause(conn.getInsertInterface(), "  ") + "{");
		ret.append("      ?s ?p ?o . \n");
		ret.append("  }\n");
		ret.append("} \n");
		ret.append(generateUsingDatagraphsClause(conn, ""));
		ret.append("WHERE { \n");
		ret.append("  {\n");
		ret.append("    ?s ?p ?o . " + ValueConstraint.buildBestListConstraint("?s", itemURIs, XSDSupportedType.asSet(XSDSupportedType.URI), conn.getDeleteInterface()));
		ret.append("  } UNION {\n");
		ret.append("    ?s ?p ?o . " + ValueConstraint.buildBestListConstraint("?o", itemURIs, XSDSupportedType.asSet(XSDSupportedType.URI), conn.getDeleteInterface()));
		ret.append("  } \n");
		ret.append("} \n");
		return ret.toString();
	}
	
	/**
	 * 
	 * @param conn
	 * @param triples - need to be typed
	 * @return
	 * @throws Exception
	 */
	public static String generateInsertTriples(SparqlConnection conn, AbstractCollection<Triple> triples) throws Exception {
		StringBuilder ret = new StringBuilder();
		ret.append("INSERT DATA { \n");
		ret.append("  " + generateGraphClause(conn.getInsertInterface(), "  ") + "{\n");
		for (Triple t : triples) {
			ret.append("    " + t.getSubject() + " " + t.getPredicate() + " " + t.getObject() + "  . \n" );
		}
		ret.append("  }\n");
		ret.append("} \n");
		return ret.toString();
	}
	
	/**
	 * 
	 * @param conn
	 * @param triples - need to be typed
	 * @return
	 * @throws Exception
	 */
	public static String generateDeleteTriples(SparqlConnection conn, AbstractCollection<Triple> triples) throws Exception {
		StringBuilder ret = new StringBuilder();
		ret.append("DELETE DATA { \n");
		ret.append("  " + generateGraphClause(conn.getInsertInterface(), "  ") + "{\n");
		for (Triple t : triples) {
			ret.append("    " + t.getSubject() + " " + t.getPredicate() + " " + t.getObject() + "  . \n" );
		}
		ret.append("  }\n");
		ret.append("} \n");
		return ret.toString();
	}
	
	public static String generateCountInstanceProperties(SparqlConnection conn, OntologyInfo oInfo, String itemURI) throws Exception {
		return "select distinct ?prop (count (?obj) as ?count)  \n"
				+ generateSparqlFromOrUsing("", "FROM", conn, oInfo)
				+ " where {\n"
				+ "\n"
				+ "	 <" + itemURI + "> ?prop ?obj.\n"
				+ "\n"
				+ "} group by ?prop\n";
				
	}
	
	public static String generateTriplesIncomingOrOutgoing(SparqlConnection conn, OntologyInfo oInfo, AbstractCollection<String> itemURIs) throws Exception {
		
		StringBuilder ret = new StringBuilder();
		ret.append("SELECT DISTINCT ?subject ?predicate ?object  \n");
		ret.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		ret.append("WHERE { \n");
		ret.append("  {\n");
		ret.append("    ?subject ?predicate ?object . " + ValueConstraint.buildBestListConstraint("?subject", itemURIs, XSDSupportedType.asSet(XSDSupportedType.URI), conn.getDefaultQueryInterface()));
		ret.append("  } UNION {\n");
		ret.append("    ?subject ?predicate ?object . " + ValueConstraint.buildBestListConstraint("?object", itemURIs, XSDSupportedType.asSet(XSDSupportedType.URI), conn.getDefaultQueryInterface()));
		ret.append("  } \n");
		ret.append("} \n");
		return ret.toString();
	}
	
	public static String generateGraphClause(SparqlEndpointInterface sei, String tab) {
		return tab + "GRAPH <" + sei.getGraph() + ">\n" ;
	}
	
	public static String generateUsingDatagraphsClause(SparqlConnection conn, String tab) {
		StringBuilder ret = new StringBuilder();
		for (SparqlEndpointInterface sei : conn.getDataInterfaces()) {
			ret.append(tab + "USING <" + sei.getGraph() + ">\n" );
		}
		return ret.toString();
	}
}
