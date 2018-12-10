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


package com.ge.research.semtk.aws.client;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.NodeGroupResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;


/**
 * Client to call the SparqlQueryService
 */
public class AwsS3Client extends RestClient {
	final String ALGORITHM = "HmacSHA1";
    final String ENCODING = "UTF8";
        
	/**
	 * Constructor
	 */
	public AwsS3Client(AwsS3ClientConfig conf){
		this.conf = conf;
	}

	@Override
	public AwsS3ClientConfig getConfig() {
		return (AwsS3ClientConfig) this.conf;
	}
	
	@Override
	public void buildParametersJSON() throws Exception {
		;
	}
	
	@Override
	public void handleEmptyResponse() throws Exception {
		;
	}
	
	private void cleanUp() {
		conf.setServiceEndpoint(null);
		this.fileParameter = null;
	}
	
	/**
	 * Uploads file to S3
	 * @param file
	 * @param keyName
	 * @return JSONObject containing headers of return from PUT
	 * @throws Exception - on any failure
	 */
	public JSONObject execUploadFile(String contents, String keyName) throws Exception {
		
		this.conf.setServiceEndpoint(keyName); 
		this.conf.setMethod(RestClientConfig.Methods.PUT);
		
		this.addAwsAuthPutHeaders(keyName, "text/plain");		//    application/octet-stream   ??   

        this.putContent = contents;
        
        try {
			JSONObject res = (JSONObject)execute(false);
			if (res.keySet().size() != 0) {
				throw new Exception("Unexpected non-empty return from PUT: " + res.toJSONString());
			} else {
				for (Header h : this.httpResponse.getAllHeaders()) {
					res.put(h.getName(), h.getValue());
				}
			}
			return res;
			
		} finally {
			this.putContent = null;
			this.removeAwsAuthHeaders();
		}
	}	
	
	/**
	 * Delete file from S3.  Succeeds if file is already gone.
	 * @param keyName
	 * @return
	 * @throws Exception
	 */
	public JSONObject execDeleteFile(String keyName) throws Exception {
		
		this.conf.setServiceEndpoint(keyName); 
		this.conf.setMethod(RestClientConfig.Methods.DELETE);
		
		this.addAwsAuthDeleteHeaders(keyName);
        
        try {
			JSONObject res = (JSONObject)execute(false);
			if (res.keySet().size() != 0) {
				throw new Exception("Unexpected non-empty return from DELETE: " + res.toJSONString());
			} else {
				for (Header h : this.httpResponse.getAllHeaders()) {
					res.put(h.getName(), h.getValue());
				}
			}
			return res;
			
		} finally {
			this.putContent = null;
			this.removeAwsAuthHeaders();
		}
	}	
	
	/**
	 * Create signature AWS will like
	 * @param data
	 * @param key
	 * @return
	 */
	private String buildSignature(String data, String key) {
		// thanks to https://automationrhapsody.com/amazon-s3-file-upload-curl-java-code/
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(key.getBytes(ENCODING), ALGORITHM));
			byte[] retBytes = mac.doFinal(data.getBytes(ENCODING));
			return Base64.getEncoder().encodeToString(retBytes);
		} catch (NoSuchAlgorithmException | InvalidKeyException	| UnsupportedEncodingException e) {
			return "";
		}
	}
	
	private void addAwsAuthPutHeaders(String keyName, String contentType) {
		String accessKey = ((AwsS3ClientConfig)this.conf).getAwsAccessKeyId();
		String bucket = ((AwsS3ClientConfig)this.conf).getBucket();
		String secret = ((AwsS3ClientConfig)this.conf).getAwsSecretAccessKey();
		
		String dateFormat = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String relativePath = "/" + bucket + "/" + keyName;
        String stringToSign = "PUT\n\n" + contentType + "\n" + dateFormat + "\n" + relativePath;
        String signature = this.buildSignature(stringToSign, secret);
		
        this.addHeader("Host", ((AwsS3ClientConfig)this.conf).getBucket() + ".s3.amazonaws.com");
        this.addHeader("Date", dateFormat);
        this.addHeader("Content-Type", contentType);
        this.addHeader("Authorization", "AWS " + accessKey + ":" + signature);
	}
	
	private void addAwsAuthDeleteHeaders(String keyName) {
		String accessKey = ((AwsS3ClientConfig)this.conf).getAwsAccessKeyId();
		String bucket = ((AwsS3ClientConfig)this.conf).getBucket();
		String secret = ((AwsS3ClientConfig)this.conf).getAwsSecretAccessKey();
		
		String dateFormat = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String relativePath = "/" + bucket + "/" + keyName;
        String stringToSign = "DELETE\n\n\n" + dateFormat + "\n" + relativePath;
        String signature = this.buildSignature(stringToSign, secret);
		
        this.addHeader("Host", ((AwsS3ClientConfig)this.conf).getBucket() + ".s3.amazonaws.com");
        this.addHeader("Date", dateFormat);
        this.addHeader("Authorization", "AWS " + accessKey + ":" + signature);
	}
	
	private void removeAwsAuthHeaders() {
		this.removeHeader("Host");
		this.removeHeader("Date");
		this.removeHeader("Content-Type");
		this.removeHeader("Authorization");
	}
	
	/**  Sample code using amazonaws sdk
	     
	   <dependency>
			<groupId>com.amazonaws</groupId>
			<artifactId>aws-java-sdk</artifactId>
			<version>[1.11.461,)</version>
		</dependency>
		
	import com.amazonaws.AmazonServiceException;
	import com.amazonaws.SdkClientException;
	import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
	import com.amazonaws.auth.profile.ProfileCredentialsProvider;
	import com.amazonaws.services.s3.AmazonS3;
	import com.amazonaws.services.s3.AmazonS3ClientBuilder;
	import com.amazonaws.services.s3.model.DeleteObjectRequest;
	import com.amazonaws.services.s3.transfer.TransferManager;
	import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
	import com.amazonaws.services.s3.transfer.Upload;
	
	   public void testS3Upload() throws Exception {
		
		String clientRegion = "us-east-1";
        String bucketName =   "my-bucket";
        String keyName =      "testS3Upload.csv";
        String filePath =     "src/test/resources/test.csv";
        

        try {
        	
        	// https://stackoverflow.com/questions/40897548/aws-java-s3-uploading-error-profile-file-cannot-be-null
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .build();
            TransferManager tm = TransferManagerBuilder.standard()
                    .withS3Client(s3Client)
                    .build();
            
            // TransferManager processes all transfers asynchronously,
            // so this call returns immediately.
            Upload upload = tm.upload(bucketName, keyName, new File(filePath));
            System.out.println("Object upload started");
    
            // Optionally, wait for the upload to finish before continuing.
            upload.waitForCompletion();
            System.out.println("Object upload complete");
            
            // delete
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, keyName));
            System.out.println("Object delete complete");

        }
        catch(AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process 
            // it, so it returned an error response.
            e.printStackTrace();
        }
        catch(SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }
    }
	 */
}

