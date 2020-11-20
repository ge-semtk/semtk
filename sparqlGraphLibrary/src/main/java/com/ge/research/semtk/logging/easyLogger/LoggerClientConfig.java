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


package com.ge.research.semtk.logging.easyLogger;

import com.ge.research.semtk.services.client.RestClientConfig;

public class LoggerClientConfig extends RestClientConfig {

	private String applicationName = "UNKNOWN_APPLICATION";	// default

	public LoggerClientConfig(String applicationName, String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint) throws Exception{
		super(serviceProtocol, serviceServer, servicePort, serviceEndpoint);

		if(applicationName != null && applicationName != ""){
			applicationName = applicationName.replaceAll(" ", "_");
			this.applicationName = applicationName;
		}
	} 
	
	public String getApplicationName(){ 
		return this.applicationName;
	}
	
	// TODO REMOVE THESE
	@Deprecated
	public String getProtocol(){ return getServiceProtocol(); }
	@Deprecated
	public String getServerName(){ return getServiceServer(); }
	@Deprecated
	public int getLoggingPort(){ return getServicePort(); }
	@Deprecated
	public String getLoggingServiceLocation(){ return getServiceEndpoint(); }
	@Deprecated
	public String getLoggingURLInfo(){ return getServiceURL(); }

}
