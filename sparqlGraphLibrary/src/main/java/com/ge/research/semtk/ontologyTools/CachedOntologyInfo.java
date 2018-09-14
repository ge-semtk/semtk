package com.ge.research.semtk.ontologyTools;

import java.util.Calendar;
import java.util.Date;

import com.ge.research.semtk.sparqlX.SparqlConnection;

public class CachedOntologyInfo {
	private OntologyInfo oInfo = null;

	private Long retrievedMillis = null;
	private SparqlConnection conn = null;
	
	public CachedOntologyInfo(SparqlConnection conn) throws Exception {
		this.oInfo = new OntologyInfo(conn);
		this.retrievedMillis = Calendar.getInstance().getTimeInMillis();
		this.conn = conn;
	}

	/**
	 * Get oInfo, retrieving it again if it's too old
	 * @param maxAgeMillis
	 * @return
	 * @throws Exception
	 */
	public OntologyInfo getOInfo(long maxAgeMillis) throws Exception {
		Long ageMillis = Calendar.getInstance().getTimeInMillis() - this.retrievedMillis;
		if (ageMillis > maxAgeMillis) {
			this.oInfo = new OntologyInfo(this.conn);
		}
		return oInfo;
	}

}
