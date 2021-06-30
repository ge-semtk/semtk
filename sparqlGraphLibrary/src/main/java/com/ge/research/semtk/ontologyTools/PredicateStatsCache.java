package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

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
	public synchronized void removeSimilar(SparqlConnection conn) {
		HashSet<String> toDelete = new HashSet<String>();
		
		// search through conn's models
		for (int i=0; i < conn.getModelInterfaceCount(); i++) {
			for (String key : this.hash.keySet()) {
				// if datasets match, it's too close for comfort: delete
				if (key.contains(conn.getModelInterface(i).getGraph())) {
					if (! toDelete.contains(key)) {
						toDelete.add(key);
					}
				}
			}
		}
		
		// search through conn's models
		for (int i=0; i < conn.getDataInterfaceCount(); i++) {
			for (String key : this.hash.keySet()) {
				// if datasets match, it's too close for comfort: delete
				if (key.contains(conn.getDataInterface(i).getGraph())) {
					if (! toDelete.contains(key)) {
						toDelete.add(key);
					}
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
