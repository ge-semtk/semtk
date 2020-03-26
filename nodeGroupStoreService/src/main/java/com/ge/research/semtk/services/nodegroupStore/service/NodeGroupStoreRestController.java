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

import java.io.InputStream;
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

import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.springutilib.requests.IdRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.sparqlX.SparqlConnection;
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
	StoreProperties prop;

	@Autowired
	AuthProperties auth_prop;
	@Autowired
	ServicesGraphProperties servicesgraph_prop;
	private static final String SERVICE_NAME="nodeGroupStore";
	
	@PostConstruct
    public void init() throws Exception {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();

		// StoreProperties needs to be split up into pieces
		// and each one validated
		// then put demo nodegroup into store
		servicesgraph_prop.validateWithExit();
		
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);
		
		// upload model
		SparqlEndpointInterface modelSei = this.getStoreModelSei();
		InputStream owlStream = NodeGroup.class.getResourceAsStream("/semantics/OwlModels/prefabNodeGroup.owl");
		modelSei.uploadOwlModelIfNeeded(owlStream);
		
		setupDemo();
		
	}
	
	private void setupDemo() {
		LocalLogger.logToStdOut("loading demo...");
		try {
			// setup demoSei and demoConn
			SparqlEndpointInterface demoSei = servicesgraph_prop.buildSei();
			demoSei.setGraph("http://semtk/demo");
			SparqlConnection demoConn = new SparqlConnection("demoConn", demoSei);
			
			// put demoNodegroup into the store
			JSONObject sgJsonJson = Utility.getResourceAsJson(this, "/nodegroups/demoNodegroup.json");
			SparqlGraphJson sgJson = new SparqlGraphJson(sgJsonJson);
			sgJson.setSparqlConn(demoConn);
			
			NgStore store = new NgStore(this.getStoreDataSei());	
			JSONObject connJson = sgJson.getSparqlConnJson();
			store.deleteNodeGroup("demoNodegroup");
			store.insertNodeGroup(sgJsonJson, connJson, "demoNodegroup", "demo comments", "semTK", true);
	
			// load demo model owl
			InputStream owlStream = JobTracker.class.getResourceAsStream("/semantics/owlModels/hardware.owl");
			demoSei.uploadOwlModelIfNeeded(owlStream);
			owlStream = JobTracker.class.getResourceAsStream("/semantics/owlModels/testconfig.owl");
			demoSei.uploadOwlModelIfNeeded(owlStream);
			
			// ingest demo csv
			demoSei.clearPrefix("http://demo/prefix");  // extra safe.  No clear graph inside nodegroup store
			Dataset ds = new CSVDataset("src/main/resources/demoNodegroup_data.csv", false);
			DataLoader dl = new DataLoader(sgJson, ds, demoSei.getUserName(), demoSei.getPassword());
			dl.importData(false);
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
				NgStore store = new NgStore(this.getStoreDataSei());
				Table instanceTable = store.getNodegroupTable(requestBody.getName(), true);
				
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
	
				store.insertNodeGroup(sgJsonJson, connJson, requestBody.getName(), requestBody.getComments(), requestBody.getCreator(), true);
				
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
				NgStore store = new NgStore(this.getStoreDataSei());
				Table ngTab = store.getNodegroupTable(requestBody.getId(), true);
				
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
				Table tab = store.getFullNodeGroupList(true);
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

	@CrossOrigin
	@RequestMapping(value="/getNodeGroupMetadata", method=RequestMethod.POST)
	public JSONObject getNodeGroupMetadata(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			final String SVC_ENDPOINT_NAME = SERVICE_NAME + "/getNodeGroupMetadata";
			TableResultSet retval = new TableResultSet();		
			try{
				NgStore store = new NgStore(this.getStoreDataSei());
				Table tab = store.getNodeGroupMetadata(true);
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
				Table tbl = store.getNodegroupTable(requestBody.getId(), true);

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
	 * @throws Exception 
	 */
	@CrossOrigin
	@RequestMapping(value="/deleteStoredNodeGroup", method=RequestMethod.POST)
	public JSONObject deleteStoredNodeGroup(@RequestBody @Valid IdRequest requestBody, @RequestHeader HttpHeaders headers) throws Exception {
		HeadersManager.setHeaders(headers);
		try {
			SimpleResultSet retval = null;
	
			// NOTE:
			// this static delete works but will need to be updated should the metadata surrounding node groups be updated. 
			// really, if the insertion nodegroup itself is edited, this should be looked at.
			// ideally, the node groups would be able to write deletion queries, using filters and runtime constraints to
			// determine what to remove. if we moved to that point, we could probably use the same NG for insertions and deletions.
	
			NgStore store = new NgStore(this.getStoreDataSei());

	
			try{
				// attempt to delete the nodegroup, name and comments where there is a give ID.
				store.deleteNodeGroup(requestBody.getId(), true);
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
                prop.getSparqlConnModelDataset()
                );

        return ret;
    }
	
}
