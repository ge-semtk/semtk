package com.ge.research.semtk.services.nodegroupStore.service;

import java.io.File;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.load.utility.Utility;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.nodegroupStore.SparqlQueries;
import com.ge.research.semtk.services.nodegroupStore.StoreNodeGroup;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

@RestController
@RequestMapping("/nodeGroupStore")
@CrossOrigin
public class NodeGroupStoreRestController {


	@Autowired
	StoreProperties prop;
	
	@CrossOrigin
	@RequestMapping(value="/storeNodeGroup", method=RequestMethod.POST)
	public JSONObject storeNodeGroup(@RequestBody StoreNodeGroupRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
		// store a new nodegroup to the remote nodegroup store. 
		
		// get the nodeGroup and the connection info:
		SparqlGraphJson sgJson = new SparqlGraphJson(requestBody.getJsonNodeGroup());
		JSONObject ng = sgJson.getSNodeGroupJson();
		JSONObject connectionInfo = sgJson.getSparqlConnJson();
		
		// get the template information
		JSONObject inputTemplateContents  = Utility.getJSONObjectFromFile(new File("/" + this.prop.getTemplateLocation()));
		
		// try to store the values.
		boolean retBool = StoreNodeGroup.storeNodeGroup(ng, connectionInfo, requestBody.getName(), requestBody.getComments(),
				inputTemplateContents.toString(), prop.getIngestorLocation(), prop.getIngestorProtocol(), prop.getIngestorPort());
		
		retval = new SimpleResultSet(retBool);
		
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false, eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getNodeGroupById", method=RequestMethod.POST)
	public JSONObject getNodeGroupById(@RequestBody NodeGroupByIdRequest requestBody){
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
	public JSONObject getNodeGroupList(){
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
	@RequestMapping(value="/getNodeGroupRuntimeConstraints", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraints(@RequestBody NodeGroupByIdRequest requestBody){
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
				
				String ng = tmpRow.get(targetCol);
				
				// turn this into a nodeGroup. 
				JSONParser jParse = new JSONParser();
				JSONObject json = (JSONObject) jParse.parse(ng);
				
				// get the runtime constraints. 
			
				retval = new TableResultSet(true); 
				retval.addResults(StoreNodeGroup.getConstrainedItems(json));
			}
			
		}
		catch(Exception e){
			// something went wrong. report and exit. 
			
			System.err.println("a failure was encountered during the retrieval of runtime constraints: " + 
					e.getMessage());
			
			retval = new TableResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
		
		
		return retval.toJson();
	}
	
	
	// static method to avoid repeating the client generation code...
	
	
	private static SparqlQueryClient createClient(StoreProperties props) throws Exception{
		
		SparqlQueryClient retval = new SparqlQueryClient(new SparqlQueryClientConfig(	props.getSparqlServiceProtocol(),
				props.getSparqlServiceServer(), 
				props.getSparqlServicePort(), 
				props.getSparqlServiceEndpoint(),
                props.getSparqlServerAndPort(), 
                props.getSparqlServerType(), 
                props.getSparqlServerDataSet()));
		
		return retval;
	}
}
