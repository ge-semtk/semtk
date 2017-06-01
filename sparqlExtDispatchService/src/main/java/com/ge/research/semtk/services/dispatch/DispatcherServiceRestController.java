package com.ge.research.semtk.services.dispatch;

import java.lang.reflect.Constructor;
import java.net.ConnectException;
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
		
		StatusClient sClient = null;
		ResultsClient rClient = null;
		
		// get the things we need for the dispatcher
		try {
			SparqlQueryClient queryClient = new SparqlQueryClient(new SparqlQueryClientConfig(	props.getSparqlServiceProtocol(),
																									props.getSparqlServiceServer(), 
																									props.getSparqlServicePort(), 
																									props.getSparqlServiceEndpoint(),
				                                                                                    props.getEdcSparqlServerAndPort(), 
				                                                                                    props.getEdcSparqlServerType(), 
				                                                                                    props.getEdcSparqlServerDataset()));
		
			rClient = new ResultsClient(new ResultsClientConfig(props.getResultsServiceProtocol(), props.getResultsServiceServer(), props.getResultsServicePort()));
			sClient = new StatusClient(new StatusClientConfig(props.getStatusServiceProtocol(), props.getStatusServiceServer(), props.getStatusServicePort(), requestId));
			sClient.execSetPercentComplete(0, "Job Initialized");
			System.err.println("initialized job: " + requestId);
			
			System.err.println("Dispatcher type in use: " + props.getDispatcherClassName() );
			
			Class<?> dspType = Class.forName(props.getDispatcherClassName());
			Constructor<?> ctor = dspType.getConstructor(String.class, JSONObject.class, ResultsClient.class, StatusClient.class, SparqlQueryClient.class);
			dsp = (AsynchronousNodeGroupBasedQueryDispatcher) ctor.newInstance(requestId, requestBody.getJsonNodeGroup(), rClient, sClient, queryClient);
			
			// set up a thread for the actual processing of the request
			(new WorkThread(dsp, false, requestBody.getConstraintSetJson())).start();
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
			
			// claim a failure?
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
			SparqlQueryClient queryClient = new SparqlQueryClient(new SparqlQueryClientConfig(	props.getSparqlServiceProtocol(),
					props.getSparqlServiceServer(), 
					props.getSparqlServicePort(), 
					props.getSparqlServiceEndpoint(),
                    props.getEdcSparqlServerAndPort(), 
                    props.getEdcSparqlServerType(), 
                    props.getEdcSparqlServerDataset()));

			ResultsClient rClient = new ResultsClient(new ResultsClientConfig(props.getResultsServiceProtocol(), props.getResultsServiceServer(), props.getResultsServicePort()));
			StatusClient sClient = new StatusClient(new StatusClientConfig(props.getStatusServiceProtocol(), props.getStatusServiceServer(), props.getStatusServicePort(), fakeReqId));
			
			
			Class<?> dspType = Class.forName(props.getDispatcherClassName());
			Constructor<?> ctor = dspType.getConstructor(String.class, JSONObject.class, ResultsClient.class, StatusClient.class, SparqlQueryClient.class);
			dsp = (AsynchronousNodeGroupBasedQueryDispatcher) ctor.newInstance(fakeReqId, requestBody.getJsonNodeGroup(), rClient, sClient, queryClient);
						
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

}
