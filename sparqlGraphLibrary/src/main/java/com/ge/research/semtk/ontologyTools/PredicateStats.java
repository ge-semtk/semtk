package com.ge.research.semtk.ontologyTools;

import java.util.HashSet;
import java.util.Hashtable;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Stores a list of counts of [subject_class pred object_class] from instance data
 * 
 * Constructor using conn queries the triplestore directly through the conn's seis
 * @author 200001934
 *
 */
public class PredicateStats {
	
	// note: in the old days of experimenting, there were actual stats.  No longer needed.
	
	// exactHash :  exact count with no inheritance.  subject_class optional_predicate optional_object_class
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
		this.storeStats(statsTab, oInfo, dataConn, null, null, 0, 0);
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
		this.storeStats(statsTab, oInfo, conn, tracker, jobId, queryDonePercent, endPercent);
	}	
	
	/**
	 * Construct from JSON
	 * @param jObj
	 * @throws Exception
	 */
	public PredicateStats(JSONObject jObj) throws Exception {

		JSONObject exactTabJson =  (JSONObject) jObj.get("exactTab");
		for (Object k : exactTabJson.keySet()) {
			this.exactHash.put((String) k, (Long) exactTabJson.get(k));
		}
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject ret = new JSONObject();
		
		JSONObject exactTabJson = new JSONObject();
		for (String key : this.exactHash.keySet()) {
			exactTabJson.put(key, this.exactHash.get(key));
		}
		ret.put("exactTab", exactTabJson);
		return ret;
	}
	

	/**
	 * Get count of this exact relationship
	 * @param subjectClass
	 * @param predicate
	 * @param objectClass
	 * @return
	 * @throws PathException 
	 */
	public long getExact(String subjectClass, String predicate, String objectClass) throws PathException {
		String key = this.buildKey(subjectClass, predicate, objectClass);
		Long ret = this.exactHash.get(key);
		return (ret == null) ? 0 : ret;
	}
	
	private String buildKey(String subjectClass, String predicate, String objectClass) throws PathException {
		
		OntologyPath path = new OntologyPath(subjectClass);
		path.addTriple(subjectClass, predicate, objectClass);
		return this.buildKey(path);
	}
	
	private String buildKey(OntologyPath path) {
		return path.toJson().toJSONString();
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
	private void storeStats(Table tab, OntologyInfo oInfo, SparqlConnection conn, JobTracker tracker, String jobId, int startPercent, int endPercent) throws AuthorizationException, Exception {
		final String STATUS = "calculating stats";
//		final String W3 = "http://www.w3.org";
		
		// hash start class or endclass to a list of one-hop triples
		Hashtable<String, HashSet<Triple>> tripleHash = new Hashtable<String, HashSet<Triple>>();
		// hash an exactHash key to endClass
		Hashtable<String, OntologyPath> pathHash = new Hashtable<String, OntologyPath>();
		
		
		double rows = tab.getNumRows();
		for (int i=0; i < tab.getNumRows(); i++) {
			if (tracker != null && i % 2000 == 0) {
				int percent = (int) (startPercent + (endPercent - startPercent) * (i / rows));
				tracker.setJobPercentComplete(jobId, percent, STATUS);
			}
			// extract info from query result row
			String sclass = tab.getCell(i, 0);
			String pred = tab.getCell(i, 1);
			String oclass = tab.getCell(i, 2);
			long count =  tab.getCellAsInt(i, 3);
			
//  Need #type for Explore tab, so consider this a failed experiment
//			if (sclass.startsWith(W3) || pred.startsWith(W3) || oclass.startsWith(W3))
//				continue;
			
			// build one-hop Triple and add to tripleHash twice: once for subject, once for object
			// do not include data properties (oclass is empty)
			if (!oclass.isEmpty()) {
				Triple t = new Triple(sclass, pred, oclass);
			
				if (! tripleHash.containsKey(sclass)) {
					tripleHash.put(sclass, new HashSet<Triple>());
				}
				tripleHash.get(sclass).add(t);
				if (! tripleHash.containsKey(oclass)) {
					tripleHash.put(oclass, new HashSet<Triple>());
				}
				tripleHash.get(oclass).add(t);
			}
				
			
			// add one-hops to exactHash
			OntologyPath p = new OntologyPath(sclass);
			p.addTriple(sclass, pred, oclass);
			String key = this.buildKey(p);
			this.exactHash.put(key, count);
			
			// hash the other details
			pathHash.put(key, p);
			
		}
		
		
		
		//-------- build additional hops ---------//
		// Seems to have insurmountable performance problems //
		// TODO
		// needs to be used by path-finding: check the end of the new path
		// smarter setJobPercentComplete()
		// needs return only one-hops to SPARQLgraph
		// MAX_HOPS would be higher, and probably accessible for findExactPaths()
		final int MAX_HOPS = 1;
		HashSet<String> lastLenKeys = new HashSet<String>();
		lastLenKeys.addAll(this.exactHash.keySet());
		
		// for each length of path up the the max we want to compute
		for (int thisLen=2; thisLen <= MAX_HOPS; thisLen++) {
			HashSet<String> thisLenKeys = new HashSet<String>();
			// for each path of the previous length
			for (String key : lastLenKeys) {
				OntologyPath p = pathHash.get(key);
				// for each triple that could be added to the end
				HashSet<Triple> triples = tripleHash.get(p.getEndClassName());
				if (triples != null) {
					for (Triple t : tripleHash.get(p.getEndClassName())) {
						// if new triple wouldn't cause a loop
						if (!p.containsClass(t.getSubject()) || !p.containsClass(t.getObject())) {
							// add triple to new path
							OntologyPath pathCopy = p.deepCopy();
							pathCopy.addTriple(t.getSubject(), t.getPredicate(), t.getObject());
							
							long instanceCount = this.countInstanceData(pathCopy, conn, oInfo);
							if (instanceCount > 0) {
								String k = this.buildKey(pathCopy);
								this.exactHash.put(k, instanceCount);
								thisLenKeys.add(k);
								pathHash.put(k, pathCopy);
							}
						}
					}
				}
			}
			
			lastLenKeys = thisLenKeys;
		}
		
		
		
		///////// DEBUG ///////////
		//for (OntologyPath p : pathHash.values()) {
		//	LocalLogger.logToStdOut(p.debugString());
		//}
		///////////////////////////
		
		if (tracker != null) {
			tracker.setJobPercentComplete(jobId, endPercent, STATUS);
		}
	}
	
	private int countInstanceData(OntologyPath path, SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		String query = SparqlToXUtils.generatePathInstanceCountQuery(path, conn, oInfo);
		Table tab = conn.getDefaultQueryInterface().executeQueryToTable(query);
		return tab.getCellAsInt(0, 0);
	}
}
