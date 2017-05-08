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


package com.ge.research.semtk.sparqlX;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SparqlConnection {
/**
 * SparqlConnection is a "SparqlGraph" connection consisting of TWO Sparql Endpoints
 * 1) Ontology
 * 2) Data
 * 
 * To match files written by historical javascript, this is mostly a JSON wrangler.
 * 
 */

	// TODO replace all hardcoded json keys with Strings like this
	public final static String DSDATASET_JSONKEY = "dsDataset";
	
	private final static String QUERY_SERVER = "kdl";
	private final static String FUSEKI_SERVER = "fuseki";
	private final static String VIRTUOSO_SERVER = "virtuoso";
	
	private String name = "";
	private String serverType = "";
	
	private String dataServerUrl = "";
	private String dataKsServerURL = "";   
	private String dataSourceDataset = "";
	
	private String ontologyServerUrl = "";
	private String ontologyKsServerURL = "";   
	private String ontologySourceDataset = "";
	
	private String domain = "";
	
	private SparqlEndpointInterface dataInterface = null;
	private SparqlEndpointInterface ontologyInterface = null;
	
	public SparqlConnection () {
		
	}
	
	public SparqlConnection(String jsonText) throws Exception {
	    this.fromString(jsonText);
	}
	
	public SparqlConnection(String name, String serverType, String dataServicetURL, String knowledgeServiceURL, String dataset, String domain){
		this.name = name;
		this.dataServerUrl = dataServicetURL;
		this.dataKsServerURL = knowledgeServiceURL;
		this.serverType = serverType;
		this.dataSourceDataset = dataset;
		this.domain = domain;
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject jObj = new JSONObject();
		jObj.put("name", name);
		jObj.put("type", serverType);
		jObj.put("dsURL", dataServerUrl);
		jObj.put("dsKsURL", dataKsServerURL);
		jObj.put("dsDataset", dataSourceDataset);
		jObj.put("domain", domain);

		if (! ontologyServerUrl.equals(dataServerUrl))         { jObj.put("onURL", ontologyServerUrl); }
		if (! ontologyKsServerURL.equals(dataKsServerURL))     { jObj.put("onKsURL", ontologyKsServerURL); }
		if (! ontologySourceDataset.equals(dataSourceDataset)) { jObj.put("onDataset", ontologySourceDataset); }
		return jObj;
	}
	
	public void fromJson(JSONObject jObj) throws Exception {
		
		if(jObj.entrySet().size() == 1 && jObj.containsKey("sparqlConn")){
			throw new Exception("Cannot create SparqlConnection object because the JSON is wrapped in \"sparqlConn\"");
		}
		
		this.name = (String) jObj.get("name"); 
		this.serverType = (String) jObj.get("type"); 
			
		this.dataServerUrl = (String) jObj.get("dsURL");
		this.dataKsServerURL = (String) jObj.get("dsKsURL");
		this.dataSourceDataset = (String) jObj.get("dsDataset");
			
		this.ontologyServerUrl =     jObj.containsKey("onURL") ?     (String) jObj.get("onURL") : (String) jObj.get("dsURL");
		this.ontologyKsServerURL =   jObj.containsKey("onKsURL") ?   (String) jObj.get("onKsURL") : (String) jObj.get("dsKsURL");
		this.ontologySourceDataset = jObj.containsKey("onDataset") ? (String) jObj.get("onDataset") : (String) jObj.get("dsDataset");
		 
		this.dataInterface = this.createDataInterface();
		this.ontologyInterface = this.createOntologyInterface();
		
		this.domain = (String) jObj.get("domain");
	}

	public void fromString(String jsonText) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject jObj = (JSONObject) parser.parse(jsonText);
		fromJson(jObj);

	}
	
	public String toString() {
		return toJson().toString();
	}
	
	public boolean equals(SparqlConnection other, boolean ignoreName) {
		
		return ((ignoreName || this.name == other.name) && 
				this.serverType == other.serverType &&
				this.dataInterface.equals(other.dataInterface) && 
				this.ontologyInterface.equals(other.ontologyInterface) && 
				this.domain == other.domain);
	}
	
	public void build() throws Exception {
		// needs to be called after mucking with members
		// PEC seems like a bad idea.  Historical.
		this.dataInterface = this.createDataInterface();
		this.ontologyInterface = this.createOntologyInterface();
	}
	
	public SparqlEndpointInterface createDataInterface () throws Exception {
	
		if (serverType.equals(FUSEKI_SERVER)) {
			return new FusekiSparqlEndpointInterface(dataServerUrl, dataSourceDataset);
		} else if (this.serverType.equals(VIRTUOSO_SERVER)) {
			return new VirtuosoSparqlEndpointInterface(dataServerUrl, dataSourceDataset);
		} else if (this.serverType == SparqlConnection.QUERY_SERVER) {
			throw new Exception("Attempt to createDataInferface with unsupported serverType: " + serverType);
		}
		return null;
	}
	
	public SparqlEndpointInterface createOntologyInterface () throws Exception {
		
		if (serverType.equals(FUSEKI_SERVER)) {
			return new FusekiSparqlEndpointInterface(ontologyServerUrl, ontologySourceDataset);
		} else if (this.serverType.equals(VIRTUOSO_SERVER)) {
			return new VirtuosoSparqlEndpointInterface(ontologyServerUrl, ontologySourceDataset);
		} else if (this.serverType == SparqlConnection.QUERY_SERVER) {
			throw new Exception("Attempt to createDataInferface with unsupported serverType: " + serverType);
		}
		return null;
	}
	
	public SparqlEndpointInterface getDataInterface() {
		return this.dataInterface;
	}
	
	public SparqlEndpointInterface getOntologyInterface() {
		return this.ontologyInterface;
	}
	
	public String getDomain () {
		return this.domain;
	}
	
	// added to support more descriptive information retrieval about the connection:
	// "connectionAlias", "domain", "dsDataset", "dsKsURL", "dsURL", "originalServerType"
	
	public String getConnectionName(){
		return this.name;
	}

	public String getDataSourceDataset(){
		return this.dataSourceDataset;
	}
	
	public String getDataSourceKnowledgeServiceURL(){
		return this.dataKsServerURL;
	}
	
	public String getDataSourceURL(){
		return this.dataServerUrl;
	}
	
	public String getServerType(){
		return this.serverType;
	}
}
