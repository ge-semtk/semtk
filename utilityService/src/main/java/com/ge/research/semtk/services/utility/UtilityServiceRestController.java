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

package com.ge.research.semtk.services.utility;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.load.config.ManifestConfig;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.plotting.PlotlyPlotSpec;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.services.utility.UtilityProperties;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

import io.swagger.v3.oas.annotations.Operation;


/**
 * Service to perform utility functions for SemTK.
 */
@RestController
@RequestMapping("/utility")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class UtilityServiceRestController {			
	
 	private static final String SERVICE_NAME = "utilityService";

	@Autowired
	UtilityProperties props;
	@Autowired
	ServicesGraphProperties servicesgraph_prop;
	@Autowired 
	private AuthProperties auth_prop; 
	@Autowired 
	private ApplicationContext appContext;
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);
	}
	
	/**
	 * Show the services connection configured in the UtilityService
	 */
	@Operation(description="Show the services connection")
	@CrossOrigin
	@RequestMapping(value="/getServicesGraphConnection", method= RequestMethod.POST)
	public JSONObject getServicesGraphConnection(){	
    	SimpleResultSet res = new SimpleResultSet();	
		try {
			// get services SPARQL connection and return it as JSON		
			res.addResult("servicesGraphConnection", getServicesSparqlConnection().toJson());
	    	res.setSuccess(true);	
		} catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "getServicesGraphConnection", e);
	    	LocalLogger.printStackTrace(e);
		}
		return res.toJson();	
	}	
	
	
	@Operation(description="Get a list of EDC mnemonics")
	@CrossOrigin
	@RequestMapping(value="/edc/getEdcMnemonicList", method= RequestMethod.POST)
	public JSONObject getEdcMnemonicList(){	
		
    	TableResultSet res = new TableResultSet();	
		try {
			
			// get the query nodegroup (stored as json in src/main/resources)
			String ngStr = Utility.getResourceAsString(this, "/nodegroups/GetEdcMnemonicList.json"); // stored in sparqlGraphLibrary-GE/src/main/resources/nodegroups/
			NodeGroup ng = (new SparqlGraphJson(Utility.getJsonObjectFromString(ngStr))).getNodeGroup();
			
			// execute the nodegroup
			Table table = getNodegroupExecutionClient().dispatchSelectFromNodeGroup(ng, getServicesSparqlConnection(), null, null);
	    	res.addResults(table);
	    	res.setSuccess(true);
			
		} catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "getEdcMnemonicList", e);
	    	LocalLogger.printStackTrace(e);
		}
		
		return res.toJson();	
	}	


	@Operation(description="Insert an EDC mnemonic")
	@CrossOrigin
	@RequestMapping(value="/edc/insertEdcMnemonic", method=RequestMethod.POST, consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
	public JSONObject insertEdcMnemonic(@RequestParam("data") MultipartFile dataFile){
		
		RecordProcessResults retVal = new RecordProcessResults();
		try {

			// get the CSV data as a string
			String dataFileContent = new String( ((MultipartFile)dataFile).getBytes() ); 
			
			// get the insertion nodegroup
			String nodeGroupString = Utility.getResourceAsString(this, "/nodegroups/InsertEdcMnemonic.json"); // stored in sparqlGraphLibrary-GE/src/main/resources/nodegroups/
			SparqlGraphJson sparqlGraphJson = new SparqlGraphJson(Utility.getJsonObjectFromString(nodeGroupString));
			sparqlGraphJson.setSparqlConn(getServicesSparqlConnection());
			
			// execute the nodegroup
			LocalLogger.logToStdOut("Insert EDC mnemonic data:\n" + dataFileContent);
			LocalLogger.logToStdOut("Insert EDC mnemonic data using connection " + getServicesSparqlConnection().toJson().toString());
			retVal = getNodegroupExecutionClient().execIngestionFromCsvStr(sparqlGraphJson, dataFileContent);
			
		} catch (Exception e) {
			retVal.setSuccess(false);
			retVal.addRationaleMessage(SERVICE_NAME, "insertEdcMnemonic", e);
	    	LocalLogger.printStackTrace(e);
		}
		
		return retVal.toJson();	
	}	 
	
	
	/**
	 * NOTE: Deletes the EDCType and its *links* to Services/Parameters/Constraints/Restrictions, 
	 * but does not delete the Services/Parameters/Constraints/Restrictions themselves because they
	 * may be shared across mnemonics.
	 */
	@Operation(description="Delete an EDC mnemonic")
	@CrossOrigin
	@RequestMapping(value="/edc/deleteEdcMnemonic", method= RequestMethod.POST)
	public JSONObject deleteEdcMnemonic(@RequestParam String mnemonic){	
		
		SimpleResultSet res = new SimpleResultSet();	
		
		try {
			
			if(mnemonic == null || mnemonic.trim().isEmpty()){
				throw new Exception("Mnemonic is null or empty, cannot delete");
			}
			mnemonic = mnemonic.trim();
			
			// if mnemonic does not exist, return without deleting
			if(!mnemonicExists(mnemonic)){
				LocalLogger.logToStdOut("Mnemonic " + mnemonic + " not found, no need to delete");
		    	res.setSuccess(true);
		    	return res.toJson();
			}
	
			// get the query nodegroup
			String deleteNodegroupString = Utility.getResourceAsString(this, "/nodegroups/DeleteEdcMnemonic.json"); // stored in sparqlGraphLibrary-GE/src/main/resources/nodegroups/
			NodeGroup deleteNodegroup = (new SparqlGraphJson(Utility.getJsonObjectFromString(deleteNodegroupString))).getNodeGroup();
			
			// pass in mnemonic name as runtime constraint	
			RuntimeConstraintManager runtimeConstraints = new RuntimeConstraintManager(deleteNodegroup);
			runtimeConstraints.applyConstraintJson(Utility.getJsonArrayFromString("[ { \"SparqlID\" : \"?mnemonic\", \"Operator\" : \"MATCHES\", \"Operands\" : [ \"" + mnemonic + "\" ] } ]"));
			
			// execute the nodegroup
			LocalLogger.logToStdOut("Delete mnemonic " + mnemonic);
			LocalLogger.logToStdOut("Delete mnemonic using connection " + getServicesSparqlConnection().toJson().toString());
			res = getNodegroupExecutionClient().execDispatchDeleteFromNodeGroup(deleteNodegroup, getServicesSparqlConnection(), null, runtimeConstraints);
			
		} catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "deleteEdcMnemonic", e);
	    	LocalLogger.printStackTrace(e);
		}

		return res.toJson();	
	}	
	
	
	@Operation(description="Get a list of FDC cache specifications")
	@CrossOrigin
	@RequestMapping(value="/fdc/getFdcCacheSpecList", method=RequestMethod.POST)
	public JSONObject getFdcCacheSpecList(){	
		
    	TableResultSet res = new TableResultSet();	
		try {
			NodeGroup ng = (new SparqlGraphJson(Utility.getResourceAsJson(this, "/nodegroups/GetFdcCacheSpecList.json"))).getNodeGroup();
			Table table = getNodegroupExecutionClient().dispatchSelectFromNodeGroup(ng, getServicesSparqlConnection(), null, null);
	    	res.addResults(table);
	    	res.setSuccess(true);
		} catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "getFdcCacheSpecList", e);
	    	LocalLogger.printStackTrace(e);
		}
		
		return res.toJson();	
	}

	
	@Operation(description="Delete an FDC cache specification")
	@CrossOrigin
	@RequestMapping(value="/fdc/deleteFdcCacheSpec", method= RequestMethod.POST)
	public JSONObject deleteFdcCacheSpec(@RequestParam String specId) {
		
		SimpleResultSet res = new SimpleResultSet();
		try {
			
			if(specId == null || specId.trim().isEmpty()){
				throw new Exception("fdcCacheSpecId is null or empty, cannot delete");
			}
			specId = specId.trim();
			
			// if cache spec does not exist, return without deleting
			if(!fdcCacheSpecExists(specId)){
				LocalLogger.logToStdOut("FDC Cache specification '" + specId + "' not found, no need to delete");
		    	res.setSuccess(true);
		    	return res.toJson();
			}
	
			// get the query nodegroup
			NodeGroup nodegroup = (new SparqlGraphJson(Utility.getResourceAsJson(this, "/nodegroups/DeleteFdcCacheSpec.json"))).getNodeGroup();
			
			// pass in spec id as runtime constraint	
			RuntimeConstraintManager runtimeConstraints = new RuntimeConstraintManager(nodegroup);
			runtimeConstraints.applyConstraintJson(Utility.getJsonArrayFromString("[ { \"SparqlID\" : \"?specId\", \"Operator\" : \"MATCHES\", \"Operands\" : [ \"" + specId + "\" ] } ]"));
			
			// execute the nodegroup
			LocalLogger.logToStdOut("Delete FDC Cache Spec '" + specId + "'");
			LocalLogger.logToStdOut("Delete FDC Cache Spec using connection " + getServicesSparqlConnection().toJson().toString());
			res = getNodegroupExecutionClient().execDispatchDeleteFromNodeGroup(nodegroup, getServicesSparqlConnection(), null, runtimeConstraints);
			
		} catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "deleteFdcCacheSpec", e);
	    	LocalLogger.printStackTrace(e);
		}

		return res.toJson();
	}
	
	
	@Operation(
			description="Get user name provided by a proxy\n{ name : 'fred' } or { name : 'anonymous' } if none"
			)
	@CrossOrigin
	@RequestMapping(value="/authUser", method= RequestMethod.GET)
	public JSONObject authUser(@RequestHeader HttpHeaders headers) {	
		HeadersManager.setHeaders(headers);
		
		// debug
		Map<String, String> map = headers.toSingleValueMap();
		for (String k : map.keySet()) {
			LocalLogger.logToStdOut("header " + k + "=" + map.get(k));
		}
		
		String user = ThreadAuthenticator.getThreadUserName();
		LocalLogger.logToStdOut("user=" + user);
       
		// return { name: 'whomever' }
		JSONObject ret = new JSONObject();
		ret.put("name", user);
		return ret;
	
	}
	
	
	/**
	 * Process a plot specification (e.g. fill in data values from table)
	 */
	@Operation(description="Process a plot specification")
	@CrossOrigin
	@RequestMapping(value="/processPlotSpec", method= RequestMethod.POST)
	public JSONObject processPlotSpec(@RequestBody ProcessPlotSpecRequest requestBody){
		LocalLogger.logToStdOut(SERVICE_NAME + " processPlotSpec");
    	SimpleResultSet res = new SimpleResultSet();	
		try {
							
			requestBody.validate();
			JSONObject plotSpecJson = requestBody.getPlotSpecJson();	// the plot spec with placeholders e.g. x: "SEMTK_TABLE.col[col_name]"
			Table table = Table.fromJson(requestBody.getTableJson());	// the data table
			
			if(!plotSpecJson.containsKey("type")){
				throw new Exception("Plot type not specified");
			}
			if(plotSpecJson.get("type").equals(PlotlyPlotSpec.TYPE)){
				PlotlyPlotSpec spec = new PlotlyPlotSpec(plotSpecJson);
				spec.applyTable(table);
				JSONObject plotSpecJsonProcessed = spec.toJson();	
				res.addResult("plot", plotSpecJsonProcessed);
				res.setSuccess(true);
				LocalLogger.logToStdOut("Returning " + res.toJson().toJSONString());
			}else{
				throw new Exception("Unsupported plot type");
			}

		} catch (Exception e) {
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, "processPlotSpec", e);
	    	LocalLogger.printStackTrace(e);
		}
		return res.toJson();	
	}	
	
	
	/**
	 * Load content from an ingestion package (zip file) to triplestore
	 *
	 * Not using @RequestBody because can't use with @RequestParam
	 */
	@Operation(description="Load content from an ingestion package")
	@CrossOrigin
	@RequestMapping(value="/loadIngestionPackage", method=RequestMethod.POST, consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
	public void loadIngestionPackage(	@RequestParam("serverAndPort") String serverAndPort, // e.g. http://localhost:3030/JUNIT for fuseki, http://localhost:2420 for virtuoso
										@RequestParam("serverType") String serverType,// e.g. "fuseki"
										@RequestParam("file") MultipartFile ingestionPackageZipFile,
										@RequestParam("defaultModelGraph") String defaultModelGraph,
										@RequestParam("defaultDataGraph") String defaultDataGraph,
										@RequestHeader HttpHeaders headers,
										HttpServletResponse resp){

		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "loadIngestionPackage";
		LocalLogger.logToStdOut(SERVICE_NAME + " " + ENDPOINT_NAME);
		PrintWriter responseWriter = null;

		// set up
		try {
			resp.addHeader("content-type", "text/plain; charset=utf-8");
			resp.flushBuffer();
			responseWriter = resp.getWriter();
		}catch(Exception e) {
			LocalLogger.printStackTrace(e);
		}

		ZipInputStream zipInputStream = null;
		File tempDir = null;
		try {

			// unzip the file
			if(!ingestionPackageZipFile.getOriginalFilename().endsWith(".zip")) {
				throw new Exception("This endpoint only accepts ingestion packages in zip file format");
			}
			zipInputStream = new ZipInputStream(ingestionPackageZipFile.getInputStream());
			tempDir = Utility.createTempDirectory();
			Utility.unzip(zipInputStream, tempDir);
			LocalLogger.logToStdOut("Unzipped " + ingestionPackageZipFile.getOriginalFilename() + " to " + tempDir.toString());

			// load the manifest
			File manifestFile = null;
			try {
				manifestFile = ManifestConfig.getTopLevelManifestFile(tempDir);
			}catch(Exception e) {
				throw new Exception("Cannot find a top-level manifest in " + ingestionPackageZipFile.getOriginalFilename());
			}
			ManifestConfig manifest = new ManifestConfig(manifestFile, defaultModelGraph, defaultDataGraph);
			boolean clear = false;					// TODO make this an endpoint parameter
			boolean loadToDefaultGraph = false;		// TODO make this an endpoint parameter
			manifest.load(serverAndPort, serverType, clear, loadToDefaultGraph, true, getIngestorRestClient(), getNodegroupExecutionClient(), getNodegroupStoreClient(), responseWriter);

			responseWriter.println("Load complete");
			responseWriter.flush();
			LocalLogger.logToStdOut(SERVICE_NAME + " " + ENDPOINT_NAME + " completed");

		} catch (Exception e){
			responseWriter.println("Error: " + e.getMessage());
			responseWriter.flush();
			LocalLogger.printStackTrace(e);
		}finally {
			try {
				if(responseWriter != null) {
					responseWriter.close();
				}
				if(zipInputStream != null) {
					zipInputStream.close();
				}
				if(tempDir != null) {
					FileUtils.deleteDirectory(tempDir);
				}
			}catch(Exception e) {
				LocalLogger.printStackTrace(e);
			}
		}
	}


	/**
	 * Determine if an EDC mnemonic exists in the services config
	 */
	private boolean mnemonicExists(String mnemonic) throws Exception{
		TableResultSet t = new TableResultSet(getEdcMnemonicList());
		return Arrays.asList(t.getTable().getColumnUniqueValues("mnemonic")).contains(mnemonic);
	}
	
	
	/**
	 * Determine if an FDC Cache spec exists in the services config
	 */
	private boolean fdcCacheSpecExists(String specId) throws Exception{
		TableResultSet t = new TableResultSet(getFdcCacheSpecList());
		return Arrays.asList(t.getTable().getColumnUniqueValues("specId")).contains(specId);
	}
	
	/**
	 * Get a connection to the services dataset.
	 */
	private SparqlConnection getServicesSparqlConnection() throws Exception{
		SparqlConnection conn = new SparqlConnection();
		conn.addModelInterface(servicesgraph_prop.getEndpointType(), servicesgraph_prop.getEndpointServerUrl(), servicesgraph_prop.getEndpointDataset());
		conn.addDataInterface(servicesgraph_prop.getEndpointType(), servicesgraph_prop.getEndpointServerUrl(), servicesgraph_prop.getEndpointDataset());
		conn.setDomain(servicesgraph_prop.getEndpointDomain());
		return conn;
	}
	
	/**
	 * Get a nodegroup execution client.
	 */
	private NodeGroupExecutionClient getNodegroupExecutionClient() throws Exception{
		NodeGroupExecutionClientConfig necc = new NodeGroupExecutionClientConfig(props.getNodegroupExecutionServiceProtocol(), props.getNodegroupExecutionServiceServer(), props.getNodegroupExecutionServicePort());
		return new NodeGroupExecutionClient(necc);
	}

	/**
	 * Get a nodegroup store client.
	 */
	private NodeGroupStoreRestClient getNodegroupStoreClient() throws Exception{
		NodeGroupStoreConfig ngsc = new NodeGroupStoreConfig(props.getNodegroupStoreServiceProtocol(), props.getNodegroupStoreServiceServer(), props.getNodegroupStoreServicePort());
		return new NodeGroupStoreRestClient(ngsc);
	}

	/**
	 * Get a ingestion client.
	 */
	private IngestorRestClient getIngestorRestClient() throws Exception{
		IngestorClientConfig icc = new IngestorClientConfig(props.getIngestionServiceProtocol(), props.getIngestionServiceServer(), props.getIngestionServicePort());
		return new IngestorRestClient(icc);
	}
}
