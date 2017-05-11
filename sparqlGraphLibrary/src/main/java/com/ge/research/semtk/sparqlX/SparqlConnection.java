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

import java.util.ArrayList;

import org.json.simple.JSONArray;
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
	
	private final static String QUERY_SERVER = "kdl";
	private final static String FUSEKI_SERVER = "fuseki";
	private final static String VIRTUOSO_SERVER = "virtuoso";
	
	private String name = null;
	private ArrayList<SparqlEndpointInterface> modelInterfaces = null;
	private ArrayList<String> modelDomains = null;
	private ArrayList<String> modelNames = null;
	private ArrayList<SparqlEndpointInterface> dataInterfaces = null;
	private ArrayList<String> dataNames = null;
	
	public SparqlConnection () {
		this.name = "";
		this.modelInterfaces = new ArrayList<SparqlEndpointInterface>();
		this.modelDomains = new ArrayList<String>();
		this.modelNames = new ArrayList<String>();
		this.dataInterfaces = new ArrayList<SparqlEndpointInterface>();
		this.dataNames = new ArrayList<String>();
	}
	
	public SparqlConnection(String jsonText) throws Exception {
		this();
	    this.fromString(jsonText);
	}
	
	public SparqlConnection(String name, String serverType, String dataServicetURL, String knowledgeServiceURL, String dataset, String domain) throws Exception{
		this();
		this.name = name;
		this.addDataInterface(serverType, 
				dataServicetURL,
				dataset,
				"");
		this.addModelInterface(serverType, 
				dataServicetURL,
				dataset,
				domain,
				"");
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject jObj = new JSONObject();
		jObj.put("name", name);
		jObj.put("model", new JSONArray());
		jObj.put("data", new JSONArray());

		for (int i=0; i < this.modelInterfaces.size(); i++) {
			SparqlEndpointInterface mi = this.modelInterfaces.get(i);
			JSONObject outer = new JSONObject();
			JSONObject inner = new JSONObject();
			inner.put("type", mi.getServerType());
			inner.put("url", mi.getGetURL());
			inner.put("dataset", mi.getDataset());
			outer.put("endpoint", inner);
			outer.put("domain", this.modelDomains.get(i));
			outer.put("name", this.modelNames.get(i));
			jObj.put("model", outer);
		}
		
		for (int i=0; i < this.modelInterfaces.size(); i++) {
			SparqlEndpointInterface di = this.dataInterfaces.get(i);
			JSONObject outer = new JSONObject();
			JSONObject inner = new JSONObject();
			inner.put("type", di.getServerType());
			inner.put("url", di.getGetURL());
			inner.put("dataset", di.getDataset());
			outer.put("endpoint", inner);
			outer.put("name", this.modelNames.get(i));
			jObj.put("data", outer);
		}
		return jObj;
	}
	
	public void fromJson(JSONObject jObj) throws Exception {
		
		if(jObj.entrySet().size() == 1 && jObj.containsKey("sparqlConn")){
			throw new Exception("Cannot create SparqlConnection object because the JSON is wrapped in \"sparqlConn\"");
		}
		
		this.name = (String) jObj.get("name");
		this.modelInterfaces = new ArrayList<SparqlEndpointInterface>();
		this.modelDomains = new ArrayList<String>();
		this.modelNames = new ArrayList<String>();
		
		this.dataInterfaces = new ArrayList<SparqlEndpointInterface>();
		this.dataNames = new ArrayList<String>();
		
		if (jObj.containsKey("dsURL")) {
			
			// backwards compatible read
		
			// If any field doesn't exist, presume it exists in the other connection
			this.addModelInterface(	(String)(jObj.get("type")), 
								    (String)(jObj.containsKey("onURL") ? jObj.get("onURL") : jObj.get("dsURL")),
								    (String)(jObj.containsKey("onDataset") ? jObj.get("onDataset") : jObj.get("dsDataset")),
								    (String)(jObj.get("domain")),
							        "");
			this.addDataInterface(	(String)(jObj.get("type")), 
									(String)(jObj.containsKey("dsURL") ? jObj.get("dsURL") : jObj.get("onURL")),
									(String)(jObj.containsKey("dsDataset") ? jObj.get("dsDataset")  : jObj.get("onDataset")),
									"");
		} else {
			// normal read
			
			// read model interfaces
	    	for (int i=0; i < ((JSONArray)(jObj.get("model"))).size(); i++) {
	    		JSONObject m = (JSONObject)((JSONArray)jObj.get("model")).get(i);
	    		JSONObject endpoint = (JSONObject) m.get("endpoint");
	    		this.addModelInterface((String)(endpoint.get("type")), (String)(endpoint.get("url")), (String)(endpoint.get("dataset")), (String)(m.get("domain")), (String)(m.get("name")));
	    	}
	    	// read data interfaces
	    	for (int i=0; i < ((JSONArray)(jObj.get("data"))).size(); i++) {
	    		JSONObject d = (JSONObject)((JSONArray)jObj.get("data")).get(i);
	    		JSONObject endpoint = (JSONObject) d.get("endpoint");
	    		this.addDataInterface((String)(endpoint.get("type")), (String)(endpoint.get("url")), (String)(endpoint.get("dataset")), (String)(d.get("name")));
	    	}
		}
		
		// no deprecated field-handling
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
	
	public void addModelInterface(String sType, String url, String dataset, String domain, String name) throws Exception {
		this.modelInterfaces.add(this.createInterface(sType, url, dataset));
		this.modelDomains.add(domain);
		this.modelNames.add(name);
	}
	
	public void addDataInterface(String sType, String url, String dataset, String name) throws Exception {
		this.dataInterfaces.add(this.createInterface(sType, url, dataset));
		this.dataNames.add(name);
	}
	
	public int getModelInterfaceCount() {
		return this.modelInterfaces.size();
	}
	public SparqlEndpointInterface getModelInterface(int i) {
		return this.modelInterfaces.get(i);
	}
	public String getModelDomain(int i) {
		return this.modelDomains.get(i);
	}
	public String getModelName(int i) {
		return this.modelNames.get(i);
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
	public String getDataName(int i) {
		return this.dataNames.get(i);
	}

	//---------- private function
	private SparqlEndpointInterface createInterface(String stype, String url, String dataset) throws Exception{
		if (stype.equals(SparqlConnection.FUSEKI_SERVER)) {
			return new FusekiSparqlEndpointInterface(url, dataset);
		} else if (stype.equals(SparqlConnection.VIRTUOSO_SERVER)) {
			return new VirtuosoSparqlEndpointInterface(url, dataset);
		} else {
			throw new Error("Unsupported SparqlConnection server type: " + stype);
		}
	}
}
