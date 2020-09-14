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

package com.ge.research.semtk.services.filestaging;

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

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.aws.S3Connector;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.resultSet.ResultType;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utilityge.Utility;

import io.swagger.annotations.ApiOperation;


/**
 * Service to retrieve files from an external file store (e.g. S3)
 */
@CrossOrigin
@RestController
@RequestMapping("/fileStaging")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class FileStagingServiceRestController {

	@Autowired
	FileStagingProperties filestaging_prop; 
	@Autowired
	FileStagingResultsServiceProperties results_prop;	// results service used by filestaging service
	@Autowired
	AuthProperties auth_prop; 
	@Autowired 
	private ApplicationContext appContext;
	
	private static final String SERVICE_NAME = "FileStagingService";
	
	private final String STORETYPE_S3 = "s3";
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();	
		filestaging_prop.validateWithExit();
		results_prop.validateWithExit();
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);
	}
	
	/**
	 * Stage a file: copy from a file store (e.g. S3) to an accessible location, and then send it to the ResultsService.
	 */
	@ApiOperation(
			value="Stage a file from a file store"
			)
	@CrossOrigin
	@RequestMapping(value="/stageFile", method= RequestMethod.POST)
	public JSONObject stageFile(@RequestBody FileStagingRequestBody requestBody, @RequestHeader HttpHeaders headers) {
		
		HeadersManager.setHeaders(headers);
		final String ENDPOINT_NAME = "stageFile";
		
		try {
			TableResultSet tableResultSet = new TableResultSet();
			
			try {			
				
				// info about the file to copy
				requestBody.validate();
				final String sourceFile = requestBody.getSourceFile();			// path/name of file to stage
				final String stageFilename = requestBody.getStageFilename();	// name for file when staged 
				
				// info about source/destination
				final String storeType = filestaging_prop.getStoreType();		// store to retrieve from (e.g. s3)
				final String stageDir = filestaging_prop.getStageDirectory();   // local directory in which the file will be staged, e.g. /mnt/isilon/location
				final String stageFilePath = stageDir + "/" + stageFilename;  	// TODO do this using API for correct slashing
				
				LocalLogger.logToStdOut("Stage file " + sourceFile + " from " + storeType + " to " + stageFilePath);
				if(storeType.equals(STORETYPE_S3)){ 
					copyFromS3(sourceFile, stageFilePath);
				}else{
					throw new Exception("Cannot stage file from unsupported store type '" + storeType + "'");
				}
				
				// send file to Results Service
				String fileId = this.sendToResultsService(requestBody.jobId, stageFilePath, stageFilename);
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
				tableResultSet.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e.getMessage());
			} 
			
			return tableResultSet.toJson();
		    
		} finally {
	    	HeadersManager.clearHeaders();
	    }
	}

	
	/**
	 * Copy file from S3 to stage location
	 * @objectKey the key within the bucket (e.g. document/file.txt)
	 * @destinationPath the local copy destination
	 * 
	 * TODO this is untested
	 */
	private void copyFromS3(String objectKey, String destinationFilePath) throws Exception{		
		LocalLogger.logToStdOut("Copy " + objectKey + " from S3 to " + destinationFilePath);
		
		S3Connector s3Conn = new S3Connector("region", "bucketName"); // TODO get from Service properties
		
		// TODO move this to S3Connector as getObject(key, filename)?
		byte[] fileContents = s3Conn.getObject(objectKey);
		OutputStream os = null;   
		try {  
			os = new FileOutputStream(destinationFilePath);
			os.write(fileContents); 
		} catch (Exception e) { 
			throw e;
		} finally{
			os.close();
			os = null;
		}
		
	}
	
	
	/**
	 * Send a file to the ResultsService
	 */
	private String sendToResultsService(String jobID, String filePathStr, String filename) throws Exception{
		ResultsClient resultsClient = new ResultsClient(new ResultsClientConfig(results_prop.getProtocol(), results_prop.getServer(), results_prop.getPort()));
		return resultsClient.storeBinaryFilePath(jobID, filePathStr, filename);  
	}	
	
}
