/**
 ** Copyright 2018 General Electric Company
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


package com.ge.research.semtk.services.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.springutilib.requests.IdRequest;
import com.ge.research.semtk.springutilib.requests.SparqlEndpointTrackRequestBody;
import com.ge.research.semtk.springutilib.requests.TrackQueryRequestBody;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

import io.swagger.v3.oas.annotations.Operation;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.aws.S3Connector;
import com.ge.research.semtk.connutil.DirectoryConnector;
import com.ge.research.semtk.connutil.FileSystemConnector;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.OntologyInfoClientConfig;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.LoadTracker;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.dataset.ODBCDataset;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.load.utility.IngestionNodegroupBuilder;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.logging.DetailsTuple;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.logging.easyLogger.LoggerClientConfig;
import com.ge.research.semtk.query.rdb.PostgresConnector;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;

/**
 * Service to load data into a triple store.
 */
@RestController
@RequestMapping("/ingestion")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class IngestionRestController {
 
	@Autowired
	AuthProperties auth_prop;
	
	@Autowired
	IngestionProperties prop;
	
	@Autowired
	OInfoServiceProperties oinfo_props;
	
	@Autowired
	ResultsServiceProperties results_prop;
	
	@Autowired
	ServicesGraphProperties servicesgraph_prop;
	
	@Autowired
	StatusServiceProperties status_prop;
	
	@Autowired 
	QueryServiceProperties query_prop;
	
	@Autowired 
	private ApplicationContext appContext;
	
	static final String SERVICE_NAME = "ingestion";
	
	static final String ASYNC_NOTES = "Success returns a jobId.\n" +
			"* check for that job's status of success with status message" +
			"* if job's status is failure then fetch a results table with ingestion errors" + 
			"Failure can return a rationale explaining what prevented the ingestion or precheck from starting.";
	
	static LoadTracker tracker = null;
	static FileSystemConnector trackBucket = null;
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		
		prop.validateWithExit();
		oinfo_props.validateWithExit();
		results_prop.validateWithExit();
		status_prop.validateWithExit();
		servicesgraph_prop.validateWithExit();
		query_prop.validateWithExit();
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);

		String loadTrackRegion = prop.getLoadTrackAwsRegion().trim();
		String loadTrackS3Bucket = prop.getLoadTrackS3Bucket().trim();
		String loadTrackFolder = prop.getLoadTrackFolder().trim();
		try {
			if (!loadTrackRegion.isEmpty() && !loadTrackS3Bucket.isEmpty()) {
				trackBucket = new S3Connector(loadTrackRegion, loadTrackS3Bucket);
				tracker = new LoadTracker(servicesgraph_prop.buildSei(), servicesgraph_prop.buildSei(), prop.getSparqlUserName(), prop.getSparqlPassword());
				tracker.updateOwlModel();
				tracker.test();
				
			} else if (!loadTrackFolder.isEmpty()) {
				trackBucket = new DirectoryConnector(loadTrackFolder);
				tracker = new LoadTracker(servicesgraph_prop.buildSei(), servicesgraph_prop.buildSei(), prop.getSparqlUserName(), prop.getSparqlPassword());
				tracker.updateOwlModel();
				tracker.test();
			}
		} catch (Exception e) {
			// load tracker failure will only cause errors if a request comes in asking for tracking
			LocalLogger.printStackTrace(e);
			tracker = null;
		}
	}
	
	/**
	 * Load data from CSV
	 */
	@CrossOrigin
	@RequestMapping(value="/fromCsvFile", method= RequestMethod.POST)
	public JSONObject fromCsvFile(
			@RequestParam(name="template") MultipartFile templateFile, 
			@RequestParam(name="data") MultipartFile dataFile, 
			@RequestParam(name="trackFlag", required=false) Boolean trackFlag, 
			@RequestParam(name="overrideBaseURI", required=false) String overrideBaseURI, 
			@RequestHeader HttpHeaders headers) {
		
		HeadersManager.setHeaders(headers);
		try {
			//debug("fromCsvFile", templateFile, dataFile);
			return this.fromAnyCsv(templateFile, dataFile, null, true, false, false, trackFlag, overrideBaseURI);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	@Operation(
			summary=	"Synchronous load from multipart file, with override connection and no precheck.",
			description=	ASYNC_NOTES
			)
	@CrossOrigin
	@RequestMapping(value="/fromCsvFileWithNewConnection", method= RequestMethod.POST)
	public JSONObject fromCsvFileWithNewConnection(
			@RequestParam(name="template") MultipartFile templateFile, 
			@RequestParam(name="data") MultipartFile dataFile , 
			@RequestParam(name="connectionOverride") MultipartFile connection, 
			@RequestParam(name="trackFlag", required=false) Boolean trackFlag, 
			@RequestParam(name="overrideBaseURI", required=false) String overrideBaseURI, 
			@RequestHeader HttpHeaders headers) {
		
		HeadersManager.setHeaders(headers);
		try {
			return this.fromAnyCsv(templateFile, dataFile, connection, true, false, false, trackFlag, overrideBaseURI);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@CrossOrigin
	@RequestMapping(value="/fromCsvFilePrecheck", method= RequestMethod.POST)
	public JSONObject fromCsvFilePrecheck(
			@RequestParam(name="template") MultipartFile templateFile, 
			@RequestParam(name="data") MultipartFile dataFile, 
			@RequestParam(name="trackFlag", required=false) Boolean trackFlag, 
			@RequestParam(name="overrideBaseURI", required=false) String overrideBaseURI, 
			@RequestHeader HttpHeaders headers) {
		
		HeadersManager.setHeaders(headers);
		try {
			//debug("fromCsvFilePrecheck", templateFile, dataFile);
			return this.fromAnyCsv(templateFile, dataFile, null, true, true, false, trackFlag, overrideBaseURI);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@CrossOrigin
	@RequestMapping(value="/fromCsvFilePrecheckOnly", method= RequestMethod.POST)
	public JSONObject fromCsvFilePrecheckOnly(
			@RequestParam("template") MultipartFile templateFile, 
			@RequestParam("data") MultipartFile dataFile,
			@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			//debug("fromCsvFilePrecheck", templateFile, dataFile);
			return this.fromAnyCsv(templateFile, dataFile, null, true, true, true, false, null);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@CrossOrigin
	@RequestMapping(value="/fromCsvFileWithNewConnectionPrecheck", method= RequestMethod.POST)
	public JSONObject fromCsvFileWithNewConnectionPrecheck(
			@RequestParam(name="template") MultipartFile templateFile, 
			@RequestParam(name="data") MultipartFile dataFile,
			@RequestParam(name="connectionOverride") MultipartFile connection, 
			@RequestParam(name="trackFlag", required=false) Boolean trackFlag, 
			@RequestParam(name="overrideBaseURI", required=false) String overrideBaseURI, 
			@RequestHeader HttpHeaders headers) {
		
		HeadersManager.setHeaders(headers);
		try {
			//debug("fromCsvFileWithNewConnectionPrecheck", templateFile, dataFile, connection);
			return this.fromAnyCsv(templateFile, dataFile, connection, true, true, false, trackFlag, overrideBaseURI);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@Operation(
			summary=	"File-based no-override ASYNC endpoint.  With override connection and precheck.",
			description=	ASYNC_NOTES
			)
	@CrossOrigin
	@RequestMapping(value="/fromCsvFilePrecheckAsync", method= RequestMethod.POST)
	public JSONObject fromCsvFileAsync(
			@RequestParam("template") MultipartFile templateFile, 
			@RequestParam("data") MultipartFile dataFile, 
			@RequestParam(name="trackFlag", required=false) Boolean trackFlag, 
			@RequestParam(name="overrideBaseURI", required=false) String overrideBaseURI, 
			@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			//debug("fromCsvFileWithNewConnectionPrecheck", templateFile, dataFile, connection);
			SimpleResultSet retval = this.fromAnyCsvAsync(templateFile, dataFile, null, true, true, false, trackFlag, overrideBaseURI);
		    return retval.toJson();
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	
	/**
	 * Perform precheck only (no ingest) using a CSV file against the given connection
	 */
	@CrossOrigin
	@RequestMapping(value="/fromCsvFileWithNewConnectionPrecheckOnly", method= RequestMethod.POST)
	public JSONObject fromCsvFilePrecheckOnly(
			@RequestParam("template") MultipartFile templateFile, 
			@RequestParam("data") MultipartFile dataFile,
			@RequestParam("connectionOverride") MultipartFile connection, 
			@RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			return this.fromAnyCsv(templateFile, dataFile, connection, true, true, true, false, null);
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	
	/**
	 * Load data from CSV
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	@CrossOrigin
	@RequestMapping(value="/fromCsv", method= RequestMethod.POST)
	public JSONObject fromCsv(
			@RequestBody IngestionFromStringsRequestBody requestBody, 
			@RequestHeader HttpHeaders headers) throws JsonParseException, JsonMappingException, IOException {
		
		HeadersManager.setHeaders(headers);
		try {
			// LocalLogger.logToStdErr("the request: " + requestBody);
			//IngestionFromStringsRequestBody deserialized = (new ObjectMapper()).readValue(requestBody, IngestionFromStringsRequestBody.class);
			return this.fromAnyCsv(requestBody.getTemplate(), requestBody.getData(), null, false, false, false, requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@CrossOrigin
	@RequestMapping(value="/fromCsvWithNewConnection", method= RequestMethod.POST)
	public JSONObject fromCsvWithNewConnection(@RequestBody IngestionFromStringsWithNewConnectionRequestBody requestBody, @RequestHeader HttpHeaders headers) throws JsonParseException, JsonMappingException, IOException {
		HeadersManager.setHeaders(headers);
		try {
			// LocalLogger.logToStdErr("the request: " + requestBody);
			//IngestionFromStringsWithNewConnectionRequestBody deserialized = (new ObjectMapper()).readValue(requestBody, IngestionFromStringsWithNewConnectionRequestBody.class);
			return this.fromAnyCsv(requestBody.getTemplate(), requestBody.getData(), requestBody.getConnectionOverride(), false, false, false, requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@CrossOrigin
	@RequestMapping(value="/fromCsvPrecheck", method= RequestMethod.POST)
	public JSONObject fromCsvPrecheck(@RequestBody IngestionFromStringsRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			return this.fromAnyCsv(requestBody.getTemplate(), requestBody.getData(), null, false, true, false, requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@CrossOrigin
	@RequestMapping(value="/fromCsvPrecheckOnly", method= RequestMethod.POST)
	public JSONObject fromCsvPrecheckOnly(@RequestBody IngestionFromStringsRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			return this.fromAnyCsv(requestBody.getTemplate(), requestBody.getData(), null, false, true, true, requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	@CrossOrigin
	@RequestMapping(value="/fromCsvWithNewConnectionPrecheck", method= RequestMethod.POST)
	public JSONObject fromCsvPrecheck(@RequestBody IngestionFromStringsWithNewConnectionRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			return this.fromAnyCsv(requestBody.getTemplate(), requestBody.getData(), requestBody.getConnectionOverride(), false, true, false, requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@Operation(
			summary=	"Main ASYNC endpoint.  With override connection and precheck.",
			description=	ASYNC_NOTES
			)
	@CrossOrigin
	@RequestMapping(value="/fromCsvWithNewConnectionPrecheckAsync", method= RequestMethod.POST)
	public JSONObject fromCsvPrecheckAsync(@RequestBody IngestionFromStringsWithNewConnectionRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		try {
			SimpleResultSet res = this.fromAnyCsvAsync(requestBody.getTemplate(), requestBody.getData(), requestBody.getConnectionOverride(), false, true, false, requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
			return res.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@Operation(
			summary=	"Ingest a file against the default ingestion template for this class.",
			description=	ASYNC_NOTES
			)
	@CrossOrigin
	@RequestMapping(value="/fromCsvUsingClassTemplate", method= RequestMethod.POST)
	public JSONObject fromCsvUsingClassTemplate(@RequestBody IngestionFromStringsAndClassRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		
		// prepare everything with separate error handling
		SparqlGraphJson sgjson = null;
		try {
			
			IngestionNodegroupBuilder builder = this.buildTemplate(requestBody.buildConnection(), requestBody.getClassURI(), requestBody.getIdRegex());
			sgjson = builder.getSgjson();
			
		} catch (Exception e) {
			HeadersManager.clearHeaders();
			return this.buildExceptionReturn(SERVICE_NAME, "fromCsvUsingClassTemplate", e);
		}
			
		try {
			boolean preCheck = true;
			boolean skipIngest = false;
			SimpleResultSet res = this.fromAnyCsvAsync(sgjson, requestBody.getData(), requestBody.getConnection(), false, preCheck, skipIngest, requestBody.getTrackFlag(), requestBody.getOverrideBaseURI());
			return res.toJson();
			
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	@Operation(
			summary=	"Get a class' default ingestion template and sample CSV file.",
			description=	"synchronous.  Returns simpleResult containing \'sgjson\' JSON and \'csv\' String fields."
			)
	@CrossOrigin
	@RequestMapping(value="/getClassTemplateAndCsv", method= RequestMethod.POST)
	public JSONObject getClassTemplateAndCsv(@RequestBody IngestionFromStringsAndClassRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		
		try {
			
			IngestionNodegroupBuilder builder = this.buildTemplate(requestBody.buildConnection(), requestBody.getClassURI(), requestBody.getIdRegex());
			SimpleResultSet result = new SimpleResultSet(true);
			result.addResult("sgjson", builder.getSgjson().toJson());
			result.addResult("csv", builder.getCsvTemplate());
			return result.toJson();
			
		} catch (Exception e) {
			return this.buildExceptionReturn(SERVICE_NAME, "getClassTemplateAndCsv", e);
			
		} finally {
			HeadersManager.clearHeaders();
		}
	}
	
	/**
	 * Build an ingestion template and return the builder
	 * @param conn
	 * @param classURI
	 * @return
	 * @throws Exception
	 */
	private IngestionNodegroupBuilder buildTemplate(SparqlConnection conn, String classURI, String idRegex) throws Exception {
		// get oInfo from the service (hopefully cached)
		OntologyInfo oInfo = this.buildOInfoClient().getOntologyInfo(conn);
		
		IngestionNodegroupBuilder builder = new IngestionNodegroupBuilder(classURI, conn, oInfo);
		builder.setIdRegex(idRegex); 
		builder.build();
		return builder;
	}
	
	@Operation(
			summary=	"Clear a graph with optional trackFlag."
			)
	@CrossOrigin
	@RequestMapping(value="/clearGraph", method= RequestMethod.POST)
	public JSONObject clearGraph(@RequestBody SparqlEndpointTrackRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet resultSet;
		try {	
			if (requestBody.getTrackFlag()) {
				this.validateTracker();
			}
			
			// clear using the query service
			SparqlQueryAuthClientConfig config = new SparqlQueryAuthClientConfig(
					query_prop.getProtocol(), 
					query_prop.getServer(), 
					query_prop.getPort(), 
					"/sparqlQueryService/clearAll", 
					requestBody.getServerAndPort(),
					requestBody.getServerType(),
					requestBody.getGraph(),
					requestBody.getUser(),
					requestBody.getPassword());
			SparqlQueryClient client = new SparqlQueryClient(config);
			resultSet = client.clearAll();
				
			if (requestBody.getTrackFlag()) {
				this.trackClear(requestBody.buildSei());
			}
					
		} catch (Exception e) {		
			return this.buildExceptionReturn(SERVICE_NAME, "clearGraph", e);
			
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		
		return resultSet.toJson();
	}	
	
	private JSONObject buildExceptionReturn(String service, String endpoint, Exception e) {
		LocalLogger.printStackTrace(e);
		SimpleResultSet resultSet = new SimpleResultSet(false);
		resultSet.addRationaleMessage(service, endpoint, e);
		return resultSet.toJson();
	}
	
	@Operation(
			summary=	"Run a query of tracked events."
			)
	@CrossOrigin
	@RequestMapping(value="/runTrackingQuery", method= RequestMethod.POST)
	public JSONObject runTrackingQuery(@RequestBody TrackQueryRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		
		try {	
			this.validateTracker();

			Table tab = this.runTrackSelectQuery(
					requestBody.getKey(), 
					requestBody.buildSei(), 
					requestBody.getUser(), 
					requestBody.getStartEpoch(), 
					requestBody.getEndEpoch());
			TableResultSet resultSet = new TableResultSet(true);
			resultSet.addResults(tab);
			return resultSet.toJson();
		} catch (Exception e) {			
			return this.buildExceptionReturn(SERVICE_NAME, "runTrackingQuery", e);
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		
	}	
	
	@Operation(
			summary=	"Delete tracked events."
			)
	@CrossOrigin
	@RequestMapping(value="/deleteTrackingEvents", method= RequestMethod.POST)
	public JSONObject deleteTrackingEvents(@RequestBody TrackQueryRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		GeneralResultSet resultSet = null;
		try {	
			this.validateTracker();

			// get tracker entries with keys
			Table tab = this.runTrackSelectQuery(
					requestBody.getKey(), 
					requestBody.buildSei(), 
					requestBody.getUser(), 
					requestBody.getStartEpoch(), 
					requestBody.getEndEpoch());
			
			// delete objects
			for (String key : tab.getColumn("fileKey")) {
				IngestionRestController.trackBucket.deleteObject(key);
			}
			
			// now delete tracker entries
			this.runTrackDeleteQuery(
					requestBody.getKey(), 
					requestBody.buildSei(), 
					requestBody.getUser(), 
					requestBody.getStartEpoch(), 
					requestBody.getEndEpoch());
			
			resultSet = new SimpleResultSet(true);
		} catch (Exception e) {			
			return this.buildExceptionReturn(SERVICE_NAME, "deleteTrackingEvents", e);
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
		
		return resultSet.toJson();
	}	
	
	@Operation(
			summary=	"Get contents of file key from /runTrackingQuery, returns 'contents' field in simple results"
			)
	@CrossOrigin
	@RequestMapping(value="/getTrackedIngestFile", method= RequestMethod.POST)
	public JSONObject getTrackedIngestFile(@RequestBody IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet resultSet = new SimpleResultSet();
		try {	
			String contents = new String(this.getTrackedFile(requestBody.getId()), StandardCharsets.UTF_8);
			resultSet.addResult("contents", contents);
			resultSet.setSuccess(true);
		} catch (Exception e) {			
			return this.buildExceptionReturn(SERVICE_NAME, "getTrackedIngestFile", e);
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
	
		return resultSet.toJson();
	}	
	
	@Operation(
			summary=	"Delete data from a tracked load"
			)
	@CrossOrigin
	@RequestMapping(value="/undoLoad", method= RequestMethod.POST)
	public JSONObject undoLoad(@RequestBody IdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet resultSet = new SimpleResultSet();
		try {	
			tracker.undoLoad(requestBody.getId());
			resultSet.setSuccess(true);
		} catch (Exception e) {			
			return this.buildExceptionReturn(SERVICE_NAME, "undoLoad", e);
		} finally {
			HeadersManager.setHeaders(new HttpHeaders());
		}		
	
		return resultSet.toJson();
	}	
	
	public void debug(String endpoint, MultipartFile templateFile, MultipartFile dataFile) {
		try {
			LocalLogger.logToStdErr(endpoint);
	
			LocalLogger.logToStdErr("template file");
			LocalLogger.logToStdErr(new SparqlGraphJson(new String(templateFile.getBytes())).getJson().toJSONString());
			
			LocalLogger.logToStdErr("data file");
			LocalLogger.logToStdErr(new String(dataFile.getBytes()));
			
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
		}
	}
	
	public void debug(String endpoint, MultipartFile templateFile, MultipartFile dataFile, MultipartFile connection) {
		try {
			LocalLogger.logToStdErr(endpoint);
	
			LocalLogger.logToStdErr("template file");
			LocalLogger.logToStdErr(new SparqlGraphJson(new String(templateFile.getBytes())).getJson().toJSONString());
			
			LocalLogger.logToStdErr("connection");
			LocalLogger.logToStdErr(new SparqlConnection(new String(connection.getBytes())).toJson().toJSONString());
			
			LocalLogger.logToStdErr("data file");
			LocalLogger.logToStdErr(new String(dataFile.getBytes()));
			
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
		}
	}
	
	/**
	 * Load data from csv.
	 * @param templateFile the json template (File if fromFiles=true, else String)
	 * @param dataFile the data file (File if fromFiles=true, else String)
	 * @param sparqlConnectionOverride SPARQL connection json (File if fromFiles=true, else String)  If non-null, will override the connection in the template.
	 * @param fromFiles true to indicate that the 3 above parameters are Files, else Strings
	 * @param precheck check that the ingest will succeed before starting it
	 * @param skipIngest skip the actual ingest (e.g. for precheck only)
	 * @param async perform async
	 */
	private JSONObject fromAnyCsv(Object templateFile, Object dataFile, Object sparqlConnectionOverride, Boolean fromFiles, Boolean precheck, Boolean skipIngest, Boolean trackFlag, String overrideBaseURI){
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		int recordsProcessed = 0;
		
		// set up the logger
		LoggerRestClient logger = null;
		logger = getLoggerRestClient(logger, null);	
		ArrayList<DetailsTuple> detailsToLog = null;	
		
		RecordProcessResults retval = new RecordProcessResults();
		
		try {
			if (trackFlag != null && trackFlag) {
				// validate tracker before starting anything
				this.validateTracker();
			}
			if(logger != null){ 
				detailsToLog = LoggerRestClient.addDetails("Ingestion Type", "From CSV", null);
			}
			
			// get SparqlGraphJson from template
			String templateContent = fromFiles ? new String(((MultipartFile)templateFile).getBytes()) : (String)templateFile;
			if(templateContent != null){
				LocalLogger.logToStdErr("template size: "  + templateContent.length());
			}else{
				LocalLogger.logToStdErr("template content was null");
			}	
			
			SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJsonObjectFromString(templateContent));			
			
			// get data file content
			String dataFileContent = fromFiles ? new String(((MultipartFile)dataFile).getBytes()) : (String)dataFile; 
			String dataFileName = fromFiles ? ((MultipartFile)dataFile).getName() : null;
			if(dataFileContent != null){
				LocalLogger.logToStdErr("data size: "  + dataFileContent.length());
			}else{
				LocalLogger.logToStdErr("data content was null");
			}
					
			// override the connection, if needed
			if(sparqlConnectionOverride != null){
				String sparqlConnectionString = fromFiles ? new String(((MultipartFile)sparqlConnectionOverride).getBytes()) : (String)sparqlConnectionOverride;				
				sgJson.setSparqlConn( new SparqlConnection(sparqlConnectionString));   				
			}
						
			if(logger != null){  
				detailsToLog = LoggerRestClient.addDetails("template", templateContent, detailsToLog);
			}
					
			// get a CSV data set to use in the load. 
			Dataset ds = new CSVDataset(dataFileContent, true);

			// how about some more logging
			String startTime = dateFormat.format(Calendar.getInstance().getTime());
			if(logger != null) { 
				detailsToLog = LoggerRestClient.addDetails("Start Time", startTime, detailsToLog); 				
			}
			
			String trackKey = UUID.randomUUID().toString();
			this.overrideBaseURI(sgJson, trackFlag, overrideBaseURI, trackKey);

			// clear caches
			uncache(sgJson.getSparqlConn().getInsertInterface());
			
			// load
			DataLoader dl = new DataLoader(sgJson, ds, prop.getSparqlUserName(), prop.getSparqlPassword());
			dl.overrideMaxThreads(prop.getMaxThreads());

			recordsProcessed = dl.importData(precheck, skipIngest);
	
			// yet some more logging
			String endTime = dateFormat.format(Calendar.getInstance().getTime());
			if(logger != null) { 
				detailsToLog = LoggerRestClient.addDetails("End Time", endTime, detailsToLog); 
				detailsToLog = LoggerRestClient.addDetails("input record size", recordsProcessed + "", detailsToLog);	
			}
			
			// set success values
			if(precheck && dl.getLoadingErrorReport().getRows().size() == 0){
				if (trackFlag != null && trackFlag) {
					this.trackLoad(trackKey, dataFileName, dataFileContent, sgJson.getSparqlConn().getInsertInterface());
				}
				retval.setSuccess(true);
			} else if(precheck && dl.getLoadingErrorReport().getRows().size() != 0){
				retval.setSuccess(false);
			} else if(!precheck && recordsProcessed > 0){
				if (trackFlag != null && trackFlag) {
					this.trackLoad(trackKey, dataFileName, dataFileContent, sgJson.getSparqlConn().getInsertInterface());
				}
				retval.setSuccess(true);
			} else {
				retval.setSuccess(false);
			}			
			
			retval.setRecordsProcessed(recordsProcessed);
			retval.setFailuresEncountered(dl.getLoadingErrorReport().getRows().size());
			retval.addResults(dl.getLoadingErrorReport());
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);			
			retval.setSuccess(false);
			retval.addRationaleMessage("ingestion", "fromCsv*", e);
		}  
		
		if(logger != null){  
			// what are we returning
			detailsToLog = LoggerRestClient.addDetails("error code", retval.getResultCodeString(), detailsToLog);
			detailsToLog = LoggerRestClient.addDetails("results", retval.toJson().toJSONString(), detailsToLog);
			detailsToLog = LoggerRestClient.addDetails("records Processed", recordsProcessed + "", detailsToLog);
		}
		
		if(logger != null){ 
			logger.logEvent("Data ingestion", detailsToLog, "Add Instance Data To Triple Store");
		}
		return retval.toJson();
	}	
	
	/** 
	 * Are we configured for tracking loads
	 * @throws Exception
	 */
	public void validateTracker() throws Exception {
		if (IngestionRestController.tracker == null || IngestionRestController.trackBucket == null) {
			throw new Exception("Tracking is not configured in the ingestion service");
		}
	}
	
	/**
	 * Track a load
	 * @param dataFile
	 * @param sei
	 * @throws Exception
	 */
	public void trackLoad(String key, MultipartFile dataFile, SparqlEndpointInterface sei) throws Exception {
		String fileName = dataFile.getName();
		IngestionRestController.trackBucket.putObject(key, dataFile.getBytes());
		IngestionRestController.tracker.trackLoad(key, fileName, sei);
	}
	
	public void trackLoad(String key, String fileName, String dataStr, SparqlEndpointInterface sei) throws Exception {
		IngestionRestController.trackBucket.putObject(key, dataStr.getBytes());
		IngestionRestController.tracker.trackLoad(key, fileName, sei);
	}
	
	public void trackClear(SparqlEndpointInterface sei) throws Exception {
		IngestionRestController.tracker.trackClear(sei);
	}
	
	public byte[] getTrackedFile(String key) throws Exception {
		return IngestionRestController.trackBucket.getObject(key);
	}
	
	public Table runTrackSelectQuery(String fileKey, SparqlEndpointInterface sei, String user, Long startEpoch, Long endEpoch) throws Exception {
		return IngestionRestController.tracker.query(fileKey, sei, user, startEpoch, endEpoch);
	}
	
	public void runTrackDeleteQuery(String fileKey, SparqlEndpointInterface sei, String user, Long startEpoch, Long endEpoch) throws Exception {
		
		// get and delete actual files
		Table tab = IngestionRestController.tracker.query(fileKey, sei, user, startEpoch, endEpoch);
		for (String key : tab.getColumn(LoadTracker.KEY_COL)) {
			IngestionRestController.trackBucket.deleteObject(key);
		}
		
		// remove entries from tracker
		IngestionRestController.tracker.delete(fileKey, sei, user, startEpoch, endEpoch);
	}

	/**
	 * Load data from csv.
	 * Note this is a newer copy of fromAsynCsv with Async added
	 *     - more "modern" logging
	 * @param template the json template (SparqlGraphJson or MultipartFile if fromFiles=true, else String)
	 * @param dataFile the data file (File if fromFiles=true, else String)
	 * @param sparqlConnectionOverride SPARQL connection json (File if fromFiles=true, else String)  If non-null, will override the connection in the template.
	 * @param fromFiles true to indicate that the 3 above parameters are Files, else Strings
	 * @param precheck check that the ingest will succeed before starting it
	 * @param skipIngest skip the actual ingest (e.g. for precheck only)
	 */
	private SimpleResultSet fromAnyCsvAsync(Object template, Object dataFile, Object sparqlConnectionOverride, Boolean fromFiles, Boolean precheck, Boolean skipIngest, Boolean trackFlag, String overrideBaseURI){

		SimpleResultSet simpleResult = new SimpleResultSet();
		
		// set up the logger
		LoggerRestClient logger = LoggerRestClient.getInstance(prop, ThreadAuthenticator.getThreadUserName());
				
		try {
			
			if (trackFlag != null && trackFlag) {
				// validate tracker before starting anything
				this.validateTracker();
			}
			
			SparqlGraphJson sgJson = null;
			// get SparqlGraphJson from template
			if (template instanceof SparqlGraphJson) {
				// newer: just use the  SparqlGraphJson
				sgJson = (SparqlGraphJson) template;
			} else {
				// older: if fromFiles then read from files, else read from String
				String templateContent = fromFiles ? new String(((MultipartFile)template).getBytes()) : (String)template;	
				sgJson = new SparqlGraphJson(Utility.getJsonObjectFromString(templateContent));	
			}
			
			// get data file content
			String dataFileContent = fromFiles ? new String(((MultipartFile)dataFile).getBytes()) : (String)dataFile;
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "fromAnyCsvAsync", "chars", String.valueOf(dataFileContent != null ? dataFileContent.length() : 0));
		
			// override the connection, if needed
			if(sparqlConnectionOverride != null){
				String sparqlConnectionString = fromFiles ? new String(((MultipartFile)sparqlConnectionOverride).getBytes()) : (String)sparqlConnectionOverride;				
				sgJson.setSparqlConn( new SparqlConnection(sparqlConnectionString));   				
			}
					
			String trackKey = UUID.randomUUID().toString();
			String jobId = "job-" + UUID.randomUUID().toString();
			this.overrideBaseURI(sgJson, trackFlag, overrideBaseURI, trackKey);
			// get a CSV data set to use in the load. 
			Dataset ds = new CSVDataset(dataFileContent, true);
			DataLoader dl = new DataLoader(sgJson, ds, prop.getSparqlUserName(), prop.getSparqlPassword());
			dl.overrideMaxThreads(prop.getMaxThreads());
			
			
			dl.runAsync(precheck, skipIngest, 
					new StatusClient(new StatusClientConfig(status_prop.getProtocol(), status_prop.getServer(), status_prop.getPort(), jobId)), 
					new ResultsClient(new ResultsClientConfig(results_prop.getProtocol(), results_prop.getServer(), results_prop.getPort()))
					);
			
			if (trackFlag != null && trackFlag) {
				AsyncLoadTrackThread thread = new AsyncLoadTrackThread(
						new JobTracker(servicesgraph_prop.buildSei()),
						jobId, tracker, trackBucket, trackKey, 
						((MultipartFile)dataFile).getName(), 
						sgJson.getSparqlConn().getInsertInterface(), 
						dataFileContent.getBytes());
				thread.start();
			}
			
			simpleResult.setSuccess(true);
			simpleResult.addResult(SimpleResultSet.JOB_ID_RESULT_KEY, jobId);
					
			
		} catch (Exception e) {
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "fromAnyCsvAsync exception", "message", e.toString());
			LocalLogger.printStackTrace(e);			
			simpleResult.setSuccess(false);
			simpleResult.addRationaleMessage("ingestion", "fromCsv*", e);
		}  
		
		return simpleResult;
	}	
	
	/**
	 * Override baseURI.  
	 * @param sgjson
	 * @param trackFlag
	 * @param overrideBaseURI - no-op if this is null or empty
	 * @param fileKey
	 * @throws Exception
	 */
	private void overrideBaseURI(SparqlGraphJson sgjson, Boolean trackFlag, String overrideBaseURI, String fileKey) throws Exception {
		if (overrideBaseURI != null && ! overrideBaseURI.trim().isEmpty()) {
			String override = overrideBaseURI.trim();
			
			// replace $TRACK_KEY
			if (override.equals("$TRACK_KEY")) {
				if (trackFlag != true) {
					throw new Exception("overrideBaseURI is $TRACK_KEY but trackFlag is false");
				} else {
					override = LoadTracker.buildBaseURI(fileKey); 
				}
			}
			
			// set the new baseURI
			JSONObject spec = sgjson.getImportSpecJson();
			spec = ImportSpecHandler.overrideBaseURI(spec, override);
			sgjson.setImportSpecJson(spec);
		}
	}
	
	@CrossOrigin
	@RequestMapping(value="/fromPostgresODBC", method= RequestMethod.POST)
	public JSONObject fromPostgresODBC(
			@RequestParam("template") MultipartFile templateFile, 
			@RequestParam("dbHost")     @Size(min=4, max=256) @Pattern(regexp="[\\d\\w:/\\?=&-]+", message="string contains invalid characters") String dbHost, 
			@RequestParam("dbPort")     @Size(min=1, max=5  ) @Pattern(regexp="[\\d]+"           , message="string contains non-numbers"       ) String dbPort, 
			@RequestParam("dbDatabase") @Size(min=4, max=64 ) @Pattern(regexp="[\\d\\w:/\\?=&-]+", message="string contains invalid characters") String dbDatabase, 
			@RequestParam("dbUser")     @Size(min=4, max=64 )                                                                                    String dbUser, 
			@RequestParam("dbPassword") @Size(min=4, max=128)                                                                                    String dbPassword, 
			@RequestParam("dbQuery") String dbQuery){
		
		TableResultSet retval = new TableResultSet();

		try {
					
			String sparqlEndpointUser = prop.getSparqlUserName();
			String sparqlEndpointPassword = prop.getSparqlPassword();
			
			// log the attempted user name and password
			LocalLogger.logToStdErr("the user name was: " + sparqlEndpointUser);
			LocalLogger.logToStdErr("the password was: " + sparqlEndpointPassword);
			
			// get template file content and convert to json object for use. 
			String templateContent = new String(templateFile.getBytes());
			JSONParser parser = new JSONParser();
			JSONObject json = null;
			json = (JSONObject) parser.parse(templateContent);
		
			// get an ODBC data set to use in the load. 
			String postgresDriver = PostgresConnector.getDriver();
			String dbUrl = PostgresConnector.getDatabaseURL(dbHost, Integer.valueOf(dbPort), dbDatabase);
			Dataset ds = new ODBCDataset(postgresDriver, dbUrl, dbUser, dbPassword, dbQuery);
			
			// perform actual load
			DataLoader dl = new DataLoader(new SparqlGraphJson(json), ds, sparqlEndpointUser, sparqlEndpointPassword);
			dl.overrideMaxThreads(prop.getMaxThreads());
			dl.importData(true);	// defaulting to precheck
	
			retval.setSuccess(true);
			retval.addResultsJSON(dl.getLoadingErrorReport().toJson());

		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			retval.setSuccess(false);
			retval.addRationaleMessage("ingestion", "fromPostgresODBC", e);
		} 
		
		return retval.toJson();
	}	
	
	private LoggerRestClient getLoggerRestClient(LoggerRestClient logger, LoggerClientConfig lcc){
		// send a log of the load having occurred.
		try{	// wrapped in a try block because logging never announces a failure.
			if(prop.getLoggingEnabled()){
				// logging was set to occur. 
				lcc = new LoggerClientConfig(prop.getApplicationLogName(), prop.getLoggingProtocol(), prop.getLoggingServer(), Integer.parseInt(prop.getLoggingPort()), prop.getLoggingServiceLocation());
				logger = new LoggerRestClient(lcc);
			}
		}
		catch(Exception eee){
			// do nothing. 
			LocalLogger.logToStdErr("logging failed. No other details available.");
			LocalLogger.printStackTrace(eee);
		}
		return logger;
	}
	
	/**
	 * Remove sei and anything that looks like it from SemTK ontology and PredicateStats caches
	 * @param sei
	 * @throws Exception
	 */
	private void uncache(SparqlEndpointInterface sei) throws Exception {
		OntologyInfoClient oClient = this.buildOInfoClient();
		SparqlConnection conn = new SparqlConnection();
		conn.addModelInterface(sei);
		oClient.uncacheChangedConn(conn);
	}
	
	private OntologyInfoClient buildOInfoClient() throws Exception {
		return new OntologyInfoClient(new OntologyInfoClientConfig(oinfo_props.getProtocol(), oinfo_props.getServer(), oinfo_props.getPort()));
	}
}
