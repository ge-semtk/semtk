/**
 ** Copyright 2020 General Electric Company
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
package com.ge.research.semtk.demo;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.utility.Utility;

/**
 * @author 200001934
 *
 */
public class JavaApiDemo {
	// Set up the nodegroup execution service
	static String PROTOCOL = "http";
	static String SERVER = "localhost";
	static int    NGE_PORT = 12058;
	static int 	  STORE_PORT = 12056;
	static int 	  QUERY_PORT = 12050;

	
	// set up a triplestore connection
	static String CONN_TYPE = "virtuoso";                        // "fuseki", "neptune"
	static String CONN_URL = "http://localhost:8890";            // "http://localhost:3030/DATASET";
	static String CONN_GRAPH = "http://semtk/demo";
	// virtuoso requires password
	static String USER = "dba";
	static String PASSWORD = "dba";
	
	// nodegroups
	static String QUERY_NODEGROUP = "demoNodegroup";
	static String RTC_QUERY_NODEGROUP = "demoNodegroupRTC";

	
	/**
	 * Run a demo
	 * @param args:  type url  optional
	 *               e.g. (fuseki|virtuoso|neptune) http://localhost:port/OPTIONAL_DATASET
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length == 2) {
			CONN_TYPE = args[0];
			CONN_URL = args[1];
			System.out.println("Using: " + CONN_TYPE + " connection " + CONN_URL);
		}
		
		// build the nodegroup executor client
		NodeGroupExecutionClientConfig config = new NodeGroupExecutionClientConfig(PROTOCOL, SERVER, NGE_PORT);
		NodeGroupExecutionClient client = new NodeGroupExecutionClient(config);
		
		// define an endpoint graph, and build a connection that uses it for model and data
		SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(CONN_TYPE, CONN_URL, CONN_GRAPH, USER, PASSWORD);
		SparqlConnection override = new SparqlConnection("override", sei);
		
		// For the demo using the "semtk/demo" graph and "demoNodegroup"
		// Pre-load model and data.
		// This clears the graph.  Be careful if you've changed the connection.
		if (CONN_GRAPH.contains("semtk/demo") && QUERY_NODEGROUP.equals("demoNodegroup")) { 
			setupDemoNodeGroup(override);
		}
		
		// run a query from the store by id
		Table results = client.execDispatchSelectByIdToTable(QUERY_NODEGROUP, override, null, null);
		if (results.getNumRows() > 0) {
			System.out.println("Cell 0 0: " + results.getCellAsString(0, 0));
			System.out.println("--plain select query--");
			System.out.println(results.toCSVString());
		}
		
		// check if a nodegroup has runtime constraints
		Table constraints = client.getRuntimeConstraintsByNodeGroupID(RTC_QUERY_NODEGROUP);
		System.out.println("--list of runtime constraints--");
		System.out.println(constraints.toCSVString());
		
		// get all the existing values for ?Value
		Table values = client.execDispatchFilterByIdToTable(RTC_QUERY_NODEGROUP, "?Value", override, null, null);
		System.out.println("--possible values for runtime constraint ?Value--");
		System.out.println(values.toCSVString());
	
		// runtime constrained query
		if (values.getNumRows() > 1) {
			// sort this single column table of values, and pick the 2nd one as a lower limit
			values.sortByColumnDouble(values.getColumnNames()[0]);   
			double v = values.getCellAsFloat(1, 0);
			
			// build an array of runtime constraints with ?Value > v
			JSONObject constraintJSON = RuntimeConstraintManager.buildRuntimeConstraintJson("?Value", SupportedOperations.GREATERTHAN, String.valueOf(v));
			JSONArray runtimeConstraintsJson = new JSONArray();
			runtimeConstraintsJson.add(constraintJSON);
			
			// run the query with constraints
			Table values2 = client.execDispatchSelectByIdToTable(RTC_QUERY_NODEGROUP, override, null, runtimeConstraintsJson);
			System.out.println("--constrained select query--");
			System.out.println(values.toCSVString());
		}

		// ingestion: check column names
		String columns[] = client.getIngestionColumnsById("demoNodegroup");
		System.out.println("--ingestion columns--");
		System.out.println(String.join(",", columns));
		
		// preform ingestion
		// Clearly csvContentStr could be read from a file
		// and chunked if necessary.
		
		// First, fail by using an epoch instead of a legal date timestamp
		String csvFailureStr = 
						"layer_code,meas_name,meas_tag,meas_units,test_number,timestamp,value\n" +
					    "CODE1,     pressure, p21     ,psi       , 127       ,1600782264, 14.0\n" + 
					    "CODE1,     speed   , p22     ,kph       , 127        ,1600782264, 55.0\n";
		try {
			
			String message = client.dispatchIngestFromCsvStringsByIdSync("demoNodegroup", csvFailureStr, override);
		} catch (Exception e) {
			// failure is in the exception
			System.out.println("Expected ingestion failure using a 'dispatch' function which throws exceptions:");
			System.out.println(e.getMessage());
		}
		
		
		// Failed ingest where we can retrieve a table of errors instead of using exceptions
		String jobId = client.execIngestFromCsvStringsByIdAsync("demoNodegroup", csvFailureStr, override);
		client.waitForCompletion(jobId);
		if (client.getJobSuccess(jobId)) {
			System.out.println("Success message: " + client.getJobStatusMessage(jobId));
		} else {
			System.out.println("Failure table:\n" + client.getResultsTable(jobId).toCSVString());
		}
		
		
		// successful ingestion
		String csvContentStr = 
						"layer_code,meas_name,meas_tag,meas_units,test_number,timestamp,value\n" +
						"CODE1,     pressure, p21     ,psi       , 127       ,2020-05-20T16:31:19, 14.0\n" + 
						"CODE1,     speed   , p22     ,kph       , 127       ,2020-05-20T16:31:19, 55.0\n";
		String message = client.dispatchIngestFromCsvStringsByIdSync("demoNodegroup", csvContentStr, override);
		System.out.println("Successful ingest: " + message);
		
		// use a nodegroup instead of an id
		// get a nodegroup from the "store"
		NodeGroupStoreRestClient storeClient = new NodeGroupStoreRestClient(new NodeGroupStoreConfig(PROTOCOL, SERVER, STORE_PORT));
		SparqlGraphJson sgjson = storeClient.executeGetNodeGroupByIdToSGJson(RTC_QUERY_NODEGROUP);
		
		// example reading and writing to file instead
		File tempFile = File.createTempFile("demo-", ".json");
		FileUtils.writeStringToFile(tempFile, sgjson.toJson().toJSONString(), Charset.defaultCharset());
		String jsonStr = FileUtils.readFileToString(tempFile, Charset.defaultCharset());
		SparqlGraphJson sgjson2 = new SparqlGraphJson(jsonStr);
		NodeGroup ng = sgjson2.getNodeGroup();
		
		// build an array of runtime constraints with ?Value > 5
		// since we have the nodegroup, use RuntimeConstraintManager directly
		RuntimeConstraintManager mgr = new RuntimeConstraintManager(ng);
		mgr.applyConstraint("?Value", SupportedOperations.GREATERTHAN, new String [] { "5" });
	
		// add a return and constrain it too
		PropertyItem pItem = ng.getNodeBySparqlID("?Layer").getPropertyByKeyname("code");
		ng.changeSparqlID(pItem, "layer_code");
		pItem.setIsReturned(true);
		pItem.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
		pItem.addConstraint(ValueConstraint.buildFilterInConstraint(pItem, "CODE1"));
		
		// run a select with a nodegroup
		Table ngSelectTable = client.dispatchSelectFromNodeGroup(ng, override, null, mgr);
		System.out.println("--select by nodegroup--");
		System.out.println(ngSelectTable.toCSVString());
		
		// show some SPARQL
		String rawSparql = ng.generateSparqlSelect();
		System.out.println("SPARQL: " + rawSparql);
		
		// run raw sparql
		Table ngSparqlTable = client.dispatchRawSparql(rawSparql, override);
		System.out.println("--select using  raw sparql--");
		System.out.println(ngSparqlTable.toCSVString());
		
		System.out.println("\nSuccess.");

				
	}
	
	/**
	 * Set up the demo graph.   Advanced topics demo.
	 * @param override
	 * @throws Exception
	 */
	public static void setupDemoNodeGroup(SparqlConnection override) throws Exception {
		NodeGroupStoreRestClient storeClient = new NodeGroupStoreRestClient(new NodeGroupStoreConfig(PROTOCOL, SERVER, STORE_PORT));
		NodeGroupExecutionClient ngeClient = new NodeGroupExecutionClient( new NodeGroupExecutionClientConfig(PROTOCOL, SERVER, NGE_PORT));
		// query client for the model graph
		SparqlEndpointInterface modelSei = override.getModelInterface(0);
		SparqlQueryClient queryClientModel = new SparqlQueryClient(new SparqlQueryAuthClientConfig(
				PROTOCOL, SERVER, QUERY_PORT, 
				"/query", 
				modelSei.getServerAndPort(), modelSei.getServerType(), modelSei.getGraph(), 
				USER, PASSWORD));
		// query client for the data graph
		SparqlEndpointInterface dataSei = override.getDataInterface(0);
		SparqlQueryClient queryClientData = new SparqlQueryClient(new SparqlQueryAuthClientConfig(
				PROTOCOL, SERVER, QUERY_PORT, 
				"/query", 
				dataSei.getServerAndPort(), dataSei.getServerType(), dataSei.getGraph(), 
				USER, PASSWORD));
	
		// set up a class so we can load resources from semtkLibrary
		Class aSemtkLibClass = NodeGroupStoreRestClient.class;
		
		// put demoNodegroup into the store
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getResourceAsJson(aSemtkLibClass, "/nodegroups/demoNodegroup.json"));
		sgJson.setSparqlConn(override);
		storeClient.deleteStoredNodeGroup("demoNodegroup");
		storeClient.executeStoreNodeGroup("demoNodegroup", "demo comments", "semTK", sgJson.toJson());
		
		// put demoNodegroupRTC into the store
		SparqlGraphJson sgJsonRTC = new SparqlGraphJson(Utility.getResourceAsJson(aSemtkLibClass, "/nodegroups/demoNodegroupRTC.json"));
		sgJsonRTC.setSparqlConn(override);
		storeClient.deleteStoredNodeGroup("demoNodegroupRTC");
		storeClient.executeStoreNodeGroup("demoNodegroupRTC", "runtime constraint on ?value", "semTK", sgJsonRTC.toJson());
			
		// load demo model owl		
		queryClientModel.clearAll();
		queryClientData.clearAll();
		queryClientModel.uploadOwl(Utility.getResourceAsFile(aSemtkLibClass, "/semantics/OwlModels/hardware.owl"));
		queryClientModel.uploadOwl(Utility.getResourceAsFile(aSemtkLibClass, "/semantics/OwlModels/testconfig.owl"));
		
		// ingest demo csv
		String data = Utility.getResourceAsString(aSemtkLibClass, "demoNodegroup_data.csv");
		ngeClient.dispatchIngestFromCsvStringsSync(sgJson, data);
		
	}
}
