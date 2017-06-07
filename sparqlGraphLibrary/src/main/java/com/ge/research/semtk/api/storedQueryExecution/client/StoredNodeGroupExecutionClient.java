/**
 ** Copyright 2016 General Electric Company
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

package com.ge.research.semtk.api.storedQueryExecution.client;

import java.net.ConnectException;
import java.net.URL;

import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore.Processor.isPartitionMarkedForEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.Utility;

public class StoredNodeGroupExecutionClient extends RestClient {

	private static String mappingPrefix = "/nodegroupExecution";
	private static String jobStatusEndpoint = "/jobStatus";
	private static String jobStatusMessageEndpoint = "/jobStatusMessage";
	private static String jobCompletionCheckEndpoint = "/getJobCompletionCheck";
	private static String jobCompletionPercentEndpoint = "/getJobCompletionPercentage";
	private static String resultsLocationEndpoint = "/getResultsLocation";
	private static String dispatchByIdEndpoint = "/dispatchById";
	private static String dispatchFromNodegroupEndpoint = "/dispatchFromNodegroup";
	private static String ingestFromCsvStringsNewConnection = "/ingestFromCsvStringsNewConnection";
	
	@Override
	public void buildParametersJSON() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEmptyResponse() throws Exception {
		// TODO Auto-generated method stub

	}
	
	public StoredNodeGroupExecutionClient (StoredNodeGroupExecutionClientConfig snecc){
		this.conf = snecc;
	}
	
	
	public String  executeGetJobStatusWithSimpleReturn(String jobId) throws Exception{
		SimpleResultSet ret = this.executeGetJobStatus(jobId);
		return ret.getResult("status");
	}
	
	public SimpleResultSet executeGetJobStatus(String jobId) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(this.mappingPrefix + this.jobStatusEndpoint);
		this.parametersJSON.put("jobID", jobId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jobId");
		}
		
		return retval;
	}
	
	public Boolean executeGetJobCompletionCheckWithSimpleReturn(String jobId) throws Exception{
		SimpleResultSet ret = this.executeGetJobCompletionCheck(jobId);
		
		String val = ret.getResult("completed");
		if(val.equalsIgnoreCase("true")) { return true; }
		else{ return false; }
	}
	
	public SimpleResultSet executeGetJobCompletionCheck(String jobId) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(this.mappingPrefix + this.jobCompletionCheckEndpoint);
		this.parametersJSON.put("jobID", jobId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jobId");
		}
		
		return retval;		
	}
	
	//execGetStatusMessage()
	public String executeGetJobStatusMessageWithSimpleReturn(String jobId) throws Exception{
		SimpleResultSet ret = executeGetJobStatusMessage(jobId);
		return ret.getResult("message");
	}
	
	public SimpleResultSet executeGetJobStatusMessage(String jobId) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(this.mappingPrefix + this.jobStatusMessageEndpoint);
		this.parametersJSON.put("jobID", jobId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jobId");
		}
		
		return retval;		
	}
	
	public SimpleResultSet executeGetJobCompletionPercentage(String jobId) throws Exception {
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(this.mappingPrefix + this.jobCompletionPercentEndpoint);
		this.parametersJSON.put("jobID", jobId);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jobId");
		}
		
		return retval;
	}
	
	public Table executeGetResults(String jobID) throws Exception {
		Table ret = this.executeGetResultsLocationWithSimpleReturn(jobID);
		
		// get the actual location of the full result.
		String locationForFullResults = ret.getCell(1, 0); // this is the cell with the full results location.
		
		// get the results themselves.
		Table retval = Utility.getURLResultsContentAsTable(new URL(locationForFullResults));
		return retval;
	}
	
	public Table executeGetResultsLocationWithSimpleReturn(String jobId) throws Exception{
		TableResultSet ret = this.executeGetResultsLocation(jobId);
		return ret.getTable();
	}
	
	public TableResultSet executeGetResultsLocation(String jobId) throws Exception{
	TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint(this.mappingPrefix + this.resultsLocationEndpoint);
		this.parametersJSON.put("jobID", jobId);
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			JSONObject tblWrapper = (JSONObject)jobj.get("table");
			
			Table tbl = Table.fromJson((JSONObject)tblWrapper.get("@table"));
			retval.addResults(tbl);
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jobID");
		}
		
		return retval;
	}
/**
 * 	
 * @param nodegroupID    -- string ID for the nodegroup to be executed. this assumes that the node group resides in a nodegroup store that was config'd on the far end (service)
 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
 * @return
 */
	public String ExecuteDispatchByIdWithSimpleReturn(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret =  this.executeDispatchById(nodegroupID, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	public SimpleResultSet executeDispatchById(String nodegroupID, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONArray runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(this.mappingPrefix + this.dispatchByIdEndpoint);
		this.parametersJSON.put("nodeGroupId", nodegroupID);
		this.parametersJSON.put("sparqlConnection", sparqlConnectionJson.toJSONString());
		if(edcConstraintsJson != null){
			this.parametersJSON.put("externalDataConnectionConstraints", edcConstraintsJson.toJSONString());
		}
		else{ 
			this.parametersJSON.put("externalDataConnectionConstraints", null);
		}
		if(runtimeConstraintsJson != null){
			this.parametersJSON.put("runtimeConstraints", runtimeConstraintsJson.toJSONString());
		}
		else{
			this.parametersJSON.put("runtimeConstraints", null);
		}
		
		try{
			System.err.println("sending ExecuteDispatchByIdWithSimpleReturn request");
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("nodeGroupId");
			this.parametersJSON.remove("sparqlConnection");
			this.parametersJSON.remove("externalDataConnectionConstraints");
			this.parametersJSON.remove("runtimeConstraints");
		}
		System.err.println("ExecuteDispatchByIdWithSimpleReturn request finished without exception");
		return retval;
	}
	/**
	 * 	
	 * @param ng   -- the nodegroup to execute a selection query from
	 * @param sparqlConnectionJson -- the sparql connection rendered to JSON. please see com.ge.research.semtk.sparqlX.SparqlConnection for details.
	 * @param edcConstraintsJson -- the EDC Constraints rendered as JSON. expected format {\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[]}} . these will be better documented in the future.
	 * @param runtimeConstraintsJson -- the runtime constraints rendered as JSON. this is an array of JSON objects of the format 
	 * 									{"SparqlID" : "<value>", "Operator" : "<operator>", "Operands" : [<operands>] }
	 * 									for more details, please the package com.ge.research.semtk.belmont.runtimeConstraints .
	 * @return
	 */
	
	public String executeDispatchFromNodeGroupWithSimpleReturn(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONObject runtimeConstraintsJson) throws Exception{
		SimpleResultSet ret = this.executeDispatchFromNodeGroup(ng, sparqlConnectionJson, edcConstraintsJson, runtimeConstraintsJson);
		return ret.getResult("JobId");
	}
	
	public SimpleResultSet executeDispatchFromNodeGroup(NodeGroup ng, JSONObject sparqlConnectionJson, JSONObject edcConstraintsJson, JSONObject runtimeConstraintsJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint(this.mappingPrefix + this.dispatchFromNodegroupEndpoint);
		this.parametersJSON.put("jsonRenderedNodeGroup", ng.toJson().toJSONString());
		this.parametersJSON.put("sparqlConnection", sparqlConnectionJson.toJSONString());
		this.parametersJSON.put("externalDataConnectionConstraints", edcConstraintsJson.toJSONString());
		this.parametersJSON.put("runtimeConstraints", runtimeConstraintsJson.toJSONString());
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute() );
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jsonRenderedNodeGroup");
			this.parametersJSON.remove("sparqlConnection");
			this.parametersJSON.remove("externalDataConnectionConstraints");
			this.parametersJSON.remove("runtimeConstraints");
		}
		
		return retval;
	}

	public RecordProcessResults execIngestionFromCsvStr(String nodegroupAndTemplateId, String csvContentStr, JSONObject sparqlConnectionAsJsonObject) throws Exception {
		RecordProcessResults retval = null;
		
		conf.setServiceEndpoint(this.mappingPrefix + this.ingestFromCsvStringsNewConnection);
		this.parametersJSON.put("templateId", nodegroupAndTemplateId);
		this.parametersJSON.put("sparqlConnection", sparqlConnectionAsJsonObject.toJSONString());
		this.parametersJSON.put("csvContent", csvContentStr);
	
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			retval = new RecordProcessResults(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("templateId");
			this.parametersJSON.remove("sparqlConnection");
			this.parametersJSON.remove("csvContent");
		}
		System.err.println("execIngestionFromCsvStr request finished without exception");
		
		return retval;
	}

}
