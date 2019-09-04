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


package com.ge.research.semtk.properties;

// meant to be inherited into a spring boot microservice property object
// properties for a triple-store endpoint
public class EndpointProperties extends Properties {
	private String endpointType = "";
	private String endpointUrl = "";
	private String endpointUsername = "";
	private String endpointPassword = "";
	
	public String getEndpointUsername() {
		return endpointUsername;
	}
	public void setEndpointUsername(String jobEndpointUsername) {
		this.endpointUsername = jobEndpointUsername;
	}
	public String getEndpointPassword() {
		return endpointPassword;
	}
	public void setEndpointPassword(String jobEndpointPassword) {
		this.endpointPassword = jobEndpointPassword;
	}
	
	public String getEndpointType() {
		return endpointType;
	}
	public void setEndpointType(String jobEndpointType) {
		this.endpointType = jobEndpointType;
	}
	
	public String getEndpointServerUrl() {
		return endpointUrl;
	}
	public void setEndpointServerUrl(String jobEndpointServerUrl) {
		this.endpointUrl = jobEndpointServerUrl;
	}
	
	public void validate() throws Exception {
		super.validate();
		checkNotEmpty("endpointType", endpointType);
		checkNotEmpty("endpointServerUrl", endpointUrl);
		checkNone("endpointUsername", endpointUsername);
		checkNone("endpointPassword", endpointPassword);
	}
	
}
