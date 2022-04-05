package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.ge.research.semtk.sparqlX.SparqlConnection;

public class OntologyInfoCache {
	
	long maxAgeMillis = 1000 * 60 * 5;   // five minutes
	ConcurrentHashMap<String, CachedOntologyInfo> hash = new ConcurrentHashMap<String, CachedOntologyInfo>();
	
	public OntologyInfoCache(long maxAgeMillis) {
		this.maxAgeMillis = maxAgeMillis;
	}
	
	/**
	 * Get an oInfo based on the connection.  Retrieve from cache if possible.
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public OntologyInfo get(SparqlConnection conn) throws Exception {
		String key = conn.getUniqueModelKey();
		
		this.clearExpired();
		
		// in addition to the ConcurrentHashMap synchronization
		// 1) synchronize the check for contains with the insertion of new
		// 2) synchronize the CachedOntologyInfo(conn) and .getOInfo() 
		//       because they both run queries and build the oInfo
		synchronized(key) {
			if (!hash.containsKey(key)) {
				hash.put(key, new CachedOntologyInfo(conn));
			}
		
			return hash.get(key).getOInfo(this.maxAgeMillis);
		}
	}
	
	/**
	 * Clear a connection from the cache, presumably because the ontology has changed.
	 * @param conn
	 */
	public void remove(SparqlConnection conn) {
		String key = conn.getUniqueModelKey();
		if (this.hash.containsKey(key)) {
			this.hash.remove(key);
		}
	}
	
	/**
	 * Clear any connection from cache if it shares a model dataset with conn
	 * @param conn
	 */
	public void removeOverlapping(SparqlConnection conn) {
		ArrayList<String> toDelete = new ArrayList<String>();
		
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
