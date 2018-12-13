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

import java.util.ArrayList;

import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Configuration for SparqlQueryClient (non-auth query)
 */
public class AwsS3ClientConfig extends RestClientConfig {
	 String awsAccessKeyId = null;
	 String awsSecretAccessKey = null;
	 String bucket = null;

	 public AwsS3ClientConfig(String bucket, String accessKey, String secret) throws Exception {

		 super("http", bucket + ".s3.amazonaws.com", 80, "fake");
		 this.awsAccessKeyId = accessKey;
		 this.awsSecretAccessKey = secret;
		 this.bucket = bucket;
	 }

	 public String getAwsAccessKeyId() {
		 return awsAccessKeyId;
	 }

	 public String getAwsSecretAccessKey() {
		 return awsSecretAccessKey;
	 }

	 public String getBucket() {
		 return bucket;
	 }
}
