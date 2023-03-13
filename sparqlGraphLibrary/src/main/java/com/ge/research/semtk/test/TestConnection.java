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

package com.ge.research.semtk.test;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;

/**
 * A utility class to load data to a semantic graph.  Intended for use in tests.
 * 
 * NOTE: This class cannot be put in under src/test/java because it must remain accessible to other projects.
 */
public class TestConnection {
	SparqlConnection conn = null;
	
	// PEC TODO: if we want the option of sharing or splitting model from data in different graphs then static functions will not do.
	//           It will have to be an object instantiated by a json nodegroup (shows whether they're split) or NULL (pick a default)
	public TestConnection(int modelCount, int dataCount, String domain) throws Exception {
		conn = new SparqlConnection();
		conn.setName("JUnitTest");
		conn.setDomain(domain);
		
		SparqlEndpointInterface sei = TestGraph.getSei();
		for (int i=0; i < modelCount; i++) {
			sei.setGraph(generateDatasetName(String.format("model%d", i)));
			conn.addModelInterface( sei );
		}
		for (int i=0; i < dataCount; i++) {
			sei.setGraph(generateDatasetName(String.format("data%d", i)));
			conn.addModelInterface( sei );
		}
		
		// make sure everything is empty
		this.clearGraphs();
	}
	
	
	public SparqlConnection getSparqlConn() {
		return this.conn;
	}
	
	/**
	 * Clear the test graph
	 */
	public void clearGraphs() throws Exception {
		ArrayList<SparqlEndpointInterface> seiList = new ArrayList<SparqlEndpointInterface>();
		seiList.addAll(this.conn.getModelInterfaces());
		seiList.addAll(this.conn.getDataInterfaces());
		
		for (SparqlEndpointInterface sei : seiList) {
			IntegrationTestUtility.clearGraph(sei);
		}
	}
	
	/**
	 * Drop the test graph (DROP lets the graph be CREATEd again, whereas CLEAR does not)
	 */
	public void dropGraphs() throws Exception {
		ArrayList<SparqlEndpointInterface> seiList = new ArrayList<SparqlEndpointInterface>();
		seiList.addAll(this.conn.getModelInterfaces());
		seiList.addAll(this.conn.getDataInterfaces());
		
		for (SparqlEndpointInterface sei : seiList) {
			String query = SparqlToXUtils.generateDropGraphSparql(sei);
			GeneralResultSet resultSet = sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
			if (!resultSet.getSuccess()) {
				throw new Exception(resultSet.getRationaleAsString(" "));
			}
		}
	}
	
	public Table execTableSelect(String query) throws Exception {
		// execute a select query
		// exception if there's any problem
		// return the table
		SparqlEndpointInterface sei = this.conn.getDefaultQueryInterface();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		res.throwExceptionIfUnsuccessful();
		
		return res.getResults();
	}
	
	/**
	 * 
	 * @param owlFilename  "src/test/resources/file.owl"
	 * @throws Exception
	 */
	public  void uploadOwl(int modelConnIndex, String owlFilename) throws Exception {
		
		SparqlEndpointInterface sei = this.conn.getModelInterface(modelConnIndex);
		
		Path path = Paths.get(owlFilename);
		byte[] owl = Files.readAllBytes(path);
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	

	/**
	 * Get SparqlGraphJson modified with Test connection
	 * @param jsonFilename
	 */
	public SparqlGraphJson getSparqlGraphJsonFromFile(String jsonFilename) throws Exception {		
		InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonFilename));
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(reader);		
		return getSparqlGraphJsonFromJson(jObj);
	}	
	
	/**
	 * Get SparqlGraphJson modified with Test connection
	 * @param jsonString
	 */
	public SparqlGraphJson getSparqlGraphJsonFromString(String jsonString) throws Exception {		
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(jsonString);		
		return getSparqlGraphJsonFromJson(jObj);
	}	
	
	/**
	 * Get SparqlGraphJson modified with Test connection
	 * @param jsonObject
	 */	
	@SuppressWarnings("unchecked")
	public SparqlGraphJson getSparqlGraphJsonFromJson(JSONObject jObj) throws Exception{
		
		SparqlGraphJson s = new SparqlGraphJson(jObj);
		HashMap<String,String> hash = new HashMap<String,String>();
		this.conn = s.getSparqlConn();
		
		// replace dataset names with test dataset names 1-for-1
		String oldDs = null;
		String newDs = null;
		for (int i=0; i < conn.getDataInterfaceCount(); i++) {
			oldDs = conn.getDataInterface(i).getGraph();
			if (hash.containsKey(oldDs)) {
				newDs = hash.get(oldDs);
			} else {
				newDs = generateDatasetName(String.format("data%d", i));
				hash.put(oldDs, newDs);
			}
			conn.getDataInterface(i).setGraph(newDs);
		}
		for (int i=0; i < conn.getModelInterfaceCount(); i++) {
			oldDs = conn.getModelInterface(i).getGraph();
			if (hash.containsKey(oldDs)) {
				newDs = hash.get(oldDs);
			} else {
				newDs = generateDatasetName(String.format("model%d", i));
				hash.put(oldDs, newDs);
			}
			conn.getModelInterface(i).setGraph(newDs);
		}
		
		s.setSparqlConn(conn);
		
		return s;
	}
	
	/**
	 * Generate a dataset name (unique per user)
	 */
	private static String generateDatasetName(String sub) {
		return String.format("http://%s/junit/%s", System.getProperty("user.name"), sub);
	}	
}
