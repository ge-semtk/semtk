package com.ge.research.semtk.api.storedQueryExecution.utility;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.api.storedQueryExecution.client.StoredNodeGroupExecutionClient;
import com.ge.research.semtk.api.storedQueryExecution.client.StoredNodeGroupExecutionClientConfig;
import com.ge.research.semtk.api.storedQueryExecution.utility.insert.ColumnToRequestMapping;
import com.ge.research.semtk.api.storedQueryExecution.utility.insert.GenericInsertionRequestBody;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;

public class StoredNodeGroupExecutionLauncher {
	public static TableResultSet launchSelectJob(String id, JSONArray runtimeConstraintsJson, JSONObject edcConstraints, JSONObject sparqlConnectionAsJsonObject, StoredNodeGroupExecutionClientConfig sncc) throws Exception{
		TableResultSet retval = null;
		
		// create the executor we will be using
		StoredNodeGroupExecutionClient snec 		= new StoredNodeGroupExecutionClient(sncc);
		
		
		// get a name for the job.
		System.err.println("About to request job");
		String jobId = snec.ExecuteDispatchByIdWithSimpleReturn(id, sparqlConnectionAsJsonObject, edcConstraints, runtimeConstraintsJson);
		System.err.println("job request created. Job ID returned was " + jobId);
		
		// waiting on results.
		System.err.println("About to wait for completion");
		while(snec.executeGetJobCompletionCheckWithSimpleReturn(jobId) == false){
			Thread.sleep(100);
		}
		System.err.println("requested Job completed");
		
		// finished now. check the result.
		String status = snec.executeGetJobStatusWithSimpleReturn(jobId);
		
		if(status.equalsIgnoreCase("success") ){
			System.err.println("requested job succeeded");
			// get results.
			Table retTable = snec.executeGetResults(jobId);
			retval = new TableResultSet(true);
			retval.addResults(retTable); 
		}
		
		else{
			// report failure.
			System.err.println("requested job failed");
			retval = new TableResultSet(false);
			retval.addRationaleMessage(snec.executeGetJobStatusMessageWithSimpleReturn(jobId));
		}
		
		return retval;
	}	
	
	public static RecordProcessResults launchInsertJob(String nodegroupAndTemplateId, String csvContentStr, JSONObject sparqlConnectionAsJsonObject, StoredNodeGroupExecutionClientConfig sncc) throws Exception{
		RecordProcessResults retval = null;
		
		// create the executor we will be using
		StoredNodeGroupExecutionClient snec 		= new StoredNodeGroupExecutionClient(sncc);

		// send the request
		retval = snec.execIngestionFromCsvStr(nodegroupAndTemplateId, csvContentStr, sparqlConnectionAsJsonObject);
		
		// good, bad or other: send the results. 
		return retval;
	}
	
	public static RecordProcessResults launchInsertJob(String nodegroupAndTemplateId, JSONArray mappingArray, GenericInsertionRequestBody requestBody, JSONObject sparqlConnectionAsJsonObject, StoredNodeGroupExecutionClientConfig sncc) throws Exception{
		RecordProcessResults retval = null;
		
		ColumnToRequestMapping colMapper = new ColumnToRequestMapping(mappingArray, requestBody);
		String csvData = colMapper.getCsvString();
		
		// send the request using the call which already takes a plain csv
		retval = launchInsertJob(nodegroupAndTemplateId, csvData, sparqlConnectionAsJsonObject, sncc);
			
		return retval;
	}
	
	
}
