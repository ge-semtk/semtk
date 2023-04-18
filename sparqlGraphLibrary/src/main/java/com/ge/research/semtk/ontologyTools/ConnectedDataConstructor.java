package com.ge.research.semtk.ontologyTools;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.utility.LocalLogger;

public class ConnectedDataConstructor extends Thread {
	
	private String instanceVal = null;
	private XSDSupportedType instanceType = null;
	private SparqlResultTypes resultType = SparqlResultTypes.GRAPH_JSONLD;
	private SparqlConnection conn = null;
	private OntologyInfo oInfo = null;
	private JobTracker tracker = null;
	private String jobId = null;
	private ResultsClient resClient = null;
	private HeaderTable headerTable = null;
	private Exception e = null;
	
	/**
	 * 
	 * @param instanceVal
	 * @param instanceType
	 * @param conn
	 * @param oInfo - oInfo if cached, leave null and it will be read from conn
	 * @param tracker - may be null if not intending to use as a thread
	 * @param resClient - "
	 * @throws Exception 
	 */
	public ConnectedDataConstructor(String instanceVal, XSDSupportedType instanceType, SparqlResultTypes resultType, SparqlConnection conn, OntologyInfo oInfo, JobTracker tracker, ResultsClient resClient) throws Exception {
		
		this.instanceVal = instanceVal;
		this.instanceType = instanceType;
		this.resultType = resultType;
		
		if (resultType != SparqlResultTypes.GRAPH_JSONLD && resultType != SparqlResultTypes.N_TRIPLES)
			throw new Exception("Unsupported result type: " + resultType.toString());
		
		this.conn = conn;
		this.oInfo = oInfo;
		this.tracker = tracker;
		if (tracker != null) {
			this.jobId = JobTracker.generateJobId();
			tracker.createJob(this.jobId);
		}
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
		this.resClient = resClient;
	}
	
	public ConnectedDataConstructor(String instanceVal, XSDSupportedType instanceType, SparqlResultTypes resultType, SparqlConnection conn) throws Exception {
		this(instanceVal, instanceType, resultType, conn, null, null, null);
	}

	public String getJobId() {
		return this.jobId;
	}

    public void run() {
    	
    	try {
    		if (this.tracker == null || this.jobId == null || this.resClient == null)
    			throw new Exception("Internal error: can't run thread without tracker, jobId and results client");
    		
    		ThreadAuthenticator.authenticateThisThread(this.headerTable);
    		
    		this.tracker.setJobPercentComplete(this.jobId, 10, "Querying data");
    		
    		tracker.setJobPercentComplete(this.jobId, 90, "Storing results");
    		
    		if (this.oInfo == null) {
    			this.oInfo = new OntologyInfo(this.conn);
    		}
    		String sparql = SparqlToXLibUtil.generateConstructConnected(this.conn, this.oInfo, this.instanceVal, this.instanceType);
    		
    		if (this.resultType == SparqlResultTypes.GRAPH_JSONLD) {
	    		resClient.execStoreGraphResults(this.jobId, this.conn.getDefaultQueryInterface().executeQueryToJsonLd(sparql));
	    		
    		} else if (this.resultType == SparqlResultTypes.N_TRIPLES) {
    			JSONObject jObj = new JSONObject();
    			jObj.put(SparqlResultTypes.N_TRIPLES.toString(), this.conn.getDefaultQueryInterface().executeQueryToNtriples(sparql));
	    		resClient.execStoreGraphResults(this.jobId, jObj);
    		} 
    		
    		this.tracker.setJobSuccess(this.jobId);
    		
    	} catch (Exception e) {
    		// if tracking, fail job
    		if (this.tracker != null) {
    			try {
    				this.tracker.setJobFailure(this.jobId, e.getMessage());
    			} catch (Exception ee) {
    	    		LocalLogger.printStackTrace(e);
    			}
    		}
    		// log
    		LocalLogger.printStackTrace(e);
    		
    		// save first exception
    		if (this.e == null) {
    			this.e = e;
    		}
    	}
	}

}
