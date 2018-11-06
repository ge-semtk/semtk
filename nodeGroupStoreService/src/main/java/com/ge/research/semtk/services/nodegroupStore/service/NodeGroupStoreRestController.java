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

package com.ge.research.semtk.services.nodegroupStore.service;

import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.springutilib.requests.IdRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.nodegroupStore.NgStoreSparqlGenerator;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

@RestController
@RequestMapping("/nodeGroupStore")
@CrossOrigin
public class NodeGroupStoreRestController {

	
	@CrossOrigin
	@RequestMapping(value= "/**", method=RequestMethod.OPTIONS)
	public void corsHeaders(HttpServletResponse response) {
	    response.addHeader("Access-Control-Allow-Origin", "*");
	    response.addHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
	    response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, x-requested-with");
	    response.addHeader("Access-Control-Max-Age", "3600");
	}
	
	@Autowired
	StoreProperties prop;
	
	private static final String SERVICE_NAME="nodeGroupStore";
	
	/**
	 * Store a new nodegroup to the nodegroup store.
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/storeNodeGroup", method=RequestMethod.POST)
	public JSONObject storeNodeGroup(@RequestBody StoreNodeGroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/storeNodeGroup";
			SimpleResultSet retval = null;
	
			try{
	
				// throw a meaningful exception if needed info is not present in the request
				requestBody.validate();	
	
				// check that the ID does not already exist. if it does, fail.
				ArrayList<String> queries = NgStoreSparqlGenerator.getNodeGroupByID(requestBody.getName());
				Table instanceTable = this.createSuperuserSei().executeQueryToTable(queries.get(0));
				if(instanceTable.getNumRows() > 0){
					throw new Exception("Unable to store node group:  ID (" + requestBody.getName() + ") already exists");
				}
	
				// get the nodeGroup and the connection info:
				JSONObject sgJsonJson = requestBody.getJsonNodeGroup();			// changed to allow for more dynamic nodegroup actions. 
				SparqlGraphJson sgJson = new SparqlGraphJson(sgJsonJson);
				JSONObject connJson = sgJson.getSparqlConnJson();
				if(connJson == null){
					throw new Exception("storeNodeGroup :: sparqlgraph json serialization passed to store node group did not contain a valid connection block. it is possible that only the node group itself was passed. please check that complete input is sent.");
				}
	
				// insert
				ArrayList<String> insertQueries = NgStoreSparqlGenerator.insertNodeGroup(sgJsonJson, connJson, requestBody.getName(), requestBody.getComments(), requestBody.getCreator());
				
				for (String insertQuery : insertQueries) {
					this.createSuperuserSei().executeQueryAndConfirm(insertQuery);
				}
				
				retval = new SimpleResultSet(true);
	
			}
			catch(Exception e){
				retval = new SimpleResultSet(false);
				retval.addRationaleMessage(SVC_ENDPOINT_NAME, e);
				LocalLogger.printStackTrace(e);
			} 	
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	@CrossOrigin
	@RequestMapping(value="/getNodeGroupById", method=RequestMethod.POST)
	public JSONObject getNodeGroupById(@RequestBody @Valid IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getNodeGroupById";
			TableResultSet retval = new TableResultSet();	
			try{
				Table ngTab = this.getNodegroupTable(requestBody.getId());
				retval.setSuccess(true);
				retval.addResults(ngTab);
			}
			catch(Exception e){
				// something went wrong. report and exit. 
				retval.setSuccess(false);
				retval.addRationaleMessage(SVC_ENDPOINT_NAME, e);
			} 
	
			return retval.toJson();  // whatever we have... send it out. 
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	@CrossOrigin
	@RequestMapping(value="/getNodeGroupList", method=RequestMethod.POST)
	public JSONObject getNodeGroupList(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getNodeGroupList";
			TableResultSet retval = new TableResultSet(true);		
	
			try{
				String query = NgStoreSparqlGenerator.getFullNodeGroupList();
				retval.addResults(this.createSuperuserSei().executeQueryToTable(query));
				retval.setSuccess(true);
			}
			catch(Exception e){
				// something went wrong. report and exit. 
	
				retval.setSuccess(false);
				retval.addRationaleMessage(SVC_ENDPOINT_NAME, e);
			}  
	
			return retval.toJson();  // whatever we have... send it out. 
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	@CrossOrigin
	@RequestMapping(value="/getNodeGroupMetadata", method=RequestMethod.POST)
	public JSONObject getNodeGroupMetadata(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getNodeGroupMetadata";
			TableResultSet retval = new TableResultSet();		
			try{
				String query = NgStoreSparqlGenerator.getNodeGroupMetadata();
				retval.addResults(this.createSuperuserSei().executeQueryToTable(query));
				retval.setSuccess(true);
			}
			catch(Exception e){
				// something went wrong. report and exit. 
				retval.setSuccess(false);
				retval.addRationaleMessage(SVC_ENDPOINT_NAME, e);
			} 
			return retval.toJson();   
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	@CrossOrigin
	@RequestMapping(value="/getNodeGroupRuntimeConstraints", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraints(@RequestBody @Valid IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getNodeGroupRuntimeConstraints";
			TableResultSet retval = null;
	
			try{
				// get the nodegroup
				
				Table tbl = this.getNodegroupTable(requestBody.getId());
				if(tbl.getNumRows() > 0){
					// we have a result. for now, let's assume that only the first result is valid.
					ArrayList<String> tmpRow = tbl.getRows().get(0);
					int targetCol = tbl.getColumnIndex("NodeGroup");
	
					String ngJSONstr = tmpRow.get(targetCol);
					JSONParser jParse = new JSONParser();
					JSONObject json = (JSONObject) jParse.parse(ngJSONstr); 
	
					// check if this is a wrapped or unwrapped 
					// check that sNodeGroup is a key in the json. if so, this has a connection and the rest.
					if (SparqlGraphJson.isSparqlGraphJson(json)) {
						SparqlGraphJson sgJson = new SparqlGraphJson(json);
						LocalLogger.logToStdErr("located key: sNodeGroup");
						json = sgJson.getSNodeGroupJson();
					}
	
					// otherwise, check for a truncated one that is only the nodegroup proper.
					else if(! NodeGroup.isNodeGroup(json)) {
	
						throw new Exception("Value given for encoded node group can't be parsed");
					}
	
	
					// get the runtime constraints. 
	
					retval = new TableResultSet(true); 
					
					// process encoded node group
					NodeGroup temp = new NodeGroup();
					temp.addJsonEncodedNodeGroup(json);
					
					// get the constrained values from the NodeGroup.
					RuntimeConstraintManager rtci = new RuntimeConstraintManager(temp);
					
					Table constrainedItemTab = rtci.getConstrainedItemsDescription();
					retval.addResults(constrainedItemTab);
				} else {
					retval = new TableResultSet(false);
					retval.addRationaleMessage(SVC_ENDPOINT_NAME, "Nodegroup was not found: " + requestBody.getId());
				}
	
			}
			catch(Exception e){
				// something went wrong. report and exit. 
	
				LocalLogger.logToStdErr("a failure was encountered during the retrieval of runtime constraints: " + 
						e.getMessage());
	
				retval = new TableResultSet(false);
				retval.addRationaleMessage(SVC_ENDPOINT_NAME, "Nodegroup was not found: " + requestBody.getId());
			}
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * This method uses a static delete query. it would be better to use a local nodegroup and have belmont 
	 * generate a deletion query itself. 
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/deleteStoredNodeGroup", method=RequestMethod.POST)
	public JSONObject deleteStoredNodeGroup(@RequestBody @Valid IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			SimpleResultSet retval = null;
	
			// NOTE:
			// this static delete works but will need to be updated should the metadata surrounding node groups be updated. 
			// really, if the insertion nodegroup itself is edited, this should be looked at.
			// ideally, the node groups would be able to write deletion queries, using filters and runtime constraints to
			// determine what to remove. if we moved to that point, we could probably use the same NG for insertions and deletions.
	
			
			String query = NgStoreSparqlGenerator.deleteNodeGroup(requestBody.getId());
	
			try{
				// attempt to delete the nodegroup, name and comments where there is a give ID.
				this.createSuperuserSei().executeQueryAndConfirm(query);
				retval = new SimpleResultSet(true);
			}
			catch(Exception e){

				LocalLogger.logToStdErr("a failure was encountered during the deletion of " +  requestBody.getId() + ": " + 
						e.getMessage());
	
				retval = new SimpleResultSet(false);
				retval.addRationaleMessage(e.getMessage());
			} 	
	
			return retval.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	// static method to avoid repeating the client generation code...


	private SparqlEndpointInterface createSuperuserSei() throws Exception{

		AuthorizationManager.nextQuerySemtkSuper();
		SparqlEndpointInterface ret = SparqlEndpointInterface.getInstance((SparqlQueryClientConfig)(new SparqlQueryAuthClientConfig(	
				prop.getSparqlServiceProtocol(),
				prop.getSparqlServiceServer(), 
				prop.getSparqlServicePort(), 
				prop.getSparqlServiceEndpoint(),
				prop.getSparqlConnServerAndPort(), 
				prop.getSparqlConnType(), 
				prop.getSparqlConnDataDataset(),
				prop.getSparqlServiceUser(),
				prop.getSparqlServicePass())
				));

		return ret;
	}
	
	/**
	 * Return nodegroup table with zero rows, or one row containing full nodegroup json string
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private Table getNodegroupTable(String id)  throws Exception {
		StringBuilder ngStr = new StringBuilder();
		ArrayList<String> queries = NgStoreSparqlGenerator.getNodeGroupByID(id);
		
		// run the base query
		Table retTable = this.createSuperuserSei().executeQueryToTable(queries.get(0));
		
		if (retTable.getNumRows() > 0) {
			ngStr = new StringBuilder(retTable.getCellAsString(0,  "NodeGroup"));

			// look for additional text, using second query
			Table catTable =  this.createSuperuserSei().executeQueryToTable(queries.get(1));
		
			for (int i=0; i < catTable.getNumRows(); i++) {
				ngStr.append(catTable.getCellAsString(i, "NodeGroup"));
			}
			
			int col = retTable.getColumnIndex("NodeGroup");
			retTable.setCell(0, col, ngStr.toString());
		}

		
		return retTable;
	}
}
