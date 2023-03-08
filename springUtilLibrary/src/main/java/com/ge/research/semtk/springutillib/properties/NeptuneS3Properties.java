/**
 ** Copyright 2023 General Electric Company
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

package com.ge.research.semtk.springutillib.properties;

import com.ge.research.semtk.properties.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="neptuneupload", ignoreUnknownFields = true)
public class NeptuneS3Properties extends Properties {

	public NeptuneS3Properties() {
		super();
		this.setPrefix("neptuneupload");
	}

	// optional for Neptune upload Owl
	private String s3ClientRegion;   
	private String s3BucketName;
	private String awsIamRoleArn;

	public String getS3ClientRegion() {
		return s3ClientRegion;
	}

	public void setS3ClientRegion(String s3ClientRegion) {
		this.s3ClientRegion = s3ClientRegion;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public void setS3BucketName(String s3BucketName) {
		this.s3BucketName = s3BucketName;
	}

	public String getAwsIamRoleArn() {
		return awsIamRoleArn;
	}

	public void setAwsIamRoleArn(String awsIamRoleArn) {
		this.awsIamRoleArn = awsIamRoleArn;
	}
	
	public void validate() throws Exception {
		super.validate();
		checkNone("s3ClientRegion", s3ClientRegion);
		checkNone("s3BucketName", s3BucketName);
		checkNone("awsIamRoleArn", awsIamRoleArn);
	}
}

