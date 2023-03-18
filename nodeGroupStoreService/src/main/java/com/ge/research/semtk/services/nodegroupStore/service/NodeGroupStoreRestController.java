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

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import com.ge.research.semtk.springutilib.requests.IdRequest;
import com.ge.research.semtk.springutilib.requests.StoredItemRenameRequest;
import com.ge.research.semtk.springutilib.requests.StoredItemRequest;
import com.ge.research.semtk.springutilib.requests.StoredItemTypeRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthorizationProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.springutillib.properties.NodegroupStoreProperties;
import com.ge.research.semtk.springutillib.properties.OntologyInfoServiceProperties;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.demo.DemoSetupThread;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.v3.oas.annotations.Operation;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.services.nodegroupStore.NgStore.StoredItemTypes;
import com.ge.research.semtk.servlet.utility.StartupUtilities;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

@RestController
@RequestMapping("/nodeGroupStore")
@CrossOrigin
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
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
	private ApplicationContext appContext;
	@Autowired
	private NodegroupStoreProperties prop;
	@Autowired
	private AuthorizationProperties auth_prop;
	@Autowired
	private ServicesGraphProperties servicesgraph_prop;
	@Autowired
	private OntologyInfoServiceProperties oinfo_props;
	
	private static final String SERVICE_NAME="nodeGroupStore";
	
	@PostConstruct
    public void init() throws Exception {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);

		// StoreProperties needs to be split up into pieces
		// and each one validated
		// then put demo nodegroup into store
		servicesgraph_prop.validateWithExit();
		oinfo_props.validateWithExit();
		
		// upload model in case it has changed

		StartupUtilities.updateOwlIfNeeded(this.getStoreModelSei(), oinfo_props.getClient(), getClass(), "/semantics/OwlModels/prefabNodeGroup.owl");

		LocalLogger.logToStdOut("loading demo...");
		try {
			DemoSetupThread thread = new DemoSetupThread(this.getStoreDataSei(), servicesgraph_prop.buildSei(), oinfo_props.getClient());
			thread.start();
		} catch (Exception e) {
			LocalLogger.printStackTrace(new Exception("Error setting up demo", e));
		} 
	}
	
	/**
	 * Store a new nodegroup to the nodegroup store.
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value={"/storeNodeGroup", "/storeItem"} , method=RequestMethod.POST)
	public JSONObject storeNodeGroup(@RequestBody StoreNodeGroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/storeNodeGroup";
			SimpleResultSet retval = null;
	
			try{
	
				// throw a meaningful exception if needed info is not present in the request
				requestBody.validate();	
				NgStore store = new NgStore(this.getStoreDataSei());
				
				// check that the ID does not already exist
				Table instanceTable = store.getStoredItemTable(requestBody.getName(), requestBody.getItemType());
				
				if(instanceTable.getNumRows() > 0) {
					if (requestBody.getOverwriteFlag()) {
						store.deleteStoredItem(requestBody.getName(), requestBody.getItemType());
					} else {
						throw new Exception("Unable to store item:  ID (" + requestBody.getName() + ") already exists and overwriteFlag=false");
					}
				}
	
				
				if (requestBody.getItemType() == StoredItemTypes.PrefabNodeGroup) {
					// insert nodegroup (in legacy fashion)
					JSONObject sgJsonJson = requestBody.getJsonNodeGroup();			// changed to allow for more dynamic nodegroup actions. 
					SparqlGraphJson sgJson = new SparqlGraphJson(sgJsonJson);
					JSONObject connJson = sgJson.getSparqlConnJson();
					if(connJson == null){
						throw new Exception("Item '" + requestBody.getName() + "' does not contain a valid connection block. it is possible that only the node group itself was passed. please check that complete input is sent.");
					}
		
					store.insertNodeGroup(sgJsonJson, connJson, requestBody.getName(), requestBody.getComments(), requestBody.getCreator());
				
				} else {
					// insert anything else
					store.insertStringBlob(requestBody.getItem(), requestBody.getItemType(),  requestBody.getName(), requestBody.getComments(), requestBody.getCreator());
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

	@Operation(
			summary="Get nodegroup as table with only one cell.",
			description="Column name is NodeGroup."
			)     
	@CrossOrigin
	@RequestMapping(value={"/getNodeGroupById"}, method=RequestMethod.POST)
	public JSONObject getNodeGroupById(@RequestBody @Valid IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getNodeGroupById";

			TableResultSet retval = new TableResultSet();	
			try{
				NgStore store = new NgStore(this.getStoreDataSei());
				Table ngTab = store.getStoredItemTable(requestBody.getId(), StoredItemTypes.PrefabNodeGroup);

				retval.setSuccess(true);
				retval.addResults(ngTab);
			}
			catch (Exception e) {
				// something went wrong. report and exit. 
				retval.setSuccess(false);
				retval.addRationaleMessage(SVC_ENDPOINT_NAME, e);
			} 

			return retval.toJson();  // whatever we have... send it out. 

		} finally {
			HeadersManager.clearHeaders();
		}
	}


	@Operation(
			summary="Get stored item as table with only one cell.",
			description="Column name is item."
			)
	@CrossOrigin
	@RequestMapping(value={"/getStoredItemById"}, method=RequestMethod.POST)
	public JSONObject getNodeGroupById(@RequestBody @Valid StoredItemRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getStoredItemById";

			TableResultSet retval = new TableResultSet();	
			try{
				NgStore store = new NgStore(this.getStoreDataSei());
					Table ngTab = store.getStoredItemTable(requestBody.getId(), requestBody.getItemType());
					// smooth out legacy code by reconciling column name to "item"
					if (ngTab.hasColumn("NodeGroup"))
						ngTab.renameColumn("NodeGroup", "item");
					else
						ngTab.renameColumn("stringChunk", "item");
					retval.setSuccess(true);
					retval.addResults(ngTab);
				}
				catch (Exception e) {
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
				NgStore store = new NgStore(this.getStoreDataSei());
				Table tab = store.getFullStoredItemList(StoredItemTypes.PrefabNodeGroup);
				retval.addResults(tab);
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


	@Operation(
			summary="Get a table of info about stored items of a given type.",
			description="More general version of /getNodeGroupMetadata."
			)
	@CrossOrigin
	@RequestMapping(value="/getStoredItemsMetadata", method=RequestMethod.POST)
	public JSONObject getStoredItemsMetadata(@RequestBody StoredItemTypeRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getStoredItemsMetadata";
			TableResultSet retval = new TableResultSet();		
			try{
				NgStore store = new NgStore(this.getStoreDataSei());
				Table tab = store.getStoredItemMetadata(requestBody.getItemType());
				retval.addResults(tab);
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


	@Operation(
			summary="Get a table of info about stored nodegroups.",
			description="Legacy.  Use /getStoredItemsMetadata."
			)
	@CrossOrigin
	@RequestMapping(value="/getNodeGroupMetadata", method=RequestMethod.POST)
	public JSONObject getNodeGroupMetadata(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getNodeGroupMetadata";
			TableResultSet retval = new TableResultSet();		
			try{
				NgStore store = new NgStore(this.getStoreDataSei());
				Table tab = store.getStoredItemMetadata(StoredItemTypes.PrefabNodeGroup);
				retval.addResults(tab);
				retval.setSuccess(true);
			}
			catch(Exception e){ 
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
				NgStore store = new NgStore(this.getStoreDataSei());
				Table tbl = store.getNodegroupTable(requestBody.getId());

				if(tbl.getNumRows() > 0){
					// we have a result. for now, let's assume that only the first result is valid.
					ArrayList<String> tmpRow = tbl.getRows().get(0);
					int targetCol = tbl.getColumnIndex("NodeGroup");
	
					String ngJSONstr = tmpRow.get(targetCol);
					JSONParser jParse = new JSONParser();
					JSONObject json = (JSONObject) jParse.parse(ngJSONstr); 
					SparqlGraphJson sgJson = new SparqlGraphJson(json);
					NodeGroup ng = sgJson.getNodeGroup();
	
					// get the runtime constraints. 
					RuntimeConstraintManager rtci = new RuntimeConstraintManager(ng);
					retval = new TableResultSet(true); 

					retval.addResults(rtci.getConstrainedItemsDescription());
				} else {
					retval = new TableResultSet(false);
					retval.addRationaleMessage(SVC_ENDPOINT_NAME, "Nodegroup was not found: " + requestBody.getId());
				}
			}
			catch(Exception e){
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
	 * Delete a nodegroup or item
	 * This method uses a static delete query. it would be better to use a local nodegroup and have belmont 
	 * generate a deletion query itself. 
	 * @param requestBody - defaults to nodegroup
	 * @return
	 * @throws Exception on error or if item does not exist
	 */
	@CrossOrigin
	@RequestMapping(value={"/deleteStoredNodeGroup", "/deleteStoredItem"}, method=RequestMethod.POST)
	public JSONObject deleteStoredNodeGroup(@RequestBody @Valid StoredItemRequest requestBody, @RequestHeader HttpHeaders headers) throws Exception {
		HeadersManager.setHeaders(headers);
		try {
			SimpleResultSet retval = null;	
			NgStore store = new NgStore(this.getStoreDataSei());

			try{
				// check that the ID does not already exist. if it does, fail.
				Table instanceTable = store.getStoredItemTable(requestBody.getId(), requestBody.getItemType());
				
				if(instanceTable.getNumRows() < 1){
					throw new Exception("No stored item exists with id: " + requestBody.getId() + " and type: " + requestBody.getItemType());
				
				} else if(instanceTable.getNumRows() > 1){
					throw new Exception("Internal error: multiple stored items exists with id: " + requestBody.getId() + " and type: " + requestBody.getItemType());
				
				} else {
				
					// attempt to delete the nodegroup, name and comments where there is a give ID.
					store.deleteStoredItem(requestBody.getId(), requestBody.getItemType());
					retval = new SimpleResultSet(true);
				}
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
	

	/**
	 * Rename a stored item.
	 */
	@CrossOrigin
	@RequestMapping(value={"/renameStoredItem"}, method=RequestMethod.POST)
	public JSONObject renameStoredItem(@RequestBody @Valid StoredItemRenameRequest requestBody, @RequestHeader HttpHeaders headers) throws Exception {
		HeadersManager.setHeaders(headers);
		try {
			SimpleResultSet retval = null;
			NgStore store = new NgStore(this.getStoreDataSei());
			try{
				store.renameStoredItem(requestBody.getId(), requestBody.getNewId(), requestBody.getItemType());
				retval = new SimpleResultSet(true);
			}catch(Exception e){
				LocalLogger.logToStdErr("Failure renaming " +  requestBody.getId() + ": " + e.getMessage());
				retval = new SimpleResultSet(false);
				retval.addRationaleMessage(e.getMessage());
			}
			return retval.toJson();

		} finally {
			HeadersManager.clearHeaders();
	    }
	}


    private SparqlEndpointInterface getStoreDataSei() throws Exception{
        SparqlEndpointInterface ret = SparqlEndpointInterface.getInstance(    
                prop.getSparqlConnType(), 
                prop.getSparqlConnServerAndPort(), 
                prop.getSparqlConnDataDataset(),
                servicesgraph_prop.getEndpointUsername(),
                servicesgraph_prop.getEndpointPassword()
                );
        return ret;
    }
    
    private SparqlEndpointInterface getStoreModelSei() throws Exception{
        SparqlEndpointInterface ret = SparqlEndpointInterface.getInstance(    
                prop.getSparqlConnType(), 
                prop.getSparqlConnServerAndPort(), 
                prop.getSparqlConnModelDataset(),
                servicesgraph_prop.getEndpointUsername(),
                servicesgraph_prop.getEndpointPassword()
                );
        return ret;
    }
	
}
