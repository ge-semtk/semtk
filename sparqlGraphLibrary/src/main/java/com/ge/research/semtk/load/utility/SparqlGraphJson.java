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


package com.ge.research.semtk.load.utility;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/*
 * JSON handler for sparqlGraph.json files
 * 
 */
public class SparqlGraphJson {
	static final String JKEY_NODEGROUP = "sNodeGroup";
	
	static final String JKEY_SPARQLCONN = "sparqlConn";
	
	static final String JKEY_IMPORTSPEC = "importSpec";
	static final String JKEY_IS_VERSION = "version";
	static final String JKEY_IS_BASE_URI = "baseURI";
	static final String JKEY_IS_COLUMNS = "columns";
	static final String JKEY_IS_COL_COL_ID = "colId";
	static final String JKEY_IS_COL_COL_NAME = "colName";
	static final String JKEY_IS_TEXTS = "texts";
	static final String JKEY_IS_TEXT_ID = "textId";
	static final String JKEY_IS_TEXT_TEXT = "text";
	static final String JKEY_IS_TRANSFORMS = "transforms";
	static final String JKEY_IS_TRANS_ID = "transId";
	static final String JKEY_IS_TRANS_NAME = "name";
	static final String JKEY_IS_TRANS_TYPE = "transType";
	static final String JKEY_IS_TRANS_ARG1 = "arg1";
	static final String JKEY_IS_TRANS_ARG2 = "arg2";
	static final String JKEY_IS_NODES = "nodes";
	static final String JKEY_IS_NODE_SPARQL_ID = "sparqlID";
	static final String JKEY_IS_NODE_TYPE = "type";
	static final String JKEY_IS_NODE_LOOKUP_MODE = "URILookupMode";
	static final String JKEY_IS_URI_LOOKUP = "URILookup";
	static final String JKEY_IS_MAPPING = "mapping";
	static final String JKEY_IS_MAPPING_TEXT_ID = "textId";
	static final String JKEY_IS_MAPPING_TEXT = "text";
	static final String JKEY_IS_MAPPING_COL_ID = "colId";
	static final String JKEY_IS_MAPPING_COL_NAME = "colName";
	static final String JKEY_IS_MAPPING_TRANSFORM_LIST = "transformList";
	static final String JKEY_IS_MAPPING_PROPS = "props";
	static final String JKEY_IS_MAPPING_PROPS_URI_REL = "URIRelation";

	
	static final String JKEY_RUNTIMECONST = "RuntimeConstraints";
	
	private JSONObject jObj = null;
	
	private SparqlConnection conn = null;
	private OntologyInfo oInfo = null;
	private ImportSpecHandler importSpec = null;
	
	public SparqlGraphJson() {
		// nothing
	}
	
	public SparqlGraphJson(JSONObject jsonObj) {
		this.jObj = jsonObj;
	}
	
	public SparqlGraphJson(String s) throws ParseException {
		this((JSONObject) (new JSONParser()).parse(s));		
	}
	
	/*
	 * Basic check that jsonObj looks like a sparqlGraphJson
	 */
	public static boolean isSparqlGraphJson(JSONObject jsonObj) {
		return 	(	jsonObj.containsKey(JKEY_NODEGROUP) );
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception - if no Conn
	 */
	public SparqlConnection getSparqlConn() throws Exception {
		if (this.conn == null) {
			JSONObject connJson = (JSONObject) (jObj.get(JKEY_SPARQLCONN));
			if (connJson == null) {
				throw new Exception("JSON does not contain a sparqlConn");
			}
			this.conn = new SparqlConnection();
			this.conn.fromJson(connJson);
		}
		return this.conn;
	}
	
	public JSONObject getJson(){
		return jObj;
	}
	
	public JSONObject getSparqlConnJson() throws Exception {
		if (jObj.containsKey(JKEY_SPARQLCONN)) {
			return (JSONObject)jObj.get(JKEY_SPARQLCONN);
		}
		else{
			return null;
		}
	}
	
	public JSONObject getSNodeGroupJson() {
		if (jObj.containsKey(JKEY_NODEGROUP)) {
			return (JSONObject) jObj.get(JKEY_NODEGROUP);
		} else {
			return null;
		}
	}
	
	public JSONObject getImportSpecJson() {
		if (jObj.containsKey(JKEY_IMPORTSPEC)) {
			return (JSONObject) jObj.get(JKEY_IMPORTSPEC);
		} else {
			return null;
		}
	}
	
	public JSONArray getRuntimeConstraintsJson(){
		if (jObj.containsKey(JKEY_RUNTIMECONST)) {
			return (JSONArray) jObj.get(JKEY_RUNTIMECONST);
		} else {
			return null;
		}
	}
	
	// new convenience functions not in javascript
	
	/**
	 * 
	 * @return NodeGroup - could be null
	 * @throws Exception
	 */
	public NodeGroup getNodeGroup() throws Exception {
		return this.getNodeGroup(null);
	}
	
	public NodeGroup getNodeGroup(OntologyInfo uncompressOInfo) throws Exception {
		JSONObject json = getSNodeGroupJson();
		if (json == null) {
			//throw new Exception("SparqlGraphJson getNodeGroup:: nodegroup json was null.");
			// return null;   // used to return null here. turned into Exception.
			return (new NodeGroup() );
		} else {
			NodeGroup ng = NodeGroup.getInstanceFromJson(json, uncompressOInfo);
			ng.setSparqlConnection(this.getSparqlConn());
			return ng;
		}
	}
	
	public String getDomain() throws Exception {
		return getSparqlConn().getDomain();
	}
	
	public OntologyInfo getOntologyInfo() throws Exception {
		if (oInfo == null) {
			oInfo = new OntologyInfo(this.getSparqlConn());
		}
		return oInfo;
	}
	
	/**
	 * 
	 * @return importSpecHandler, might be NULL
	 * @throws Exception
	 */
	public ImportSpecHandler getImportSpec() throws Exception {
		JSONObject json = this.getImportSpecJson();
		if (importSpec == null && json != null) {
			importSpec =  new ImportSpecHandler(json, this.getSNodeGroupJson(), this.getOntologyInfo());
		}
		return importSpec;
	}
	
	// ------ end new convenience functions ----------------
	
	
	// matches javascript
	public JSONObject getMappingTabJson() {
		return getImportSpecJson();
	}
	
	public void setSparqlConn(SparqlConnection conn) {
		if(jObj != null){
			jObj.remove(JKEY_SPARQLCONN);					// remove the older one
			jObj.put(JKEY_SPARQLCONN, conn.toJson());		// add the new one.
			this.conn = conn;							// insert the new one.
			this.oInfo = null;
		}
		else{
			this.jObj = new JSONObject();
			this.jObj.put(JKEY_SPARQLCONN, conn.toJson());		// add the new one.
			this.conn = conn;							// insert the new one.
			this.oInfo = null;
		}
	}
	
	public void parse(String jsonString) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject jObj = (JSONObject) parser.parse(jsonString);
	}
	
	// override the connection info in the SparqlGraphJson
	
}
