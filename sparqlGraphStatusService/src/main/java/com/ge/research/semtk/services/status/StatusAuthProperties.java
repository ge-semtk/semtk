package com.ge.research.semtk.services.status;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.auth.AuthorizationProperties;

@Configuration
@ConfigurationProperties(prefix="auth", ignoreUnknownFields = true)
public class StatusAuthProperties extends AuthorizationProperties {

	public StatusAuthProperties() {
		super();
		setPrefix("auth");
	}
	
}
