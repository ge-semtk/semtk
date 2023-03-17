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

package com.ge.research.semtk.nodeGroupStore.client;

import java.io.File;
import java.nio.file.Paths;

import org.json.simple.JSONObject;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.services.nodegroupStore.StoreDataCsvReader;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Logger;
import com.ge.research.semtk.utility.Utility;


public class NodeGroupStoreRestClient extends RestClient {
	
	@Override
	public void buildParametersJSON() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEmptyResponse() throws Exception {
		// TODO Auto-generated method stub
	}

	public NodeGroupStoreRestClient (NodeGroupStoreConfig config) {
		this.conf = config;
	}
	
	/**
	 * Get TableResultSet with status "success"
	 * @param id
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public TableResultSet executeGetNodeGroupById(String id) throws Exception{
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupById");
		this.parametersJSON.put("id", id);
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
		}
		
		return retval;
	}
	
	/**
	 * Get SparqlGraphJson or throw exception
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public SparqlGraphJson executeGetNodeGroupByIdToSGJson(String id) throws Exception {
		Table table = this.executeGetNodeGroupById(id).getTable();
		
		if (table.getNumRows() != 1) {
			throw new Exception(String.format("Retrieving '%s': expecting 1 nodegroup row, got %d", id, table.getNumRows()));
		}
		
		return new SparqlGraphJson(table.getCellAsString(0, 1));
	}

	
	public TableResultSet executeGetNodeGroupList() throws Exception {
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupList");
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
		
		return retval;
		
	}
	
	/**
	 * Get only nodegroup metadata
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public TableResultSet executeGetNodeGroupMetadata() throws Exception {
		return this.executeGetStoredItemsMetadata(NgStore.StoredItemTypes.PrefabNodeGroup);
	}

	/**
	 * Get metadata about one particular type.
	 * @param itemType
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public TableResultSet executeGetStoredItemsMetadata(NgStore.StoredItemTypes itemType) throws Exception {
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("nodeGroupStore/getStoredItemsMetadata");
		this.parametersJSON.put("itemType", itemType.toString() );
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("itemType");

		}
		return retval;
	}
	
	@SuppressWarnings("unchecked")
	public TableResultSet executeGetNodeGroupRuntimeConstraints(String nodeGroupId) throws Exception {
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupRuntimeConstraints");		
		this.parametersJSON.put("id", nodeGroupId);
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
		}
		
		return retval;		
	}
	public SimpleResultSet executeStoreNodeGroup(String proposedId, String comments, String creator, JSONObject nodeGroupJSON) throws Exception {
		return this.executeStoreItem(proposedId, comments, creator, nodeGroupJSON, null);
	}
	
	public SimpleResultSet executeStoreItem(String proposedId, String comments, String creator, JSONObject nodeGroupJSON, NgStore.StoredItemTypes itemType) throws Exception {
		return this.executeStoreItem(proposedId, comments, creator, nodeGroupJSON, itemType, false);
	}
	@SuppressWarnings({ "unchecked" })
	public SimpleResultSet executeStoreItem(String proposedId, String comments, String creator, JSONObject nodeGroupJSON, NgStore.StoredItemTypes itemType, boolean overwriteFlag) throws Exception {
		SimpleResultSet retval = null;
		
		if(nodeGroupJSON == null){
			throw new Exception("Cannot store null nodegroup");
		}
		
		conf.setServiceEndpoint("nodeGroupStore/storeItem");
		this.parametersJSON.put("id", proposedId);
		this.parametersJSON.put("name", proposedId);
		this.parametersJSON.put("comments", comments);
		this.parametersJSON.put("creator", creator); 
		this.parametersJSON.put("item", nodeGroupJSON.toJSONString() );
		this.parametersJSON.put("overwriteFlag", overwriteFlag);
		if (itemType != null) {
			this.parametersJSON.put("itemType", itemType.toString() );
		}
		
		try{
			JSONObject interim = (JSONObject) this.execute();
			retval = SimpleResultSet.fromJson( interim );
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
			this.parametersJSON.remove("name");
			this.parametersJSON.remove("item");
			this.parametersJSON.remove("comments");
			this.parametersJSON.remove("creator");
			this.parametersJSON.remove("itemType");
			this.parametersJSON.remove("overwriteFlag");
		}
		
		return retval;				
	}
	
	/**
	 * Delete nodegroup with no error if it doesn't exist, and no return value
	 * @param nodeGroupID
	 * @throws Exception
	 */
	public void deleteStoredNodeGroupIfExists(String nodeGroupID) throws Exception {
		try {
			deleteStoredNodeGroup(nodeGroupID);
		} catch (DoesNotExistException e) {
		}
	}
	
	public SimpleResultSet deleteStoredNodeGroup(String nodeGroupID) throws DoesNotExistException, Exception{
		return deleteStoredItem(nodeGroupID, NgStore.StoredItemTypes.PrefabNodeGroup);
	}
	/**
	 * 
	 * @param nodeGroupID
	 * @return
	 * @throws DoesNotExistException - nodegroup doesn't exist
	 * @throws Exception - other error in the REST call
	 */
	@SuppressWarnings("unchecked")
	public SimpleResultSet deleteStoredItem(String nodeGroupID, NgStore.StoredItemTypes itemType) throws DoesNotExistException, Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroupStore/deleteStoredNodeGroup");
		this.parametersJSON.put("id", nodeGroupID);
		this.parametersJSON.put("itemType", itemType.toString());
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		} catch (Exception e) {
			if (e.getMessage().contains("No stored item exists with id")) {
				throw new DoesNotExistException(e.getMessage());
			} else {
				throw e;
			}
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
			this.parametersJSON.remove("itemType");

		}

		return retval;
	}

	/**
	 * Process a store_data.csv file
	 * @param csvFileName
	 * @param sparqlConnOverrideFile - override all the nodegroup connections
	 * @param logger - write status messages
	 * @throws Exception
	 */
	public void loadStoreDataCsv(String csvFileName, String sparqlConnOverrideFile, Logger logger) throws Exception {

		StoreDataCsvReader br = new StoreDataCsvReader(csvFileName);
		
		while (br.readNext() != null) {
			
			String ngId = br.getId();           
			String ngComments = br.getComments();
			String ngOwner = br.getCreator();
			String ngFilePath = br.getJsonFile();
			NgStore.StoredItemTypes itemType = br.getItemType();

			// if nodegroup json path is bad, try same directory as csv file
			if (!(new File(ngFilePath).exists())) {
				String parent = (Paths.get(csvFileName)).getParent().toString();
				String fname =  (Paths.get(ngFilePath)).getFileName().toString();
				ngFilePath = (Paths.get(parent, fname)).toString();
			}

			// add
			this.storeItem(ngId, ngComments, ngOwner, ngFilePath, itemType, sparqlConnOverrideFile, true);
			if (logger != null) {
				logger.info("Stored: " + ngId);
			}
		}
	}


	/**
	 * Store nodegroup which as been confirmed not to exist
	 * @param ngId
	 * @param ngComments
	 * @param ngOwner
	 * @param ngFilePath
	 * @param sparqlConnOverrideFile
	 * @throws Exception - on failures (including already exists)
	 */
	public void storeItem(String ngId, String ngComments, String ngOwner, String ngFilePath, NgStore.StoredItemTypes itemType, String sparqlConnOverrideFile, boolean overwriteFlag) throws Exception {

		// validate nodegroup file
		if(!ngFilePath.endsWith(".json")){
			throw new Exception("Error: Nodegroup file " + ngFilePath + " is not a JSON file");
		}
		JSONObject ngJson = Utility.getJSONObjectFromFilePath(ngFilePath);
		
		// if a SPARQL connection override is provided, use it
		if(sparqlConnOverrideFile != null){
			if(!sparqlConnOverrideFile.endsWith(".json")){
				throw new Exception("Error: SPARQL connection override file " + sparqlConnOverrideFile + " is not a JSON file");
			}
			SparqlConnection sparqlConnOverride = new SparqlConnection(Utility.getJSONObjectFromFilePath(sparqlConnOverrideFile).toJSONString());
			LocalLogger.logToStdOut("Overriding SPARQL connection");
			SparqlGraphJson sgJson = new SparqlGraphJson(ngJson);
			sgJson.setSparqlConn(sparqlConnOverride);
			ngJson = sgJson.getJson();
		}

		// store it
		SimpleResultSet r = this.executeStoreItem(ngId, ngComments, ngOwner, ngJson, itemType, overwriteFlag);
		r.throwExceptionIfUnsuccessful("Error while storing nodegroup");

	}
}
