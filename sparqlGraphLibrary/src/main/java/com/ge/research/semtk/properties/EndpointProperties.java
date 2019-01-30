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
public class EndpointProperties {
	private String jobEndpointType = "";
	private String jobEndpointServerUrl = "";
	private String jobEndpointUsername = "";
	private String jobEndpointPassword = "";
	
	public String getJobEndpointUsername() {
		return jobEndpointUsername;
	}
	public void setJobEndpointUsername(String jobEndpointUsername) {
		this.jobEndpointUsername = jobEndpointUsername;
	}
	public String getJobEndpointPassword() {
		return jobEndpointPassword;
	}
	public void setJobEndpointPassword(String jobEndpointPassword) {
		this.jobEndpointPassword = jobEndpointPassword;
	}
	
	public String getJobEndpointType() {
		return jobEndpointType;
	}
	public void setJobEndpointType(String jobEndpointType) {
		this.jobEndpointType = jobEndpointType;
	}
	
	public String getJobEndpointServerUrl() {
		return jobEndpointServerUrl;
	}
	public void setJobEndpointServerUrl(String jobEndpointServerUrl) {
		this.jobEndpointServerUrl = jobEndpointServerUrl;
	}
	
}
