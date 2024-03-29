package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.sparqlX.SparqlConnection;

public class PredicateStatsCache {
	
	long maxAgeMillis = 1000 * 60 * 120;   // 120 minutes
	Hashtable<String, CachedPredicateStats> hash = new Hashtable<String, CachedPredicateStats>();
	
	public PredicateStatsCache(long maxAgeMillis) {
		this.maxAgeMillis = maxAgeMillis;
	}
	
	/**
	 * Get an oInfo based on the connection.  Retrieve from cache if possible.
	 * @param conn
	 * @param oInfo - Used to create new stats.  Passed in so caller can control performance.
	 * @return
	 * @throws Exception
	 */
	public synchronized PredicateStats get(SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		String key = conn.getUniqueKey();
		
		this.clearExpired();
		
		if (!hash.containsKey(key)) {
			hash.put(key, new CachedPredicateStats(conn, oInfo));
		}
		
		return hash.get(key).getPredicateStats(this.maxAgeMillis, oInfo);
	}
	
	public synchronized PredicateStats get(SparqlConnection conn, OntologyInfo oInfo, JobTracker tracker, String jobId, int startPercent, int endPercent) throws Exception {
		String key = conn.getUniqueKey();
		
		this.clearExpired();
		
		if (!hash.containsKey(key)) {
			hash.put(key, new CachedPredicateStats(conn, oInfo, tracker, jobId, startPercent, endPercent));
		}
		
		return hash.get(key).getPredicateStats(this.maxAgeMillis, oInfo);
	}
	
	/**
	 * Get if cached, else null
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public synchronized PredicateStats getIfCached(SparqlConnection conn) throws Exception {
		String key = conn.getUniqueKey();
		this.clearExpired();
		
		CachedPredicateStats cached = this.hash.get(key);
		return cached == null ? null : cached.getPredicateStats();
	}
	
	/**
	 * Clear a connection from the cache, presumably because the ontology has changed.
	 * @param conn
	 */
	public synchronized void remove(SparqlConnection conn) {
		String key = conn.getUniqueKey();
		if (this.hash.containsKey(key)) {
			this.hash.remove(key);
		}
	}
	
	/**
	 * Clear any connection from cache if it shares a model dataset with conn
	 * @param conn
	 */
	public synchronized void removeOverlapping(SparqlConnection conn) {
		HashSet<String> toDelete = new HashSet<String>();
		
		// search through conn's models
		for (String key : this.hash.keySet()) {
			if (conn.overlapsSparqlConnKey(key)) {
				if (! toDelete.contains(key)) {
					toDelete.add(key);
				}
			}
		}
		
		for (String d : toDelete) {
			this.hash.remove(d);
		}
	}
	
	private void clearExpired() {
		ArrayList<String> keysToRemove = new ArrayList<String>();
		
		// find expired keys
		for (String key : this.hash.keySet()) {
			if (this.hash.get(key).isExpired(this.maxAgeMillis)) {
				keysToRemove.add(key);
			}
		}
		
		// remove expired keys (avoiding concurrentModificationException)
		for (String key : keysToRemove) {
			this.hash.remove(key);
		}
	}
}
