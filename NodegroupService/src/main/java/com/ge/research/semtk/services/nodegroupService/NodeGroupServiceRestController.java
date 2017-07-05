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

package com.ge.research.semtk.services.nodegroupService;

import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstrainedItems;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.nodegroupService.requests.NodegroupRequest;
import com.ge.research.semtk.services.nodegroupService.requests.NodegroupSparqlIdRequest;

@RestController
@RequestMapping("/nodeGroupService")
@CrossOrigin
public class NodeGroupServiceRestController {
	
	public static final String QUERYFIELDLABEL = "SparqlQuery";
	public static final String QUERYTYPELABEL = "QueryType";


	@CrossOrigin
	@RequestMapping(value= "/**", method=RequestMethod.OPTIONS)
	public void corsHeaders(HttpServletResponse response) {
	    response.addHeader("Access-Control-Allow-Origin", "*");
	    response.addHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
	    response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, x-requested-with");
	    response.addHeader("Access-Control-Max-Age", "3600");
	}
	
	@CrossOrigin
	@RequestMapping(value="/generateSelect", method=RequestMethod.POST)
	public JSONObject generateSelectSparql(@RequestBody NodegroupRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
			String query = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, null, null);
			
			retval = this.generateSuccessOutput("SELECT", query);
			
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false, eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/generateCountAll", method=RequestMethod.POST)
	public JSONObject generateCountAllSparql(@RequestBody NodegroupRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
			String query = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_COUNT, false, null, null);
			
			retval = this.generateSuccessOutput("COUNT_ALL", query);
			
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false, eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();
	}	
	
	@CrossOrigin
	@RequestMapping(value="/generateDelete", method=RequestMethod.POST)
	public JSONObject generateDeleteSparql(@RequestBody NodegroupRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
			String query = ng.generateSparqlDelete(null);
			
			retval = this.generateSuccessOutput("DELETE", query);
			
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false, eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/generateFilter", method=RequestMethod.POST)
	public JSONObject generateFilterSparql(@RequestBody NodegroupSparqlIdRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
			Returnable item = ng.getNodeBySparqlID(requestBody.getTargetObjectSparqlId());
			if (item == null) {
				item = ng.getPropertyItemBySparqlID(requestBody.getTargetObjectSparqlId());
			}
			String query = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_CONSTRAINT, false, -1, item);
			
			retval = this.generateSuccessOutput("FILTER", query);
			
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false, eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/generateAsk", method=RequestMethod.POST)
	public JSONObject generateAskSparql(@RequestBody NodegroupRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
			String query = ng.generateSparqlAsk();
			
			retval = this.generateSuccessOutput("ASK", query);
			
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false, eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/generateConstruct", method=RequestMethod.POST)
	public JSONObject generateConstructSparql(@RequestBody NodegroupRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
			String query = ng.generateSparqlConstruct();
			
			retval = this.generateSuccessOutput("CONSTRUCT", query);
			
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false, eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getRuntimeConstraints", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraints(@RequestBody NodegroupRequest requestBody){
		TableResultSet retval = null;
		
		try{
			NodeGroup ng = this.getNodeGroupFromJson(requestBody.getJsonNodeGroup());
			RuntimeConstrainedItems rtci = new RuntimeConstrainedItems(ng);
			retval = new TableResultSet(); 
			retval.addResults(rtci.getConstrainedItemsDescription() );
		}
		catch(Exception eee){
			retval = new TableResultSet(false);
			retval.addRationaleMessage(eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();		
	}
	
	
	// helper method to figure out if we are looking at a nodegroup alone or a sparqlgraphJSON
	// and return a nodegroup from it.
	private NodeGroup getNodeGroupFromJson(JSONObject jobj) throws Exception{
		NodeGroup retval = new NodeGroup();
		
		if(jobj.containsKey("sNodeGroup")){
			// this was a sparqlGraphJson. unwrap before using.
			JSONObject innerObj = (JSONObject) jobj.get("sNodeGroup");
			retval.addJsonEncodedNodeGroup(innerObj);
		}
		else if(jobj.containsKey("sNodeList")){
			// this was just a node group
			retval.addJson((JSONArray) jobj.get("sNodeList"));
		}
		else{
			// something insane was passed. fail with some dignity.
			throw new Exception("Request object does not seem to contain a valid nodegroup serialization");
		}
		
		return retval;
	}
	
	private SimpleResultSet generateSuccessOutput(String queryType, String query) throws Exception {
		SimpleResultSet retval = new SimpleResultSet(true);
		retval.addResult(this.QUERYFIELDLABEL, query);
		retval.addResult(QUERYTYPELABEL, queryType);
		
		return retval;
	}
}
