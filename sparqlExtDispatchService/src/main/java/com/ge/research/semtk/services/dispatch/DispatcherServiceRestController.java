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
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.sparqlX.asynchronousQuery.AsynchronousNodeGroupBasedQueryDispatcher;
import com.ge.research.semtk.sparqlX.asynchronousQuery.AsynchronousNodeGroupDispatcher;


@RestController
@RequestMapping("/dispatcher")
public class DispatcherServiceRestController {

	@Autowired
	DispatchProperties props;
	
	@CrossOrigin
	@RequestMapping(value="/queryFromNodeGroup", method=RequestMethod.POST)
	public JSONObject queryFromNodeGroup(@RequestBody QueryRequestBody requestBody){
		String requestId = this.getRequestId();
		SimpleResultSet retval = new SimpleResultSet(true);
		retval.addResult("requestID", requestId);
		
		AsynchronousNodeGroupBasedQueryDispatcher dsp = null;
		
		// create a request ID and set the value.
		
		// get the things we need for the dispatcher
		try {
			dsp = getDispatcher(props, requestId, (NodegroupRequestBody) requestBody);
			
			// set up a thread for the actual processing of the request
			(new WorkThread(dsp, false, requestBody.getConstraintSetJson())).start();
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
			
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
			
			dsp = getDispatcher(props, fakeReqId, (NodegroupRequestBody) requestBody);
			
			retval.addResult("constraintType", dsp.getConstraintType());
			retval.addResultStringArray("variableNames", dsp.getConstraintVariableNames());
		
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			retval.setSuccess(false);
			retval.addRationaleMessage(e.toString());
			
		}
		// send back the request ID.
		// the request is not finished but that is okay
		return retval.toJson();
	}
	
	private String getRequestId(){
		return "req_" + UUID.randomUUID();
	}
	
	private AsynchronousNodeGroupBasedQueryDispatcher getDispatcher(DispatchProperties prop, String requestId, NodegroupRequestBody requestBody ) throws Exception{
		
		// check the qry request body.
		
		AsynchronousNodeGroupBasedQueryDispatcher dsp = null;

		System.err.println("Dispatcher type in use: " + props.getDispatcherClassName() );
	
		SparqlQueryClient queryClient = new SparqlQueryClient(new SparqlQueryClientConfig(	
						props.getSparqlServiceProtocol(),
						props.getSparqlServiceServer(), 
						props.getSparqlServicePort(), 
						props.getSparqlServiceEndpoint(),
		                props.getEdcSparqlServerAndPort(), 
		                props.getEdcSparqlServerType(), 
		                props.getEdcSparqlServerDataset()));
		
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
			// build it.
			System.err.println("attempting to get constructor for dispatcher subtype:");

			Constructor ctor = null ; //dspType.getConstructor(String.class, JSONObject.class, ResultsClient.class, StatusClient.class, SparqlQueryClient.class);	
		
			for (Constructor c : dspType.getConstructors() ){
				// try to find the right constructor?
				
				Class[] params = c.getParameterTypes();
				for(Class p : params){
				}
				
				if(params[0].isAssignableFrom( String.class )) {
					if(params[1].isAssignableFrom( JSONObject.class )) {
						if(params[2].isAssignableFrom( ResultsClient.class )){
							if( params[3].isAssignableFrom( StatusClient.class )){
								if(params[4].isAssignableFrom( SparqlQueryClient.class )){
									ctor = c;
								}}}}
				}
				else{
				}
			}
			dsp = (AsynchronousNodeGroupBasedQueryDispatcher) ctor.newInstance(requestId, requestBody.getJsonNodeGroup(), rClient, sClient, queryClient);
			
		}
		catch(Exception failedToFindClass){
			System.err.println("retrieval of external dispatcher class failed. message from exception as follows:");
			System.err.println( failedToFindClass.getMessage() );
			failedToFindClass.printStackTrace();
			throw new Exception("getDispatcher :: unable to get requested dispatcher type " + props.getDispatcherClassName() + " from semtk or given additional classes loaded. please check dispatch.externalDispatchJar in the dispatcher configuration file.");
		}
		
		
		System.err.println("initialized job: " + requestId);
	
		return dsp;
	}

}
