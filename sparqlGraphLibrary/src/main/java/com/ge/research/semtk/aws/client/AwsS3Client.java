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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.Header;
import org.json.simple.JSONObject;

import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.utility.LocalLogger;
import com.javaquery.aws.AWSV4Auth;


import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;


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
	public JSONObject execUploadFileREST(String contents, String keyName) throws Exception {
		
		this.conf.setServiceEndpoint(keyName); 
		this.conf.setMethod(RestClientConfig.Methods.PUT);
		
		this.addAwsAuthPutHeadersManualV3(keyName, "text/plain");		//    application/octet-stream   ??   

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
		
		this.addAwsAuthDeleteHeadersManualV3(keyName);
        
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

	/**
	 * Apparently S3 v4 signatures are a little different than neptune?   
	 * Use manual v3 signatures instead.
	 * @param keyName
	 * @param contents
	 */
	private void addAwsAuthPutHeadersBROKEN(String keyName, String contents) {
		String accessKey = ((AwsS3ClientConfig)this.conf).getAwsAccessKeyId();
		String bucket = ((AwsS3ClientConfig)this.conf).getBucket();
		String secret = ((AwsS3ClientConfig)this.conf).getAwsSecretAccessKey();
		String region = new DefaultAwsRegionProviderChain().getRegion().id();
		String relativePath = "/" + bucket + "/" + keyName;
        
		TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
		this.addHeader("Host", bucket);
		this.addHeader("Content-Type", "text/plain");
		
		AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder(accessKey, secret)
                .regionName(region)
                .serviceName("s3") // es - elastic search. use your service name
                .httpMethodName("PUT") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(relativePath) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .payload(contents) // payload if any
                .debug() // turn on the debug mode
                .build();

		/* Get header calculated for request */
		Map<String, String> header = aWSV4Auth.getHeaders();
		for (Map.Entry<String, String> entrySet : header.entrySet()) {
			String k = entrySet.getKey();
			String val = entrySet.getValue();

			this.addHeader(k, val);
			LocalLogger.logToStdErr("PUT " + k + ":" + val);
		}
	}
	/**
	 * Apparently S3 v4 signatures are a little different than neptune?   
	 * Use manual v3 signatures instead.
	 * 
	 */
	private void addAwsAuthDeleteHeadersBROKEN(String keyName) {
		String accessKey = ((AwsS3ClientConfig)this.conf).getAwsAccessKeyId();
		String bucket = ((AwsS3ClientConfig)this.conf).getBucket();
		String secret = ((AwsS3ClientConfig)this.conf).getAwsSecretAccessKey();
		String region = new DefaultAwsRegionProviderChain().getRegion().id();
		String relativePath = "/" + bucket + "/" + keyName;
		
		this.addHeader("Host", bucket);
		
		TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
		AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder(accessKey, secret)
                .regionName(region)
                .serviceName("s3") // es - elastic search. use your service name
                .httpMethodName("DELETE") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(relativePath) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .payload(null) // payload if any
                .debug() // turn on the debug mode
                .build();

		/* Get header calculated for request */
		Map<String, String> header = aWSV4Auth.getHeaders();
		for (Map.Entry<String, String> entrySet : header.entrySet()) {
			String k = entrySet.getKey();
			String val = entrySet.getValue();

			this.addHeader(k, val);
			LocalLogger.logToStdErr("DELETE " + k + ":" + val);
		}
	}
	
	private void addAwsAuthPutHeadersManualV3(String keyName, String contentType) {
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
	
	private void addAwsAuthDeleteHeadersManualV3(String keyName) {
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

}

