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


package com.ge.research.semtk.sparqlX.client;

import java.io.File;

import org.json.simple.JSONObject;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.NodeGroupResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

/**
 * Client to call the SparqlQueryService
 */
public class SparqlQueryClient extends RestClient {
	
	private String savedEndpoint = "";
	
	/**
	 * Constructor
	 */
	public SparqlQueryClient(SparqlQueryClientConfig conf){
		this.conf = conf;
	}

	@Override
	public SparqlQueryClientConfig getConfig() {
		return (SparqlQueryClientConfig) this.conf;
	}
	
	/**
	 * Create a params JSON object
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void buildParametersJSON() throws Exception {
		
		if(! (this.conf instanceof SparqlQueryClientConfig)){
			throw new Exception("Unrecognized config for SparqlQueryClient");
		}
		
		// all queries will have these
		parametersJSON.put("serverAndPort", ((SparqlQueryClientConfig)this.conf).getSparqlServerAndPort());
		parametersJSON.put("serverType", ((SparqlQueryClientConfig)this.conf).getSparqlServerType());
		
		// everyone except syncOwl uses this
		if (! this.conf.getServiceEndpoint().endsWith("syncOwl")) {
			parametersJSON.put("dataset", ((SparqlQueryClientConfig)this.conf).getSparqlDataset());
		}
		
		// auth queries will have these as well
		if(this.conf instanceof SparqlQueryAuthClientConfig){		
			parametersJSON.put("user", ((SparqlQueryAuthClientConfig)this.conf).getSparqlServerUser());
			parametersJSON.put("password", ((SparqlQueryAuthClientConfig)this.conf).getSparqlServerPassword());			
		}
	}
	

	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response");
	}
	

	
	/**
	 * Drop graph
	 * TODO could have made this simply execute(). Chose dropGraph() to reduce accidental drops.
	 */
	public SimpleResultSet dropGraph() throws Exception{
		this.overrideConfEndpoint("dropGraph");
		
		try {
			JSONObject resultJSON = (JSONObject)super.execute();
			SimpleResultSet retval = new SimpleResultSet(true);
			retval.readJson(resultJSON);
			return retval;
		} finally {
			this.restoreConfEndpoint();
		}
	}
	
	/**
	 * Clear all
	 * TODO could have made this simply execute(). Chose dropGraph() to reduce accidental drops.
	 */
	public SimpleResultSet clearAll() throws Exception{
		this.overrideConfEndpoint("clearAll");
		
		try {
			JSONObject resultJSON = (JSONObject)super.execute();
			SimpleResultSet retval = new SimpleResultSet(true);
			retval.readJson(resultJSON);
			return retval;
		} finally {
			this.restoreConfEndpoint();
		}
	}
	
	public SimpleResultSet uploadOwl(File owlFile) throws Exception{
		this.overrideConfEndpoint("uploadOwl");
		
		try {
			this.fileParameter = owlFile;
			this.fileParameterName = "owlFile";

			JSONObject resultJSON = (JSONObject)super.execute();
			SimpleResultSet retval = new SimpleResultSet(true);
			retval.readJson(resultJSON);
			return retval;
		} finally {
			this.restoreConfEndpoint();
		}
	}
	
	public SimpleResultSet syncOwl(File owlFile) throws Exception{
		this.overrideConfEndpoint("syncOwl");
		
		try {
			this.fileParameter = owlFile;
			this.fileParameterName = "owlFile";
			
			JSONObject resultJSON = (JSONObject)super.execute();
			SimpleResultSet retval = new SimpleResultSet(true);
			retval.readJson(resultJSON);
			return retval;
		} finally {
			this.restoreConfEndpoint();
		}
	}

	/**
	 * Execute a query
	 */
	public GeneralResultSet execute(String query, SparqlResultTypes resultType) throws Exception{
		
		parametersJSON.put("query", query);
		parametersJSON.put("resultType", resultType.toString());  // the resultType string here is "TABLE", "CONFIRM", etc
		GeneralResultSet retval = null;
		
		JSONObject resultJSON = (JSONObject)super.execute();	
				
		// TODO parse these based on results block name, instead of query type?
		
		if(resultType == SparqlResultTypes.GRAPH_JSONLD){
			retval = new NodeGroupResultSet(true);
			retval.readJson(resultJSON);
		}else if(resultType == SparqlResultTypes.CONFIRM){
			retval = new SimpleResultSet(true);
			retval.readJson(resultJSON);
		}else{
			retval = new TableResultSet();
			retval.readJson(resultJSON);
		}
		return retval;
	}
	
	
	public static SparqlQueryClient getInstance(JSONObject encodedNodeGroupWithConnection, int servicePort, String serviceProtocol, String serviceServer, String serviceEndpoint) throws Exception{
		SparqlQueryClient retval = null;
		
		SparqlConnection conn = new SparqlGraphJson(encodedNodeGroupWithConnection).getSparqlConn();
		SparqlEndpointInterface sei = conn.getDefaultQueryInterface();
		String endpointType  = sei.getServerType();
		String serverAndPort = sei.getGetURL();
		String dataSet       = sei.getGraph();
		
		retval = new SparqlQueryClient(new SparqlQueryClientConfig(serviceProtocol, serviceServer, servicePort, serviceEndpoint, serverAndPort, endpointType, dataSet)); 		
		
		return retval;
	}
	
	private void overrideConfEndpoint(String endpoint) {
		this.savedEndpoint = this.conf.getServiceEndpoint();
		this.conf.setServiceEndpoint("/sparqlQueryService/" + endpoint);
	}
	
	private void restoreConfEndpoint() {
		this.conf.setServiceEndpoint(this.savedEndpoint);
	}

}

