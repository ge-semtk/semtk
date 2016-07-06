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


// TODO refactor to extend ClientConfig
public class LoggerClientConfig {
	// the Logger Client Config class is used to tell the logging client 
	// where it expects to find the matching logging service

	// required values
	private String protocol;
	private String serverName; // may be either the FQDN or IP address.
	private int loggingPort;
	private String loggingServiceLocation; // the service endpoint (i.e "/Logging/LogEvent")
	private String applicationName = "UNKNOWN_APPLICATION";	// should be set by the user at some point.

	// constructor default
	public LoggerClientConfig(String applicationName, String protocol, String serverName, int loggingPort, String loggingServiceLocation) throws Exception{
		// just create the basic logger config. 
		
		//validity and null checks.
		if(!protocol.equalsIgnoreCase("HTTP") && !protocol.equalsIgnoreCase("HTTPS")){
			throw new Exception("unrecognized protocol used for logger: " + protocol + ". HTTP and HTTPS supported.");
		}
		if(serverName == null || serverName == ""){
			throw new Exception("no logging server name provided.");
		}
		
		this.protocol = protocol;
		this.serverName = serverName;
		this.loggingPort = loggingPort;
		this.loggingServiceLocation = loggingServiceLocation;
		
		if(applicationName != null && applicationName != ""){
			// if this check fails. the default name will be used. oh well. at least it was logged. 
			applicationName = applicationName.replaceAll(" ", "_");
			this.applicationName = applicationName;
		}
	}
	// constructor from 
	
	
	// get
	public String getProtocol(){ return this.protocol; }
	public String getServerName(){ return this.serverName; }
	public int getLoggingPort(){ return this.loggingPort; }
	public String getLoggingServiceLocation(){ return this.loggingServiceLocation; }
	public String getApplicationName(){ return this.applicationName;}
	public String getLoggingURLInfo(){
		return this.protocol + "://" + this.serverName + ":" + this.loggingPort + "/" + this.loggingServiceLocation;
	}
	// set
 	public void setProtocol(String protocol) throws Exception{
		if(!protocol.equalsIgnoreCase("HTTPS") && !protocol.equalsIgnoreCase("HTTP")){
			throw new Exception("unrecognized protocol used for logger: " + protocol + ". HTTP and HTTPS supported.");	
		}
		this.protocol = protocol;
	}
	public void setServerName(String serverName) throws Exception{
		if(this.serverName == null || this.serverName == ""){
			throw new Exception("no logging server name provided.");
		}
		this.serverName = serverName;
	}
	public void setLoggingPort(int loggingPort) throws Exception{
		// check for valid range:
		if(loggingPort <= 1024){
			throw new Exception("Port was null or lower than 1024. this is considered invalid.");
		}
		this.loggingPort = loggingPort;
	}
	public void setLoggingServiceLocation(String loggingServiceLocation){
		this.loggingServiceLocation = loggingServiceLocation;
	}
}
