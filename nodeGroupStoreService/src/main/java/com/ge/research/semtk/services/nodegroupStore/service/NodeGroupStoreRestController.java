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
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.nodegroupStore.SparqlQueries;
import com.ge.research.semtk.services.nodegroupStore.StoreNodeGroup;
import com.ge.research.semtk.services.nodegroupStore.service.StoreNodeGroupRequest;
import com.ge.research.semtk.services.nodegroupStore.service.DeleteByIdRequest;
import com.ge.research.semtk.services.nodegroupStore.service.NodeGroupByIdRequest;

import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

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
	
	/**
	 * Store a new nodegroup to the nodegroup store.
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/storeNodeGroup", method=RequestMethod.POST)
	public JSONObject storeNodeGroup(@RequestBody StoreNodeGroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
			
		try{
			
		// throw a meaningful exception if needed info is not present in the request
		requestBody.validate();	
			
		// check that the ID does not already exist. if it does, fail.
		String qry = SparqlQueries.getNodeGroupByID(requestBody.getName());
		SparqlQueryClient clnt = createClient(prop);
		
		TableResultSet instanceTable = (TableResultSet) clnt.execute(qry, SparqlResultTypes.TABLE);
		
		if(instanceTable.getTable().getNumRows() > 0){
			throw new Exception("Unable to store node group:  ID (" + requestBody.getName() + ") already exists");
		}
			
		// get the nodeGroup and the connection info:
		JSONObject sgJsonJson = requestBody.getJsonNodeGroup();			// changed to allow for more dynamic nodegroup actions. 
		SparqlGraphJson sgJson = new SparqlGraphJson(sgJsonJson);
																// the next line was removed to make sure the node group is not "stripped" -- cut down to just the nodegroup proper when stored. 
																// the executor is smart enough to deal with both cases. 
						
		
		JSONObject connJson = sgJson.getSparqlConnJson();
		
		if(connJson == null){
			// we really should not continue if we are not sure where this came from originally. 
			// throw an error and fail gracefully... ish.
			throw new Exception("storeNodeGroup :: sparqlgraph jason serialization passed to store node group did not contain a valid connection block. it is possible that only the node group itself was passed. please check that complete input is sent.");
		}
		
		
		// try to store the values.
		boolean retBool = StoreNodeGroup.storeNodeGroup(sgJsonJson, connJson, requestBody.getName(), requestBody.getComments(), requestBody.getCreator(),
				createOverrideConnection(prop), prop.getIngestorLocation(), prop.getIngestorProtocol(), prop.getIngestorPort());
		
		retval = new SimpleResultSet(retBool);
		
		}
		catch(Exception e){
			retval = new SimpleResultSet(false, e.getMessage());
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getNodeGroupById", method=RequestMethod.POST)
	public JSONObject getNodeGroupById(@RequestBody NodeGroupByIdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		TableResultSet retval = null;
		
		try{
			String qry = SparqlQueries.getNodeGroupByID(requestBody.getId());
			SparqlQueryClient clnt = createClient(prop);
			
			retval = (TableResultSet) clnt.execute(qry, SparqlResultTypes.TABLE);
		}
		catch(Exception e){
			// something went wrong. report and exit. 
			
			retval = new TableResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
		
		return retval.toJson();  // whatever we have... send it out. 
	}

	@CrossOrigin
	@RequestMapping(value="/getNodeGroupList", method=RequestMethod.POST)
	public JSONObject getNodeGroupList(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		TableResultSet retval = null;
		
		try{
			String qry = SparqlQueries.getFullNodeGroupList();
			SparqlQueryClient clnt = createClient(prop);
			
			retval = (TableResultSet) clnt.execute(qry, SparqlResultTypes.TABLE);
		}
		catch(Exception e){
			// something went wrong. report and exit. 
			
			retval = new TableResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
		
		return retval.toJson();  // whatever we have... send it out. 
	}

	@CrossOrigin
	@RequestMapping(value="/getNodeGroupMetadata", method=RequestMethod.POST)
	public JSONObject getNodeGroupMetadata(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		TableResultSet retval = null;		
		try{
			String qry = SparqlQueries.getNodeGroupMetadata();
			SparqlQueryClient clnt = createClient(prop);
			retval = (TableResultSet) clnt.execute(qry, SparqlResultTypes.TABLE);
		}
		catch(Exception e){
			// something went wrong. report and exit. 
			retval = new TableResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
		return retval.toJson();   
	}
	
	@CrossOrigin
	@RequestMapping(value="/getNodeGroupRuntimeConstraints", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraints(@RequestBody NodeGroupByIdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		TableResultSet retval = null;
		
		try{
			// get the nodegroup
			String qry = SparqlQueries.getNodeGroupByID(requestBody.getId());
			SparqlQueryClient clnt = createClient(prop); 
			
			TableResultSet temp = (TableResultSet) clnt.execute(qry, SparqlResultTypes.TABLE);
			
			// get the first result and return all the runtime constraints for it.
			Table tbl = temp.getResults();
			
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
				else if(NodeGroup.isNodeGroup(json)) {
					// do nothing
				}
				else{
					// no idea what this is...
					throw new Exception("Value given for encoded node group does not seem to be a SparqlGraphJson nor a NodeGroup");
				}
				
				
				// get the runtime constraints. 
			
				retval = new TableResultSet(true); 
				retval.addResults(StoreNodeGroup.getConstrainedItems(json));
			}
			
		}
		catch(Exception e){
			// something went wrong. report and exit. 
			
			LocalLogger.logToStdErr("a failure was encountered during the retrieval of runtime constraints: " + 
					e.getMessage());
			
			retval = new TableResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
		
		
		return retval.toJson();
	}
	/**
	 * This method uses a static delete query. it would be better to use a local nodegroup and have belmont 
	 * generate a deletion query itself. 
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/deleteStoredNodeGroup", method=RequestMethod.POST)
	public JSONObject deleteStoredNodeGroup(@RequestBody DeleteByIdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		// NOTE:
		// this static delete works but will need to be updated should the metadata surrounding node groups be updated. 
		// really, if the insertion nodegroup itself is edited, this should be looked at.
		// ideally, the node groups would be able to write deletion queries, using filters and runtime constraints to
		// determine what to remove. if we moved to that point, we could probably use the same NG for insertions and deletions.
		
		String qry =
			"prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
			"Delete " + 
			"{" +
			 "  ?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup." +
			 "  ?PrefabNodeGroup prefabNodeGroup:ID \"" + requestBody.getId() + "\"^^<http://www.w3.org/2001/XMLSchema#string> ." +
			 "  ?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup ." +
			 "  ?PrefabNodeGroup prefabNodeGroup:comments ?comments . " +
			 "  ?PrefabNodeGroup prefabNodeGroup:creator ?creator . " +
			 "  ?PrefabNodeGroup prefabNodeGroup:creationDate ?creationDate . " +
			"}" + 
			 "where { " +
			 "  ?PrefabNodeGroup prefabNodeGroup:ID \"" + requestBody.getId()  +"\"^^<http://www.w3.org/2001/XMLSchema#string> ." +
			 "  ?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
			 "  ?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
			 "  optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
			 "  optional { ?PrefabNodeGroup prefabNodeGroup:creator ?creator . } " +
			 "  optional { ?PrefabNodeGroup prefabNodeGroup:creationDate ?creationDate . } " +
			 "}";
		
		try{
			// attempt to delete the nodegroup, name and comments where there is a give ID.
			SparqlQueryClient clnt = createClient(prop);
			retval = (SimpleResultSet) clnt.execute(qry, SparqlResultTypes.CONFIRM);
			
			
								
		}
		catch(Exception e){
			// something went wrong. report and exit. 
			
			LocalLogger.logToStdErr("a failure was encountered during the deletion of " +  requestBody.getId() + ": " + 
					e.getMessage());
			
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
		
		
		return retval.toJson();
	}
	
	// static method to avoid repeating the client generation code...
	
	
	private static SparqlQueryClient createClient(StoreProperties props) throws Exception{
		
		SparqlQueryClient retval = new SparqlQueryClient((SparqlQueryClientConfig)(new SparqlQueryAuthClientConfig(	
				props.getSparqlServiceProtocol(),
				props.getSparqlServiceServer(), 
				props.getSparqlServicePort(), 
				props.getSparqlServiceEndpoint(),
                props.getSparqlConnServerAndPort(), 
                props.getSparqlConnType(), 
                props.getSparqlConnDataDataset(),
				props.getSparqlServiceUser(),
				props.getSparqlServicePass())
				));
		
		return retval;
	}
	
	private static SparqlConnection createOverrideConnection(StoreProperties props) throws Exception {
		SparqlConnection retval = new SparqlConnection();
		retval.setName("store override");
		retval.setDomain(props.getSparqlConnDomain());
		retval.addDataInterface(props.getSparqlConnType(), props.getSparqlConnServerAndPort(), props.getSparqlConnDataDataset());
		retval.addModelInterface(props.getSparqlConnType(), props.getSparqlConnServerAndPort(), props.getSparqlConnModelDataset());
		
		return retval;
	}
	
}
