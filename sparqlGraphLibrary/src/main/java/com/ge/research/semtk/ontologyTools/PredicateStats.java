package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;

/**
 * Stores a list of counts of [subject_class pred object_class] from instance data
 * Each instance increments all super classes' and super predicates' counts
 * 
 * Constructor using conn queries the triplestore directly through the conn's seis
 * @author 200001934
 *
 */
public class PredicateStats {
	
	private Hashtable<String, Integer> statsHash = new Hashtable<String, Integer>();
	
	
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
	
	/**
	 * Construct from JSON
	 * @param jObj
	 * @throws Exception
	 */
	public PredicateStats(JSONObject jObj) throws Exception {
		JSONObject statsTabJson =  (JSONObject) jObj.get("statsTab");
		for (Object k : statsTabJson.keySet()) {
			this.statsHash.put((String) k, (Integer) statsTabJson.get(k));
		}
	}

	@SuppressWarnings("unchecked")
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
		for (int i=0; i < t.getNumRows(); i++) {
			String sub = t.getCell(i, 0);
			String pred = t.getCell(i, 1);
			String obj = t.getCell(i, 2);
			int count =  t.getCellAsInt(i, 3);
			HashSet<String> subjects = oInfo.getSuperclassNames(sub);
			HashSet<String> predicates = oInfo.getSuperPropNames(pred);
			HashSet<String> objects = oInfo.getSuperclassNames(obj);
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
