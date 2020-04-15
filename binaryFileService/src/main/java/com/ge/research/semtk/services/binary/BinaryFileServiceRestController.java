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

package com.ge.research.semtk.services.binary;

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

import com.ge.research.bigdatatk.client.RetrieveClientConfig;
import com.ge.research.bigdatatk.client.RetrieveRestClient;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.logging.easyLogger.LoggerRestClient;
import com.ge.research.semtk.resultSet.ResultType;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utilityge.Utility;

import io.swagger.annotations.ApiOperation;

/**
 * Executor to stage binary files.
 * Stages a file from an internally-available URL to a URL reachable by SemTK users
 */
@RestController
@RequestMapping("/binaryFile")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class BinaryFileServiceRestController {			
		
	@Autowired
	BinaryProperties hdfs_prop;
	@Autowired
	BinaryResultsServiceProperties results_prop;
	@Autowired
	BinaryLoggingProperties log_prop;
	@Autowired
	private AuthProperties auth_prop; 
	@Autowired 
	private ApplicationContext appContext;
	
	private final static String DELIMITER = "###"; // delimiter for the "query" (e.g. "hdfs://test1-98231834.img###test1.img")
	private final static String HDFS_URL_PREFIX = "hdfs://";
	static final String SERVICE_NAME = "BinaryFileService";

	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		
		hdfs_prop.validateWithExit();
		results_prop.validateWithExit();
		log_prop.validateWithExit();
		auth_prop.validateWithExit();
		
		AuthorizationManager.authorizeWithExit(auth_prop);

	}
	
	/**
	 * Stage a file: copy from the store (e.g. HDFS) to an accessible location, and then send it to the ResultsService.
	 */
	@ApiOperation(
			value="Stage a file",
			notes="Assign a binary fileId to a path so user can retrieve it.<br>" +
			      "Associate the file with a jobId"
			)
	@CrossOrigin
	@RequestMapping(value="/stageFile", method= RequestMethod.POST)
	public JSONObject stageFile(@RequestBody BinaryFileRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);
		LoggerRestClient logger = LoggerRestClient.loggerConfigInitialization(log_prop, ThreadAuthenticator.getThreadUserName());
		
		try {
			TableResultSet tableResultSet = new TableResultSet();
			
			try {
			
				requestBody.validate(); 	// check inputs 
				
				// parse URL and filename
				String urlAndFilename = requestBody.query;		// e.g. "hdfs://test1-98231834.img###test1.img"
				if(urlAndFilename.split(DELIMITER).length != 2){
					throw new Exception("Input is not in format \"URL filename\": '" + urlAndFilename + "'");
				}
				String sourceUrl = urlAndFilename.split(DELIMITER)[0];
				String filename = urlAndFilename.split(DELIMITER)[1];
				String sharedDir = hdfs_prop.getSharedDirectory();  // location accessible from all services, for temporary storage, e.g. /mnt/isilon/dir
				LocalLogger.logToStdOut("Stage file from " + sourceUrl);
			    LoggerRestClient.easyLog(logger, SERVICE_NAME, "stageFile", "sourceUrl", sourceUrl, "filename", filename);

				// copy the file from the source to the shared directory
				String filePathInSharedDir = "";
				if(sourceUrl.startsWith(HDFS_URL_PREFIX)){ 
					filePathInSharedDir = copyFromHDFS(sourceUrl, sharedDir);
					LocalLogger.logToStdOut("Copied from HDFS to " + filePathInSharedDir);
				}else{
					throw new Exception("Staging file from " + sourceUrl + " is not supported yet");
				}
				
				// send file to Results Service
				String fileId = this.sendToResultsService(requestBody.jobId, filePathInSharedDir, filename);
				LocalLogger.logToStdOut("File staged to fileId: " + fileId);
				
				// return staged file in a result set
				String[] cols = {Utility.COL_NAME_FILEID}; 
				String[] colTypes = {ResultType.BINARY_FILE_ID.getPrefixedName()};
				Table retTable = new Table(cols, colTypes, null);
				ArrayList<String> row = new ArrayList<String>();
				row.add(fileId);
				retTable.addRow(row);			
				tableResultSet.addResults(retTable);
				tableResultSet.setSuccess(true);
	
			} catch (Exception e) {
				LocalLogger.printStackTrace(e);
				tableResultSet.setSuccess(false);
				tableResultSet.addRationaleMessage(e.getMessage());
			} 
			
			return tableResultSet.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}
	
	/**
	 * Send a file to the ResultsService
	 * @param filePathStr the path to the file
	 * @return
	 * @throws Exception 
	 */
	private String sendToResultsService(String jobID, String filePathStr, String filename) throws Exception{
		ResultsClient resultsClient = new ResultsClient(new ResultsClientConfig(results_prop.getProtocol(), results_prop.getServer(), results_prop.getPort()));
		return resultsClient.storeBinaryFilePath(jobID, filePathStr, filename);  
	}
	
	/**
	 * Use BigDataTK to copy file from HDFS to shared location
	 * @param hdfsUrl 	the HDFS url
	 * @param sharedDir a directory to which the SemTK services and Hadoop cluster all have access
	 * @return the path to the file in the shared location
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	private String copyFromHDFS(String hdfsUrl, String sharedDir) throws Exception{		
	
		LocalLogger.logToStdOut("Copy from HDFS  : " + hdfsUrl + " to " + sharedDir);
		
		String userId = "";
		ArrayList<String> extraPaths = new ArrayList<String>(); 
		JSONArray srcURLs = new JSONArray(); 	// TODO can execRetrieveRequest be changed to take ArrayList<String> instead of JSONArray?
		JSONObject row = new JSONObject();
		row.put("file", hdfsUrl);
		srcURLs.add(row);
		
		// call BigDataTK to copy file
		RetrieveRestClient client = new RetrieveRestClient(new RetrieveClientConfig(hdfs_prop.getProtocol(), hdfs_prop.getServer(), hdfs_prop.getPort()));
		SimpleResultSet resultSet = client.execSimpleSyncRetrieveRequest(userId, "hdfs", sharedDir, extraPaths, srcURLs);	
		LocalLogger.logToStdOut(resultSet.toJson().toJSONString());  // TODO REMOVE
		resultSet.throwExceptionIfUnsuccessful();
		
		// error check and return path
		return resultSet.getResult("destURL"); // TODO define "destURL" as constant elsewhere
	}

}
