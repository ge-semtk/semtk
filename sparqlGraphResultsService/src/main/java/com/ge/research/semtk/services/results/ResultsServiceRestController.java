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


package com.ge.research.semtk.services.results;

/*
 * TODO
 * Move properties to a common place.
 *    Do we want spring boot dependencies in sparqlGraphLibraries?  Seems bad.
 *    
 * Move query functions (anything that knows the model) to new package
 * What is the best way to use properties in this situation.
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.JobFileInfo;
import com.ge.research.semtk.edc.resultsStorage.GenericJsonBlobResultsSerializer;
import com.ge.research.semtk.edc.resultsStorage.GenericJsonBlobResultsStorage;
import com.ge.research.semtk.edc.resultsStorage.JsonLdResultsSerializer;
import com.ge.research.semtk.edc.resultsStorage.JsonLdResultsStorage;
import com.ge.research.semtk.edc.resultsStorage.TableResultsSerializer;
import com.ge.research.semtk.edc.resultsStorage.TableResultsStorage;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.resultSet.ResultType;
import com.ge.research.semtk.services.results.requests.JsonBlobRequestBody;
import com.ge.research.semtk.services.results.requests.JsonLdStoreRequestBody;
import com.ge.research.semtk.services.results.requests.ResultsRequestBodyCsvMaxRows;
import com.ge.research.semtk.services.results.requests.ResultsRequestBodyFileExtContents;
import com.ge.research.semtk.services.results.requests.ResultsRequestBodyFinalizeTableResultsJson;
import com.ge.research.semtk.services.results.requests.ResultsRequestBodyInitializeTableResultsJson;
import com.ge.research.semtk.services.results.requests.ResultsRequestBodyMaxRows;
import com.ge.research.semtk.services.results.requests.ResultsRequestBodyPath;
import com.ge.research.semtk.springutilib.requests.JobIdRequest;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthorizationProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.LoggingProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.multipart.MultipartFile;


/**
 * Service to get query results.
 */
@CrossOrigin
@RestController
@RequestMapping("/results")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class ResultsServiceRestController {
 	static final String SERVICE_NAME = "ResultsService";

	@Autowired
	private ResultsProperties prop;
	@Autowired
	private ServicesGraphProperties servicesgraph_prop;
	@Autowired
	private LoggingProperties log_prop;
	@Autowired
	private AuthorizationProperties auth_prop; 
	@Autowired 
	private ApplicationContext appContext;
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);
		prop.validateWithExit();
		servicesgraph_prop.validateWithExit();
		log_prop.validateWithExit();
	}
	
	
	@CrossOrigin
	@RequestMapping(value="/storeJsonLdResults", method=RequestMethod.POST)
	public JSONObject storeJsonLdResults(@RequestBody JsonLdStoreRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		
		try {
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeJsonLdResults", "jobId", requestBody.jobId, "chars", String.valueOf(requestBody.getJsonRenderedGraph().length()));
	    	LocalLogger.logToStdOut(SERVICE_NAME + " storeJsonLdResults JobId=" + requestBody.jobId);
	
			SimpleResultSet res = new SimpleResultSet();
			try{
				URL url = getJsonLdResultsStorage().storeResults(requestBody.jobId, requestBody.getJsonRenderedHeader(), requestBody.getJsonRenderedGraph());
				getJobTracker().setJobResultsURL(requestBody.jobId, url);  // store URL with the job
			    res.setSuccess(true);
			    LocalLogger.logToStdOut(SERVICE_NAME + " storeJsonLdResults JobId=" + requestBody.jobId + " completed");
			} catch(Exception e){
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "storeJsonLdResults", e);
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeJsonLdResults exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
			} 
			return res.toJson();
			
		} finally {
			HeadersManager.clearHeaders();
		}
	}
	

	@CrossOrigin
	@RequestMapping(value="/getJsonLdResults", method= RequestMethod.POST)
	public void getJsonLdResults(@RequestBody JobIdRequest requestBody, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		resp.addHeader("content-type", "application/json; charset=utf-8");

		try {
			LocalLogger.logToStdOut(SERVICE_NAME + " getJsonLdResults JobId=" + requestBody.jobId);
			try{
		    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
				JsonLdResultsSerializer retval = getJsonLdResultsStorage().getJsonLd(url);
				retval.writeToStream(resp.getWriter());
				LocalLogger.logToStdOut(SERVICE_NAME + " getJsonLdResults JobId=" + requestBody.jobId + " completed");
		    } catch (Exception e) {
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "getJsonLdResults exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
		    } 
			
		} finally {
			HeadersManager.clearHeaders();
		}
	}
	

	@CrossOrigin
	@RequestMapping(value="/storeJsonBlobResults", method=RequestMethod.POST)
	public JSONObject storeJsonBlobResults(@RequestBody JsonBlobRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			String blobStr = requestBody.getJsonBlobString();

			LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeJsonBlobResults", "jobId", requestBody.jobId, "chars", String.valueOf(blobStr.length()));
			LocalLogger.logToStdOut(SERVICE_NAME + " storeJsonBlobResults JobId=" + requestBody.jobId);
	
			SimpleResultSet res = new SimpleResultSet();
			try{
				URL url = getJsonBlobResultsStorage().storeResults(requestBody.jobId, blobStr);
				getJobTracker().setJobResultsURL(requestBody.jobId, url);  // store URL with the job
			    res.setSuccess(true);
			    LocalLogger.logToStdOut(SERVICE_NAME + " storeJsonBlobResults JobId=" + requestBody.jobId + " completed");
			} catch(Exception e){
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "storeJsonBlobResults", e);
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeJsonBlobResults exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
			}   	
			return res.toJson();
			
		} finally {
			HeadersManager.clearHeaders();
		}	
	}
	
	
	/**
	 * Given a jobId, write back the raw json.  Empty if bad jobId.
	 */
	@CrossOrigin
	@RequestMapping(value="/getJsonBlobResults", method=RequestMethod.POST)
	public void getJsonBlobResults(@RequestBody JobIdRequest requestBody, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		resp.addHeader("content-type", "application/json; charset=utf-8");
		try {
			
			LocalLogger.logToStdOut(SERVICE_NAME + " getJsonBlobResults JobId=" + requestBody.jobId);
			try{
		    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
				GenericJsonBlobResultsSerializer retval = getJsonBlobResultsStorage().getJsonBlob(url);
				retval.writeToStream(resp.getWriter());
				LocalLogger.logToStdOut(SERVICE_NAME + " getJsonBlobResults JobId=" + requestBody.jobId + " completed");
		    } catch (Exception e) {
		    	LoggerRestClient.easyLog(logger, SERVICE_NAME, "getJsonBlobResults exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
		    } 
	
		} finally {
			HeadersManager.clearHeaders();
		}
	}
	
	
	@Operation(
			summary="Get fileIDs of all files in job",
			description="binary files are returned in a table with columns 'name' and 'fileId'"
			)
	@CrossOrigin
	@RequestMapping(value="/getResultsFiles", method=RequestMethod.POST)
	public JSONObject getResultsFiles(@RequestBody JobIdRequest requestBody,  @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		
		try {
			LocalLogger.logToStdOut(SERVICE_NAME + " getResultsFiles JobId=" + requestBody.jobId);
			
			final String ENDPOINT_NAME = "getResultsFiles";
			final String STRING_TYPE = "http://www.w3.org/2001/XMLSchema#string";  // TODO create an enum in Belmont and using it everywhere
			TableResultSet res = null;
			try{
				JobTracker tracker = getJobTracker();
				ArrayList<JobFileInfo> fileInfoList = tracker.getJobBinaryFiles(requestBody.jobId);
				
				// create table of fullResults (max 1 value) and files (many values)
				Table table = new Table(new String[] {"name",      "fileId"}, 
						                new String[] {STRING_TYPE, ResultType.BINARY_FILE_ID.getPrefixedName()});
				
				// add rest of rows
				for (JobFileInfo info : fileInfoList) {
					table.addRow(new String[] {	info.getFileName(), info.getFileId() });
				}
				
				res = new TableResultSet(true);
				res.addResults(table);
				LocalLogger.logToStdOut(SERVICE_NAME + " getResultsFiles JobId=" + requestBody.jobId + " completed");
				
		    } catch (Exception e) {
		    	LoggerRestClient.easyLog(logger, SERVICE_NAME, "getResultsFiles exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
			    res = new TableResultSet(false);
			    res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
		    } 
	
			return res.toJson();
			
		} finally {
			HeadersManager.clearHeaders();
		}
	}
    
	
	/**
	 * Given multipartFile, save a copy, assign a fileId and URL  (stores a metafile)
	 * @param requestBody
	 * @param headers
	 * @return  res.fullURL  res.fileId
	 */
	@Operation(
			summary="Store binary file to jobId",
			description="Return has fileId <br>"
			)
	@CrossOrigin
	@RequestMapping(value="/storeBinaryFile", method=RequestMethod.POST)
	public JSONObject storeBinaryFile(@RequestParam("file") MultipartFile file, @RequestParam("jobId") String jobId, HttpServletRequest req, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		
		try {
			SimpleResultSet res = null;
	
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeBinaryFile", "jobId", jobId, "bytes", String.valueOf(file.getSize()));
			LocalLogger.logToStdOut(SERVICE_NAME + " storeBinaryFile JobId=" + jobId);
	
			try{
				if (file != null) {
					String fileId = this.generateFileId();
					String originalFileName = file.getOriginalFilename();
					String storagePath = this.buildStoragePath(jobId, fileId); 
					
					LocalLogger.logToStdOut("Saving original file: " + originalFileName + " to: " + storagePath);
					
					// make any non-existent parent folders and then copy
					File storageFile = new File(storagePath);
					storageFile.getParentFile().mkdirs();
					Files.copy(file.getInputStream(), storageFile.toPath());
	
					res = this.addBinaryFile(jobId, fileId, originalFileName, storagePath.toString());
					
					LocalLogger.logToStdOut(SERVICE_NAME + " storeBinaryFile JobId=" + jobId + " completed");
				}
	
			} catch (Exception e) {
				res = new SimpleResultSet();
				res.setSuccess(false);
				res.addRationaleMessage(SERVICE_NAME, "storeBinaryFile", e);
				LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeBinaryFile exception", "message", e.toString());
				LocalLogger.printStackTrace(e);
			} 
			return res.toJson();
			
		} finally {
			HeadersManager.clearHeaders();
		}
	}

	/**
	 * Given path, assign a fileId and URL  (stores a metafile)
	 * @param requestBody
	 * @param headers
	 * @return  res.fullURL  res.fileId
	 */
	@Operation(
			summary="Associate a file with a jobId by path",
			description="Return has fileId <br>" + 
					"The path must be accessible by results service.  <br>" +
					"The file will be removed during results service cleanup."
			)
	@CrossOrigin
	@RequestMapping(value="/storeBinaryFilePath", method=RequestMethod.POST)
	public JSONObject storeBinaryFilePath(@RequestBody ResultsRequestBodyPath requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			LocalLogger.logToStdOut(SERVICE_NAME + " storeBinaryFilePath JobId=" + requestBody.jobId + " as " + ThreadAuthenticator.getThreadUserName());
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeBinaryFilePath", "jobId", requestBody.getJobId(), "bytes", String.valueOf(new File(requestBody.getPath()).length()));

			SimpleResultSet res = null;
			try{
				// make sure file is readable
				File f = new File(requestBody.getPath());
				if(! f.exists() ) { 
					this.validatePath(requestBody.getPath());  // throw validation error before not-readable exception
				    throw new Exception("File is not readable from results service: " + requestBody.getPath());
				}
				
				String fileId = this.generateFileId();
				res = this.addBinaryFile(requestBody.getJobId(), fileId, requestBody.getFilename(), requestBody.getPath());							
				LocalLogger.logToStdOut(SERVICE_NAME + " storeBinaryFilePath JobId=" + requestBody.jobId + " completed");
			} catch (Exception e) {
				res = new SimpleResultSet();
				res.setSuccess(false);
				res.addRationaleMessage(SERVICE_NAME, "storeBinaryFilePath", e);
				LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeBinaryFilePath exception", "message", e.toString());
				LocalLogger.printStackTrace(e);
			} 
			return res.toJson();
			
		} finally {
			HeadersManager.clearHeaders();
		}
	}
	
	
	private SimpleResultSet addBinaryFile(String jobId, String fileId, String originalFileName, String fullPath) throws Exception {
		
		this.validatePath(fullPath);
		
		this.getJobTracker().addBinaryFile(jobId, fileId, originalFileName, fullPath);
	
		SimpleResultSet res = new SimpleResultSet(true);
		res.addResult("fileId", fileId);
		return res;
	}
	
	
	@Operation(
			summary="Get binary file",
			description="On error, filename will be authorizationError.html or resultsExpired.html, with message inside."
			)
    @CrossOrigin
    @RequestMapping(value="/getBinaryFile/{fileId}", method=RequestMethod.GET)
    @ResponseBody
    public FileSystemResource getBinaryFile(@PathVariable("fileId") String fileId, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			LocalLogger.logToStdOut("getBinaryFile as " + ThreadAuthenticator.getThreadUserName());
		        
	        try {
	        	JobFileInfo metaFile = this.getJobTracker().getBinaryFile(fileId);
	            String originalFileName = metaFile.getFileName();
	            String dataFilePath = metaFile.getPath();
	       
	            // Testing only.  Non-binary endpoints will use the JobTracker for matching Principal.
	            if (ThreadAuthenticator.getThreadUserName().equals(metaFile.getUserName())) {
	            	LocalLogger.logToStdOut("getBinaryFile users match: " + metaFile.getUserName());
	            } else {
	            	LocalLogger.logToStdOut("getBinaryFile file username: " + metaFile.getUserName() + " DOES NOT MATCH CALLER: " + ThreadAuthenticator.getThreadUserName());
	            } 
	            	
	            File file = new File(dataFilePath);
	        
	        	// return the file if it exists
	            resp.setHeader("Content-Disposition", "attachment; filename=\"" + originalFileName + "\"");
	            return new FileSystemResource(file);
	            
	        } catch (Exception e) {
			    LocalLogger.printStackTrace(e);
	        	try {
	        		// try to return an error
	        		String filename = "";
	        		String message = "";
	        		if (e instanceof AuthorizationException) {
	        			filename = "authorizationError.html";
	        			message = "<html><body>AuthorizationException - " + e.getMessage() + "</body></html>\n";
	        		} else {
	        			filename = "resultExpired.html";
	        			message = 	"<html><body>Exception - Result was not found. It is incorrect or it has expired.<br><br>\n" +
	        						"message: " + e.getMessage() + 
	        						"</body></html>\n";
	        		}
	        		resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
	        		File f = File.createTempFile("error", ".html");
		            f.deleteOnExit();
		            BufferedWriter bw = new BufferedWriter(new FileWriter(f ));
		    	    bw.write(message);
		    	    bw.close();
		            return new FileSystemResource(f);
	        	} catch (Exception e1) {
	        		
	        		// failed to return error; return null
	        		LoggerRestClient.easyLog(logger, SERVICE_NAME, "getBinaryFile exception", "message", e.toString());
	    		    LocalLogger.printStackTrace(e1);
	    		    return null;
	        	}
	        } 
		} finally {
			HeadersManager.clearHeaders();
		} 
    }


    /**
	 * Call 1 of 3 for storing JSON results.
	 * Writes JSON start, column names, and column types.
	 */
	@Operation(
			summary="initialize json table storage",
			description="Use this before storeTableResultsJsonAddIncremental"
			)
	@CrossOrigin
	@RequestMapping(value="/storeTableResultsJsonInitialize", method=RequestMethod.POST)
	public JSONObject storeTableResultsJsonInitialize(@RequestBody ResultsRequestBodyInitializeTableResultsJson requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			//LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeTableResultsJsonInitialize", "jobId", requestBody.jobId);
			LocalLogger.logToStdOut(SERVICE_NAME + " storeTableResultsJsonInitialize JobId=" + requestBody.jobId);
	
			SimpleResultSet res = new SimpleResultSet();
			try{
				getTableResultsStorage().storeTableResultsJsonInitialize(requestBody.jobId, requestBody.getJsonRenderedHeader());
			    res.setSuccess(true);
			} catch(Exception e){
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "storeTableResultsJsonInitialize", e);
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeTableResultsJsonInitialize exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
			}
			return res.toJson();
			
		} finally {
			HeadersManager.clearHeaders();
		}
	}
	
	/**
	 * Call 2 of 3 for storing JSON results.  Repeat for multiple batches.
	 * Writes rows of data.  
	 * The input data ("contents") must:
	 * 		1) be ZLIB compressed, using Utility.compress() or other
	 * 		2) omit tailing comma for the last row of the last call
	 * 
	 * Sample input (decompressed):
	 * ["a1","b1","c1"],
	 * ["a2","b2","c2"],
	 * ["a3","b3","c3"]
	 */
	@Operation(
			summary="store chunk of json table storage",
			description="Use this after storeTableResultsJsonInitialize<br>" +
					"Call storeTableResultsJsonFinalize when done. "
			)
	@CrossOrigin
	@RequestMapping(value="/storeTableResultsJsonAddIncremental", method=RequestMethod.POST)
	public JSONObject storeTableResultsJsonAddIncremental(@RequestBody ResultsRequestBodyFileExtContents requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		SimpleResultSet res = new SimpleResultSet();
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());

		try{
			String decompressed = Utility.decompress(requestBody.getContents());

			LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeTableResultsJsonAddIncremental", "jobId", requestBody.jobId, "chars", String.valueOf(decompressed.length()) );
			LocalLogger.logToStdOut(SERVICE_NAME + " storeTableResultsJsonAddIncremental JobId=" + requestBody.jobId);

			getTableResultsStorage().storeTableResultsJsonAddIncremental(requestBody.jobId, decompressed);
			res.setSuccess(true);
		}
		catch(Exception e){
			res.setSuccess(false);
			res.addRationaleMessage(SERVICE_NAME, "storeTableResultsJsonAddIncremental", e);
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeTableResultsJsonAddIncremental exception", "message", e.toString());
			LocalLogger.printStackTrace(e);
		} finally {
			HeadersManager.clearHeaders();
		}    	
		return res.toJson();
			
		
	}
	
	/**
	 * Call 3 of 3 for storing JSON results.
	 * Writes row count and JSON end.
	 */
	@Operation(
			summary="finish json table storage",
			description="Use this after storeTableResultsJsonAddIncremental<br>" 
			)
	@CrossOrigin
	@RequestMapping(value="/storeTableResultsJsonFinalize", method=RequestMethod.POST)
	public JSONObject storeTableResultsJsonFinalize(@RequestBody ResultsRequestBodyFinalizeTableResultsJson requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());

		try {
			// logging
			//LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeTableResultsJsonFinalize", "jobId", requestBody.jobId);
			LocalLogger.logToStdOut(SERVICE_NAME + " storeTableResultsJsonFinalize JobId=" + requestBody.jobId);
	
			SimpleResultSet res = new SimpleResultSet();
			try{
				URL url = getTableResultsStorage().storeTableResultsJsonFinalize(requestBody.jobId); 
			    getJobTracker().setJobResultsURL(requestBody.jobId, url);  // store URL with the job		
			    res.setSuccess(true);
			} catch(Exception e){
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "storeTableResultsJsonFinalize", e);
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "storeTableResultsJsonFinalize exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
			} finally {
				HeadersManager.setHeaders(new HttpHeaders());
			}    	
			return res.toJson();
			
		} finally {
			HeadersManager.clearHeaders();
		}
	}
	
	/**
	 * Return a CSV file containing results (possibly truncated) for job
	 */
	@Operation(
			summary="Get CSV table",
			description="Too large a file can fail.  <br>" +
				  "This is good for a preview, for example, in a browser with limited memory.<br>" +
			      "GET /getTableResultsCsvForWebClient safely retrieves entire large files."
			)
	@CrossOrigin
	@RequestMapping(value="/getTableResultsCsv", method= RequestMethod.POST)
	public void getTableResultsCsv(@RequestBody ResultsRequestBodyCsvMaxRows requestBody, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		resp.addHeader("content-type", "text/plain; charset=utf-8");

		try{			
			LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsCsv JobId=" + requestBody.jobId);
			long startTimeMillis = System.currentTimeMillis();
			try {
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + requestBody.jobId + ".csv" + "\"; filename*=\"" + requestBody.jobId + ".csv" +"\"");
				URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
				TableResultsSerializer retval = getTableResultsStorage().getCsvTable(url, requestBody.maxRows, requestBody.getStartRow()); 			
				
				if(requestBody.getAppendDownloadHeaders()){
					retval.writeToStream(resp.getWriter());
				}else{
					retval.writeToStream(resp.getWriter());
				}
				LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsCsv JobId=" + requestBody.jobId + " completed in " + Utility.getSecondsSince(startTimeMillis) + " sec");
				
			} catch (AuthorizationException ae) {
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "getTableResultsCsv exception", "message", ae.toString());
				resp.getWriter().println("AuthorizationException\n" + ae.getMessage());
			}
		} catch (Exception e) {
			LoggerRestClient.easyLog(logger, SERVICE_NAME, "getTableResultsCsv exception", "message", e.toString());
			LocalLogger.printStackTrace(e);
		    
	    } finally {
			HeadersManager.clearHeaders();
		} 

	}

	@Operation(
			summary="Get JSON table results",
			description="GET triggers browser MIME type handling"
			)
	@CrossOrigin
	@RequestMapping(value= "/getTableResultsJsonForWebClient" , method= RequestMethod.GET)
	public ResponseEntity<Resource> getTableResultsJsonForWebClient(@RequestParam String jobId, @RequestParam(required=false) Integer maxRows, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		resp.addHeader("content-type", "application/json; charset=utf-8");

		try{
			LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsJsonForWebClient JobId=" + jobId);
			long startTimeMillis = System.currentTimeMillis();
			
			if(jobId == null){ throw new Exception("no jobId passed to endpoint."); }
			
	    	URL url = getJobTracker().getFullResultsURL(jobId);  
			TableResultsSerializer retval = getTableResultsStorage().getJsonTable(url, maxRows, 0); 			
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + jobId + ".json" + "\"; filename*=\"" + jobId + ".json" +"\"");
			retval.writeToStream(resp.getWriter());
			
			LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsJsonForWebClient JobId=" + jobId + " completed in " + Utility.getSecondsSince(startTimeMillis) + " sec");
	    } catch (Exception e) {
	    	LoggerRestClient.easyLog(logger, SERVICE_NAME, "getTableResultsJsonForWebClient exception", "message", e.toString());
		    LocalLogger.printStackTrace(e);
	    } finally {
	    	HeadersManager.clearHeaders();
	    }
		return null;
	}

	
	@Operation(
			summary="Get CSV table results",
			description="GET triggers browser MIME type handling"
			)
	@CrossOrigin
	@RequestMapping(value="/getTableResultsCsvForWebClient", method= RequestMethod.GET)
	public void getTableResultsCsvForWebClient(@RequestParam String jobId, @RequestParam(required=false) Integer maxRows, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		TableResultsSerializer retval = null;
		resp.addHeader("content-type", "text/plain; charset=utf-8");

		try{
			LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsCsvForWebClient JobId=" + jobId);
			long startTimeMillis = System.currentTimeMillis();
			
			if(jobId == null){ throw new Exception("no jobId passed to endpoint."); }
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + jobId + ".csv" + "\"; filename*=\"" + jobId + ".csv" +"\"");

	    	URL url = getJobTracker().getFullResultsURL(jobId);  

	    	try {
	    		retval = getTableResultsStorage().getCsvTable(url, maxRows, 0); 			
				retval.writeToStream(resp.getWriter());
	    	} catch (AuthorizationException ae) {
		    	LoggerRestClient.easyLog(logger, SERVICE_NAME, "getTableResultsCsvForWebClient exception", "message", ae.toString());
				resp.getWriter().println("AuthorizationException\n" + ae.getMessage());
			}
	    	
	    	LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsCsvForWebClient JobId=" + jobId + "completed in " + Utility.getSecondsSince(startTimeMillis) + " sec");
	    	
	    } catch (Exception e) {
	    	LoggerRestClient.easyLog(logger, SERVICE_NAME, "getTableResultsCsvForWebClient exception", "message", e.toString());
		    LocalLogger.printStackTrace(e);
	    }  finally {
	    	HeadersManager.clearHeaders();
	    } 
	}
	
	
	@Operation(
			summary="Get table results row count",
			description=""
			)
	@CrossOrigin
	@RequestMapping(value="/getTableResultsRowCount", method= RequestMethod.POST)
	public JSONObject getTableResultsRowCount(@RequestBody JobIdRequest requestBody, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try {
			LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsRowCount JobId=" + requestBody.getJobId());
			SimpleResultSet retTrue = null;
			
			try{
		    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
				int retval = getTableResultsStorage().getResultsRowCount(url);	
				
				retTrue = new SimpleResultSet(true);
				retTrue.addResult("rowCount", retval);				
		    } catch (Exception e) {
		    	LoggerRestClient.easyLog(logger, SERVICE_NAME, "getTableResultsRowCount exception", "message", e.toString());
			    retTrue = new SimpleResultSet(false);
			    retTrue.addRationaleMessage(SERVICE_NAME, "getTableResultsRowCount", e);
		    } finally {
				HeadersManager.setHeaders(new HttpHeaders());
			} 
			
			return retTrue.toJson();
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	
	}
	
	
	@Operation(
			summary="Get JSON table",
			description="Too large a file can fail.  <br>" +
					  "This is good for a preview, for example, in a browser with limited memory.<br>" +
				      "GET /getTableResultsCsvForWebClient safely retrieves entire large files.")
	@CrossOrigin
	@RequestMapping(value="/getTableResultsJson", method=RequestMethod.POST)
	public void getTableResultsJson(@RequestBody ResultsRequestBodyMaxRows requestBody, HttpServletResponse resp, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		resp.addHeader("content-type", "application/json; charset=utf-8");
        
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		final String ENDPOINT = "getTableResultsJson";
		try{
			LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsJson JobId=" + requestBody.getJobId());
			long startTimeMillis = System.currentTimeMillis();
			
	    	URL url = getJobTracker().getFullResultsURL(requestBody.jobId);  
			TableResultsSerializer retval = getTableResultsStorage().getJsonTable(url, requestBody.maxRows, requestBody.getStartRow());	
			wrapJsonInTableToSend(retval, resp);
			LocalLogger.logToStdOut(SERVICE_NAME + " getTableResultsJson JobId=" + requestBody.getJobId() + " completed in " + Utility.getSecondsSince(startTimeMillis) + " sec");
	    } catch (Exception e) {
	    	try {
	    		writeError(e, ENDPOINT, resp);
	    	} catch (Exception ee) {}
	    	LoggerRestClient.easyLog(logger, SERVICE_NAME, "getTableResultsJson exception", "message", e.toString());
		    LocalLogger.printStackTrace(e);
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	// PEC TODO: shouldn't this be a TableResultSet.toJson()
	private void writeError(Exception e, String endpoint, HttpServletResponse resp) throws Exception {
		PrintWriter outPrint = resp.getWriter();
		outPrint.write("\n\n==============\nInternal error in " + SERVICE_NAME + "/" + endpoint + "\n==============\n" );
		e.printStackTrace(outPrint);
		outPrint.write("\n==============\n" );
		outPrint.flush();
		outPrint.close();
	}

	private void wrapJsonInTableToSend(TableResultsSerializer trs, HttpServletResponse resp) throws Exception {
		PrintWriter outPrint = resp.getWriter();
		outPrint.write("{\"message\":\"operations succeeded.\",\"table\":{\"@table\":");
		outPrint.flush();
		trs.writeToStream(resp.getWriter());
		outPrint.write("},\"status\":\"success\"}");
		outPrint.flush();
		outPrint.close();
	}
	
	/**
	 * Gets a CSV URL and a JSON URL containing results for a job 
	 * Keeping this only for backwards compatibility with v1.3 or earlier
	 * "fullURL"   is the CSV  file (retaining bad label for backward compatibility)
	 * "sampleURL" is the JSON file (retaining bad label for backward compatibility)
	 */
	@Operation(
			summary="Get fullURL (csv) and sampleURL (JSON)",
			description="DEPRECATED<br>" +
			      "Use /getTableResultsJsonForWebClient and /getTableResultsCsvForWebClient instead <br>" +
				  "URL's may not work in a secure deployment of SemTK"
			)
	@CrossOrigin
	@RequestMapping(value="/getResults", method= RequestMethod.POST)
	public JSONObject getResults(@RequestBody JobIdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		try { 
		    SimpleResultSet res = new SimpleResultSet(); 	
	    	LocalLogger.logToStdOut(SERVICE_NAME + " getResults JobId=" + requestBody.jobId);
	    	
		    try {
		    	// check that the job id exists
		    	if(!getJobTracker().jobExists(requestBody.jobId)){
		    		throw new Exception("No job exists with id " + requestBody.jobId);
		    	}
		    	
		    	res.addResult("fullURL", this.getFullCsvUserURL(requestBody.jobId));  	// csv  - retain bad label for backward compatibility
			    res.addResult("sampleURL", this.getSampleJsonUserURL(requestBody.jobId));	// json - retain bad label for backward compatibility
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "getResults URLs", 
			    		"sampleURL", this.getSampleJsonUserURL(requestBody.jobId), 
			    		"fullURL", this.getFullCsvUserURL(requestBody.jobId));
			    res.setSuccess(true);	
			    
			    LocalLogger.logToStdOut(SERVICE_NAME + " getResults JobId=" + requestBody.jobId + " completed");
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "getResults", e);
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "getResults exception", "message", e.toString());
			    LocalLogger.printStackTrace(e);
		    }
		    
		    return res.toJson();
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * Delete file and metadata associated with this jobId
	 */
	@Operation(
			summary="delete job",
			description=""
			)
	@CrossOrigin
	@RequestMapping(value="/deleteJob", method= RequestMethod.POST)
	public JSONObject deleteStorage(@RequestBody JobIdRequest requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.getInstance(log_prop, ThreadAuthenticator.getThreadUserName());
		
		try {
			LocalLogger.logToStdOut(SERVICE_NAME + " deleteStorage JobId=" + requestBody.jobId);
	
		    SimpleResultSet res = new SimpleResultSet();
	    	
		    try {   	
		    	getJobTracker().deleteJob(requestBody.jobId, getTableResultsStorage()); 
			    res.setSuccess(true);		    
		    } catch (Exception e) {
		    	res.setSuccess(false);
		    	res.addRationaleMessage(SERVICE_NAME, "deleteStorage", e);
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "deleteJob exception", "message", e.toString());
		    } finally {
				HeadersManager.setHeaders(new HttpHeaders());
			} 	    
		    return res.toJson();
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	private TableResultsStorage getTableResultsStorage() throws Exception {
		return new TableResultsStorage(prop.getFileLocation());
	}

	private JsonLdResultsStorage getJsonLdResultsStorage() throws Exception {
		return new JsonLdResultsStorage(prop.getFileLocation());
	}
	
	private GenericJsonBlobResultsStorage getJsonBlobResultsStorage() throws Exception {
		return new GenericJsonBlobResultsStorage(prop.getFileLocation());
	}
	
	private JobTracker getJobTracker() throws Exception{
		return new JobTracker(servicesgraph_prop.buildSei());
	}
	
	private String getSampleJsonUserURL(String jobId) throws MalformedURLException {
		return new URL(prop.getBaseURL() + "/results/getTableResultsJsonForWebClient?jobId=" + jobId + "&maxRows=200").toString();
	}
	
	private String getFullCsvUserURL(String jobId) throws MalformedURLException {
		return new URL(prop.getBaseURL() + "/results/getTableResultsCsvForWebClient?jobId=" + jobId).toString();
	}
	
	private String generateFileId() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Check for only simple path characters and build path
	 * @param jobId
	 * @param fileId
	 * @return
	 * @throws Exception
	 */
	private String buildStoragePath(String jobId, String fileId) throws Exception {
		return prop.getFileLocation() + "/" + jobId + "/" + fileId;
	}
	
	private void validatePathElement(String elem) throws Exception {
		if (Pattern.matches("[^0-9a-zA-Z\\.\\s_-]", elem)  ||
				elem.contains("..") ) {
			throw new Exception("Value contains bad characters: " + elem);
		}
	}
	
	/**
	 * Make sure path starts with fileLocation or additionalFileLocations
	 * Make sure path contains no wonky characters
	 * @param path
	 * @throws Exception
	 */
	private void validatePath(String path) throws Exception {
		
		// is path in prop.fileLocation or prop.additionalFileLocations
		Path p = Paths.get(path);
		boolean found = p.startsWith(Paths.get(prop.getFileLocation()));
		for (String legal : prop.getAdditionalFileLocations()) {
			if (p.startsWith(Paths.get(legal))) {
				found = true;
				break;
			}
		}
		if (!found) {
			throw new Exception("Permission is denied.  Path is not white-listed in properties fileLocation or additionalFileLocations: " + path);
		}
		
		// are all path elements legal.
		// Skip the root path component since it would be in prop.fileLocation or prop.additionalFileLocations
		// nameCount will be number of path components not including the root.
		int nameCount = p.getNameCount();
		for (int i=0; i < nameCount; i++) {
			Path last = p.getFileName();                // pull last component
			this.validatePathElement(last.toString());  // validate it
			p = p.getParent();                          // shift to parent
		}
	}
}
