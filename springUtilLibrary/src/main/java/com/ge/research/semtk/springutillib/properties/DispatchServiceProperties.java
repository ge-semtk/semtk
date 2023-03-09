package com.ge.research.semtk.springutillib.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix="dispatch.service", ignoreUnknownFields = true)
public class DispatchServiceProperties extends com.ge.research.semtk.properties.DispatchServiceProperties {
	public DispatchServiceProperties() {
		super();
		setPrefix("dispatch.service");
	}
}
