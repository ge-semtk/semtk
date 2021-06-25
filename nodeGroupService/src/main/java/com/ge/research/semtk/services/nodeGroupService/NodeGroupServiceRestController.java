/**
 ** Copyright 2016-18 General Electric Company
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

package com.ge.research.semtk.services.nodeGroupService;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.belmont.*;
import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.OntologyInfoClientConfig;
import com.ge.research.semtk.services.nodeGroupService.requests.*;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.springutilib.requests.NodegroupRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.load.utility.ImportSpec;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupService.SparqlIdReturnedTuple;
import com.ge.research.semtk.nodeGroupService.SparqlIdTuple;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.plotting.PlotSpec;
import com.ge.research.semtk.plotting.PlotSpecs;
import com.ge.research.semtk.plotting.PlotlyPlotSpec;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;
import com.google.gson.JsonArray;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/nodeGroup")
@CrossOrigin
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class NodeGroupServiceRestController {
 	static final String SERVICE_NAME = "nodeGroupService";

	public static final String QUERYFIELDLABEL = "SparqlQuery";
	public static final String QUERYTYPELABEL = "QueryType";
	public static final String INVALID_SPARQL_RATIONALE_LABEL = "InvalidSparqlRationale";

	public static final String RET_KEY_NODEGROUP = "nodegroup";
	public static final String RET_KEY_SPARQLID = "sparqlID";
		
	@Autowired
	OInfoServiceProperties oinfo_props;
	@Autowired
	private AuthProperties auth_prop;
	@Autowired 
	private ApplicationContext appContext;
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();

		// these are still in the older NodegroupExecutionServiceStartup
		
		oinfo_props.validateWithExit();
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);

	}
    //OntologyInfoCache oInfoCache = new OntologyInfoCache(5 * 60 * 1000);   // 5 seconds.  TODO make this a property

	@CrossOrigin
	@RequestMapping(value= "/**", method=RequestMethod.OPTIONS)
	public void corsHeaders(HttpServletResponse response) {
	    response.addHeader("Access-Control-Allow-Origin", "*");
	    response.addHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
	    response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, x-requested-with");
	    response.addHeader("Access-Control-Max-Age", "3600");
	}
	
	@ApiOperation(
			value="Generate a SELECT query",
			notes="Generic query with no special options."
			)
	@CrossOrigin
	@RequestMapping(value="/findAllPaths", method=RequestMethod.POST)
	public JSONObject generateSelectSparql(@RequestBody PathRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			// more efficient way than some of the others to get everything
			// note that ng should be inflated and contain model and data connections
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			SparqlConnection conn = sgJson.getSparqlConn();
			OntologyInfo oInfo = retrieveOInfo(conn);
			NodeGroup ng = sgJson.getNodeGroupNoInflateNorValidate(oInfo);
			ArrayList<String> classes = new ArrayList<String>();
			classes.addAll(ng.getNodeURIStrings());
			
			// TODO both flags
			ArrayList<OntologyPath> pathList = oInfo.findAllPaths(requestBody.getAddClass(), classes);
			
			retval = new SimpleResultSet(true);
			JSONArray pathJsonArr = new JSONArray();
			for (OntologyPath p: pathList) {
				pathJsonArr.add(p.toJson());
			}
			retval.addResult("pathList", pathJsonArr);
			
		}
		catch(Exception e){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "findAllPaths", e);
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();
	}
	
	/*
	 * generate** endpoints return a SimpleResultSet
	 *     failure - unexpected exception 
	 *     		rationale - the exception message
	 *     no valid sparql - succeeds
	 *         SparqlQuery - is a short SPARQL comment
	 *         QueryType - "INVALID"
	 *     success -
	 *       	SparqlQuery - the SPARQL
	 *          QueryType - same "SELECT" "COUNT_ALL" "DELETE" "FILTER" "ASK" "CONSTRUCT"
	 */
	@ApiOperation(
			value="Generate a SELECT query",
			notes="Generic query with no special options."
			)
	@CrossOrigin
	@RequestMapping(value="/generateSelect", method=RequestMethod.POST)
	public JSONObject generateSelectSparql(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = requestBody.buildNodeGroup();
			ng.noInflateNorValidate(retrieveOInfo(requestBody.buildConnection()));

			String query = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, null, null);
			
			retval = this.generateSuccessOutput("SELECT", query);
			
		}
		catch(NoValidSparqlException ise) {
			retval = this.generateNoValidSparqlOutput("generateSelect", ise.getMessage());
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "generateSelect", eee);
			LocalLogger.printStackTrace(eee);
		}
		
		return retval.toJson();
	}
	
	@ApiOperation(
			value="Generate a COUNT query",
			notes="Generic query with no special options"
			)
	@CrossOrigin
	@RequestMapping(value="/generateCountAll", method=RequestMethod.POST)
	public JSONObject generateCountAllSparql(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = requestBody.buildNodeGroup();
			ng.noInflateNorValidate(retrieveOInfo(requestBody.buildConnection()));

			String query = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_COUNT, false, null, null);
			
			retval = this.generateSuccessOutput("COUNT_ALL", query);
			
		}
		catch(NoValidSparqlException ise) {
			retval = this.generateNoValidSparqlOutput("generateCountAll", ise.getMessage());
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "generateCountAll", eee);
			LocalLogger.printStackTrace(eee);
		}
		
		return retval.toJson();
	}

	@ApiOperation(
			value="Generate DELETE query"
			)
	@CrossOrigin
	@RequestMapping(value="/generateDelete", method=RequestMethod.POST)
	public JSONObject generateDeleteSparql(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = requestBody.buildNodeGroup();
			ng.noInflateNorValidate(retrieveOInfo(requestBody.buildConnection()));

			String query = ng.generateSparqlDelete(null);
			
			retval = this.generateSuccessOutput("DELETE", query);
			
		}
		catch(NoValidSparqlException ise) {
			retval = this.generateNoValidSparqlOutput("generateDelete", ise.getMessage());
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "generateDelete", eee);
			LocalLogger.printStackTrace(eee);
		}
		
		return retval.toJson();
	}

	@ApiOperation(
			value="Generate filter query",
			notes="Returns all values for a given sparqlId"
			)
	@CrossOrigin
	@RequestMapping(value="/generateFilter", method=RequestMethod.POST)
	public JSONObject generateFilterSparql(@RequestBody NodegroupSparqlIdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = requestBody.buildNodeGroup();
			ng.noInflateNorValidate(retrieveOInfo(requestBody.buildConnection()));

			Returnable item = ng.getNodeBySparqlID(requestBody.getSparqlID());
			if (item == null) {
				item = ng.getPropertyItemBySparqlID(requestBody.getSparqlID());
			}
			String query = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_CONSTRAINT, false, -1, item);
			
			retval = this.generateSuccessOutput("FILTER", query);
			
		}
		catch(NoValidSparqlException ise) {
			retval = this.generateNoValidSparqlOutput("generateFilter", ise.getMessage());
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "generateFilter", eee);
			LocalLogger.printStackTrace(eee);
		}
		
		return retval.toJson();
	}

	@ApiOperation(
			value="Generate ASK query"
			)
	@CrossOrigin
	@RequestMapping(value="/generateAsk", method=RequestMethod.POST)
	public JSONObject generateAskSparql(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = requestBody.buildNodeGroup();
			ng.noInflateNorValidate(retrieveOInfo(requestBody.buildConnection()));

			String query = ng.generateSparqlAsk();
			
			retval = this.generateSuccessOutput("ASK", query);
			
		}
		catch(NoValidSparqlException ise) {
			retval = this.generateNoValidSparqlOutput("generateAsk", ise.getMessage());
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "generateAsk", eee);
			LocalLogger.printStackTrace(eee);
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/generateConstructForInstanceManipulation", method=RequestMethod.POST)
	public JSONObject generateConstructSparqlForInstanceManipulation(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = requestBody.buildNodeGroup();
			ng.noInflateNorValidate(retrieveOInfo(requestBody.buildConnection()));

			String query = ng.generateSparqlConstruct(true);
			
			retval = this.generateSuccessOutput("CONSTRUCT", query);
			
		}
		catch(NoValidSparqlException ise) {
			retval = this.generateNoValidSparqlOutput("generateConstruct", ise.getMessage());
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "generateConstruct", eee);
			LocalLogger.printStackTrace(eee);
		}
		
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/generateConstruct", method=RequestMethod.POST)
	public JSONObject generateConstructSparql(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = null;
		
		try{
			NodeGroup ng = requestBody.buildNodeGroup();
			ng.noInflateNorValidate(retrieveOInfo(requestBody.buildConnection()));

			String query = ng.generateSparqlConstruct(false);
			
			retval = this.generateSuccessOutput("CONSTRUCT", query);
			
		}
		catch(NoValidSparqlException ise) {
			retval = this.generateNoValidSparqlOutput("generateConstruct", ise.getMessage());
		}
		catch(Exception eee){
			retval = new SimpleResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "generateConstruct", eee);
			LocalLogger.printStackTrace(eee);
		}
		
		return retval.toJson();
	}
	@ApiOperation(
			value="Get table of runtime constraints",
			notes="Returns table of: \"valueId\", \"itemType\", \"valueType\""
			)
	@CrossOrigin
	@RequestMapping(value="/getRuntimeConstraints", method=RequestMethod.POST)
	public JSONObject getRuntimeConstraints(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		TableResultSet retval = null;
		
		try{
			NodeGroup ng = requestBody.buildNodeGroup();
			RuntimeConstraintManager rtci = new RuntimeConstraintManager(ng);
			retval = new TableResultSet(true); 
			
			// TODO: it is awful that this returns a table of descriptions
			//       the RunTimeConstrainedItems needs fromJson but it has a nodegroup pointer
			//       Serious re-design may be needed
			retval.addResults(rtci.getConstrainedItemsDescription() );
		}
		catch(Exception eee){
			retval = new TableResultSet(false);
			retval.addRationaleMessage(SERVICE_NAME, "getRuntimeConstraints", eee);
			LocalLogger.printStackTrace(eee);
		}
		
		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Give a property a SparqlID and set isReturned.",
			notes="If it is illegal or duplicate, SparqlID will be modified to a close match."
			)
	@CrossOrigin
	@RequestMapping(value="/setPropertySparqlId", method=RequestMethod.POST)
	public JSONObject setAndReturnNewSparqlId(@RequestBody NodegroupPropertyRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);
		
		try{
			requestBody.validate();
			
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			NodeGroup ng = sgJson.getNodeGroup();
			
			
			Node snode = ng.getNodeBySparqlID(requestBody.getNodeSparqlId());
			if (snode == null) {
				throw new Exception("Can't find node with sparqlID: " + requestBody.getNodeSparqlId());
			}
			PropertyItem prop = snode.getPropertyByURIRelation(requestBody.getPropertyUri());
			if (prop == null) {
				throw new Exception("Can't find property: " + requestBody.getPropertyUri());
			}
			ng.changeSparqlID(prop, requestBody.getNewPropSparqlId());
			prop.setIsReturned(requestBody.isReturned());
			
			sgJson.setNodeGroup(ng);
			retval.addResult(RET_KEY_NODEGROUP, sgJson.toJson());
			retval.addResult(RET_KEY_SPARQLID, prop.getSparqlID());
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.addRationaleMessage(SERVICE_NAME, "setIsReturned", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Set isReturned for existing sparqlID(s)"
			)
	@CrossOrigin
	@RequestMapping(value="/setIsReturned", method=RequestMethod.POST)
	public JSONObject setReturnsBySparqlId(@RequestBody NodegroupSparqlIdReturnedRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);
		
		try{
			requestBody.validate();
			
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			NodeGroup ng = sgJson.getNodeGroup();
			
			for (SparqlIdReturnedTuple tuple : requestBody.getSparqlIdReturnedTuples()) {
				Returnable item = ng.getItemBySparqlID(tuple.getSparqlId());
				if (item == null) {
					throw new Exception("sparqlId was not found: " + tuple.getSparqlId());
				}
				item.setIsReturned(tuple.isReturned());
			}
			
			// put modified nodegroup back into sgJson and return
			sgJson.setNodeGroup(ng);
			retval.addResult(RET_KEY_NODEGROUP, sgJson.toJson());
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.addRationaleMessage(SERVICE_NAME, "setIsReturned", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();		
	}
	@ApiOperation(
			value="Change sparqlIds in nodegroup"
			)
	@CrossOrigin
	@RequestMapping(value="/changeSparqlIds", method=RequestMethod.POST)
	public JSONObject renameItems(@RequestBody NodegroupSparqlIdTupleRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);
		
		try{
			requestBody.validate();
			
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			NodeGroup ng = sgJson.getNodeGroup();
			
			
			for (SparqlIdTuple tuple : requestBody.getSparqlIdTuples()) {
				Returnable itemFrom = ng.getItemBySparqlID(tuple.getSparqlIdFrom());
				if (itemFrom == null) {
					throw new Exception("sparqlId was not found: " + tuple.getSparqlIdFrom());
				}
				String newId = ng.changeSparqlID(itemFrom, tuple.getSparqlIdTo());
				if (!newId.equals(tuple.getSparqlIdTo())) {
					throw new Exception("sparqlId is already in use: " + tuple.getSparqlIdTo() + ". System suggested: " + newId);
				}
			}
			
			// put modified nodegroup back into sgJson and return
			sgJson.setNodeGroup(ng);
			retval.addResult(RET_KEY_NODEGROUP, sgJson.toJson());
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.addRationaleMessage(SERVICE_NAME, "changeSparqlIds", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Get list of nodegroup returns"
			)
	@CrossOrigin
	@RequestMapping(value="/getReturnedSparqlIds", method=RequestMethod.POST)
	public JSONObject getReturns(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		TableResultSet retval = new TableResultSet(false);

		try {
			requestBody.validate();
			
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			NodeGroup ng = sgJson.getNodeGroup();
			
			ArrayList<String> ids = ng.getReturnedSparqlIDs();
			
			// create a new table of sparqlId strings
			Table table = new Table(new String[]{"sparqlId"}, new String[]{"string"});
			for (String id : ids) {
				table.addRow(new String[] { id } );
			}
			retval.addResults(table);
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "getReturnedSparqlIds", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Get columns required by import spec",
			notes="Returns \"columnNames\" array"
			)
	@CrossOrigin
	@RequestMapping(value="/getIngestionColumns", method=RequestMethod.POST)
	public JSONObject  getIngestionColumns(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			ImportSpecHandler handler = sgJson.getImportSpecHandler();
			String colNames[] = handler.getColNamesUsed();
			
			retval.addResult("columnNames", colNames);
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "getIngestionColumns", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Get ingestion validation rules",
			notes="Returns columnNames array and dataValidator json array"
			)
	@CrossOrigin
	@RequestMapping(value="/getIngestionColumnInfo", method=RequestMethod.POST)
	public JSONObject  getIngestionValidations(@RequestBody NodegroupRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			ImportSpecHandler handler = sgJson.getImportSpecHandler();
			retval.addResult("columnNames", handler.getColNamesUsed());
			retval.addResult("dataValidator", handler.getDataValidator().toJsonArray());
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "getIngestionColumnInfo", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Get a sample CSV that could be ingested",
			notes="Returns \"sampleCSV\" string which may be \"\" if there is no import spec in the nodegroup"
			)
	@CrossOrigin
	@RequestMapping(value="/getSampleIngestionCSV", method=RequestMethod.POST)
	public JSONObject getSampleIngestionCSV(@RequestBody SampleCsvRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			ImportSpecHandler handler = sgJson.getImportSpecHandler();
			
			String sampleCSV="";
			switch (requestBody.getFormat()) {
			case("default"):
				sampleCSV = handler.getSampleIngestionCSV();
				break;
			case "simple":
				sampleCSV = handler.getSampleCSV();
				break;
			default:
			} // getter threw exception
			
			retval.addResult("sampleCSV", sampleCSV);
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "getSampleIngestionCSV", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}

	@ApiOperation(
			value="Build a valid nodegroup constrain, with the provided parameters, to be used in nodegroup queries",
			notes="Returns \"sampleOBJ\" with a JSON constrain populated with the the provided request parameters: \n" +
					"sparqlId, operation and operandsList. \n It also validates the parameters producing 400 Bad Request exceptions" +
					" including error explanations"
	)
	@CrossOrigin
	@RequestMapping(value="/buildRuntimeConstraintJSON", method=RequestMethod.POST,
			consumes= MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public JSONObject buildRuntimeConstraintJSON(@RequestBody ConstraintRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);

		try {

			// the buildRuntimeConstraintJson() method below will validate the parameters for this method, producing
			// an exception

			String sparqlID = requestBody.getSparqlID();
			SupportedOperations operation = requestBody.getOperation();
			ArrayList<String> operandList = requestBody.getOperandList();
			JSONObject constraintJSON = RuntimeConstraintManager.buildRuntimeConstraintJson(sparqlID, operation, operandList);

			retval.addResult("constraintJSON", constraintJSON);
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "buildRuntimeConstraintJSON", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();
	}
	
	@ApiOperation(
			value="Get a sample CSV that could be ingested",
			notes="Returns \"sampleCSV\" string which may be \"\" if there is no import spec in the nodegroup"
			)
	@CrossOrigin
	@RequestMapping(value="/getSampleIngestionCSVMulti", method=RequestMethod.POST)
	public JSONObject getSampleIngestionCSVMulti(@RequestBody NodegroupListRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			
			SparqlGraphJson [] sgJsonArr = requestBody.getSparqlGraphJsonArray();
			ArrayList<ImportSpecHandler> specList = new ArrayList<>();
			for (int i=0; i < sgJsonArr.length; i++) {
				specList.add(sgJsonArr[i].getImportSpecHandler());
			}
			
			String sampleCSV = ImportSpecHandler.getSampleIngestionCSV(specList);
			
			retval.addResult("sampleCSV", sampleCSV);
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "getSampleIngestionCSVMulti", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	
	/*
	 * SPARQL can't be generated. 
	 * e.g. SELECT DISTINCT but nothing isReturned in the nodegroup
	 */
	private SimpleResultSet generateNoValidSparqlOutput(String endpoint, String message) {
		// Failure SimpleResultSet
		SimpleResultSet retval = new SimpleResultSet(false);
		retval.addRationaleMessage(SERVICE_NAME, endpoint, "SPARQL query can't be generated from this nodegroup");

		// InvalidSparqlRationale
		retval.addResult(INVALID_SPARQL_RATIONALE_LABEL, message);
		
		// Query type "Invalid"
		retval.addResult(QUERYTYPELABEL, "INVALID");
		
		return retval;
	}
	
	private SimpleResultSet generateSuccessOutput(String queryType, String query) throws Exception {
		SimpleResultSet retval = new SimpleResultSet(true);
		retval.addResult(QUERYFIELDLABEL, query);
		retval.addResult(QUERYTYPELABEL, queryType);
		
		return retval;
	}
	
	//========== nodegroup building / editing endpoints ===========
	//
	//  Use 'newer' SparqlConnectionRequest and SNodeGroupRequest, etc. out of SpringUtilLib project
	//  These don't need Java Client functions since in Java, you'd just call the NodeGroup functions themselves.
	//  Usually return RET_KEY_NODEGROUP in a SimpleResultSet
	//  Use the OInfoCache
	//  JUnit testing is covered by the NodeGroup tests, etc.
	
	@ApiOperation(
			value="Create a node group with given class"
			)
	@CrossOrigin
	@RequestMapping(value="/createNodeGroup", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public JSONObject createNodeGroup(@RequestBody ConnectionUriRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "createNodeGroup";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			// requestBody holds sparqlGraphJson for consistency.  It only contains a connection.
			
			OntologyInfo oInfo = retrieveOInfo(requestBody.buildSparqlConnection());
			
			NodeGroup ret = new NodeGroup();
			Node newNode = ret.addNode(requestBody.getUri(), oInfo); 
			
			if (requestBody.getSparqlID() != null) { 
				ret.changeSparqlID(newNode, requestBody.getSparqlID());
			}
			retval.addResult(RET_KEY_NODEGROUP, ret.toJson());
			retval.addResult(RET_KEY_SPARQLID, newNode.getSparqlID());
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
			value="Adds a new node to an existing nodeGroup"
	)
	@CrossOrigin
	@RequestMapping(value="/addNode", method=RequestMethod.POST, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public JSONObject addNode(@RequestBody AddNodeRequest requestBody, @RequestHeader HttpHeaders headers){
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "addNode";
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			// requestBody holds sparqlGraphJson for consistency.  It only contains a connection.

			OntologyInfo oInfo = retrieveOInfo(requestBody.buildConnection());
			
			String newNodeUri = requestBody.getNewNodeUri();
			String existingNodeSparqlId = requestBody.getExistingNodeSparqlId();

			NodeGroup ng = requestBody.buildNodeGroup();
			Node existingNode = ng.getNodeBySparqlID(existingNodeSparqlId);
			Node newNode = ng.returnBelmontSemanticNode(newNodeUri, oInfo);

			String connectionUri = requestBody.getObjectPropertyUri();
			if (requestBody.isFromExistingToNewNode()) {
				ng.addOneNode(newNode, existingNode, null, connectionUri );
			} else {
				ng.addOneNode(newNode, existingNode, connectionUri, null );
			}


			retval.addResult(RET_KEY_NODEGROUP, ng.toJson());
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
			value="Build default importSpec from returns in nodegroup",
			notes="Intended for use by the SPARQLgraph client.  Not too useful otherwise."
			)
	@CrossOrigin
	@RequestMapping(value="/setImportSpecFromReturns", method=RequestMethod.POST)
	public JSONObject setImportSpecFromReturns(@RequestBody SetImportSpecRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);
		
		try{
			
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			NodeGroup ng = sgJson.getNodeGroup();
			ImportSpecHandler isHandler = sgJson.getImportSpecHandler();
			ImportSpec oldSpec = isHandler == null ? new ImportSpec() : isHandler.getImportSpec();
			ImportSpec newSpec;
			
			// generate the columns  OR  copy the old spec
			if (requestBody.getAction().equals("Build from nodegroup")) {
				newSpec = ImportSpec.createSpecFromReturns(ng);
			} else {
				newSpec = oldSpec;
			}
			
			// add URI Lookups
			if (!requestBody.getLookupRegex().isEmpty()) {
				newSpec.addURILookups(ng, requestBody.getLookupRegex(), requestBody.getLookupMode());
			}
			
			// copy forward any options if importSpec was sent inside sgjson
			if (isHandler != null) {
				newSpec.setBaseURI(oldSpec.getBaseURI());
			}
			
			// return
			sgJson.setImportSpec(newSpec);
			retval.addResult(RET_KEY_NODEGROUP, sgJson.toJson());
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.addRationaleMessage(SERVICE_NAME, "setImportSpecFromReturns", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Get a list of supported server types.",
			notes="return key is 'serverTypes'"
			)
	@CrossOrigin
	@RequestMapping(value="/getServerTypes", method=RequestMethod.POST)
	public JSONObject getServerTypes(@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);
		try {
			retval.addResult("serverTypes", SparqlEndpointInterface.getServerTypes());
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.addRationaleMessage(SERVICE_NAME, "getServerTypes", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}
	
		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Inflate a nodegroup, return validation errors.",
			notes="returns 'nodegroup' 'modelErrorMessages' and 'invalidItemStrings'"
			)
	@CrossOrigin
	@RequestMapping(value="/inflateAndValidate", method=RequestMethod.POST)
	public JSONObject inflateAndValidate(@RequestBody NodegroupRequest ngRequest, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);
		try {
			SparqlGraphJson sgjson = ngRequest.buildSparqlGraphJson();
			SparqlConnection conn = sgjson.getSparqlConn();
			OntologyInfo oInfo = this.retrieveOInfo(conn);
			NodeGroup ng = sgjson.getNodeGroup();
			ImportSpec importSpec = sgjson.getImportSpec();
			ArrayList<String> modelErrorMessages = new ArrayList<String>();
			ArrayList<NodeGroupItemStr> invalidNodeGroupItems = new ArrayList<NodeGroupItemStr>();
			ArrayList<String> warnings = new ArrayList<String>();
			
			// do the work
			ng.inflateAndValidate(oInfo, importSpec, modelErrorMessages, invalidNodeGroupItems, warnings);
			
			// translate invalid items
			String invalidItemStrings[] = new String[invalidNodeGroupItems.size()];
			for (int i=0; i < invalidNodeGroupItems.size(); i++) {
				invalidItemStrings[i] = invalidNodeGroupItems.get(i).getStr();
			}
			
			// build the return
			retval.addResult("nodegroup", ng.toJson());
			retval.addResult("modelErrorMessages", modelErrorMessages.toArray(new String[modelErrorMessages.size()]));
			retval.addResult("invalidItemStrings", invalidItemStrings);
			retval.addResult("warnings", warnings.toArray(new String[warnings.size()]));
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.addRationaleMessage(SERVICE_NAME, "inflateAndValidate", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}
	
		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Suggest a valid class URI for a node",
			notes="returns classList = array of class URI's sorted from best to worst match"
			)
	@CrossOrigin
	@RequestMapping(value="/suggestNodeClass", method=RequestMethod.POST)
	public JSONObject suggestNodeClass(@RequestBody NodegroupItemStrRequest request, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);
		try {
			OntologyInfo oInfo = this.retrieveOInfo(request.buildConnection());
			NodeGroup ng = request.buildNodeGroup();
			NodeGroupItemStr itemStr = new NodeGroupItemStr(request.getItemStr(), ng);
			if (itemStr.getType() != Node.class)
				throw new Exception("Invalid itemStr param.  Expecting a Node URI");
				
			ArrayList<String> classList = ValidationAssistant.suggestNodeClass(oInfo, ng, itemStr);
			retval.addResult("classList", classList.toArray(new String[0]));
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.addRationaleMessage(SERVICE_NAME, "suggestNodeClass", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}
	
		return retval.toJson();		
	}
	
	@ApiOperation(
			value="Add a sample plot to a nodegroup"
			)
	@CrossOrigin
	@RequestMapping(value="/plot/addSamplePlot", method=RequestMethod.POST)
	public JSONObject addSamplePlot(@RequestBody NodegroupPlotRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);
		
		try {
			requestBody.validate();
			String plotType = requestBody.getPlotType(); 		// e.g. plotly
			String plotName = requestBody.getPlotName();
			String graphType = requestBody.getGraphType(); 		// e.g. scatter
			String[] columnNames = requestBody.getColumnNames();
			
			if(!plotType.equals(PlotlyPlotSpec.TYPE)){
				throw new Exception("Cannot add sample plot of type '" + plotType + "': only '" + PlotlyPlotSpec.TYPE + "' is supported");
			}
			
			// create the plot spec
			PlotSpec plotSpec = PlotlyPlotSpec.getSample(plotName, graphType, columnNames);
			
			// add it to the json
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			PlotSpecs plotSpecs = sgJson.getPlotSpecs() != null ? sgJson.getPlotSpecs() : new PlotSpecs();
			plotSpecs.addPlotSpec(plotSpec);
			sgJson.setPlotSpecsJson(plotSpecs.toJson());
			
			// build the return
			retval.addResult(RET_KEY_NODEGROUP, sgJson.toJson());
			retval.setSuccess(true);
		}
		catch(Exception e){
			retval.addRationaleMessage(SERVICE_NAME, "addSamplePlot", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();	
	}

	@ApiOperation(
			value="change the domain or range of a nodegroup element",
			notes="domain: if domain is a valid property, sets the range too"
			)
	@CrossOrigin
	@RequestMapping(value="/changeItemURI", method=RequestMethod.POST)
	public JSONObject  changeItemURI(@RequestBody NodeGroupItemUriRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet retval = new SimpleResultSet(false);

		try {
			
			// get things once in the right order
			SparqlGraphJson sgJson = requestBody.buildSparqlGraphJson();
			ImportSpec  importSpec = sgJson.getImportSpec();
			SparqlConnection conn = sgJson.getSparqlConn();
			OntologyInfo oInfo = this.retrieveOInfo(conn);
			NodeGroup nodegroup = sgJson.getNodeGroupNoInflateNorValidate(oInfo);
			NodeGroupItemStr itemStr = new NodeGroupItemStr(requestBody.getItemStr(), nodegroup);
			String newURI = requestBody.getNewURI();
			boolean deleteFlag = ! newURI.contains("#") && newURI.contains("<") && newURI.toLowerCase().contains("delete");
			
			if (requestBody.isDomain()) {
				// do the work for DOMAIN
				if (itemStr.getType() == Node.class) {
					nodegroup.changeItemDomain(itemStr.getSnode(), newURI);
					importSpec.changeNodeDomain(itemStr.getSnode().getSparqlID(), newURI);
					
				} else if (itemStr.getType() == PropertyItem.class) {
					Node node = itemStr.getSnode();
					PropertyItem prop = itemStr.getpItem();
					if (deleteFlag) {
						nodegroup.deleteProperty(node, prop);
						importSpec.deleteProperty(node.getSparqlID(), prop.getUriRelationship());
					} else {
						PropertyItem newProp = nodegroup.changeItemDomain(node, prop, newURI);
						importSpec.changePropertyDomain(node.getSparqlID(), prop.getUriRelationship(), newURI);
						
						// If newURI is a valid property in ontology, make sure range is correct too
						OntologyProperty oProp = oInfo.getProperty(newURI);
						if (oProp != null) {
							nodegroup.changeItemRange(newProp, oProp.getRangeStr());
						}
					}
					
					
				} else {
					// nodeItem
					Node node = itemStr.getSnode();
					NodeItem nItem = itemStr.getnItem();
					Node target = itemStr.getTarget();
					
					if (deleteFlag) {
						nodegroup.deleteNodeItem(node, nItem);
						// no effect on importSpec
					} else {
						NodeItem newNodeItem = nodegroup.changeItemDomain(node, nItem, target, newURI);
						// no effect on importSpec
						
						// If newURI is a valid property in ontology, make sure range is correct too
						OntologyProperty oProp = oInfo.getProperty(newURI);
						if (oProp != null) {
							nodegroup.changeItemRange(newNodeItem, oProp.getRangeStr());
						}
					}
					
				}
			} else {
				// do the work for RANGE
				if (itemStr.getType() == Node.class) {
					throw new Exception("Can not change Range of a class node");
					
				} else if (itemStr.getType() == PropertyItem.class) {
					nodegroup.changeItemRange(itemStr.getpItem(), newURI);
					// no effect on importSpec
				} else {
					// nodeItem
					nodegroup.changeItemRange(itemStr.getnItem(), newURI);
					// no effect on importSpec
				}
			}
			
			// return
			sgJson.setNodeGroup(nodegroup);
			sgJson.setImportSpec(importSpec);
			retval.addResult(RET_KEY_NODEGROUP, sgJson.toJson());
			retval.setSuccess(true);
		}
		catch (Exception e) {
			retval.addRationaleMessage(SERVICE_NAME, "changeItemURI", e);
			retval.setSuccess(false);
			LocalLogger.printStackTrace(e);
		}

		return retval.toJson();		
	}
	/**
	 * Retrieve oInfo.
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	private OntologyInfo retrieveOInfo(SparqlConnection conn) throws Exception {
		/* This option is slower
		 * But cache is cleared when owl is loaded or model might have changed
		 */
		OntologyInfoClient oClient = new OntologyInfoClient(new OntologyInfoClientConfig(oinfo_props.getProtocol(), oinfo_props.getServer(), oinfo_props.getPort()));
		return oClient.getOntologyInfo(conn);
		
		/* Faster option: Local cache
		 * It doesn't get cleared if model changes
		 */
		//return oInfoCache.get(conn);
	}

}
