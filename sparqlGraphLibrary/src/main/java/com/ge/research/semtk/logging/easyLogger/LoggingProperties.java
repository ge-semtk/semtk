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

import com.ge.research.semtk.properties.Properties;

public class LoggingProperties extends Properties {

	private Boolean loggingEnabled = false;
	private String loggingProtocol = "HTTP";
	private String loggingServer = "";
	private String loggingPort = "";
	private String loggingServiceLocation = "/Logging/usageLog";
	private String applicationLogName = "";
	
	public void setLoggingEnabled(Boolean loggingEnabled){ this.loggingEnabled = loggingEnabled;}
	public void setLoggingProtocol(String loggingProtocol){ this.loggingProtocol = loggingProtocol;}
	public void setLoggingServer(String loggingServer){ this.loggingServer = loggingServer;}
	public void setLoggingPort(String loggingPort){ this.loggingPort = loggingPort;}
	public void setLoggingServiceLocation(String loggingServiceLocation){ this.loggingServiceLocation = loggingServiceLocation;}
	public void setApplicationLogName(String applicationLogName){ this.applicationLogName = applicationLogName;}
	
	public Boolean getLoggingEnabled(){ return this.loggingEnabled; }
	public String getLoggingProtocol(){ return this.loggingProtocol; }
	public String getLoggingServer(){ return this.loggingServer; }
	public String getLoggingPort(){ return this.loggingPort; }
	public String getLoggingServiceLocation(){ return this.loggingServiceLocation; }
	public String getApplicationLogName(){ return this.applicationLogName; }
	
	public void validate() throws Exception {
		super.validate();
		checkNone("loggingEnabled", loggingEnabled);
		checkNone("loggingProtocol", loggingProtocol);
		checkNone("loggingServer", loggingServer);
		checkNone("loggingPort", loggingPort);
		checkNone("loggingServiceLocation", loggingServiceLocation);
		checkNone("applicationLogName", applicationLogName);
	}

	public LoggerRestClient getClient() throws Exception {
		LoggerClientConfig config = new LoggerClientConfig(applicationLogName, loggingProtocol, loggingServer, Integer.parseInt(loggingPort), loggingServiceLocation);
		return new LoggerRestClient(config);
	}
}
