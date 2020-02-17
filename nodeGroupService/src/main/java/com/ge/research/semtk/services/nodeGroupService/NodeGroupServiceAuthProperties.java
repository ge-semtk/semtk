package com.ge.research.semtk.services.nodeGroupService;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.auth.AuthorizationProperties;

@Configuration
@ConfigurationProperties(prefix="auth", ignoreUnknownFields = true)
public class NodeGroupServiceAuthProperties extends AuthorizationProperties {

	public NodeGroupServiceAuthProperties() {
		super();
		setPrefix("auth");
	}

}
