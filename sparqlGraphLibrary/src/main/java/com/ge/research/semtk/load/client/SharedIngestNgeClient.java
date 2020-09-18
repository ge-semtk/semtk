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

package com.ge.research.semtk.load.client;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * client of all exact shared calls between ingestion and node group execution
 * (may not be complete, just starting to avoid duplicate code)
 * @author 200001934
 *
 */
public abstract class SharedIngestNgeClient  extends RestClient {

	String mappingPrefix = null;
	
	/**
	 * Constructor
	 */
	public SharedIngestNgeClient(String mappingPrefix) {
		super();
		this.mappingPrefix = mappingPrefix;
	}
	
	public SharedIngestNgeClient(RestClientConfig conf, String mappingPrefix){
		super(conf);
		this.mappingPrefix = mappingPrefix;
	}
	
	
	public void setServicePrefix(String servicePrefix) {
		this.mappingPrefix = servicePrefix;
	}

	public SimpleResultSet execClearGraph(SparqlEndpointInterface sei, boolean trackFlag) throws Exception {
		conf.setServiceEndpoint(this.mappingPrefix + "clearGraph");
		this.parametersJSON.put("serverAndPort", sei.getServerAndPort());
		this.parametersJSON.put("serverType", sei.getServerType());
		this.parametersJSON.put("graph", sei.getGraph());
		if (sei.getUserName() != null) {
			this.parametersJSON.put("user", sei.getUserName());
			this.parametersJSON.put("password", sei.getPassword());
		}
		this.parametersJSON.put("trackFlag", trackFlag);

				
		try{
			JSONObject resultJSON = (JSONObject)super.execute();
			return new SimpleResultSet(resultJSON);
		} 
		finally {
			this.reset();
		}
	}
	
	public void clearGraph(SparqlEndpointInterface sei, boolean trackFlag) throws Exception {
		SimpleResultSet res = this.execClearGraph(sei, trackFlag);
		res.throwExceptionIfUnsuccessful();
	}
	
	/**
	 * Run a query of tracked uploads
	 * @param sei - filter or null
	 * @param key- filter or null
	 * @param user- filter or null
	 * @param startEpoch - filter or null
	 * @param endEpoch - filter or null
	 * @return
	 * @throws Exception
	 */
	public TableResultSet execRunTrackingQuery(SparqlEndpointInterface sei, String key, String user, Long startEpoch, Long endEpoch) throws Exception {
		conf.setServiceEndpoint(this.mappingPrefix + "runTrackingQuery");
		if (sei != null) {
			this.parametersJSON.put("serverAndPort", sei.getServerAndPort());
			this.parametersJSON.put("serverType", sei.getServerType());
			this.parametersJSON.put("graph", sei.getGraph());
			if (sei.getUserName() != null) {
				this.parametersJSON.put("user", sei.getUserName());
				this.parametersJSON.put("password", sei.getPassword());
			}
		}
		if (key != null) this.parametersJSON.put("key", key);
		if (user != null) this.parametersJSON.put("user", key);
		if (startEpoch != null) this.parametersJSON.put("startEpoch", key);
		if (endEpoch != null) this.parametersJSON.put("endEpoch", key);
				
		try{
			JSONObject resultJSON = (JSONObject)super.execute();
			return new TableResultSet(resultJSON);
		} 
		finally {
			this.reset();
		}
	}
	
	/**
	 * Run a query of tracked uploads
	 * @param sei - filter or null
	 * @param key- filter or null
	 * @param user- filter or null
	 * @param startEpoch - filter or null
	 * @param endEpoch - filter or null
	 * @return
	 * @throws Exception
	 */
	public Table runTrackingQuery(SparqlEndpointInterface sei, String key, String user, Long startEpoch, Long endEpoch) throws Exception {
		TableResultSet ret = this.execRunTrackingQuery(sei, key, user, startEpoch, endEpoch);
		ret.throwExceptionIfUnsuccessful();
		return ret.getTable();
	}
	
	/**
	 * Run a query of tracked uploads
	 * @param sei - filter or null
	 * @param key- filter or null
	 * @param user- filter or null
	 * @param startEpoch - filter or null
	 * @param endEpoch - filter or null
	 * @return
	 * @throws Exception
	 */
	public SimpleResultSet execDeleteTrackingEvents(SparqlEndpointInterface sei, String key, String user, Long startEpoch, Long endEpoch) throws Exception {
		conf.setServiceEndpoint(this.mappingPrefix + "deleteTrackingEvents");
		if (sei != null) {
			this.parametersJSON.put("serverAndPort", sei.getServerAndPort());
			this.parametersJSON.put("serverType", sei.getServerType());
			this.parametersJSON.put("graph", sei.getGraph());
			if (sei.getUserName() != null) {
				this.parametersJSON.put("user", sei.getUserName());
				this.parametersJSON.put("password", sei.getPassword());
			}
		}
		if (key != null) this.parametersJSON.put("key", key);
		if (user != null) this.parametersJSON.put("user", key);
		if (startEpoch != null) this.parametersJSON.put("startEpoch", key);
		if (endEpoch != null) this.parametersJSON.put("endEpoch", key);
				
		try{
			JSONObject resultJSON = (JSONObject)super.execute();
			return new SimpleResultSet(resultJSON);
		} 
		finally {
			this.reset();
		}
	}
	
	/**
	 * Deletes tracked uploads
	 * @param sei - filter or null
	 * @param key- filter or null
	 * @param user- filter or null
	 * @param startEpoch - filter or null
	 * @param endEpoch - filter or null
	 * @return
	 * @throws Exception
	 */
	public void deleteTrackingEvents(SparqlEndpointInterface sei, String key, String user, Long startEpoch, Long endEpoch) throws Exception {
		SimpleResultSet ret = this.execDeleteTrackingEvents(sei, key, user, startEpoch, endEpoch);
		ret.throwExceptionIfUnsuccessful();
	}
		
	/**
	 * Get contents of tracked ingest file
	 * @param id
	 * @return simpleResultSet with "contents"
	 * @throws Exception
	 */
	public SimpleResultSet execGetTrackedIngestFile(String id) throws Exception {
		conf.setServiceEndpoint(this.mappingPrefix + "getTrackedIngestFile");
		this.parametersJSON.put("id", id);
		
		try{
			JSONObject resultJSON = (JSONObject)super.execute();
			return new SimpleResultSet(resultJSON);
		} 
		finally {
			this.reset();
		}
	}
	
	/**
	 * Get contents of tracked ingest file
	 * @param id
	 * @return String
	 * @throws Exception
	 */
	public String getTrackedIngestFile(String id) throws Exception {
		SimpleResultSet res = this.execGetTrackedIngestFile(id);
		res.throwExceptionIfUnsuccessful();
		return res.getResult("contents");
	}
	
	public SimpleResultSet execUndoLoad(String id) throws Exception {
		conf.setServiceEndpoint(this.mappingPrefix + "undoLoad");
		this.parametersJSON.put("id", id);
		
		try{
			JSONObject resultJSON = (JSONObject)super.execute();
			return new SimpleResultSet(resultJSON);
		} 
		finally {
			this.reset();
		}
	}
	
	/**
	 * Get contents of tracked ingest file
	 * @param id
	 * @return String
	 * @throws Exception
	 */
	public void undoLoad(String id) throws Exception {
		SimpleResultSet res = this.execUndoLoad(id);
		res.throwExceptionIfUnsuccessful();
	}
	
	
	//=========== end shared with NGE =================//
}
