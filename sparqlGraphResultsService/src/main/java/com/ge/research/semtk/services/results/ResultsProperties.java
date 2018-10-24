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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.edc.SemtkEndpointProperties;

@Configuration
@ConfigurationProperties(prefix="results", ignoreUnknownFields = true)
public class ResultsProperties {
	private String fileLocation = "";
	private String baseURL = "";
	private int sampleLines = 1;
	private Boolean cleanUpThreadEnabled = true;
	private Integer cleanUpThreadFrequency;
	
	public String getBaseURL() {
		return baseURL;
	}
	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}
	public String getFileLocation() {
		return fileLocation;
	}
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}
	public int getSampleLines() {
		return sampleLines;
	}
	public void setSampleLines(int sampleLines) {
		this.sampleLines = sampleLines;
	}
	public Boolean getCleanUpThreadEnabled() {
		return cleanUpThreadEnabled;
	}
	public void setCleanUpThreadEnabled(Boolean cleanUpThreadEnabled) {
		this.cleanUpThreadEnabled = cleanUpThreadEnabled;
	}
	public Integer getCleanUpThreadFrequency() {
		return cleanUpThreadFrequency;
	}
	public void setCleanUpThreadFrequency(Integer cleanUpThreadFrequency) {
		this.cleanUpThreadFrequency = cleanUpThreadFrequency;
	}
}
