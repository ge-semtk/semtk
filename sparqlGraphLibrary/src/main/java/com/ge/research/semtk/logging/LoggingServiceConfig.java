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


package com.ge.research.semtk.logging;

import java.util.regex.Pattern;

public class LoggingServiceConfig {
	// basic information needed to initiate the logging. 
	// this was once gotten from the servlet properties file via the servlet context. 
	// in order to be more compatible with the microservice initiative (or to allow standalone use)
	// these values are being re-arch'd into a wrapper class.
	
	// used in all logging
	private String loggingType;
	private String prefixPath;
	private String secondaryType = null;
	private String cloudConnection = null;
	private Pattern mainPattern = null;
	
	public LoggingServiceConfig(String logOption, String prefix){
		this.loggingType = logOption;
		this.prefixPath = prefix;
	}
	
	public LoggingServiceConfig(String logOption, String prefix, String mainRegex, String secondaryType, String cloudConnection){
		this.loggingType = logOption;
		this.prefixPath = prefix;
		if (mainRegex != null && ! mainRegex.isEmpty()) {
			this.mainPattern = Pattern.compile(mainRegex);
		}
		this.secondaryType = secondaryType;
		this.cloudConnection = cloudConnection;
	}
	
	// "abnormal" getters
	public String returnPrefix(){
		return this.prefixPath;
	}
	
	public String returnLoggingType(){
		return this.loggingType;
	}
	
	// "normal" getters
	public String getLoggingType() {
		return loggingType;
	}

	public String getPrefixPath() {
		return prefixPath;
	}

	public String getSecondaryType() {
		return secondaryType;
	}

	public String getCloudConnection() {
		return cloudConnection;
	}
	
	public Pattern getMainPattern() {
		return this.mainPattern;
	}	

}
