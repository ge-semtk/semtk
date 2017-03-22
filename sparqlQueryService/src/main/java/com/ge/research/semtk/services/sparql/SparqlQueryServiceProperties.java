package com.ge.research.semtk.services.sparql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="triplestore", ignoreUnknownFields = true)
public class SparqlQueryServiceProperties {
	
	private String serverAndPort;  	
	
	private String serverType;

	public String getServerAndPort() {
		return serverAndPort;
	}

	public void setServerAndPort(String serverAndPort) {
		this.serverAndPort = serverAndPort;
	}

	public String getServerType() {
		return serverType;
	}

	public void setServerType(String serverType) {
		this.serverType = serverType;
	}
	
	

}
