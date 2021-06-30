package com.ge.research.semtk.ontologyTools;

import java.util.Calendar;

import com.ge.research.semtk.sparqlX.SparqlConnection;

public class CachedPredicateStats {
	private PredicateStats stats = null;

	private Long retrievedMillis = null;
	private SparqlConnection conn = null;
	
	public CachedPredicateStats(SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		this.stats = new PredicateStats(conn, oInfo);
		this.retrievedMillis = Calendar.getInstance().getTimeInMillis();
		this.conn = conn;
	}

	/**
	 * Get PredicateStats, retrieving it again if it's too old
	 * @param maxAgeMillis
	 * @return
	 * @throws Exception
	 */
	public PredicateStats getPredicateStats(long maxAgeMillis, OntologyInfo oInfo) throws Exception {
		if (this.isExpired(maxAgeMillis)) {
			this.stats = new PredicateStats(conn, oInfo);
		}
		return this.stats;
	}
	
	public boolean isExpired(long maxAgeMillis) {
		Long ageMillis = Calendar.getInstance().getTimeInMillis() - this.retrievedMillis;
		return (ageMillis > maxAgeMillis);
	}

}
