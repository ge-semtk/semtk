/**
 ** Copyright 2017-2018 General Electric Company
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.ontologyTools.DataDictionaryGenerator;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyInfoCache;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.ontologyinfo.OntologyInfoLoggingProperties;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.annotations.ApiOperation;

@CrossOrigin
@RestController
@RequestMapping("/ontologyinfo")
public class OntologyInfoServiceRestController {
 	static final String SERVICE_NAME = "ontologyInfoService";

 	OntologyInfoCache oInfoCache = new OntologyInfoCache(5 * 60 * 1000);
 	
	@Autowired
	OntologyInfoLoggingProperties log_prop;
	
	@Autowired
	private OntologyServiceProperties service_prop;

	
	/**
	 * Get a tabular data dictionary report.
	 */
	@CrossOrigin
	@RequestMapping(value="/getDataDictionary", method=RequestMethod.POST)
	public JSONObject getDataDictionary(@RequestBody SparqlConnectionRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
	
    	TableResultSet res = new TableResultSet();	
	    try {
	    	OntologyInfo oInfo = oInfoCache.get(requestBody.getSparqlConnection());
	    	Table dataDictionaryTable = DataDictionaryGenerator.generate(oInfo, true);
	    	res.addResults(dataDictionaryTable);
	    	res.setSuccess(true);
	    } catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "getDataDictionary", e);
	    	LocalLogger.printStackTrace(e);
	    }
	    return res.toJson();
	}
	
	
	@CrossOrigin
	@RequestMapping(value="/getRdfOWL", method=RequestMethod.POST)
	public JSONObject getRdfOWL(@RequestBody OntologyInfoJsonRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			OntologyInfo oInfo = requestBody.getOInfo();
			retval = new SimpleResultSet(); 
			retval.addResult("rdfOWL", oInfo.generateRdfOWL(requestBody.getBase()));
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getRdfOWL", e);
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();		
	}
	
	@CrossOrigin
	@RequestMapping(value="/getSADL", method=RequestMethod.POST)
	public JSONObject getSADL(@RequestBody OntologyInfoJsonRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			OntologyInfo oInfo = requestBody.getOInfo();
			retval = new SimpleResultSet(); 
			retval.addResult("SADL", oInfo.generateSADL(requestBody.getBase()));
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getSADL", e);
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();		
	}

	
	@CrossOrigin
	@RequestMapping(value="/getOntologyInfo", method=RequestMethod.POST)
	public JSONObject getOntologyInfo(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			SparqlConnection conn = requestBody.getJsonRenderedSparqlConnection();
			OntologyInfo oInfo = oInfoCache.get(conn);
			JSONObject oInfoDetails = oInfo.toAdvancedClientJson();
			
			retval = new SimpleResultSet();
			retval.addResult("ontologyInfo", oInfoDetails);
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getOntologyInfo", e);
			LocalLogger.printStackTrace(e);
		}
		// send it out.
		return retval.toJson();
	}
	
	@ApiOperation(
		    value= "Get a table of:  type (class|property), uri, label",
		    notes= "If a URI has no labels, it will appear with label = \"\""
		)
	@CrossOrigin
	@RequestMapping(value="/getUriLabelTable", method=RequestMethod.POST)
	public JSONObject getUriLabelTable(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		final String ENDPOINT_NAME = "getUriLabelTable";
		HeadersManager.setHeaders(headers);
		TableResultSet retval = new TableResultSet();
		
		try{
			SparqlConnection conn = requestBody.getJsonRenderedSparqlConnection();
			
			retval.addResults(oInfoCache.get(conn).getUriLabelTable());
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.setSuccess(false);
			retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			LocalLogger.printStackTrace(e);
		}
		// send it out.
		return retval.toJson();
	}
	
	@ApiOperation(
		    value= "Get the oInfo JSON",
		    notes= "This is the 'main' endpoint for this service."
		)
	@CrossOrigin
	@RequestMapping(value="/getOntologyInfoJson", method=RequestMethod.POST)
	public JSONObject getOntologyInfoJson(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			SparqlConnection conn = requestBody.getJsonRenderedSparqlConnection();
			OntologyInfo oInfo = oInfoCache.get(conn);
			JSONObject json = oInfo.toJson();
			
			retval = new SimpleResultSet();
			retval.addResult("ontologyInfo", json);
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getOntologyInfo", e);
			LocalLogger.printStackTrace(e);
		}
		// send it out.
		return retval.toJson();
	}
	
	@ApiOperation(
			value="Un-cache any oInfo that was built from any model dataset in the given connection."
			)
	@CrossOrigin
	@RequestMapping(value="/uncacheChangedModel", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public JSONObject uncacheChangedModel(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "uncacheChangedModel";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {			
			SparqlConnection conn = requestBody.getJsonRenderedSparqlConnection();
			
			oInfoCache.removeSimilar(conn);
			
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Un-cache the ontology loaded by given connection.",
			notes="For very specific UI operations only.<p>Generally, use <i>/uncacheChangedModel</i> instead."
			)
	@CrossOrigin
	@RequestMapping(value="/uncacheOntology", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public JSONObject uncacheOntology(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "uncacheChangedModel";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {			
			SparqlConnection conn = requestBody.getJsonRenderedSparqlConnection();
			
			oInfoCache.remove(conn);
			
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
}
