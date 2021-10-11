/**
 ** Copyright 2016-2018 General Electric Company
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


package com.ge.research.semtk.sparqlX;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SparqlConnection {
/**
 * SparqlConnection is a "SparqlGraph" connection consisting of multiple Sparql Endpoints
 *    1) Ontology Model
 *    2) Data
 * 
 * 
 */
	
	private final String KEY_ENABLE_OWL_IMPORTS = "enableOwlImports";
	
	private String name = null;
	private String domain = "";  // deprecated
	private ArrayList<SparqlEndpointInterface> modelInterfaces = null;
	private ArrayList<SparqlEndpointInterface> dataInterfaces = null;
	private boolean enableOwlImports = false;
	
	/**
	 * Empty constructor
	 */
	public SparqlConnection () {
		this.name = "";
		this.domain = "";
		this.modelInterfaces = new ArrayList<SparqlEndpointInterface>();
		this.dataInterfaces = new ArrayList<SparqlEndpointInterface>();
		this.enableOwlImports = false;
	}
	
	/**
	 * Construct from json text
	 * @param jsonText
	 * @throws Exception
	 */
	public SparqlConnection(String jsonText) throws Exception {
		this();
	    this.fromString(jsonText);
	}
	
	/**
	 * Constructor
	 * @param name
	 * @param serverType virtuoso|fuseki|neptune
	 * @param serverURL  http://localhost:3030/DATASET where some triplestores don't accept /DATASET
	 * @param dataset  - will be used for model and data
	 * @throws Exception
	 */
	public SparqlConnection(String name, String serverType, String serverURL, String dataset) throws Exception{
		this();
		this.name = name;
		this.addDataInterface(serverType, 
				serverURL,
				dataset);
		this.addModelInterface(serverType, 
				serverURL,
				dataset);
	}
	
	/**
	 * Constructor
	 * @param name
	 * @param sei for model and data
	 * @throws Exception
	 */
	public SparqlConnection(String name, SparqlEndpointInterface sei) throws Exception {
		this();
		this.name = name;
		this.addDataInterface(sei);
		this.addModelInterface(sei);
	}
	
	/**
	 * Constructor
	 * @param name
	 * @param modelSei - model endpoint
	 * @param dataSei - data endpoint
	 * @throws Exception
	 */
	public SparqlConnection(String name, SparqlEndpointInterface modelSei, SparqlEndpointInterface dataSei) throws Exception {
		this();
		this.name = name;
		this.addDataInterface(dataSei);
		this.addModelInterface(modelSei);
	}
	
	@Deprecated
	public SparqlConnection(String name, String serverType, String dataServicetURL, String knowledgeServiceURL, String dataset) throws Exception{
		this();
		this.name = name;
		this.addDataInterface(serverType, 
				dataServicetURL,
				dataset);
		this.addModelInterface(serverType, 
				dataServicetURL,
				dataset);
	}
	
	/**
	 * Old constructor including "domain"
	 * @param name
	 * @param serverType
	 * @param dataServicetURL
	 * @param knowledgeServiceURL
	 * @param dataset
	 * @param domain
	 * @throws Exception
	 */
	@Deprecated
	public SparqlConnection(String name, String serverType, String dataServicetURL, String knowledgeServiceURL, String dataset, String domain) throws Exception{
		this();
		this.name = name;
		this.domain = (domain == null) ? "" : domain;
		this.addDataInterface(serverType, 
				dataServicetURL,
				dataset);
		this.addModelInterface(serverType, 
				dataServicetURL,
				dataset);
	}
	
	/**
	 * build json
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject jObj = new JSONObject();
		jObj.put("name", name);
		jObj.put("domain", (domain == null) ? "" : domain);
		
		JSONArray model = new JSONArray();
		JSONArray data = new JSONArray();

		for (int i=0; i < this.modelInterfaces.size(); i++) {			
			model.add(this.modelInterfaces.get(i).toJson());
		}
		
		for (int i=0; i < this.dataInterfaces.size(); i++) {
			data.add(this.dataInterfaces.get(i).toJson());
		}
		
		jObj.put("model", model);
		jObj.put("data", data);
		
		jObj.put(KEY_ENABLE_OWL_IMPORTS, this.enableOwlImports);
		return jObj;
	}
	
	/**
	 * Instantiate empty object from json
	 * @param jObj
	 * @throws Exception
	 */
	public void fromJson(JSONObject jObj) throws Exception {
		
		if(jObj.entrySet().size() == 1 && jObj.containsKey("sparqlConn")){
			throw new Exception("Cannot create SparqlConnection object because the JSON is wrapped in \"sparqlConn\"");
		}
		
		this.name = (String) jObj.get("name");
		this.domain = (String) jObj.get("domain");
		this.domain = (this.domain == null) ? "" : this.domain;
		
		this.modelInterfaces = new ArrayList<SparqlEndpointInterface>();
		
		this.dataInterfaces = new ArrayList<SparqlEndpointInterface>();
		
		if (jObj.containsKey("dsURL")) {
			
			// super-duper-backwards compatible read
		
			// If any field doesn't exist, presume it exists in the other connection
			this.addModelInterface(	(String)(jObj.get("type")), 
								    (String)(jObj.containsKey("onURL") ? jObj.get("onURL") : jObj.get("dsURL")),
								    (String)(jObj.containsKey("onDataset") ? jObj.get("onDataset") : jObj.get("dsDataset"))
							        );
			this.addDataInterface(	(String)(jObj.get("type")), 
									(String)(jObj.containsKey("dsURL") ? jObj.get("dsURL") : jObj.get("onURL")),
									(String)(jObj.containsKey("dsDataset") ? jObj.get("dsDataset")  : jObj.get("onDataset"))
									);
		} else {
			// normal read
			
			// read model interfaces
	    	for (int i=0; i < ((JSONArray)(jObj.get("model"))).size(); i++) {
	    		JSONObject m = (JSONObject)((JSONArray)jObj.get("model")).get(i);
	    		this.addModelInterface(SparqlEndpointInterface.getInstance(m));
	    	}
	    	// read data interfaces
	    	for (int i=0; i < ((JSONArray)(jObj.get("data"))).size(); i++) {
	    		JSONObject d = (JSONObject)((JSONArray)jObj.get("data")).get(i);
	    		this.addDataInterface(SparqlEndpointInterface.getInstance(d));
	    	}
		}
		
		this.enableOwlImports = (Boolean) jObj.getOrDefault(KEY_ENABLE_OWL_IMPORTS, false);
		
		// no deprecated field-handling
	}

	/**
	 * Override server in all data and model interfaces
	 * @param serverAndPort
	 * @throws Exception
	 */
	public void overrideSparqlServer(String serverAndPort) throws Exception {
		for (SparqlEndpointInterface sei : this.dataInterfaces) {
			sei.setServerAndPort(serverAndPort);
		}
		for (SparqlEndpointInterface sei : this.modelInterfaces) {
			sei.setServerAndPort(serverAndPort);
		}
	}
	
	/**
	 * Create deep copy
	 * @param other
	 * @return
	 * @throws Exception
	 */
	public static SparqlConnection deepCopy(SparqlConnection other) throws Exception {
		return new SparqlConnection(other.toJson().toString());
	}
	
	/**
	 * Instantiate from json text
	 * @param jsonText
	 * @throws Exception
	 */
	public void fromString(String jsonText) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject jObj = (JSONObject) parser.parse(jsonText);
		fromJson(jObj);

	}
	
	/**
	 * To json string
	 */
	public String toString() {
		return toJson().toString();
	}
	
	/**
	 * Compare
	 * @param other
	 * @param ignoreName
	 * @return
	 */
	public boolean equals(SparqlConnection other, boolean ignoreName) {
		String thisStr = this.toString();
		String otherStr = other.toString();
		if (ignoreName) {
			thisStr = thisStr.replaceFirst(this.name, "NAME");
			otherStr = otherStr.replaceFirst(other.name, "NAME");
		}
		return (thisStr.equals(otherStr));
	}
	
	public void setName (String name) {
		this.name = name;
	}
	
	public void setDomain (String domain) {
		this.domain = (domain == null) ? "" : domain;
	}
	
	public void addModelInterface(SparqlEndpointInterface sei) {
		this.modelInterfaces.add(sei);
	}
	public void addModelInterface(String sType, String url, String graph) throws Exception {
		this.modelInterfaces.add(SparqlEndpointInterface.getInstance(sType, url, graph));
	}
	
	public void addDataInterface(String sType, String url, String graph) throws Exception {
		this.dataInterfaces.add(SparqlEndpointInterface.getInstance(sType, url, graph));
	}
	public void addModelInterface(String sType, String url, String graph, String user, String passwd) throws Exception {
		this.modelInterfaces.add(SparqlEndpointInterface.getInstance(sType, url, graph, user, passwd));
	}
	
	public void addDataInterface(SparqlEndpointInterface sei) {
		this.dataInterfaces.add(sei);
	}
	public void addDataInterface(String sType, String url, String graph, String user, String passwd) throws Exception {
		this.dataInterfaces.add(SparqlEndpointInterface.getInstance(sType, url, graph, user, passwd));
	}
	public int getModelInterfaceCount() {
		return this.modelInterfaces.size();
	}
	
	public SparqlEndpointInterface getModelInterface(int i) {
		return this.modelInterfaces.get(i);
	}
	
	public ArrayList<SparqlEndpointInterface> getModelInterfaces() {
		return this.modelInterfaces;
	}
	
	public void clearInterfaces() {
		this.clearDataInterfaces();
		this.clearModelInterfaces();
	}
	
	public void clearDataInterfaces() {
		this.dataInterfaces = new ArrayList<SparqlEndpointInterface>();
	}
	public void clearModelInterfaces() {
		this.modelInterfaces = new ArrayList<SparqlEndpointInterface>();
	}
	
	public String getDomain() {
		return this.domain;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getDataInterfaceCount() {
		return this.dataInterfaces.size();
	}
	
	public SparqlEndpointInterface getDataInterface(int i) {
			return this.dataInterfaces.get(i);
	}
	
	public ArrayList<SparqlEndpointInterface> getDataInterfaces() {
		return this.dataInterfaces;
	}
	
	public SparqlEndpointInterface getDefaultQueryInterface() throws Exception {
		if (this.dataInterfaces.size() > 0) {
			return this.dataInterfaces.get(0);
		} else if (this.modelInterfaces.size() > 0) {
			return this.modelInterfaces.get(0);
		} else {
			throw new Exception("This SparqlConnection has no endpoints.");
		}
	}
	
	public SparqlEndpointInterface getInsertInterface() throws Exception {
		return this.dataInterfaces.get(0);
	}
	
	public SparqlEndpointInterface getDeleteInterface() throws Exception {
		return this.dataInterfaces.get(0);
	}
	
	public boolean isOwlImportsEnabled() {
		return enableOwlImports;
	}

	/**
	 * Default is false for backwards-compatibility reasons
	 * @param enableOwlImports
	 */
	public void setOwlImportsEnabled(boolean enableOwlImports) {
		this.enableOwlImports = enableOwlImports;
	}

	/**
	 * Do all endpoints point to the same server
	 * @return
	 */
	public boolean isSingleServerURL() {
		String url = "";
		for (int i=0; i < this.modelInterfaces.size(); i++) {
			String e =  this.modelInterfaces.get(i).getServerAndPort();
			if (url.equals("")) {
				url = e;
			} else if (! e.equals(url)) {
				return false;
			}
		}
		
		// add data interfaces
		for (int i=0; i < this.dataInterfaces.size(); i++) {
			String e =  this.dataInterfaces.get(i).getServerAndPort();
			if (url.equals("")) {
				url = e;
			} else if (! e.equals(url)) {
				return false;
			}
		}
		
		// if there are no serverURLs then false
		if (url.equals("")) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Throw exception if connection contains multiple ServerURLs
	 * @return
	 */
	public void confirmSingleServerURL() throws Exception {
		String url = "";
		
		// check data interfaces
		for (int i=0; i < this.dataInterfaces.size(); i++) {
			String e =  this.dataInterfaces.get(i).getServerAndPort();
			if (url.equals("")) {
				url = e;
			} else if (! e.equals(url)) {
				throw new Exception("Connection has two different SPARQL endpoints: " + url + " and " + e);
			}
		}
		// check model interfaces
		for (int i=0; i < this.modelInterfaces.size(); i++) {
			String e =  this.modelInterfaces.get(i).getServerAndPort();
			if (url.equals("")) {
				url = e;
			} else if (! e.equals(url)) {
				throw new Exception("Connection has two different SPARQL endpoints: " + url + " and " + e);
			}
		}
		
		// if there are no serverURLs then false
		if (url.equals("")) {
			throw new Exception("Connection has no SPARQL endpoints: " + this.toString());
		} 
	}
	
	/**
	 * get list of graphs for a given serverURL
	 * @param serverURL
	 * @return
	 */
	public ArrayList<String> getAllGraphsForServer(String serverURL) {
		ArrayList<String> ret = new ArrayList<String>();
		
		ret.addAll(this.getDataDatasetsForServer(serverURL));
		
		// add any models that aren't duplicates of data
		ArrayList<String> modelDatasets = this.getModelDatasetsForServer(serverURL);
		for (String ds : modelDatasets) {
			if (! ret.contains(ds)) {
				ret.add(ds);
			}
		}
		
		return ret;
	}
	
	/**
	 *  get list of DATA datasets for a given serverURL
	 * @param serverURL
	 * @return
	 */
	public ArrayList<String> getDataDatasetsForServer(String serverURL) {
		ArrayList<String> ret = new ArrayList<String>();
		
		for (int i=0; i < this.dataInterfaces.size(); i++) {
			SparqlEndpointInterface e =  this.dataInterfaces.get(i);
			if (e.getServerAndPort().equals(serverURL)  &&  ret.indexOf(e.getGraph()) == -1) {
				ret.add(e.getGraph());
			}
		}
		
		return ret;
	}
	
	/**
	 *  get list of MODEL datasets for a given serverURL
	 * @param serverURL
	 * @return
	 */
	public ArrayList<String> getModelDatasetsForServer(String serverURL) {
		ArrayList<String> ret = new ArrayList<String>();
		
		for (int i=0; i < this.modelInterfaces.size(); i++) {
			SparqlEndpointInterface e =  this.modelInterfaces.get(i);
			if (e.getServerAndPort().equals(serverURL) &&  ret.indexOf(e.getGraph()) == -1) {
				ret.add(e.getGraph());
			}
		}
		
		return ret;
	}
	
	/**
	 * Generate a string that uniquely identifies the model connection(s)
	 * @return
	 */
	public String getUniqueModelKey() {
		
		String modelKeys[] = new String[this.getModelInterfaceCount()];
		
		for (int i=0; i < this.getModelInterfaceCount(); i++) {
			modelKeys[i] = this.buildSeiKey(this.getModelInterface(i));
		}
		Arrays.sort(modelKeys);
		
		StringBuilder ret = new StringBuilder();
		ret.append(this.domain + ";");
		for (int i=0; i < modelKeys.length; i++) {
			ret.append(modelKeys[i] + ";");
		}
		ret.append(this.enableOwlImports ? "owlImports;" : "noImports;");
		return ret.toString();
	}
	
	private String buildSeiKey(SparqlEndpointInterface sei) {
		return sei.getServerAndPort() + "-" + sei.getGraph();
	}
	
	/**
	 * Are any of the graphs in this connection also in the Connection that generated sparqlConnKey
	 * @param sparqlConnKey
	 * @return boolean
	 */
	public boolean overlapsSparqlConnKey(String sparqlConnKey) {
		ArrayList<SparqlEndpointInterface> seiList = new ArrayList<SparqlEndpointInterface>();
		seiList.addAll(this.getModelInterfaces());
		seiList.addAll(this.getDataInterfaces());
		
		for (SparqlEndpointInterface sei : seiList) {
			if (sparqlConnKey.contains(this.buildSeiKey(sei) + ";"))
				return true;
		}
		return false;
	}
	
	/**
	 * Generate a string that uniquely identifies the model and data connection(s)
	 * @return
	 */
	public String getUniqueKey() {
		
		String seiKeys[] = new String[this.getModelInterfaceCount() + this.getDataInterfaceCount()];
		int index = 0;
		
		// build a list of keys: one per graph
		for (int i=0; i < this.getModelInterfaceCount(); i++) {
			seiKeys[index++] = this.buildSeiKey(this.getModelInterface(i));
		}
		for (int i=0; i < this.getDataInterfaceCount(); i++) {
			seiKeys[index++] = this.buildSeiKey(this.getDataInterface(i));
		}
		Arrays.sort(seiKeys);
		
		// append keys if they are not duplicates.  everything is followed by ';'
		StringBuilder ret = new StringBuilder();
		ret.append(this.domain + ";");
		for (int i=0; i < seiKeys.length; i++) {
			if (i == 0 || !seiKeys[i].equals(seiKeys[i-1])) {
				ret.append(seiKeys[i] + ";");
			}
		}
		ret.append(this.enableOwlImports ? "owlImports;" : "noImports;");
		return ret.toString();
	}
}
