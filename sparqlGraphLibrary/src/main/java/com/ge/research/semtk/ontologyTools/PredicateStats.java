package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.edc.JobTracker;
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
	
	private Hashtable<String, Long> statsHash = new Hashtable<String, Long>();
	private Hashtable<String, Long> exactHash = new Hashtable<String, Long>();
	
	
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
		this.storeStats(statsTab, oInfo, null, null, 0, 0);
	}	
	
	/**
	 * Constructor that keeps a jobTracker up to date on progress
	 * @param conn
	 * @param oInfo
	 * @param tracker
	 * @param jobId
	 * @param startPercent
	 * @param endPercent
	 * @throws Exception
	 */
	public PredicateStats(SparqlConnection conn, OntologyInfo oInfo, JobTracker tracker, String jobId, int startPercent, int endPercent) throws Exception {
		SparqlConnection dataConn = SparqlConnection.deepCopy(conn);
		dataConn.clearModelInterfaces();
		String sparql = SparqlToXUtils.generatePredicateStatsQuery(dataConn, oInfo);
		
		tracker.setJobPercentComplete(jobId, startPercent, "querying predicate statistics");
		Table statsTab = conn.getDefaultQueryInterface().executeQueryToTable(sparql);
		
		int queryDonePercent = startPercent + (endPercent - startPercent) / 2;
		this.storeStats(statsTab, oInfo, tracker, jobId, queryDonePercent, endPercent);
	}	
	
	/**
	 * Construct from JSON
	 * @param jObj
	 * @throws Exception
	 */
	public PredicateStats(JSONObject jObj) throws Exception {
		JSONObject statsTabJson =  (JSONObject) jObj.get("statsTab");
		for (Object k : statsTabJson.keySet()) {
			this.statsHash.put((String) k, (Long) statsTabJson.get(k));
		}
		JSONObject exactTabJson =  (JSONObject) jObj.get("exactTab");
		for (Object k : exactTabJson.keySet()) {
			this.exactHash.put((String) k, (Long) exactTabJson.get(k));
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
		
		JSONObject exactTabJson = new JSONObject();
		for (String key : this.exactHash.keySet()) {
			exactTabJson.put(key, this.exactHash.get(key));
		}
		ret.put("exactTab", exactTabJson);
		return ret;
	}
	
	/**
	 * Get count of this relationship including subects predicates and objects that are sub-class or sub-pred
	 * @param subject
	 * @param predicate
	 * @param object
	 * @return
	 */
	public long getStat(String subject, String predicate, String object) {
		String key = this.buildKey(subject, predicate, object);
		Long ret = this.statsHash.get(key);
		return (ret == null) ? 0 : ret;
	}
	
	/**
	 * Get count of this exact relationship
	 * @param subject
	 * @param predicate
	 * @param object
	 * @return
	 */
	public long getExact(String subject, String predicate, String object) {
		String key = this.buildKey(subject, predicate, object);
		Long ret = this.exactHash.get(key);
		return (ret == null) ? 0 : ret;
	}
	
	private String buildKey(String subject, String predicate, String object) {
		return subject + "|" + predicate + "|" + object;
	}
	
	/**
	
	 * @param t
	 * @param oInfo
	 * @throws Exception 
	 * @throws AuthorizationException 
	 */

	/**
	 * For a table of exact results  subj pred obj count
	 * set statsHash such that hash(s,p,o) gets sum of all triples where:
	 *         subject_superclass* predicate_superclass* object_superclass*	 * @param t
	 *         
	 * If tracker is not null, send job percent complete info.  
	 * 
	 * @param oInfo
	 * @param tracker
	 * @param jobId
	 * @param startPercent
	 * @param endPercent
	 * @throws AuthorizationException
	 * @throws Exception
	 */
	private void storeStats(Table t, OntologyInfo oInfo, JobTracker tracker, String jobId, int startPercent, int endPercent) throws AuthorizationException, Exception {
		final String STATUS = "calculating stats";
		
		double rows = t.getNumRows();
		for (int i=0; i < t.getNumRows(); i++) {
			if (tracker != null && i % 2000 == 0) {
				int percent = (int) (startPercent + (endPercent - startPercent) * (i / rows));
				tracker.setJobPercentComplete(jobId, percent, STATUS);
			}
			String sub = t.getCell(i, 0);
			String pred = t.getCell(i, 1);
			String obj = t.getCell(i, 2);
			long count =  t.getCellAsInt(i, 3);
			HashSet<String> subjects = oInfo.getSuperclassNames(sub);
			HashSet<String> predicates = oInfo.getSuperPropNames(pred);
			HashSet<String> objects = oInfo.getSuperclassNames(obj);
			subjects.add(sub);
			predicates.add(pred);
			objects.add(obj);
			this.exactHash.put(this.buildKey(sub, pred, obj), count);
			
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
		if (tracker != null) {
			tracker.setJobPercentComplete(jobId, endPercent, STATUS);
		}
	}
}
