package com.ge.research.semtk.ontologyTools;

import java.util.Hashtable;

import com.ge.research.semtk.sparqlX.SparqlConnection;

public class OntologyInfoCache {
	
	long maxAgeMillis = 1000 * 60 * 5;   // five minutes
	Hashtable<String, CachedOntologyInfo> hash = new Hashtable<String, CachedOntologyInfo>();
	
	public OntologyInfoCache(long maxAgeMillis) {
		this.maxAgeMillis = maxAgeMillis;
	}
	
	/**
	 * Get an oInfo based on the connection.  Retrieve from cache if possible.
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public synchronized OntologyInfo get(SparqlConnection conn) throws Exception {
		String key = conn.getUniqueModelKey();
		
		if (!hash.containsKey(key)) {
			hash.put(key, new CachedOntologyInfo(conn));
		}
		
		return hash.get(key).getOInfo(this.maxAgeMillis);
	}
}
