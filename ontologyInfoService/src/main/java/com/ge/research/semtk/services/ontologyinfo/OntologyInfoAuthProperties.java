package com.ge.research.semtk.services.ontologyinfo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.auth.AuthorizationProperties;

@Configuration
@ConfigurationProperties(prefix="auth", ignoreUnknownFields = true)
public class OntologyInfoAuthProperties extends AuthorizationProperties {
	public OntologyInfoAuthProperties()  {
		super();
		this.setPrefix("auth");
	}
}
