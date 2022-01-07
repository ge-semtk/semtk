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

import org.json.simple.JSONObject;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.utility.LocalLogger;

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
	
	public TableResultSet executeGetNodeGroupMetadata() throws Exception {
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupMetadata");
		
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
	
	@SuppressWarnings({ "unchecked" })
	public SimpleResultSet executeStoreNodeGroup(String proposedId, String comments, String creator, JSONObject nodeGroupJSON) throws Exception {
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
	
	/**
	 * 
	 * @param nodeGroupID
	 * @return
	 * @throws DoesNotExistException - nodegroup doesn't exist
	 * @throws Exception - other error in the REST call
	 */
	public SimpleResultSet deleteStoredNodeGroup(String nodeGroupID) throws DoesNotExistException, Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroupStore/deleteStoredNodeGroup");
		this.parametersJSON.put("id", nodeGroupID);
		
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
		}

		return retval;
	}
}
