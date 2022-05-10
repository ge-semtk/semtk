/**
 ** Copyright 2020 General Electric Company
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

import io.swagger.v3.oas.annotations.Operation;

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
	@Operation(
			description="Return a nodegroup json (full SparqlGraphJson)"
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
