package com.ge.research.semtk.resultSet;

import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * ResultSet type that could be either a table or a jobId (that could be used to get a table)
 * 
 * Backwards compatible with TableResultSet in these ways:
 *    sending:  only use the table features if receiver is expecting TableResultSet
 *    receiving:  isTable() will be true if sender sent TableResultSet
 * 
 * @author 200001934
 *
 */
public class TableOrJobIdResultSet extends TableResultSet {
	public final String JOB_ID_KEY = "jobId";
	
	public TableOrJobIdResultSet() {
		super();
	}
	
	public TableOrJobIdResultSet(JSONObject jObj) throws Exception {
		super(jObj);
	}
	
	public void addResults(String jobId) throws Exception {
		if (this.resultsContents != null) {
			throw new Exception("Can't addResults a second time to same TableOrJobIdResultSet");
		}
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(JOB_ID_KEY, jobId); 
		addResultsJSON(jsonObj);
	}
	
	public void addResults(Table table) throws Exception {
		if (this.resultsContents != null) {
			throw new Exception("Can't addResults a second time to same TableOrJobIdResultSet");
		}
		
		super.addResults(table);
	}
	
	public boolean isJobId() {
		return this.resultsContents != null && this.resultsContents.containsKey(JOB_ID_KEY);
	}
	public boolean isTable() {
		return this.resultsContents != null && this.resultsContents.containsKey(TableResultSet.TABLE_JSONKEY);
	}
	
	/**
	 * could be null if ! isJobId()  or if jobId was somehow set to null.
	 * @return
	 */
	public String getJobId() {
		return (String) (this.resultsContents.get(JOB_ID_KEY));
	}

	@Override
	public Table getResults() throws Exception {
		if (this.isTable()) {
			return super.getResults();
		} else {
			// if you see this error, use getResults(tracker, resultsClient);
			throw new Exception("Can't get table from TableOrJobIdResultSet that has only jobId.");
		}
	}
	
	public Table getResults(JobTracker tracker, ResultsClient resultsClient) throws Exception {
		if (this.isTable()) {
			return this.getResults();
		} else {
			String jobId = this.getJobId();
			// put new jobId into the status client
			int percent = 0;
			int totalSeconds = 0;
			
			// TODO: should this go forever?   What timeout is safe?
			while (percent < 100) {
				percent = tracker.waitForPercentOrMsec(jobId, 100, 27000);
				totalSeconds += 27;
				
				if (percent < 100) {
					LocalLogger.logToStdOut("Waiting for job " + String.valueOf(jobId) + " for " + String.valueOf(totalSeconds) + " sec");
				}
			}
			
			if (tracker.jobSucceeded(jobId)) {
				// TODO: this allows a string overflow to happen and generate an error
				//t = this.resultsClient.getTableResultsJson(jobId, Integer.MAX_VALUE);
				return Table.fromJson(resultsClient.execGetBlobResult(jobId));
			} else {
				String msg = tracker.getJobStatusMessage(jobId);
				throw new Exception(msg);
			}
		}
	}
}
