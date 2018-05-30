package com.ge.research.semtk.springsvclib.security.props;



// Your service needs a class that extends this with annotations like
// @Configuration 
// @EnableConfigurationProperties 
// @ConfigurationProperties(prefix="credentials") 
 

public class CredentialsProperties {
	private String resourceId;

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	
}

