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
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import org.json.simple.JSONObject;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.services.nodegroupStore.NgStore.StoredItemTypes;
import com.ge.research.semtk.services.nodegroupStore.StoreDataCsvReader;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.opencsv.CSVReader;


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
		return this.executeStoreNodeGroup(proposedId, comments, creator, nodeGroupJSON, null);
	}
	
	@SuppressWarnings({ "unchecked" })
	public SimpleResultSet executeStoreNodeGroup(String proposedId, String comments, String creator, JSONObject nodeGroupJSON, NgStore.StoredItemTypes itemType) throws Exception {
		SimpleResultSet retval = null;
		
		if(nodeGroupJSON == null){
			throw new Exception("Cannot store null nodegroup");
		}
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupById");
		this.parametersJSON.put("id", proposedId);
		this.parametersJSON.put("name", proposedId);
		this.parametersJSON.put("comments", comments);
		this.parametersJSON.put("creator", creator); 
		this.parametersJSON.put("jsonRenderedNodeGroup", nodeGroupJSON.toJSONString() );
		if (itemType != null) {
			this.parametersJSON.put("itemType", itemType.toString() );
		}
		
		try{
		
			TableResultSet ret = new TableResultSet((JSONObject) this.execute());
			ret.throwExceptionIfUnsuccessful();
			if(ret.getTable().getNumRows() >= 1){
				// this is a problem as this already exists. 
				throw new Exception ("executeStoreNodeGroup :: nodegroup with ID (" + proposedId + ") already exists. Exiting without adding.");
			}		
			else{
				this.parametersJSON.remove("id");
				LocalLogger.logToStdErr("existence check succeeded. proceeding to insert node group: " + proposedId);
				
				// perform actual insertion.
				conf.setServiceEndpoint("nodeGroupStore/storeNodeGroup");
				JSONObject interim = (JSONObject) this.execute();
				retval = SimpleResultSet.fromJson( interim );
			}
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
			this.parametersJSON.remove("name");
			this.parametersJSON.remove("jsonRenderedNodeGroup");
			this.parametersJSON.remove("comments");
			this.parametersJSON.remove("creator");
			this.parametersJSON.remove("itemType");
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
	 * @param statusWriter - get status messages
	 * @throws Exception
	 */
	public void loadStoreDataCsv(String csvFileName, String sparqlConnOverrideFile, PrintWriter statusWriter) throws Exception {

		StoreDataCsvReader br = new StoreDataCsvReader(csvFileName, statusWriter);
		

		// build "exists" as hashset of nodegroup ids
		HashSet<String> exists = new HashSet<String>();
		
		TableResultSet res = this.executeGetStoredItemsMetadata(NgStore.StoredItemTypes.StoredItem);
		res.throwExceptionIfUnsuccessful("Error while checking of nodegroup Id already exists");
		for (int i=0; i < res.getTable().getNumRows(); i++) {
			String key = res.getTable().getCell(i, "itemType").split("#")[1] + ":" + res.getTable().getCell(i, "ID");
			exists.add(key);
		}
		
		
		String errorMsg = "";
		int lineNumber=1; // header is line #1
		while ((errorMsg = br.readNext()) != null) {
			
			if (errorMsg.length() > 0 && statusWriter != null)
				statusWriter.println(errorMsg);
			else {
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
	
				// delete if exists
				if (exists.contains(itemType.toString() + ":" + ngId)) {
					SimpleResultSet r = this.deleteStoredItem(ngId, itemType);
					r.throwExceptionIfUnsuccessful("Error while removing preview version of nodegroup");
				}
				
				// add
				this.storeNodeGroup(ngId, ngComments, ngOwner, ngFilePath, itemType, sparqlConnOverrideFile);
				exists.add(ngId);
				if (statusWriter != null) statusWriter.println("Stored: " + ngId);
			}	
			
		}
		if (statusWriter != null) statusWriter.println("Finished processing file: " + csvFileName);
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
	public void storeNodeGroup(String ngId, String ngComments, String ngOwner, String ngFilePath, NgStore.StoredItemTypes itemType, String sparqlConnOverrideFile) throws Exception {

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
		SimpleResultSet r = this.executeStoreNodeGroup(ngId, ngComments, ngOwner, ngJson, itemType);
		r.throwExceptionIfUnsuccessful("Error while storing nodegroup");

	}
}
