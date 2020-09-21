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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="filestaging", ignoreUnknownFields = true)
public class FileStagingProperties extends ServiceProperties {
	
	public final static String STORETYPE_DIR = "directory";	// retrieve from a directory in a locally-accessible file system
	public final static String STORETYPE_S3 = "s3";			// retrieve from an S3 bucket
	
	public FileStagingProperties() {
		super();
		setPrefix("filestaging");
	}
	
	private String storeType;		// see valid options above
	private String stageDirectory;	// directory into which to stage the file
	private String directory;		// for store type directory
	private String s3Region;		// for store type S3
	private String s3Bucket;		// for store type S3 
	
	public void validate() throws Exception {
		checkNotEmpty("stageDirectory", stageDirectory);
		checkNotEmpty("storeType", storeType);
		checkNone("directory", directory);
		checkNone("s3Region", s3Region);
		checkNone("s3Bucket", s3Bucket);
	}
	
	public String getStoreType() {
		return storeType;
	}

	public void setStoreType(String storeType) {
		this.storeType = storeType;
	}
	
	public String getStageDirectory() {
		return stageDirectory;
	}
	
	public void setStageDirectory(String stageDirectory) {
		this.stageDirectory = stageDirectory;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}
	
	public String getS3Region() {
		return s3Region;
	}

	public void setS3Region(String s3Region) {
		this.s3Region = s3Region;
	}

	public String getS3Bucket() {
		return s3Bucket;
	}

	public void setS3Bucket(String s3Bucket) {
		this.s3Bucket = s3Bucket;
	}
	
}
