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

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.ontologyTools.DataDictionaryGenerator;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyInfoCache;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.PredicateStats;
import com.ge.research.semtk.ontologyTools.PredicateStatsCache;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.ontologyinfo.requests.OntologyInfoClassRequestBody;
import com.ge.research.semtk.services.ontologyinfo.requests.OntologyInfoRequestBody;
import com.ge.research.semtk.services.ontologyinfo.requests.SparqlConnectionRequestBody;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.annotations.ApiOperation;

@CrossOrigin
@RestController
@RequestMapping("/ontologyinfo")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class OntologyInfoServiceRestController {
 	static final String SERVICE_NAME = "ontologyInfoService";

 	OntologyInfoCache oInfoCache = new OntologyInfoCache(5 * 60 * 1000);
 	PredicateStatsCache predStatsCache = new PredicateStatsCache(5 * 60 * 1000);
 	
	@Autowired
	OntologyInfoLoggingProperties log_prop;
	@Autowired
	private OntologyServiceProperties service_prop;
	@Autowired 
	private ApplicationContext appContext;
	@Autowired
	AuthProperties auth_prop;

	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();

		log_prop.validateWithExit();
		service_prop.validateWithExit();
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);

	}
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
	
	@ApiOperation(
		    value= "Experimental"
		)
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
	@ApiOperation(
		    value= "Experimental"
		)
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

	
	@ApiOperation(
		    value= "Get all data and object properties of a class, including those inherited from superclasses.",
		    notes= "Returns classInfo = { name : 'classname', properties = [ { domain : 'http://model#hasTree', range: ['http://model#Tree'] } ]"
		)
	@CrossOrigin
	@RequestMapping(value="/getClassInfo", method=RequestMethod.POST)
	public JSONObject getClassInfo(@RequestBody OntologyInfoClassRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet();;
		
		try{
			SparqlConnection conn = requestBody.buildSparqlConnection();
			OntologyClass oClass = requestBody.buildClass();
			OntologyInfo oInfo = oInfoCache.get(conn);
			
			if (oInfo.getClass(oClass.getName()) == null) {
				retval.setSuccess(false);
				retval.addRationaleMessage("/getClassInfo", "Class does not exist in model: " + oClass.getName());
				
			} else {
				ArrayList<OntologyProperty> props = oInfo.getInheritedProperties(oClass);
				JSONArray retArray = new JSONArray();
				for (OntologyProperty p : props) {
					JSONObject propJson = new JSONObject();
					
					propJson.put("domain", p.getNameStr());
					
					// Range can theoretically be complex (list of 'types')
					// This will/should eventually be supported by SemTK too
					// so make Range an array to prevent future backwards-compatibility issues
					JSONArray rangeArr = new JSONArray();
					rangeArr.add(p.getRangeStr());
					propJson.put("range", rangeArr);
					
					retArray.add(propJson);
				}

				JSONObject retObj = new JSONObject();
				retObj.put("name", oClass.getName());
				retObj.put("properties", retArray);

				retval.addResult("classInfo", retObj);
				retval.setSuccess(true);
			}
		}
		catch(Exception e){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getClassInfo", e);
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
			SparqlConnection conn = requestBody.buildSparqlConnection();
			
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
		    value= "Get 'obj class prop str' table for all strings in the data interface graphs",
		    notes= "TESTING ONLY.  <br>" +
		           "This needs to be async <br> " +
		           "and handle results > 50000 gracefully"
		)
	@CrossOrigin
	@RequestMapping(value="/getStringTable", method=RequestMethod.POST)
	public JSONObject getStringTable(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		final String ENDPOINT_NAME = "getStringTable";
		HeadersManager.setHeaders(headers);
		TableResultSet retval = new TableResultSet();
		
		try{
			SparqlConnection conn = requestBody.buildSparqlConnection();
			SparqlEndpointInterface querySei = conn.getDefaultQueryInterface();
			ArrayList<SparqlEndpointInterface> dataSeiList = conn.getDataInterfaces();
			
			String sparql = "select distinct ?obj ?class ?prop ?str";
			for (SparqlEndpointInterface sei : dataSeiList) {
				sparql += " FROM <" + sei.getGraph() + ">";
			}
			sparql += "where {\r\n" + 
					"	filter (isLiteral(?str)).\r\n" + 
					"    ?obj ?prop ?str.\n" + 
					"    ?obj a ?class.\n" + 
					"}";
			Table t = querySei.executeQueryToTable(sparql);
			
			retval.addResults(t);
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
			SparqlConnection conn = requestBody.buildSparqlConnection();
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
	@RequestMapping(value="/uncacheChangedModel", method=RequestMethod.POST)
	public JSONObject uncacheChangedModel(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "uncacheChangedModel";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {			
			SparqlConnection conn = requestBody.buildSparqlConnection();
			
			oInfoCache.removeSimilar(conn);
			// Also remove any predicate stats that have this model
			conn.clearDataInterfaces();
			predStatsCache.removeSimilar(conn);
			
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
			value="Un-cache anything based on any dataset in the given connection."
			)
	@CrossOrigin
	@RequestMapping(value="/uncacheChangedConn", method=RequestMethod.POST)
	public JSONObject uncacheChangedConn(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "uncacheChangedConn";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {			
			SparqlConnection conn = requestBody.buildSparqlConnection();
			
			oInfoCache.removeSimilar(conn);
			predStatsCache.removeSimilar(conn);
			
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
	@RequestMapping(value="/uncacheOntology", method=RequestMethod.POST)
	public JSONObject uncacheOntology(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "uncacheChangedModel";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {			
			SparqlConnection conn = requestBody.buildSparqlConnection();
			
			oInfoCache.remove(conn);
			
			// Also remove any predicate stats that have this model
			conn.clearDataInterfaces();
			predStatsCache.removeSimilar(conn);
			
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
			value="Get predicate stats for a given connections ontology and data."
			)
	@CrossOrigin
	@RequestMapping(value="/getPredicateStats", method=RequestMethod.POST)
	public JSONObject uncacheOntology(@RequestBody SparqlConnectionRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "getPredicateStats";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {			
			SparqlConnection conn = requestBody.buildSparqlConnection();
			
			OntologyInfo oInfo = oInfoCache.get(conn);
			PredicateStats stats = new PredicateStats(conn, oInfo);
			retval.addResult("predicateStats", stats.toJson());
			
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
