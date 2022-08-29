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


package com.ge.research.semtk.services.sparql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.OntologyInfoClientConfig;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.sparql.requests.SparqlAuthRequestBody;
import com.ge.research.semtk.services.sparql.requests.SparqlParallelQueryRequestBody;
import com.ge.research.semtk.services.sparql.requests.SparqlPrefixAuthRequestBody;
import com.ge.research.semtk.services.sparql.requests.SparqlPrefixesAuthRequestBody;
import com.ge.research.semtk.services.sparql.requests.SparqlQueryAuthRequestBody;
import com.ge.research.semtk.services.sparql.requests.SparqlQueryRequestBody;
import com.ge.research.semtk.services.sparql.requests.SparqlRequestBody;
import com.ge.research.semtk.sparqlX.NeptuneSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.sparqlX.parallel.SparqlParallelQueries;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Service to execute SPARQL queries.
 * All Synchronous.
 * Async wrappers are found in the Dispatch and NodeGroupExecution servers, et. al.
 * 
 * 
 * Sample select query response:
 * {"message":"operations succeeded.","status":"success","table":{"@table":{"col_names":["number"],"rows":[["1272"],["1274"],["1276"],["1277"],["1278"],["1279"],["1280"],["1281"],["1283"],["1284"]],"col_type":["http://www.w3.org/2001/XMLSchema#integer"],"col_count":1,"row_count":10}}}
 *
 * Sample construct query response:
 * {"message":"operations succeeded.","status":"success","NodeGroup":{"@graph":[{"@type":[{"@id":"http:\/\/research.ge.com\/configuration#TestType"}],"@id":"http:\/\/research.ge.com\/configuration#Test"},{"@type":[{"@id":"http:\/\/research.ge.com\/configuration#Request"}]...
 * 
 * Sample auth query response:
 * {"message":"operations succeeded.","status":"success","simpleresults":{"@message":"Insert into <http://research.ge.com/sandbox>, 1 (or less) triples -- done"}}
 *
 * Sample error response:
 * {"message":"operations failed.","rationale":"This query must be run using the /queryAuth endpoint","status":"failed"}
 *
 * 
 *  Note:  all endpoints that change data contact oinfo to clear the cache.
 */
@RestController
@RequestMapping("/sparqlQueryService")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class SparqlQueryServiceRestController {			
 	static final String SERVICE_NAME = "sparqlQueryService";
	
	@CrossOrigin
	@RequestMapping(value= "/**", method=RequestMethod.OPTIONS)
	public void corsHeaders(HttpServletResponse response) {
	    response.addHeader("Access-Control-Allow-Origin", "*");
	    response.addHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
	    response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, x-requested-with");
	    response.addHeader("Access-Control-Max-Age", "3600");
	}
	@Autowired
	OInfoServiceProperties oinfo_props;
	@Autowired
	private QueryUploadNeptuneProperties serviceProps; 
	@Autowired
	private AuthProperties auth_prop; 
	@Autowired 
	private ApplicationContext appContext;
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		
		oinfo_props.validateWithExit();
		serviceProps.validateWithExit();
		auth_prop.validateWithExit();
		
		AuthorizationManager.authorizeWithExit(auth_prop);

	}
	
	/**
	 * Execute (non-auth) query 
	 */
	@CrossOrigin
	@RequestMapping(value="/query", method= RequestMethod.POST)
	public JSONObject query(@RequestBody SparqlQueryRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);	
		
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start query");
		long startTime = System.nanoTime();

		try{
			
			
			// disallow running drop graph query here (keep this check first, before the subsequent check)
			if(SparqlQueryServiceRestController.isDropGraphQuery(requestBody.query)){ 
				SimpleResultSet res = new SimpleResultSet(false);
				res.addRationaleMessage(SERVICE_NAME, "query", "This query must be run using the /dropGraph endpoint");
				return res.toJson();
			}
			// disallow running auth queries here (they'll get rejected later anyway)
			if(SparqlResultTypes.valueOf(requestBody.resultType) == SparqlResultTypes.CONFIRM){ 
				SimpleResultSet res = new SimpleResultSet(false);
				res.addRationaleMessage(SERVICE_NAME, "query", "A query expecting resultType " + requestBody.resultType + " must be run using the /queryAuth endpoint");
				return res.toJson();
			}
		
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 			
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph());
			resultSet = sei.executeQueryAndBuildResultSet(requestBody.query, SparqlResultTypes.valueOf(requestBody.resultType));
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);	
			resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, "query", e);
		}  finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}
		
		// print elapsed time
		long endTime = System.nanoTime();
		double elapsed = ((endTime - startTime) / 1000000000.0);
		LocalLogger.logToStdOut(String.format("Query time: %.2f sec", elapsed));
			
		return resultSet.toJson();
		
	}		
	
	
	@CrossOrigin
	@RequestMapping(value="/selectGraphNames", method= RequestMethod.POST)
	public JSONObject selectGraphNames(@RequestBody SparqlRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);	
		final String ENDPOINT_NAME = "/selectGraphNames";
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start query");
		long startTime = System.nanoTime();

		try{
		
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 	
			String query = SparqlToXUtils.generateSelectGraphNames();
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph());
			resultSet = sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
			
			// add default graph name
			Table tab = ((TableResultSet) resultSet).getResults();
			tab.addRow(0, new String [] {sei.getDefaultGraphName()});
			((TableResultSet) resultSet).addResults(tab);
			
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);	
			resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
		}  finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}
		
		// print elapsed time
		long endTime = System.nanoTime();
		double elapsed = ((endTime - startTime) / 1000000000.0);
		LocalLogger.logToStdOut(String.format("Query time: %.2f sec", elapsed));
			
		return resultSet.toJson();
		
	}		
	
	/**
	 * Execute auth query 
	 */
	@CrossOrigin
	@RequestMapping(value="/queryAuth", method= RequestMethod.POST)
	public JSONObject queryAuth(@RequestBody SparqlQueryAuthRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		
		
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start queryAuth");

		try{
			// disallow running drop graph query here - require client to explicitly use /dropGraph to avoid accidental drops
			if(SparqlQueryServiceRestController.isDropGraphQuery(requestBody.query)){ 
				return (new SimpleResultSet(false, "This query must be run using the /dropGraph endpoint")).toJson();
			}
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 		
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph(), requestBody.getUser(), requestBody.getPassword());	
			resultSet = sei.executeQueryAndBuildResultSet(requestBody.query, SparqlResultTypes.valueOf(requestBody.resultType));
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);
			resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, "queryAuth", e);
			return resultSet.toJson();
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		LocalLogger.logToStdOut("Result code:" + resultSet.getResultCodeString());
		return resultSet.toJson();
	}	
	
	
	/**
	 * Execute drop graph query 
	 */
	@CrossOrigin
	@RequestMapping(value="/dropGraph", method= RequestMethod.POST)
	public JSONObject dropGraph(@RequestBody SparqlAuthRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start dropGraph");

		try {			
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 		
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph(), requestBody.getUser(), requestBody.getPassword());	
			uncache(sei);
			String dropGraphQuery = SparqlToXUtils.generateDropGraphSparql(sei);
			resultSet = sei.executeQueryAndBuildResultSet(dropGraphQuery, SparqlResultTypes.CONFIRM);
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);	
			resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, "dropGraph", e);
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		
		return resultSet.toJson();
	}	
	
	
	/**
	 * Executes multiple subqueries in parallel and fuses the results.  
	 * Returns a table result set in JSON format.
	 * 
	 * subqueriesJson		JSON containing triple store connection and subqueries
	 * subqueryType			"select" supported for now, perhaps others in the future
	 * isSubqueryOptional	true to return records from one subquery result that do not match another subquery result
	 * columnsToFuseOn		columns to merge subquery results, comma-separated
	 * columnsToReturn		columns to return from the overall query, comma-separated
	 */
	@CrossOrigin
	@RequestMapping(value="/parallelQuery", method= RequestMethod.POST)
	public JSONObject parallelQuery(@RequestBody SparqlParallelQueryRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);

		LocalLogger.logToStdOut("Sparql Query Service start parallelQuery");

		try{
			SparqlParallelQueries spq = new SparqlParallelQueries (requestBody.subqueriesJson, requestBody.subqueryType, requestBody.isSubqueryOptional, requestBody.columnsToFuseOn, requestBody.columnsToReturn);
			spq.runQueries ();
			JSONObject tableResultSetJSON = spq.returnFusedResults();
			return tableResultSetJSON;			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);	
			GeneralResultSet resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, "parallelQuery", e);
			return resultSet.toJson();
		} catch (Throwable e) {
			LocalLogger.printStackTrace(e);	
			GeneralResultSet resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, "parallelQuery", "Throwable: " + e.getMessage());
			return resultSet.toJson();
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}
	}	

	/**
	 * Execute auth query 
	 */
	@CrossOrigin
	@RequestMapping(value="/clearPrefix", method= RequestMethod.POST)
	public JSONObject clearPrefix(@RequestBody SparqlPrefixAuthRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		
		
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start clearPrefix");
		
		String query = "";
		try{
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 	
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph(), requestBody.getUser(), requestBody.getPassword());	
			query = SparqlToXUtils.generateDeletePrefixQuery(sei, requestBody.prefix);
			resultSet = sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
			uncache(sei);
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);
			resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, "clearPrefix", e);
			return resultSet.toJson();
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}	
		
		return resultSet.toJson();
	}	
	
	/**
	 * Removes model with given uri prefix(es)
	 * Practically: it removes ?s ?o ?p where ?s starts with one of the uri prefixes
	 * NOTE: leaves any SADL blank nodes
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/clearModelPartial", method= RequestMethod.POST)
	public JSONObject clearModelPartial(@RequestBody SparqlPrefixesAuthRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		return this.clearModel(requestBody, "clearModelPartial", false);
	}
	
	/**
	 * Removes model with given uri prefix(es), and all blank nodes
	 * Practically: it removes ?s ?o ?p where ?s starts with one of the uri prefixes or the blank node prefix
	 * NOTE: If you're not sure about removing blank nodes, use "/clearModelPartial"
	 * @param requestBody
	 * @return
	 */
	@CrossOrigin
	@RequestMapping(value="/clearModelFull", method= RequestMethod.POST)
	public JSONObject clearModelFull(@RequestBody SparqlPrefixesAuthRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		return this.clearModel(requestBody, "clearModelPartial", true);
	}
	
	public JSONObject clearModel(SparqlPrefixesAuthRequestBody requestBody, String endpointName, Boolean deleteBlankNodes){

		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start " + endpointName);
		
		try{
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 	
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph(), requestBody.getUser(), requestBody.getPassword());	
			uncache(sei);
			String query = SparqlToXUtils.generateDeleteModelTriplesQuery(sei, requestBody.prefixes, deleteBlankNodes);
			resultSet = sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);
			resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, endpointName, e);
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}	
		
		return resultSet.toJson();
	}	
	
	
	/**
	 * Execute clear all query 
	 */
	@CrossOrigin
	@RequestMapping(value={"/clearAll", "/clearGraph"}, method= RequestMethod.POST)
	public JSONObject clearAll(@RequestBody SparqlAuthRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start clearAll");
		
		try {			
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 		
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph(), requestBody.getUser(), requestBody.getPassword());	
			sei.clearGraph();
			uncache(sei);
			resultSet = new SimpleResultSet(true);
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);
			resultSet = new SimpleResultSet(false);
			resultSet.addRationaleMessage(SERVICE_NAME, "clearAll", e);
			return resultSet.toJson();
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		
		return resultSet.toJson();
	}	
	
	/**
	 * get <rdf>....</rdf> dump of a graph
	 * or <error>message</error>
	 */
	@Operation(
			description="Deprecated previous /downloadOwl. <br> Could fail with large files."
			)
	@CrossOrigin
	@RequestMapping(value={"/downloadOwl"}, method= RequestMethod.POST, produces="rdf/xml")
	public String downloadOwl(@RequestBody SparqlAuthRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start downloadOwl");
		
		try {			
			requestBody.validate(); 	// check inputs 		
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph(), requestBody.getUser(), requestBody.getPassword());	
			return sei.downloadOwl();
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);
			return "<error>\n\t" + e.toString() + "\n</error>";
			
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		
	}

// TODO: this version is still breaking pipes somewhere
//	@Operation(
//			description="Download owl with streaming instead of result set.  Better for large files."
//			)
//	@CrossOrigin
//	@RequestMapping(value={"/downloadOwlStreamed"}, method= RequestMethod.POST, produces="rdf/xml")
	public void downloadOwlStreamed(@RequestBody SparqlAuthRequestBody requestBody, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start downloadOwlStreamed");
		
		try {			
			requestBody.validate(); 	// check inputs 		
			sei = SparqlEndpointInterface.getInstance(requestBody.getServerType(), requestBody.getServerAndPort(), requestBody.getGraph(), requestBody.getUser(), requestBody.getPassword());	
			sei.downloadOwlStreamed(resp.getWriter());
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);
			try {
				resp.getWriter().print("<error>\n\t" + e.toString() + "\n</error>");
			} catch (Exception ee) {
				LocalLogger.printStackTrace(ee);
			}
			
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
	}	
	
	//public JSONObject uploadOwl(@RequestBody SparqlAuthRequestBody requestBody, @RequestParam("owlFile") MultipartFile owlFile){
    // We can't use a @RequestBody with a @RequestParam,
	// So the SparqlAuthRequestBody is broken into individual string @RequestParams
	@Operation(
			description="Upload owl to specified connection information"
			)
	@CrossOrigin
	@RequestMapping(value="/uploadOwl", method= RequestMethod.POST)
	public JSONObject uploadOwl(@RequestParam("serverAndPort") String serverAndPort, 
								@RequestParam("serverType") String serverType, 
								@RequestParam(value="dataset", required=false) String dataset, // deprecated in favor of graph
								@RequestParam(value="graph", required=false) String graph, 
								@RequestParam("user") String user, 
								@RequestParam("password") String password, 
								@RequestParam("owlFile") MultipartFile owlFile, 
								@RequestHeader HttpHeaders headers) {
		
		return this.uploadFile("uploadTurtle", serverAndPort, serverType, (dataset!=null)?dataset:graph, user, password, owlFile, headers);
	}
	
	
	@Operation(
			description="Upload turtle file to specified connection information"
			)
	@CrossOrigin
	@RequestMapping(value="/uploadTurtle", method= RequestMethod.POST)
	public JSONObject uploadTurtle(@RequestParam(value="serverAndPort", required=true) String serverAndPort, 
								@RequestParam(value="serverType", required=true) String serverType, 
								@RequestParam(value="graph", required=true) String graph, 
								@RequestParam(value="user", required=false) String user, 
								@RequestParam(value="password", required=false) String password, 
								@RequestParam(value="ttlFile", required=true) MultipartFile ttlFile, 
								@RequestHeader HttpHeaders headers) {
		return this.uploadFile("uploadTurtle", serverAndPort, serverType, graph, user, password, ttlFile, headers);
	}	
	
	private JSONObject uploadFile(String endpointName,
								String serverAndPort, 
								String serverType, 
								String graph, 
								String user, 
								String password, 
								MultipartFile multiFile, 
								HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		
		SimpleResultSet resultSet = null;
		JSONObject simpleResultSetJson = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start uploadTurtle");
		
		
		try {	
			if (serverAndPort == null || serverAndPort.trim().isEmpty() ) throw new Exception("serverAndPort is empty.");
			if (serverType == null || serverType.trim().isEmpty() ) throw new Exception("serverType is empty.");

			sei = SparqlEndpointInterface.getInstance(serverType, serverAndPort, graph, user, password);
			
			if (sei instanceof NeptuneSparqlEndpointInterface) {
				// S3 bucket is option.  It can be filled with blanks and nulls
				((NeptuneSparqlEndpointInterface)sei).setS3Config(
						serviceProps.getS3ClientRegion(),
						serviceProps.getS3BucketName(), 
						serviceProps.getAwsIamRoleArn());
			}
			
			if (! sei.isAuth()) { 
				throw new Exception("Required SPARQL endpoint user/password weren't provided.");
			}
			
			simpleResultSetJson = sei.executeAuthUploadStreamed(multiFile.getInputStream(), multiFile.getName());
			// previous non-streaming code:
			// simpleResultSetJson = sei.executeAuthUploadOwl(ttlFile.getBytes());
			
			SimpleResultSet sResult = SimpleResultSet.fromJson(simpleResultSetJson);
			if (sResult.getSuccess()) {
				uncache(sei);
			}
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);
			resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, endpointName, e);
			return resultSet.toJson();
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		
		return simpleResultSetJson;	
	}	
	
	@Operation(
			description="Using graph in owl's rdf:RDF xml:base, clear graph and (re-)import owl" 
			)
	@CrossOrigin
	@RequestMapping(value="/syncOwl", method= RequestMethod.POST)
	public JSONObject syncOwl(@RequestParam(  required=true,  name="serverAndPort") String serverAndPort, 
								@RequestParam(required=true,  name="serverType") String serverType, 
								@RequestParam(required=false, name="user") String user, 
								@RequestParam(required=false, name="password") String password, 
								@RequestParam(required=true,  name="owlFile") MultipartFile owlFile, 
								@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "syncOwl";
		SimpleResultSet resultSet = null;
		JSONObject simpleResultSetJson = null;
		SparqlEndpointInterface sei = null;
		LocalLogger.logToStdOut("Sparql Query Service start syncOwl");

		
		try {	
			// monkey around with optional stuff
			if (serverAndPort == null || serverAndPort.trim().isEmpty() ) throw new Exception("serverAndPort is empty.");
			if (serverType == null || serverType.trim().isEmpty() ) throw new Exception("serverType is empty.");
			
			// get graphName from owl
			byte [] owlBytes = owlFile.getBytes();
			String graphName = Utility.getXmlBaseFromOwlRdf(new ByteArrayInputStream(owlBytes));
			sei = SparqlEndpointInterface.getInstance(serverType, serverAndPort, graphName, user, password);
						
			if (sei instanceof NeptuneSparqlEndpointInterface) {

				((NeptuneSparqlEndpointInterface)sei).setS3Config(
						serviceProps.getS3ClientRegion(),
						serviceProps.getS3BucketName(), 
						serviceProps.getAwsIamRoleArn());
			}
			
			if (! sei.isAuth()) { 
				throw new Exception("Required SPARQL endpoint user/password weren't provided.");
			}
			
			// clear the graph, upload owl, uncache the ontology
			sei.clearGraph();
			simpleResultSetJson = sei.executeAuthUploadStreamed(owlFile.getInputStream(), owlFile.getName());
			// previous non-streaming code:
			// simpleResultSetJson = sei.executeAuthUploadOwl(owlFile.getBytes());
			uncache(sei);
			
		} catch (Exception e) {			
			LocalLogger.printStackTrace(e);
			resultSet = new SimpleResultSet();
			resultSet.setSuccess(false);
			resultSet.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
			return resultSet.toJson();
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		
		return simpleResultSetJson;	
	}	
	 
	/**
	 * Remove ontology and anything that looks like it from SemTK cache
	 * @param sei
	 * @throws Exception
	 */
	private void uncache(SparqlEndpointInterface sei) throws Exception {
		OntologyInfoClient oClient = new OntologyInfoClient(new OntologyInfoClientConfig(oinfo_props.getProtocol(), oinfo_props.getServer(), oinfo_props.getPort()));
		SparqlConnection conn = new SparqlConnection();
		conn.addModelInterface(sei);
		oClient.uncacheChangedConn(conn);
	}
	
	/**
	 * Determines if a query is a DROP GRAPH query or not.
	 * e.g. clear graph <http://com.ge.research/knowledge/graph>
	 */
	private static boolean isDropGraphQuery(String query){
		return containsRegexIgnoreCase(query, "drop\\s+graph\\s+\\<");
	}


	/**
	 * Determine if a string contains a regular expression
	 */
	private static boolean containsRegexIgnoreCase(String s, String regex){
		s = s.toLowerCase().trim();		
		Pattern whitespace = Pattern.compile(regex);    
		Matcher matcher = whitespace.matcher(s);
		if (matcher.find()) {
			return true;
		}
		return false;
	}
}
