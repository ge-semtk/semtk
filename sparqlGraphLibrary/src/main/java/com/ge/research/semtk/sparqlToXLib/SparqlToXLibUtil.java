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


import java.util.ArrayList;
import java.util.HashSet;

import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.Triple;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.sparqlX.XSDSupportedType;


/*
 * Split from SparqlToXUtils - functions requiring Ontology and other advance sparqlGraphLibrary imports
 */
public class SparqlToXLibUtil {
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
		conn.confirmSingleServerURL();
		
		// get graphs/datasets for first model server.  All others must be equal
		ArrayList<String> datasets = conn.getAllGraphsForServer(conn.getDataInterface(0).getServerAndPort());
		
		// add graphs from owlImports
		if (oInfo != null) {
			ArrayList<String> owlImports = oInfo.getImportedGraphs();
			for (String g : owlImports) {
				datasets.add(g);
			}
		}
				
		StringBuilder sparql = new StringBuilder().append("\n");
		// No optimization: always "from" all datasets
		tab = SparqlToXUtils.tabIndent(tab);
		for (int i=0; i < datasets.size(); i++) {
			sparql.append(tab + fromOrUsing + " <" + datasets.get(i) + ">\n");
		}
		tab = SparqlToXUtils.tabOutdent(tab);
		
		return sparql.toString();
	}
	
	public static String generatePredicateStatsQuery(SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		
		String sparql = String.format(
				"select ?s_class ?p ?o_class (COUNT(*) as ?count)\n"
				+ "%s\n"
				+ "WHERE {\n"
				+ "	   ?s a ?s_class.\n"
				+ "	   ?s ?p ?o .\n"
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
		
		System.out.println(sparql.toString());
		return sparql.toString();
	}

	public static String generateConstructConnected(SparqlConnection conn, OntologyInfo oInfo, String instance, XSDSupportedType instanceType) throws Exception {
		
		if (instanceType == XSDSupportedType.NODE_URI) {
			String val = instanceType.buildRDF11ValueString(instance);
			
			return  "CONSTRUCT { ?s ?p ?o.  ?s a ?st. ?o a ?ot } \n" +
					generateSparqlFromOrUsing("", "FROM", conn, oInfo) +
					"WHERE { " + 
					"{ BIND ( " + val + " as ?s) . ?s ?p ?o } \n" +
					"UNION \n" +
					"{ BIND ( " + val + " as ?o) . ?s ?p ?o } \n" +
					"OPTIONAL { ?s a ?st } \n" +
					"OPTIONAL { ?o a ?ot } \n" +
					"}";
		} else if (instanceType != null) {
			String val = instanceType.buildRDF11ValueString(instance);
			
			return  "CONSTRUCT { ?s ?p ?o.  ?s a ?st.  } \n" +
					generateSparqlFromOrUsing("", "FROM", conn, oInfo) +
					"WHERE { " + 
					"BIND ( " + val + " as ?o) . ?s ?p ?o . \n" +
					"OPTIONAL { ?s a ?st } \n" +
					"}";
		} else {
			
			// try to guess whether type matters (only if it is a date
			ArrayList<String> rdf11vals = XSDSupportedType.buildPossibleRDF11Values(instance);
			
			String values = ValueConstraint.buildValuesConstraint("?o", rdf11vals);
			return  "CONSTRUCT { ?s ?p ?o.  ?s a ?st.  } \n" +
				generateSparqlFromOrUsing("", "FROM", conn, oInfo) +
				"WHERE { " + 
				values + ". ?s ?p ?o . \n"  +
				"OPTIONAL { ?s a ?st } \n" +
				"}";
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
		StringBuffer sparql = new StringBuffer();
		sparql.append("SELECT DISTINCT ?class ?property ?restriction ?limit  \n");
		sparql.append(generateSparqlFromOrUsing("", "FROM", conn, oInfo) + "\n");
		sparql.append("WHERE { \n");
		sparql.append("  ?r  a <http://www.w3.org/2002/07/owl#Restriction>. \n");
		sparql.append("  ?r <http://www.w3.org/2002/07/owl#onProperty> ?property.  \n");
		// note:  very weird that the class is subClassOf the restriction -Paul
		sparql.append("  OPTIONAL { ?class <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?r. }  \n");   
		sparql.append("  ?r  ?restriction ?limit. \n");
		sparql.append("  FILTER REGEX (str(?restriction), \"ardinality\" ). \n");  
		sparql.append("} ORDER BY ?class ?property ?restriction \n");
		return sparql.toString();
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
		
		return sparql.toString();
	}
}
