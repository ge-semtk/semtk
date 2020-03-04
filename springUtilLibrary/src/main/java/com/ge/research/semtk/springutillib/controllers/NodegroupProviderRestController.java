package com.ge.research.semtk.springutillib.controllers;

import org.json.simple.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.springutilib.requests.IdRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

import io.swagger.annotations.ApiOperation;

/**
 * Super-class for FDC services.
 * Your service can extend this class.
 * Store nodegroups in your /src/main/resources/nodegroups/<nodegroupID>.json
 * Your service will then supply nodegroups to FDC Dispatcher and FDC Cache Services
 * 
 * You may alternatively store nodegroups in the nodegroup store.
 * If your service does not return them, the dispatcher and cache services will check the store.
 * 
 * @author 200001934
 *
 */
public class NodegroupProviderRestController {
	@ApiOperation(
			value="Return a nodegroup json (full SparqlGraphJson)"
			//notes=""
			)
	@CrossOrigin
	@RequestMapping(value="/getNodegroup", method= RequestMethod.POST)
	public JSONObject getNodegroup(@RequestBody IdRequest request,  @RequestHeader HttpHeaders headers) {
		// retrieves nodegroup from /src/main/resources/<id>.json
		HeadersManager.setHeaders(headers);		
		final String ENDPOINT_NAME = "getNodegroup";
		String resourcePath = "/nodegroups/" + request.getId() + ".json";
		SimpleResultSet results = new SimpleResultSet();
		try {
			JSONObject sgJsonJson = Utility.getResourceAsJson(this, resourcePath);
			results.addResult("sgjson", sgJsonJson);
			results.setSuccess(true);
			LocalLogger.logToStdOut(ENDPOINT_NAME + ": " + resourcePath + " was found");
			return results.toJson();
			
		} catch(Exception e){
			LocalLogger.logToStdOut(ENDPOINT_NAME + ": " + resourcePath + " was not found");
	    	results.setSuccess(false);
	    	results.addRationaleMessage("NodegroupProvider", ENDPOINT_NAME, e);
		    LocalLogger.printStackTrace(e);
		    return results.toJson();
		}  
	}
}
