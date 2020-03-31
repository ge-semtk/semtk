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
package com.ge.research.semtk.query.rdb;


import java.util.ArrayList;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClientBuilder;
import com.amazonaws.services.athena.model.ColumnInfo;
import com.amazonaws.services.athena.model.EncryptionConfiguration;
import com.amazonaws.services.athena.model.EncryptionOption;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryExecutionResult;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;


/**
 * A connector to AWS Athena.
 */
public class AthenaConnector extends Connector {  

	private static final Long SLEEP_MS = 2000L;	// milliseconds to wait before checking again to see if query is complete
			
	private final String awsS3OutputBucket;		// AWS S3 bucket to temporarily store query results
	private final String awsKey; 				// AWS KMS key (global, not tied to a specific bucket)											
	private final AmazonAthena awsClient;		// an AWS client
	private final String database;				// the database to connect to

	/**
	 * Constructor
	 * @param awsRegion the AWS region
	 * @param awsS3OutputBucket AWS S3 bucket to temporarily store query results
	 * @param awsKey AWS KMS key (global, not tied to a specific bucket)	
	 * @param awsClientExecutionTimeout how long the AWS client should wait for results before timing out
	 * @param database the database
	 * @throws Exception 
	 */
	public AthenaConnector(Regions awsRegion, String awsS3OutputBucket, String awsKey, int awsClientExecutionTimeout, String database) throws Exception{		
		this.awsS3OutputBucket = awsS3OutputBucket;
		this.awsKey = awsKey;
		this.awsClient = (new AthenaClientFactory(awsRegion, awsClientExecutionTimeout)).createClient(); 
		this.database = database;
		// not testing connection, for efficiency
	}
	
	/**
	 * Execute a query: 1) submit 2) wait for completion 3) process results
	 */
	public Table query(String query) throws Exception {
		String queryExecutionId = submitQuery(query);
		waitForQueryToComplete(queryExecutionId);
		Table results = getResults(queryExecutionId);
		return results;
	}

	/**
	 * Submit an Athena query.
	 */
	private String submitQuery(String query) {
		
		// create a QueryExecutionContext
		LocalLogger.logToStdOut("Submit query on database " + database + ": " + query + "...");
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext().withDatabase(database);  // withDatabase() seems to be required - not sufficient to include it in the query

		// create a ResultConfiguration (specifies S3 output location and encryption options)
		EncryptionConfiguration encryptionConfiguration = new EncryptionConfiguration();
		encryptionConfiguration.withEncryptionOption(EncryptionOption.SSE_KMS);
		encryptionConfiguration.setKmsKey(awsKey);  
		ResultConfiguration resultConfiguration = new ResultConfiguration()
		.withEncryptionConfiguration(encryptionConfiguration)
		.withOutputLocation(awsS3OutputBucket);

		// create a StartQueryExecutionRequest to send to Athena which will start the query.
		StartQueryExecutionRequest startQueryExecutionRequest = new StartQueryExecutionRequest()
		.withQueryString(query).withQueryExecutionContext(queryExecutionContext)
		.withResultConfiguration(resultConfiguration);

		StartQueryExecutionResult startQueryExecutionResult = awsClient.startQueryExecution(startQueryExecutionRequest);
		return startQueryExecutionResult.getQueryExecutionId();
	}

	
	/**
	 * Wait for a query to complete.
	 * @Exception if a query fails or is canceled
	 */
	private void waitForQueryToComplete(String queryExecutionId)
			throws InterruptedException {

		LocalLogger.logToStdOut("Waiting for query id " + queryExecutionId + "...");
		
		GetQueryExecutionRequest getQueryExecutionRequest = new GetQueryExecutionRequest()
		.withQueryExecutionId(queryExecutionId);

		GetQueryExecutionResult getQueryExecutionResult = null;
		boolean stillRunning = true;
		while (stillRunning) {
			getQueryExecutionResult = awsClient.getQueryExecution(getQueryExecutionRequest);
			String queryState = getQueryExecutionResult.getQueryExecution().getStatus().getState();
			if (queryState.equals(QueryExecutionState.FAILED.toString())) {
				throw new RuntimeException("Query Failed to run with Error Message: " + getQueryExecutionResult.getQueryExecution().getStatus().getStateChangeReason());
			} else if (queryState.equals(QueryExecutionState.CANCELLED.toString())) {
				throw new RuntimeException("Query was canceled.");
			} else if (queryState.equals(QueryExecutionState.SUCCEEDED.toString())) {
				stillRunning = false;
			} else {
				// Sleep an amount of time before retrying again.
				Thread.sleep(SLEEP_MS);
			}
			LocalLogger.logToStdOut("Current Status is: " + queryState);
		}
	}

	/**
	 * Retrieve and process query results (the query must be completed before calling this)
	 */
	private Table getResults(String queryExecutionId) throws Exception {
		
		LocalLogger.logToStdOut("Retrieving results...");
		
		// request and get results
		GetQueryResultsRequest getQueryResultsRequest = new GetQueryResultsRequest().withQueryExecutionId(queryExecutionId); // tried to speed up by increasing batch size using withMaxResults(1000) - didn't have an effect
		GetQueryResultsResult getQueryResultsResult = awsClient.getQueryResults(getQueryResultsRequest);
		List<ColumnInfo> columnInfoList = getQueryResultsResult.getResultSet().getResultSetMetadata().getColumnInfo();
		
		// gather results rows 		
		int rowCount = 0;
		ArrayList<ArrayList<String>> tableRows = new ArrayList<ArrayList<String>>(); // will add rows to this		
		while (true) {
			List<Row> results = getQueryResultsResult.getResultSet().getRows();
			for (Row row : results) {	
				if(rowCount > 0){  // skip the first row of the first page - it's the column headers 
					ArrayList<String> tmp = processRow(row, columnInfoList.size()); 
					tableRows.add(tmp);	
				}
				rowCount++;
			}
			if (getQueryResultsResult.getNextToken() == null) {   // confirmed that this logic comes from https://docs.aws.amazon.com/athena/latest/ug/code-samples.html (calls getNextToken() twice - once to check null and once to proceed)
				break;  // there are no more pages to read
			}
			getQueryResultsResult = awsClient.getQueryResults(getQueryResultsRequest.withNextToken(getQueryResultsResult.getNextToken()));  
		}
		
		// gather column names and types
		String[] colNames = new String[columnInfoList.size()];
		String[] colTypes = new String[columnInfoList.size()];
		for (int i = 0; i < columnInfoList.size(); ++i) {
			colNames[i] = columnInfoList.get(i).getName();
			colTypes[i] = columnInfoList.get(i).getType();
		}
		return new Table(colNames, colTypes, tableRows);
	}

	
	/**
	 * Process a single results row
	 */
	private ArrayList<String> processRow(Row row, int numCols) {
		ArrayList<String> ret = new ArrayList<String>();
		for (int i = 0; i < numCols; ++i) {	
			ret.add(row.getData().get(i).getVarCharValue());  // TODO treating every entry as a string for now (similar to JDBC connector).  May need special treatment per type later.
		}
		return ret;
	}

	
	/**
	 * Main method - for testing purposes only.
	 */
	public static void main(String[] args) throws IllegalArgumentException, InterruptedException {
		try{ 			 
			
			// paste in when testing
			String awsS3OutputBucket = "";		
			String awsKey = "";																
			String query = 	"";	
			String database = "";
			
			AthenaConnector athenaConnector = new AthenaConnector(Regions.US_EAST_1, awsS3OutputBucket, awsKey, 7000, database);
			Table results = athenaConnector.query(query);
			LocalLogger.logToStdOut(results.toCSVString());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
}


/**
 * Athena Client Factory
 */
class AthenaClientFactory {	

	private final AmazonAthenaClientBuilder builder;
	
	/**
	 * Constructor for Athena Client Factor with the following settings: 
	 * - Set the region of the client 
	 * - Use the instance profile from the EC2 instance as the credentials provider 
	 * - Configure the client to increase the execution timeout.
	 */
	@SuppressWarnings("deprecation")
	public AthenaClientFactory(Regions region, int timeout){
		builder = AmazonAthenaClientBuilder.standard().withRegion(region) 
				.withCredentials(new InstanceProfileCredentialsProvider())	// get the credentials from the EC2 instance on which this code will run
				.withClientConfiguration(new ClientConfiguration().withClientExecutionTimeout(timeout));
	}					

	public AmazonAthena createClient() {
		return builder.build();
	}
}
