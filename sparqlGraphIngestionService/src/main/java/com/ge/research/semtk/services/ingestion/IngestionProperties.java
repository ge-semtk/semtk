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


package com.ge.research.semtk.services.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.logging.easyLogger.EasyLogEnabledConfigProperties;
import com.ge.research.semtk.utility.LocalLogger;

@Configuration
@ConfigurationProperties(prefix="ingestion", ignoreUnknownFields = true)
public class IngestionProperties extends EasyLogEnabledConfigProperties{
	// stuff
	private String sparqlUserName = "";
	private String sparqlPassword = "";
	private int maxThreads = 0;
	
	private String loadTrackAwsRegion = "";
	private String loadTrackS3Bucket = "";
	private String loadTrackFolder = "";
	
	public IngestionProperties() {
		super();
		setPrefix("ingestion");
	}
	
	// get, set, etc
	public String getSparqlUserName(){
		return this.sparqlUserName;
	}
	
	public String getSparqlPassword(){
		return this.sparqlPassword;
	}
	
	public void setSparqlUserName(String sparqlUserName){
		this.sparqlUserName = sparqlUserName;
	}
	
	public void setSparqlPassword(String sparqlPassword){
		this.sparqlPassword = sparqlPassword;
	}
	
	/**
	 * Override system computation of how many threads to use.  0 means let system decide.
	 * @param maxThreads
	 */
	public void setMaxThreads(int maxThreads){
		LocalLogger.logToStdOut("ingestionMaxThreads=" + String.valueOf(maxThreads));
		this.maxThreads = maxThreads;
	}
	
	/**
	 * Get a number of threads if set, else 0.
	 * @return
	 */
	public int getMaxThreads(){
		return this.maxThreads;
	}
	
	public String getLoadTrackAwsRegion() {
		return loadTrackAwsRegion;
	}

	public void setLoadTrackAwsRegion(String loadTrackAwsRegion) {
		this.loadTrackAwsRegion = loadTrackAwsRegion;
	}

	public String getLoadTrackS3Bucket() {
		return loadTrackS3Bucket;
	}

	public void setLoadTrackS3Bucket(String loadTrackS3Bucket) {
		this.loadTrackS3Bucket = loadTrackS3Bucket;
	}

	public String getLoadTrackFolder() {
		return loadTrackFolder;
	}

	public void setLoadTrackFolder(String loadTrackFolder) {
		this.loadTrackFolder = loadTrackFolder;
	}

	public void validate() throws Exception {
		super.validate();
// Neptune uses empty.  Maybe fuseki too?
//		checkNotEmpty("sparqlUserName", sparqlUserName);
//		checkNotEmptyMaskValue("sparqlPassword", sparqlPassword);
		checkNone("sparqlUserName", sparqlUserName);
		checkNoneMaskValue("sparqlPassword", sparqlPassword);
		checkNone("maxThreads", maxThreads);
	}
}
