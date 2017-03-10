package com.ge.research.semtk.services.ontologyinfo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="oinfo", ignoreUnknownFields = true)
public class OntologyServiceProperties {

	private String serverType; 
	
	private String serverURL;

	public String getServerType() {
		return serverType;
	}

	public void setServerType(String serverType) {
		this.serverType = serverType;
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	} 
	
	
}