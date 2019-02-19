package com.ge.research.semtk.services.results;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.auth.AuthorizationProperties;

@Configuration
@ConfigurationProperties(prefix="auth", ignoreUnknownFields = true)
public class ResultsAuthProperties extends AuthorizationProperties {

	public ResultsAuthProperties() {
		super();
		setPrefix("auth");
	}
}
