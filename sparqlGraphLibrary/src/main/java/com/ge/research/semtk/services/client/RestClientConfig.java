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


package com.ge.research.semtk.services.client;

/**
 * Configuration class for a REST client.
 * Create subclass clients for specific services.
 */
public abstract class RestClientConfig {

	public enum Methods {POST, GET};

	protected String serviceProtocol;
	protected String serviceServer; // may be either the FQDN or IP address.
	protected int servicePort;
	protected String serviceEndpoint; // the service endpoint (i.e "/Logging/LogEvent")
	//protected String serviceUser; 	// TODO
	//protected String servicePassword; // TODO
	protected Methods method = Methods.POST;
	
	/**
	 * Default constructor
	 */
	public RestClientConfig(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint) throws Exception{ 
		
		// validate
		if(!serviceProtocol.equalsIgnoreCase("HTTP") && !serviceProtocol.equalsIgnoreCase("HTTPS")){
			throw new Exception("Unrecognized protocol: " + serviceProtocol + ". HTTP and HTTPS supported.");
		}
		if(serviceServer == null || serviceServer == ""){
			throw new Exception("No server name provided.");
		}
		if(serviceEndpoint == null || serviceEndpoint == ""){
			throw new Exception("No service endpoint provided.");
		}		
		
		this.serviceProtocol = serviceProtocol;
		this.serviceServer = serviceServer;
		this.servicePort = servicePort;
		this.serviceEndpoint = serviceEndpoint;
	}


	public RestClientConfig(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint, Methods method) throws Exception {
		this(serviceProtocol, serviceServer, servicePort, serviceEndpoint);
		this.method = method;
	}

	public String getServiceProtocol(){
		return this.serviceProtocol; 
	}
	
	public String getServiceServer(){ 
		return this.serviceServer; 
	}
	
	public int getServicePort(){ 
		return this.servicePort; 
	}
	
	public String getServiceEndpoint(){ 
		return this.serviceEndpoint; 
	}
	
	public String getServiceURL(){
		return this.serviceProtocol + "://" + this.serviceServer + ":" + this.servicePort + "/" + this.serviceEndpoint;
	}
	
	

 	public void setServiceProtocol(String serviceProtocol) throws Exception{
		if(!serviceProtocol.equalsIgnoreCase("HTTPS") && !serviceProtocol.equalsIgnoreCase("HTTP")){
			throw new Exception("Unrecognized protocol: " + serviceProtocol + ". HTTP and HTTPS supported.");	
		}
		this.serviceProtocol = serviceProtocol;
	}
 	
	public void setServiceServer(String serviceServer) throws Exception{
		if(this.serviceServer == null || this.serviceServer == ""){
			throw new Exception("No server name provided.");
		}
		this.serviceServer = serviceServer;
	}
	
	public void setServicePort(int servicePort) throws Exception{
		// check for valid range:
		if(servicePort <= 1024){
			throw new Exception("Service port was null or lower than 1024 - this is considered invalid.");
		}
		this.servicePort = servicePort;
	}
	
	public void setServiceEndpoint(String serviceEndpoint){
		this.serviceEndpoint = serviceEndpoint;
	}

	public Methods getMethod() {
		return method;
	}

	public void setMethod(Methods method) {
		this.method = method;
	}

}
