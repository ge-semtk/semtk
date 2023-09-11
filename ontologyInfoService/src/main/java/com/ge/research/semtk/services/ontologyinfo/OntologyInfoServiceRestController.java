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
import java.util.concurrent.CompletableFuture;

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
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.ontologyTools.DataDictionaryGenerator;
import com.ge.research.semtk.ontologyTools.InstanceDictGenerator;
import com.ge.research.semtk.ontologyTools.RestrictionChecker;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyInfoCache;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.ontologyTools.PredicateStats;
import com.ge.research.semtk.ontologyTools.PredicateStatsCache;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.ontologyinfo.requests.CardinalityReportRequest;
import com.ge.research.semtk.services.ontologyinfo.requests.InstanceDictionaryRequest;
import com.ge.research.semtk.services.ontologyinfo.requests.OntologyInfoClassRequestBody;
import com.ge.research.semtk.services.ontologyinfo.requests.OntologyInfoRequestBody;
import com.ge.research.semtk.services.ontologyinfo.requests.SparqlConnectionRequestBody;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthorizationProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.LoggingProperties;
import com.ge.research.semtk.springutillib.properties.OntologyInfoServiceProperties;
import com.ge.research.semtk.springutillib.properties.QueryServiceProperties;
import com.ge.research.semtk.springutillib.properties.ResultsServiceProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.v3.oas.annotations.Operation;

@CrossOrigin
@RestController
@RequestMapping("/ontologyinfo")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class OntologyInfoServiceRestController {
 	static final String SERVICE_NAME = "ontologyInfoService";

 	static final int MINUTE = 1000 * 60;
 	static final int HOUR = MINUTE * 60;
 	
 	OntologyInfoCache oInfoCache = new OntologyInfoCache(HOUR * 2); 
 	PredicateStatsCache predStatsCache = new PredicateStatsCache(HOUR * 24);   
 	
	@Autowired
	private LoggingProperties log_prop;
	@Autowired 
	private QueryServiceProperties query_prop;
	@Autowired
	private ResultsServiceProperties results_props;
	@Autowired
	private ServicesGraphProperties servicesgraph_props;
	@Autowired 
	private ApplicationContext appContext;
	@Autowired
	private AuthorizationProperties auth_prop;

	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);
		query_prop.validateWithExit();
		results_props.validateWithExit();
		servicesgraph_props.validateWithExit();
		log_prop.validateWithExit();
	}
	
	/**
	 * Get a tabular data dictionary report.
	 */
	@SuppressWarnings("deprecation") // This uses a deprecated request body for backwards-compatibility
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
	
	@Operation(
		    summary= "Experimental"
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
	@Operation(
		    summary= "Experimental"
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

	
	@SuppressWarnings("unchecked")
	@Operation(
		    summary= "Get all data and object properties of a class, including those inherited from superclasses.",
		    description= "Returns classInfo = { name : 'classname', properties = [ { domain : 'http://model#hasTree', range: ['http://model#Tree'] } ]"
		)
	@CrossOrigin
	@RequestMapping(value="/getClassInfo", method=RequestMethod.POST)
	public JSONObject getClassInfo(@RequestBody OntologyInfoClassRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet();;
		
		try{
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
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
					OntologyRange oRange = p.getRange(oClass, oInfo);
					rangeArr.addAll(oRange.getUriList());
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
	
	@Operation(
		    summary= "Get a table of:  type (class|property), uri, label",
		    description= "If a URI has no labels, it will appear with label = \"\""
		)
	@CrossOrigin
	@RequestMapping(value="/getUriLabelTable", method=RequestMethod.POST)
	public JSONObject getUriLabelTable(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		final String ENDPOINT_NAME = "getUriLabelTable";
		HeadersManager.setHeaders(headers);
		TableResultSet retval = new TableResultSet();
		
		try{
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
			
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
	
	@Operation(
		    summary= "Get 'obj class prop str' table for all strings in the data interface graphs",
		    description= "TESTING ONLY.  <br>" +
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
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
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
	
	@Operation(
		    summary= "Get the oInfo JSON",
		    description= "This is the 'main' endpoint for this service.  Synchronous result: 'ontologyInfo'"
		)
	@CrossOrigin
	@RequestMapping(value="/getOntologyInfoJson", method=RequestMethod.POST)
	public JSONObject getOntologyInfoJson(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
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
	
	@Operation(
			summary="Un-cache anything based on any dataset in the given connection."
			)	
	@CrossOrigin
	@RequestMapping(value="/uncacheChangedConn", method=RequestMethod.POST)
	public JSONObject uncacheChangedConn(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "uncacheChangedConn";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {			
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
			
			oInfoCache.removeOverlapping(conn);
			predStatsCache.removeOverlapping(conn);
			
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@Operation(
			summary="Un-cache the ontology loaded by given connection.",
			description="For very specific UI operations only.<p>Generally, use <i>/uncacheChangedModel</i> instead."
			)
	@CrossOrigin
	@RequestMapping(value="/uncacheOntology", method=RequestMethod.POST)
	public JSONObject uncacheOntology(@RequestBody OntologyInfoRequestBody requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "uncacheChangedModel";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {			
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
			
			oInfoCache.remove(conn);
			
			// Also remove any predicate stats that have this model
			conn.clearDataInterfaces();
			predStatsCache.removeOverlapping(conn);
			
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@Operation(
			summary="Get predicate stats for a given connections ontology and data.",
			description="Async.  Returns a jobID.  If stats are in the cache, job will already be complete."
			)
	@CrossOrigin
	@RequestMapping(value="/getPredicateStats", method=RequestMethod.POST)
	public JSONObject getPredicateStats(@RequestBody SparqlConnectionRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "getPredicateStats";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {		
			String jobId = JobTracker.generateJobId();
			JobTracker tracker = new JobTracker(servicesgraph_props.buildSei());
			ResultsClient rclient = results_props.getClient();
			
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
			PredicateStats stats = this.predStatsCache.getIfCached(conn);
			tracker.createJob(jobId);
			
			if (stats != null) {
				// already cached:  send to results service and complete job
				rclient.execStoreBlobResults(jobId, stats.toJson());
				tracker.setJobSuccess(jobId);
			
			} else {
				// spin up an async thread
				new Thread(() -> {
					try {
						HeadersManager.setHeaders(headers);
						// since getIfCached() failed, presume this get() will lead to creating a new one
						PredicateStats newStats = this.predStatsCache.get(conn, this.oInfoCache.get(conn), tracker, jobId, 1, 99);
						
						rclient.execStoreBlobResults(jobId, newStats.toJson());
						tracker.setJobSuccess(jobId);
						
					} catch (Exception e) {
						try {
							tracker.setJobFailure(jobId, e.getMessage());
						} catch (Exception ee) {
							LocalLogger.logToStdErr(ENDPOINT_NAME + " error accessing job tracker");
							LocalLogger.printStackTrace(ee);
						}
					}
				}).start();
			}
			
			retval.addJobId(jobId);
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@Operation(
			summary="Check for violations of cardinality restrictions.",
			description="Async.  Returns a jobID."
			)
	@CrossOrigin
	@RequestMapping(value="/getCardinalityViolations", method=RequestMethod.POST)
	public JSONObject getCardinalityViolations(@RequestBody CardinalityReportRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "getCardinalityViolations";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {		
			String jobId = JobTracker.generateJobId();
			JobTracker tracker = new JobTracker(servicesgraph_props.buildSei());
			ResultsClient rclient = results_props.getClient();
			
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
			tracker.createJob(jobId);
			
			// spin up an async thread
			new Thread(() -> {
				try {
					HeadersManager.setHeaders(headers);
					
					OntologyInfo oInfo = oInfoCache.get(conn);
					RestrictionChecker checker = new RestrictionChecker(conn, oInfo, tracker, jobId, 1, 99);
					Table table = checker.getCardinalityViolations(requestBody.getMaxRows(), requestBody.getConciseFormat());
					
					rclient.execStoreTableResults(jobId, table);
					tracker.setJobSuccess(jobId);
					
				} catch (Exception e) {
					try {
						tracker.setJobFailure(jobId, e.getMessage());
					} catch (Exception ee) {
						LocalLogger.logToStdErr(ENDPOINT_NAME + " error accessing job tracker");
						LocalLogger.printStackTrace(ee);
					}
				}
			}).start();
			
			retval.addJobId(jobId);
			retval.addResultType(SparqlResultTypes.TABLE);
			retval.setSuccess(true);
		} catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@Operation(
			summary="Get predicate stats for a given connections ontology and data.",
			description="Sync.   if stats are not currently cached returns cached: 'none', else 'predicateStats': json"
			)
	@CrossOrigin
	@RequestMapping(value="/getCachedPredicateStats", method=RequestMethod.POST)
	public JSONObject getCachedPredicateStats(@RequestBody SparqlConnectionRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "getCachedPredicateStats";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {		
		
			
			SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
			PredicateStats stats = this.predStatsCache.getIfCached(conn);
			
			if (stats != null) {
				retval.addResult("cached", "true");
				retval.addResult("predicateStats", stats.toJson());
				retval.setSuccess(true);
			
			} else {
				retval.addResult("cached", "false");
				retval.setSuccess(false);
			}
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	/**
	 * Build a dictionary of identifiers:  GUID, type(s), string
	 */
	@Operation(
			summary="Get table of URIs and string identifiers associated with them.",
			description="Async.  Returns a jobID."
			)
	@CrossOrigin
	@RequestMapping(value="/getInstanceDictionary", method= RequestMethod.POST)
	public JSONObject getInstanceDictionary(@RequestBody InstanceDictionaryRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "getInstanceDictionary";
		SimpleResultSet res = new SimpleResultSet(false);
    	
		try {	
			// setup job tracker and results client
			String jobId = JobTracker.generateJobId();
			JobTracker tracker = new JobTracker(servicesgraph_props.buildSei());
			tracker.createJob(jobId);
			ResultsClient rclient = results_props.getClient();
			
			// spin up an async thread
			new Thread(() -> {
				try {
					HeadersManager.setHeaders(headers);
					
					SparqlConnection conn = query_prop.setUserAndPasswordIfMissing(requestBody.buildSparqlConnection());
					OntologyInfo oInfo = oInfoCache.get(conn);
					InstanceDictGenerator generator = new InstanceDictGenerator(conn, oInfo, requestBody.getMaxWords(), requestBody.getSpecificityLimit());
					Table tab = generator.generate();
					
					rclient.execStoreTableResults(jobId, tab);
					tracker.setJobSuccess(jobId);
					
				} catch (Exception e) {
					try {
						tracker.setJobFailure(jobId, e.getMessage());
					} catch (Exception ee) {
						LocalLogger.logToStdErr(ENDPOINT_NAME + " error accessing job tracker");
						LocalLogger.printStackTrace(ee);
					}
				}
			}).start();
			
			res.addJobId(jobId);
			res.addResultType(SparqlResultTypes.TABLE);
			res.setSuccess(true);

		} catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
	    	LocalLogger.printStackTrace(e);
		}
		return res.toJson();	
	}
}
