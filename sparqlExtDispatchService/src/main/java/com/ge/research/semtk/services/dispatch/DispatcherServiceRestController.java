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

package com.ge.research.semtk.services.dispatch;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.dispatch.DispatchProperties;
import com.ge.research.semtk.services.dispatch.NodegroupRequestBody;
import com.ge.research.semtk.services.dispatch.WorkThread;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.sparqlX.asynchronousQuery.AsynchronousNodeGroupBasedQueryDispatcher;
import com.ge.research.semtk.sparqlX.asynchronousQuery.AsynchronousNodeGroupDispatcher;
import com.ge.research.semtk.sparqlX.asynchronousQuery.DispatcherSupportedQueryTypes;

@RestController
@RequestMapping("/dispatcher")
public class DispatcherServiceRestController {
 	static final String SERVICE_NAME = "dispatcher";
 	
	@Autowired
	DispatchProperties props;
	
	// select uses the original endpoint name for BC
	@CrossOrigin
	@RequestMapping(value="/queryFromNodeGroup", method=RequestMethod.POST)
	public JSONObject querySelectFromNodeGroup_BC(@RequestBody QueryRequestBody requestBody){
		return queryFromNodeGroup(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT, false);
	}
	
	@CrossOrigin
	@RequestMapping(value="/querySelectFromNodeGroup", method=RequestMethod.POST)
	public JSONObject querySelectFromNodeGroup(@RequestBody QueryRequestBody requestBody){
		return queryFromNodeGroup(requestBody, DispatcherSupportedQueryTypes.SELECT_DISTINCT, false);
	}

	@CrossOrigin
	@RequestMapping(value="/queryCountFromNodeGroup", method=RequestMethod.POST)
	public JSONObject queryCounttFromNodeGroup(@RequestBody QueryRequestBody requestBody){
		return queryFromNodeGroup(requestBody, DispatcherSupportedQueryTypes.COUNT, false);
	}
	
	@CrossOrigin
	@RequestMapping(value="/queryDeleteFromNodeGroup", method=RequestMethod.POST)
	public JSONObject queryDeleteFromNodeGroup(@RequestBody QueryRequestBody requestBody){
		return queryFromNodeGroup(requestBody, DispatcherSupportedQueryTypes.DELETE, true);
	}

	@CrossOrigin
	@RequestMapping(value="/queryFilterFromNodeGroup", method=RequestMethod.POST)
	public JSONObject queryFilterFromNodeGroup(@RequestBody FilterConstraintsRequestBody requestBody){
		return queryFromNodeGroup(requestBody, DispatcherSupportedQueryTypes.FILTERCONSTRAINT, false);
	}

	@CrossOrigin
	@RequestMapping(value="/asynchronousDirectQuery", method=RequestMethod.POST)
	public JSONObject asynchronousDirectQuery(@RequestBody SparqlRequestBody requestBody){
		return queryFromSparql(requestBody, DispatcherSupportedQueryTypes.RAW_SPARQL);
	}
	
	@CrossOrigin
	@RequestMapping(value="/queryConstructFromNodeGroup", method=RequestMethod.POST)
	public JSONObject queryConstructFromNodeGroup(@RequestBody QueryRequestBody requestBody){
		return queryFromNodeGroup(requestBody, DispatcherSupportedQueryTypes.CONSTRUCT, false);
	}
	
	public JSONObject queryFromSparql(@RequestBody SparqlRequestBody requestBody, DispatcherSupportedQueryTypes qt){
		String requestId = this.getRequestId();
		SimpleResultSet retval = new SimpleResultSet(true);
		retval.addResult("requestID", requestId);
		
		AsynchronousNodeGroupBasedQueryDispatcher dsp = null;
		
		// create a request ID and set the value.
		
		// get the things we need for the dispatcher
		try {
			SparqlGraphJson sgjson = new SparqlGraphJson();
			sgjson.setSparqlConn( requestBody.getConnection());
			
			NodegroupRequestBody ngrb = new NodegroupRequestBody();
			ngrb.setjsonRenderedNodeGroup(sgjson.getJson().toJSONString());
			
			dsp = getDispatcher(props, requestId, ngrb, true);
			
			WorkThread doIt = new WorkThread(dsp, null, qt);

			if(qt.equals(DispatcherSupportedQueryTypes.RAW_SPARQL)){
				// we are going to launch straight from the raw sparql
				String qry = ((SparqlRequestBody)requestBody).getRawSparqlQuery();
				doIt.setRawSparqlSquery(qry);
			}
			
			// set up a thread for the actual processing of the request
			doIt.start();
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "../queryFromSparql()", e);
			
			// claim a failure?
			StatusClient sClient = null;
			try {
				sClient = new StatusClient(new StatusClientConfig(props.getStatusServiceProtocol(), props.getStatusServiceServer(), props.getStatusServicePort(), requestId));
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			if(sClient != null){ 
				try {
					sClient.execSetFailure(e.getMessage());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
		// send back the request ID.
		// the request is not finished but that is okay
		return retval.toJson();
	}
	
	public JSONObject queryFromNodeGroup(@RequestBody QueryRequestBody requestBody, DispatcherSupportedQueryTypes qt, Boolean useAuth){
		String requestId = this.getRequestId();
		SimpleResultSet retval = new SimpleResultSet(true);
		retval.addResult("requestID", requestId);
		
		AsynchronousNodeGroupBasedQueryDispatcher dsp = null;
		
		// create a request ID and set the value.
		
		// get the things we need for the dispatcher
		try {
			dsp = getDispatcher(props, requestId, (NodegroupRequestBody) requestBody, useAuth);
			
			WorkThread doIt = new WorkThread(dsp, requestBody.getConstraintSetJson(), qt);
			
			if(qt.equals(DispatcherSupportedQueryTypes.FILTERCONSTRAINT)){
				// we should have a potential target object.				
				String target = ((FilterConstraintsRequestBody)requestBody).getTargetObjectSparqlID();
				doIt.setTargetObjectSparqlID(target);
			}
		
			// set up a thread for the actual processing of the request
			doIt.start();
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "../queryFromNodegroup()", e);
			
			// claim a failure?
			StatusClient sClient = null;
			try {
				sClient = new StatusClient(new StatusClientConfig(props.getStatusServiceProtocol(), props.getStatusServiceServer(), props.getStatusServicePort(), requestId));
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			if(sClient != null){ 
				try {
					sClient.execSetFailure(e.getMessage());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
		// send back the request ID.
		// the request is not finished but that is okay
		return retval.toJson();
	}

	@CrossOrigin
	@RequestMapping(value="/getConstraintInfo", method=RequestMethod.POST)
	public JSONObject getConstraintInfo(@RequestBody NodegroupRequestBody requestBody){
		SimpleResultSet retval = new SimpleResultSet(true);
		
		AsynchronousNodeGroupBasedQueryDispatcher dsp = null;
		String fakeReqId = "unused_in_this_case";
		// get the things we need for the dispatcher
		try {
			
			dsp = getDispatcher(props, fakeReqId, (NodegroupRequestBody) requestBody, false);
			
			retval.addResult("constraintType", dsp.getConstraintType());
			retval.addResultStringArray("variableNames", dsp.getConstraintVariableNames());
		
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, "getConstraintInfo", e);
			
		}
		// send back the request ID.
		// the request is not finished but that is okay
		return retval.toJson();
	}
	
	private String getRequestId(){
		return "req_" + UUID.randomUUID();
	}
	
	private AsynchronousNodeGroupBasedQueryDispatcher getDispatcher(DispatchProperties prop, String requestId, NodegroupRequestBody requestBody, Boolean useAuth ) throws Exception{
		
		// get the sgJson...
		SparqlGraphJson sgJson = null;
		
		try{
			sgJson = new SparqlGraphJson(requestBody.getJsonNodeGroup()) ;
			if(sgJson == null){ throw new Exception("sgJson was null."); } 
		}
		catch(Exception sg){
			throw new Exception("getDispatcher :: unable to get sparqlgraphJson from request. failure was reported as: " + sg.getMessage());
		}
		
		// check the qry request body.		
		AsynchronousNodeGroupBasedQueryDispatcher dsp = null;

		System.err.println("Dispatcher type in use: " + props.getDispatcherClassName() );
	
	
		SparqlQueryClientConfig queryConf = null;
		SparqlQueryClient queryClient = null;
		
		if(useAuth){
			queryConf = new SparqlQueryAuthClientConfig(	
					props.getSparqlServiceProtocol(),
					props.getSparqlServiceServer(), 
					props.getSparqlServicePort(), 
					props.getSparqlServiceAuthEndpoint(),
	                props.getEdcSparqlServerAndPort(), 
	                props.getEdcSparqlServerType(), 
	                props.getEdcSparqlServerDataset(),
					props.getSparqlServiceUser(),
					props.getSparqlServicePass());
			
			queryClient = new SparqlQueryClient(queryConf);
			
		}
		else{

			queryConf = new SparqlQueryClientConfig(	
					props.getSparqlServiceProtocol(),
					props.getSparqlServiceServer(), 
					props.getSparqlServicePort(), 
					props.getSparqlServiceEndpoint(),
	                props.getEdcSparqlServerAndPort(), 
	                props.getEdcSparqlServerType(), 
	                props.getEdcSparqlServerDataset());
	
			queryClient = new SparqlQueryClient(queryConf);
		}
		
		if(queryClient == null){
			System.err.println("!!!!!!! QUERY CLIENT IS NULL !!!!!!!");
			throw new Exception("getDispatcher :: the attempt to create a query client failed.");
		}
		
		StatusClient sClient = null;
		ResultsClient rClient = null;
				
		rClient = new ResultsClient(new ResultsClientConfig(props.getResultsServiceProtocol(), props.getResultsServiceServer(), props.getResultsServicePort()));
		sClient = new StatusClient(new StatusClientConfig(props.getStatusServiceProtocol(), props.getStatusServiceServer(), props.getStatusServicePort(), requestId));
		sClient.execSetPercentComplete(0, "Job Initialized");

		// try to get the class we care about. 
		try{
			Class<?> dspType = null;

			dspType = Class.forName(props.getDispatcherClassName());
			
			if(dspType == null) { System.err.println("DSPTYPE IS NULL!"); }
			else{ System.err.println( "configured dispatcher type is " + dspType.getCanonicalName() ); }
			System.err.println("attempting to get constructor for dispatcher subtype:");
			// build it.

			Constructor ctor = null ; //dspType.getConstructor(String.class, JSONObject.class, ResultsClient.class, StatusClient.class, SparqlQueryClient.class);	
		
			for (Constructor c : dspType.getConstructors() ){
				// try to find the right constructor?
				Class[] params = c.getParameterTypes();
				for(Class p : params){
				}
				// this is not a great way to get the constructor but the more traditional single call was failing pretty badly.
				// it has proved easier to look for each arg by type in order as it never mysteriously fails.
				if(params[0].isAssignableFrom( String.class )) {
					if(params[1].isAssignableFrom( SparqlGraphJson.class )) {
						if(params[2].isAssignableFrom( ResultsClient.class )){
							if( params[3].isAssignableFrom( StatusClient.class )){
								if(params[4].isAssignableFrom( SparqlQueryClient.class )){
									ctor = c;
								}}}}
				}
				else{
				}
			}
			dsp = (AsynchronousNodeGroupBasedQueryDispatcher) ctor.newInstance(requestId, sgJson, rClient, sClient, queryClient);
			
		}
		catch(Exception failedToFindClass){
			System.err.println("retrieval of external dispatcher class failed:");
			System.err.println( failedToFindClass.getMessage() );
			failedToFindClass.printStackTrace();
			throw new Exception("getDispatcher :: unable to instantiate dispatcher of type " + props.getDispatcherClassName() + ".  Please check dispatch.externalDispatchJar property, additional jars directory, or dispatcher error log");
		}
		
		
		System.err.println("initialized job: " + requestId);
	
		return dsp;
	}

}
