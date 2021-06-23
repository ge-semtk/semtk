package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;

public class PredicateStats {
	
	private Hashtable<String, Integer> statsHash = new Hashtable<String, Integer>();
	
	private Table statsTab = null;    // quickly produce JSON if needed
	
	/** 
	 * Get stats from the data sei's in a connection.
	 * Use a direct call to the triplestore.
	 * @param conn
	 */
	public PredicateStats(SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		SparqlConnection dataConn = SparqlConnection.deepCopy(conn);
		dataConn.clearModelInterfaces();
		String sparql = SparqlToXUtils.generatePredicateStatsQuery(dataConn, oInfo);
		Table statsTab = conn.getDefaultQueryInterface().executeQueryToTable(sparql);
		this.storeStats(statsTab, oInfo);
	}	
	
	public PredicateStats(JSONObject jObj) throws Exception {
		JSONObject statsTabJson =  (JSONObject) jObj.get("statsTab");
		for (Object k : statsTabJson.keySet()) {
			this.statsHash.put((String) k, (Integer) statsTabJson.get(k));
		}
	}

	public JSONObject toJson() {
		JSONObject ret = new JSONObject();
		JSONObject statsTabJson = new JSONObject();
		for (String key : this.statsHash.keySet()) {
			statsTabJson.put(key, this.statsHash.get(key));
		}
		ret.put("statsTab", statsTabJson);
		return ret;
	}
	
	public int getCount(String subject, String predicate, String object) {
		String key = this.buildKey(subject, predicate, object);
		Integer ret = this.statsHash.get(key);
		return (ret == null) ? 0 : ret;
	}
	
	private String buildKey(String subject, String predicate, String object) {
		return subject + "|" + predicate + "|" + object;
	}
	
	/**
	 * For a table of exact results  subj pred obj count
	 * set statsHash such that hash(s,p,o) gets sum of all triples where:
	 *         subject_superclass* predicate_superclass* object_superclass*
	 * @param t
	 * @param oInfo
	 */
	private void storeStats(Table t, OntologyInfo oInfo) {
		this.statsTab = t;
		for (int i=0; i < t.getNumRows(); i++) {
			String sub = t.getCell(i, 0);
			String pred = t.getCell(i, 1);
			String obj = t.getCell(i, 2);
			int count =  t.getCellAsInt(i, 3);
			ArrayList<String> subjects = oInfo.getSuperclassNames(sub);
			HashSet<String> predicates = oInfo.getSuperPropNames(pred);
			ArrayList<String> objects = oInfo.getSuperclassNames(obj);
			subjects.add(sub);
			predicates.add(pred);
			objects.add(obj);
			
			// loop through super predicates, subjects and objects
			for (String p : predicates) {
				for (String s : subjects) {
					for (String o : objects) {
						String key = this.buildKey(s,p,o);
						if (this.statsHash.contains(key)) {
							// add to existing count
							this.statsHash.put(key, this.statsHash.get(key) + count);
						} else {
							// set new count
							this.statsHash.put(key, count);
						}
					}
				}
			}
		}
	}
}
