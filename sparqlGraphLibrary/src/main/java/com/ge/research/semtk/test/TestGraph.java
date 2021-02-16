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

package com.ge.research.semtk.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.aws.S3Connector;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.properties.EndpointProperties;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.NeptuneSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * A utility class to load data to a semantic graph.  Intended for use in tests.
 * 
 * NOTE: This class cannot be put in under src/test/java because it must remain accessible to other projects.
 */
public class TestGraph {

	// PEC TODO: if we want the option of sharing or splitting model from data in different graphs then static functions will not do.
	//           It will have to be an object instantiated by a json nodegroup (shows whether they're split) or NULL (pick a default)
	public TestGraph() {
	}
	
	public static SparqlEndpointInterface getSei() throws Exception {
		return getSei(generateGraphName("both"));
	}
	
	public static SparqlEndpointInterface getOSObject() throws Exception {
		return SparqlEndpointInterface.getInstance("virtuoso", "http://server:8080", "graph", "user", "password");
	}
	
	public static SparqlEndpointInterface getSei(String graphName) throws Exception {
		SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(getSparqlServerType(), getSparqlServer(), graphName, getUsername(), getPassword());
		
		try{
			sei.executeTestQuery();
		}catch(Exception e){
			LocalLogger.logToStdOut("***** Cannot connect to " + getSparqlServerType() + " server at " + getSparqlServer() + " with the given credentials for '" + getUsername() + "'.  Set up this server or change settings in TestGraph. *****");
			throw e;
		}
		
		if (sei instanceof NeptuneSparqlEndpointInterface) {

			String region = IntegrationTestUtility.get("neptuneupload.s3ClientRegion");
			String iamRoleArn = IntegrationTestUtility.get("neptuneupload.awsIamRoleArn");
			String name =   IntegrationTestUtility.get("neptuneupload.s3BucketName");
	       
			((NeptuneSparqlEndpointInterface) sei).setS3Config(region, name, iamRoleArn);
		}
		return sei;
	}
	
	public static EndpointProperties getEndpointProperties() throws Exception {
		SparqlEndpointInterface sei = getSei();
		EndpointProperties ret = new EndpointProperties();
		ret.setEndpointType(sei.getServerType());
		ret.setEndpointServerUrl(sei.getServerAndPort());
		ret.setEndpointUsername(sei.getUserName());
		ret.setEndpointPassword(sei.getPassword());
		
		return ret;
	}
	
	public static SparqlConnection getSparqlAuthConn() throws Exception {
		SparqlConnection conn = new SparqlConnection();
		conn.setName("JUnitTest");
		
		SparqlEndpointInterface sei = getSei();
		conn.addDataInterface( sei.getServerType(), sei.getServerAndPort(), sei.getGraph(), getUsername(), getPassword());
		conn.addModelInterface(sei.getServerType(), sei.getServerAndPort(), sei.getGraph(), getUsername(), getPassword());
		return conn;
	}
	
	public static SparqlConnection getSparqlConn() throws Exception {
		SparqlConnection conn = new SparqlConnection();
		conn.setName("JUnitTest");
		
		SparqlEndpointInterface sei = getSei();
		conn.addDataInterface( sei.getServerType(), sei.getServerAndPort(), sei.getGraph());
		conn.addModelInterface(sei.getServerType(), sei.getServerAndPort(), sei.getGraph());
		return conn;
	}
	
	@Deprecated
	public static SparqlConnection getSparqlConn(String domain) throws Exception {
		SparqlConnection conn = new SparqlConnection();
		conn.setName("JUnitTest");
		conn.setDomain(domain);   
		
		SparqlEndpointInterface sei = getSei();
		conn.addDataInterface( sei.getServerType(), sei.getServerAndPort(), sei.getGraph());
		conn.addModelInterface(sei.getServerType(), sei.getServerAndPort(), sei.getGraph());
		return conn;
	}
	
	/**
	 * Get the URL of the SPARQL server.
	 * @throws Exception 
	 */
	public static String getSparqlServer() throws Exception{
		return IntegrationTestUtility.get("testgraph.server");
	}
	
	/**
	 * Get the SPARQL server type.
	 * @throws Exception 
	 */
	public static String getSparqlServerType() throws Exception{
		return IntegrationTestUtility.get("testgraph.type");
	}
	
	/**
	 * Get the test dataset name.
	 */
	public static String getDataset() throws Exception {
		return getSei().getGraph();
	}
	
	
	
	/**
	 * Get the SPARQL server username.
	 * @throws Exception 
	 */
	public static String getUsername() throws Exception{
		return IntegrationTestUtility.get("sparqlendpoint.username");
	}
	
	/**
	 * Get the SPARQL server password.
	 * @throws Exception 
	 */
	public static String getPassword() throws Exception{
		return IntegrationTestUtility.get("sparqlendpoint.password");
	}
	
	/**
	 * Clear the test graph
	 */
	public static void clearGraph() throws Exception {
		getSei().clearGraph();
		IntegrationTestUtility.getOntologyInfoClient().uncacheChangedModel(TestGraph.getSparqlConn());
	}
	
	public static void clearPrefix(String prefix) throws Exception {
		getSei().clearPrefix(prefix);
		IntegrationTestUtility.getOntologyInfoClient().uncacheChangedModel(TestGraph.getSparqlConn());
	}
	/**
	 * Drop the test graph (DROP lets the graph be CREATEd again, whereas CLEAR does not)
	 */
	public static void dropGraph() throws Exception {
		getSei().dropGraph();
		IntegrationTestUtility.getOntologyInfoClient().uncacheChangedModel(TestGraph.getSparqlConn());
	}
	
	/**
	 * Get directly (no oInfoClient)
	 * @return
	 * @throws Exception
	 */
	public static OntologyInfo getOInfo() throws Exception {
		return new OntologyInfo(TestGraph.getSparqlConn());
	}
	
	public static void execDeletionQuery(String query) throws Exception{
		SparqlEndpointInterface sei = getSei();
		GeneralResultSet resultSet = sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}		
	}
	
	public static Table execTableSelect(String query) throws Exception {
		// execute a select query
		// exception if there's any problem
		// return the table
		SparqlEndpointInterface sei = getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		res.throwExceptionIfUnsuccessful();
		
		return res.getResults();
	}
	
	public static Table execTableSelect(SparqlGraphJson sgJson) throws Exception {
		// execute a select query
		// exception if there's any problem
		// return the table
		
		return SparqlGraphJson.executeSelectToTable(sgJson.toJson(), getSparqlConn(), IntegrationTestUtility.getOntologyInfoClient());
	}
	
	
	public static Table execTableSelect(JSONObject sgJsonJson, OntologyInfoClient oInfoClient) throws Exception {
		// execute a select query
		// exception if there's any problem
		// return the table
		
		return SparqlGraphJson.executeSelectToTable(sgJsonJson, getSparqlConn(), oInfoClient);
	}
	public static JSONArray execJsonConstruct(SparqlGraphJson sgJson) throws Exception {
		// execute a construct query
		// exception if there's any problem
		// return the table
		
		return SparqlGraphJson.executeConstructToJson(sgJson.toJson(), getSparqlConn(), IntegrationTestUtility.getOntologyInfoClient());
	}
	
	public static JSONArray execJsonConstruct(JSONObject sgJsonJson, OntologyInfoClient oInfoClient) throws Exception {
		// execute a construct query
		// exception if there's any problem
		// return the table
		
		return SparqlGraphJson.executeConstructToJson(sgJsonJson, getSparqlConn(), oInfoClient);
	}
	
	public static Table execSelectFromResource(Object o, String resourceName) throws Exception {
		return execSelectFromResource(o.getClass(), resourceName);
	}
	
	@SuppressWarnings("rawtypes")
	public static Table execSelectFromResource(Class c, String resourceName) throws Exception {
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(c, resourceName);
		return execTableSelect(sgjson.toJson(), IntegrationTestUtility.getOntologyInfoClient());
	}
	
	
	public static JSONArray execConstructFromResource(Object o, String resourceName) throws Exception {
		return execConstructFromResource(o.getClass(), resourceName);
	}
	
	@SuppressWarnings("rawtypes")
	public static JSONArray execConstructFromResource(Class c, String resourceName) throws Exception {
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(c, resourceName);
		return execJsonConstruct(sgjson.toJson(), IntegrationTestUtility.getOntologyInfoClient());
	}
	
	
	/**
	 * Get the number of triples in the test graph.
	 */
	public static int getNumTriples() throws Exception {
		String sparql = SparqlToXUtils.generateCountTriplesSparql(getSei());
		Table table = TestGraph.execTableSelect(sparql); 
		return (new Integer(table.getCell(0, 0))).intValue(); // this cell contains the count
	}

	/**
	 * Upload owl file to the test graph
	 * @param owlFilename  "src/test/resources/file.owl"
	 * @throws Exception
	 */
	// Use uploadOwlResource 
	@Deprecated 
	public static void uploadOwl(String owlFilename) throws Exception {
		
		SparqlEndpointInterface sei = getSei();		
		Path path = Paths.get(owlFilename);
		byte[] owl = Files.readAllBytes(path);
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
		IntegrationTestUtility.getOntologyInfoClient().uncacheChangedModel(TestGraph.getSparqlConn());
	}
	
	public static void uploadOwlResource(Object o, String resourceName) throws Exception {
		uploadOwlContents(Utility.getResourceAsString(o.getClass(), resourceName));
	}
	public static void uploadOwlResource(Class c, String resourceName) throws Exception {
		uploadOwlContents(Utility.getResourceAsString(c, resourceName));
	}
	public static void uploadOwlContents(String owl) throws Exception {
		
		SparqlEndpointInterface sei = getSei();		
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl.getBytes()));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
		IntegrationTestUtility.getOntologyInfoClient().uncacheChangedModel(TestGraph.getSparqlConn());
	}
	
	/**
	 * Find graph from inside the owl/rdf
	 * Clear the graph and load the owl
	 * @param owlFilename
	 * @throws Exception
	 */
	public static void syncOwlToItsGraph(String owlFilename) throws Exception {
		
		String base = Utility.getXmlBaseFromOwlRdf(new FileInputStream(owlFilename));
		
		SparqlEndpointInterface sei = getSei();
		
		sei.setGraph(base);
		sei.clearGraph();
		
		Path path = Paths.get(owlFilename);
		byte[] owl = Files.readAllBytes(path);
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	
	public static void clearSyncedGraph(String owlFilename) throws Exception {
		
		String base = Utility.getXmlBaseFromOwlRdf(new FileInputStream(owlFilename));
		
		SparqlEndpointInterface sei = getSei();
		
		sei.setGraph(base);
		sei.clearGraph();
		
	}
	
	public static void uploadTurtleResource(Object o, String resourceName) throws Exception {
		uploadTurtleString(Utility.getResourceAsString(o.getClass(), resourceName));
	}
	public static void uploadTurtleResource(Class c, String resourceName) throws Exception {
		uploadTurtleString(Utility.getResourceAsString(c, resourceName));
	}
	public static void uploadTurtle(String owlFilename) throws Exception {
		
		SparqlEndpointInterface sei = getSei();
		Path path = Paths.get(owlFilename);
		byte[] owl = Files.readAllBytes(path);
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadTurtle(owl));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	
	public static void uploadTurtleString(String turtleData) throws Exception {
		
		SparqlEndpointInterface sei = getSei();
		
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadTurtle(turtleData.getBytes()));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	/**
	 * Upload an owl string to the test graph
	 * @param owl
	 * @throws Exception
	 */
	public static void uploadOwlString(String owl) throws Exception {
		
		SparqlEndpointInterface sei = getSei();
		
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl.getBytes()));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	
	/**
	 * 
	 * @param jsonFilename e.g. "src/test/resources/nodegroup.json"
	 * @return
	 * @throws Exception
	 */
	public static NodeGroup getNodeGroup(String jsonFilename) throws Exception {
		// get nodegroup with TestGraph's oInfo
		return getSparqlGraphJsonFromFile(jsonFilename).getNodeGroupNoInflateNorValidate(IntegrationTestUtility.getOntologyInfoClient());
	}
	
	public static NodeGroup getNodeGroupWithOInfo(SparqlGraphJson sgjson) throws Exception {
		return sgjson.getNodeGroupNoInflateNorValidate(IntegrationTestUtility.getOntologyInfoClient());
	}
	
	public static String addRuntimeConstraint(SparqlGraphJson sgjson, String sparqlID, SupportedOperations operator, String [] operandList) throws Exception {

		NodeGroup ng = sgjson.getNodeGroup();
		
		// Try to call through the highest level of runtime constraint through value constraint code
		RuntimeConstraintManager rtci = new RuntimeConstraintManager(ng);
		JSONObject constraint = RuntimeConstraintManager.buildRuntimeConstraintJson(
				sparqlID, 
				operator,
				new ArrayList<String>(Arrays.asList(operandList)));
		JSONArray runtimeConstraints = new JSONArray();
		runtimeConstraints.add(constraint);
		rtci.applyConstraintJson(runtimeConstraints);
		String constraintSparql = ng.getItemBySparqlID(sparqlID).getValueConstraint().toString();
		sgjson.setNodeGroup(ng);
		return constraintSparql;
	}
	
	public static SparqlGraphJson getSparqlGraphJsonFromResource(Object o, String resourceName) throws Exception {
		return getSparqlGraphJsonFromJson(Utility.getResourceAsJson(o.getClass(), resourceName));
	}
	public static SparqlGraphJson getSparqlGraphJsonFromResource(Class c, String resourceName) throws Exception {
		return getSparqlGraphJsonFromJson(Utility.getResourceAsJson(c, resourceName));
	}
	/**
	 * Get SparqlGraphJson modified with Test connection
	 * @param jsonFilename
	 */
	public static SparqlGraphJson getSparqlGraphJsonFromFile(String jsonFilename) throws Exception {		
		InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonFilename));
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(reader);		
		return getSparqlGraphJsonFromJson(jObj);
	}	
	
	/**
	 * Get SparqlGraphJson modified with Test connection
	 * @param jsonString
	 */
	public static SparqlGraphJson getSparqlGraphJsonFromString(String jsonString) throws Exception {		
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(jsonString);		
		return getSparqlGraphJsonFromJson(jObj);
	}	
	
	/**
	 * Get SparqlGraphJson modified with Test connection.
	 * Not compatible with enabled owl imports.  
	 * @param jsonObject
	 */	
	@SuppressWarnings("unchecked")
	public static SparqlGraphJson getSparqlGraphJsonFromJson(JSONObject jObj) throws Exception{
		
		SparqlGraphJson s = new SparqlGraphJson(jObj);
		
		// swap out data interfaces
		SparqlConnection conn = s.getSparqlConn();
		conn.clearDataInterfaces();
		conn.addDataInterface(getSei());
		
		conn.clearModelInterfaces();
		conn.addModelInterface(getSei());
		
		s.setSparqlConn(conn);
		
		return s;
	}
	
	/**
	 * Generate a dataset name (unique per user)
	 */
	public static String generateGraphName(String sub) {
		String user = System.getProperty("user.name");
		
		String machine = null;
		try {
			machine = java.net.InetAddress.getLocalHost().getHostName().replaceAll("[^a-zA-Z0-9-]", "_");
		} catch (Exception e) {
			machine = "unknown_host";
		}
		
		return String.format("http://junit/%s/%s/%s", machine, user, sub);
	}
	@Deprecated
	public static String generateDatasetName(String sub) {
		return generateGraphName(sub);
	}	
	
	/**
	 * Clear graph, 
	 * In src/test/resources: loads baseName.owl, gets sgJson from baseName.json, and loads baseName.csv
	 * @param baseName
	 * @return SparqlGraphJson
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public static SparqlGraphJson initGraphWithData(Class c, String baseName) throws Exception {
		String jsonResource = String.format("/%s.json", baseName);
		String owlResource = String.format("/%s.owl", baseName);
		String csvResource = String.format("/%s.csv", baseName);

		// load the model
		TestGraph.clearGraph();
		TestGraph.uploadOwlContents(Utility.getResourceAsString(c, owlResource));
		String csv = Utility.getResourceAsString(c, csvResource);
		return ingestCsvString(c, jsonResource, csv);
	}
	
	@SuppressWarnings("rawtypes")
	public static SparqlGraphJson addModelAndData(Class c, String baseName) throws Exception {
		String jsonResource = String.format("/%s.json", baseName);
		String owlResource = String.format("/%s.owl", baseName);
		String csvResource = String.format("/%s.csv", baseName);

		// load the model
		TestGraph.uploadOwlContents(Utility.getResourceAsString(c, owlResource));
		String csv = Utility.getResourceAsString(c, csvResource);
		return ingestCsvString(c, jsonResource, csv);
	}
	
	@SuppressWarnings("rawtypes")
	public static SparqlGraphJson ingest(Class c, String sgjsonResource, String csvResource) throws Exception {
		String csv = Utility.getResourceAsString(c, csvResource);
		return ingestCsvString(c, sgjsonResource, csv, true);
	}
	
	@SuppressWarnings("rawtypes")
	public static SparqlGraphJson ingestCsvString(Class c, String sgjsonResource, String data) throws Exception {

		return ingestCsvString(c, sgjsonResource, data, true);
	}
	
	@SuppressWarnings("rawtypes")
	public static SparqlGraphJson ingestCsvString(Class c, String sgjsonResource, String dataOrPath, boolean isData) throws Exception {

		JSONObject jObj = Utility.getResourceAsJson(c, sgjsonResource);
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromJson(jObj); 
		
		// load the data
		Dataset ds = new CSVDataset(dataOrPath, isData);
		DataLoader dl = new DataLoader(sgJson, ds, getUsername(), getPassword());
		int rows = dl.importData(true);
		if (rows == 0) {
			throw new Exception(dl.getLoadingErrorReportBrief());
		}
		
		return sgJson;
	}
	
	public static Table runQuery(String query) throws Exception {
		return TestGraph.getSei().executeQueryToTable(query);
	}
	
	/**
	 * execute ng select and compare results to those in expectedFileName
	 * @param ng
	 * @param caller - whose resources should be checked for expectedFileName
	 * @param expectedFileName
	 * @throws Exception
	 */	
	public static void queryAndCheckResults(SparqlGraphJson sgjson, Object caller, String expectedFileName) throws Exception {
		NodeGroup ng = sgjson.getNodeGroupNoInflateNorValidate(IntegrationTestUtility.getOntologyInfoClient());
		IntegrationTestUtility.querySeiAndCheckResults(ng, TestGraph.getSei(), caller, expectedFileName);
	}
	
	/**
	 * Compare results to those in expectedFileName
	 * @param results\
	 * @param caller - whose resources should be checked for expectedFileName
	 * @param expectedFileName
	 * @throws Exception 
	 */
	public static void compareResultsOLD(String results, Object caller, String expectedFileName) throws Exception {
		String actual =  results.replaceAll("\r\n", "\n");
		String expected = null;
		try {
			expected = Utility.getResourceAsString(caller, expectedFileName).replaceAll("\r\n", "\n");
		} catch (Exception e) {
			throw new Exception ("Error retrieving file: " + expectedFileName, e);
		}
		
		actual = Utility.replaceUUIDs(actual);
		expected = Utility.replaceUUIDs(expected);
				
		// if not equal, print them out
		if (! actual.equals(expected)) {
			System.out.print("Expected:\n--------\n" + StringEscapeUtils.escapeJava(expected) + "\n--------\n");
			System.out.print("Actual:\n--------\n" + StringEscapeUtils.escapeJava(actual) + "\n--------\n");
			
			// print message if lengths are different
			int actualLen = actual.length();
			int expectedLen = expected.length();
			
			if (actualLen != expectedLen) {
				System.out.print(String.format("Expected len: %d.  Actual len: %d\n", expectedLen, actualLen));
			}
			
			// print first 10 differences
			int found = 0;
			for (int i=0; found < 10 && i < Math.min(actualLen, expectedLen); i++) {
				if (actual.charAt(i) != expected.charAt(i)) {
					found ++;
					System.out.print(String.format("Char %d: expected: '%c' actual: '%c'\n", i, expected.charAt(i), actual.charAt(i)));
				}
			}
			
			// fail
			assertTrue("Actual results did not match expected (see stdout)", false);
		}
	}
	
	

}
