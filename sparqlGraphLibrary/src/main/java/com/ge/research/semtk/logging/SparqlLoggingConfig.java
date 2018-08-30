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

import com.ge.research.semtk.logging.LoggingServiceConfig;

public class SparqlLoggingConfig extends LoggingServiceConfig {
	
	// only used in situations where sparql logging is performed. 
	private String sparqlServerUrl;
	private String sparqlServerType;
	private String userName;
	private String password;
	
	public SparqlLoggingConfig(String logOption, String prefix,  String mainRegex, String secondaryType, String cloudConnection,
			String sparqlServerUrl, String sparqlServerType, String uName, String pass) {
		super(logOption, prefix, mainRegex, secondaryType, cloudConnection);
		this.sparqlServerUrl = sparqlServerUrl;
		this.sparqlServerType = sparqlServerType;
		this.userName = uName;
		this.password = pass;
	}
	/**
	 * return the user name and password for the sparql endpoint as a string array. 
	 * @return [username, password]
	 */
	public String[] getUserAndPass(){
		String retval[] = new String[2];
		retval[0] = this.userName;
		retval[1] = this.password;
		
		return retval;
	}
	
	public String getSparqlServerType(){
		return this.sparqlServerType;
	}

	public String getSparqlServerUrl(){
		return this.sparqlServerUrl;
	}
}
