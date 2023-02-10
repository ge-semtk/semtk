/**
 ** Copyright 2016-2020 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
package com.ge.research.semtk.sparqlX.dispatch;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.lib.resultSet.TableOrJobIdResultSet;
import com.ge.research.semtk.querygen.client.QueryExecuteClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.dispatch.QueryGroup.DispatchQueryGroup;
import com.ge.research.semtk.utility.LocalLogger;

public class DispatcherWorkThread extends Thread {

	private DispatchQueryGroup queryGroup;
	private String query;
	private QueryExecuteClient execClient;
	private JobTracker tracker;
	private ResultsClient resultsClient;
	private Exception[] exceptionArr;
	private int myEntryNumber;
	private HeaderTable headerTable;
	
	public DispatcherWorkThread(DispatchQueryGroup dqg, String query, QueryExecuteClient clnt, JobTracker jobTracker, ResultsClient rClient, Exception[] exceptionArray, int myID) {
		this.queryGroup = dqg;
		this.query = query;
		this.execClient = clnt;
		this.tracker = jobTracker;
		this.resultsClient = rClient;
		this.exceptionArr = exceptionArray;
		this.myEntryNumber = myID;
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
	}
	
    public void run() {
    	// perform the actual query, i guess
    	try {
    		ThreadAuthenticator.authenticateThisThread(this.headerTable);
			
			TableOrJobIdResultSet res = this.execClient.execute(query);
			res.throwExceptionIfUnsuccessful();
		
			// TODO: consider replacing this with
			// res.getResults(this.tracker, this.resultsClient);
			
			Table t = null;
			if (res.isTable()) {
				// synchronous query executor
				t = res.getResults();	
				
			} else {
				String jobId = res.getJobId();
				// put new jobId into the status client
				int percent = 0;
				int totalSeconds = 0;
				
				// TODO: should this go forever?   What timeout is safe?
				while (percent < 100) {
					percent = this.tracker.waitForPercentOrMsec(jobId, 100, 27000);
					totalSeconds += 27;
				}
				
				// TODO:  This doesn't check for success
				
				// TODO: this allows a string overflow to happen and generate an error
				//t = this.resultsClient.getTableResultsJson(jobId, Integer.MAX_VALUE);
				t = Table.fromJson(this.resultsClient.execGetBlobResult(jobId));
				LocalLogger.logToStdOut("Job " + jobId + ": DispatcherWorkThread returning " + t.getNumRows() + " rows");
			}
			// end of consider replacing
			
			this.queryGroup.addResults(t);
			
			this.exceptionArr[myEntryNumber] = null;
    	
    	
    	} catch (Exception e) {
    		
    		// dump to log.
    		LocalLogger.logToStdErr("Work thread failed for query " + this.query);
    		LocalLogger.logToStdErr(e.toString());
    		LocalLogger.printStackTrace(e);
    		
    		this.exceptionArr[this.myEntryNumber] = e;
		}
    	
    }
}
