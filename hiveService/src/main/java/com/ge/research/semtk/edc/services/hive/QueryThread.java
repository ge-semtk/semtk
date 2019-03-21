package com.ge.research.semtk.edc.services.hive;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.properties.ServiceProperties;
import com.ge.research.semtk.querygen.client.QueryExecuteClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableOrJobIdResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;

public abstract class QueryThread extends Thread {

	private StatusClient statusClient;
	private ResultsClient resultsClient;
	private HeaderTable headerTable;

	public QueryThread(ServiceProperties statusProps, ServiceProperties resultsProps) throws Exception {
		this.statusClient = new StatusClient(new StatusClientConfig(statusProps.getProtocol(), statusProps.getServer(), statusProps.getPort()));
		this.resultsClient = new ResultsClient(new ResultsClientConfig(resultsProps.getProtocol(), resultsProps.getServer(), resultsProps.getPort()));
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
		this.statusClient.execSetPercentComplete(1);
	}

	/**
	 * 
	 * @return Table
	 * @throws Exception - please prepend a little info like "hive query failed" to the exception
	 */
	abstract Table execQuery() throws Exception;

	public String getJobId() {
		return this.statusClient.getJobId();
	}
	
	public void run() {
		String jobId = this.statusClient.getJobId();
		
		
		try {
			ThreadAuthenticator.authenticateThisThread(this.headerTable);
			
			Table table = this.execQuery();
			this.resultsClient.execStoreBlobResults(jobId, table.toJson());
			//this.resultsClient.execStoreTableResults(jobId, table);
			this.statusClient.execSetSuccess();

		} catch (Exception e) {
		
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			
			exceptionAsString = exceptionAsString.substring(0, Math.min(200, exceptionAsString.length())) + "...";
				
			try {
				this.statusClient.execSetFailure(exceptionAsString);
				
			} catch (Exception ee) {
				// couldn't get a message to the results service
				// bad things will happen.  Someone will hang up.
				LocalLogger.printStackTrace(ee);
			}
		}
	}
}
