package com.ge.research.semtk.services.ontologyinfo;

import org.json.simple.JSONObject;

import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

@CrossOrigin
@RestController
@RequestMapping("/ontologyinfo")
public class OntologyInfoServiceRestController {

	@Autowired
	OntologyInfoLoggingProperties log_prop;
	
	@CrossOrigin
	@RequestMapping(value="/getVisJs", method= RequestMethod.POST)
	public JSONObject getVisJs(@RequestBody OntologyInfoRequestBody requestBody){
		
		LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop);
	    LoggerRestClient.easyLog(logger, "OntologyInfoService", "getVisJs start");
    	logToStdout("OntologyInfo Service getVisJs start");
    	
    	SimpleResultSet res = new SimpleResultSet();
	    
	    try {
	    	SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(requestBody.serverType, requestBody.serverAndPort, requestBody.dataset);
	    	OntologyInfo oInfo = new OntologyInfo(sei, requestBody.domain);
	    	res.addResultsJSON(oInfo.toVisJs());
	    	res.setSuccess(true);
	    	
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(e.toString());
		    LoggerRestClient.easyLog(logger, "OntologyInfoService", "getVisJs exception", "message", e.toString());
		    e.printStackTrace();
	    }
	    
	    return res.toJson();
	}
	
	private void logToStdout (String message) {
		System.out.println(message);
	}
}
