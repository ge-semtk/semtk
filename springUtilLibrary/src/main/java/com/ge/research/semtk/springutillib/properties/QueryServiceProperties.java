package com.ge.research.semtk.springutillib.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="query.service", ignoreUnknownFields = true)
public class QueryServiceProperties extends ServiceProperties {
	public String user = "";
	public void setUser(String user) {
		this.user = user;
	}
	public QueryServiceProperties() {
		super();
		setPrefix("query.service");
	}
	

	public void setPassword(String password) {
		this.password = password;
	}
	public String password = "";
	
	
	
	public String getUser() { return user; }
	public String getPassword() { return password; }
}
