package com.ge.research.semtk.services.ingestion;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.load.FileBucketConnector;
import com.ge.research.semtk.load.LoadTracker;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Watch a job and do loadTracking if it succeeds
 * @author 200001934
 *
 */
public class AsyncLoadTrackThread extends Thread {

	private JobTracker jobTracker = null;
	private String jobId = null;
	private String fileKey = null;
	private byte[] content;
	private HeaderTable headerTable = null;
	private LoadTracker loadTracker = null;
	private FileBucketConnector trackBucket = null;
	private SparqlEndpointInterface sei = null;
	private String fileName = null;
	
	
	public AsyncLoadTrackThread(JobTracker jobTracker, String jobId, LoadTracker loadTracker, FileBucketConnector trackBucket, String fileKey, String fileName, SparqlEndpointInterface sei, byte[] content) {
		this.jobTracker = jobTracker;
		this.jobId = jobId;
		this.loadTracker = loadTracker;
		this.trackBucket = trackBucket;
		this.fileKey = fileKey;
		this.fileName = fileName;
		this.sei = sei;
		this.content = content;
		
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
	}
	
	public void run() {
		ThreadAuthenticator.authenticateThisThread(this.headerTable);
		
		try {
			int percent = this.jobTracker.waitForPercentOrMsec(this.jobId, 100, 120 * 60000);
			if (percent < 100) {
				throw new Exception("Waited two hours and job is not complete");
			}
			if (this.jobTracker.jobSucceeded(this.jobId)) {
				this.trackBucket.putObject(this.fileKey, this.content);
				this.loadTracker.trackLoad(this.fileKey, this.fileName, this.sei);
			}
			
		} catch (Exception e) {
			LocalLogger.logToStdErr("Trouble tracking load " + this.fileKey);
			LocalLogger.printStackTrace(e);
		}
	}
}
