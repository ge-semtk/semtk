/**
 ** Copyright 2017 General Electric Company
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


package com.ge.research.semtk.services.ontologyinfo;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.ontologyTools.DataDictionaryGenerator;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.ontologyinfo.OntologyInfoLoggingProperties;
import com.ge.research.semtk.sparqlX.SparqlConnection;

@CrossOrigin
@RestController
@RequestMapping("/ontologyinfo")
public class OntologyInfoServiceRestController {
 	static final String SERVICE_NAME = "ontologyInfoService";

	@Autowired
	OntologyInfoLoggingProperties log_prop;
	
	@Autowired
	private OntologyServiceProperties service_prop;

	
	/**
	 * Get a tabular data dictionary report.
	 */
	@CrossOrigin
	@RequestMapping(value="/getDataDictionary", method=RequestMethod.POST)
	public JSONObject getDataDictionary(@RequestBody SparqlConnectionRequestBody requestBody) {
	
    	TableResultSet res = new TableResultSet();	
	    try {
	    	OntologyInfo oInfo = new OntologyInfo(requestBody.getSparqlConnection());
	    	Table dataDictionaryTable = DataDictionaryGenerator.generate(oInfo, true);
	    	res.addResults(dataDictionaryTable);
	    	res.setSuccess(true);
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "getDataDictionary", e);
	    	e.printStackTrace();
	    }
	    return res.toJson();
	}
	
	
	@CrossOrigin
	@RequestMapping(value="/getRdfOWL", method=RequestMethod.POST)
	public JSONObject getRdfOWL(@RequestBody OntologyInfoJsonRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
			OntologyInfo oInfo = requestBody.getOInfo();
			retval = new SimpleResultSet(); 
			retval.addResult("rdfOWL", oInfo.generateRdfOWL(requestBody.getBase()));
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getRdfOWL", eee);
			eee.printStackTrace();
		}
		
		return retval.toJson();		
	}
	
	@CrossOrigin
	@RequestMapping(value="/getSADL", method=RequestMethod.POST)
	public JSONObject getSADL(@RequestBody OntologyInfoJsonRequest requestBody){
		SimpleResultSet retval = null;
		
		try{
			OntologyInfo oInfo = requestBody.getOInfo();
			retval = new SimpleResultSet(); 
			retval.addResult("SADL", oInfo.generateSADL(requestBody.getBase()));
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getSADL", eee);
			eee.printStackTrace();
		}
		
		return retval.toJson();		
	}

	
	@CrossOrigin
	@RequestMapping(value="/getOntologyInfo", method=RequestMethod.POST)
	public JSONObject getOntologyInfo(@RequestBody OntologyInfoRequestBody requestBody){
		SimpleResultSet retval = null;
		
		try{
			SparqlConnection conn = requestBody.getJsonRenderedSparqlConnection();
			OntologyInfo oInfo = new OntologyInfo(conn);
			JSONObject oInfoDetails = oInfo.toAdvancedClientJson();
			
			retval = new SimpleResultSet();
			retval.addResult("ontologyInfo", oInfoDetails);
			retval.setSuccess(true);
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getOntologyInfo", eee);
			eee.printStackTrace();
		}
		// send it out.
		return retval.toJson();
	}
	
	private void logToStdout (String message) {
		System.out.println(message);
	}
}
