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

import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForMap;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.logging.easyLogger.EasyLogEnabledConfigProperties;

@Configuration
@ConfigurationProperties(prefix="ingestion", ignoreUnknownFields = true)
public class IngestionProperties extends EasyLogEnabledConfigProperties{
	// stuff
	private String sparqlUserName = "";
	private String sparqlPassword = "";
	private int batchSize = 50;
	
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
	
	public void setBatchSize(int batchSize){
		this.batchSize = batchSize;
	}
	
	public int getBatchSize(){
		return this.batchSize;
	}
	
	public void validate() throws Exception {
		super.validate();
		checkNotEmpty("sparqlUserName", sparqlUserName);
		checkNotEmptyMaskValue("sparqlPassword", sparqlPassword);
		checkNone("batchSize", batchSize);
	}
}
