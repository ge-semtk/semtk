package com.ge.research.semtk.services.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="ingestion.status", ignoreUnknownFields = true)
public class StatusServiceProperties extends com.ge.research.semtk.properties.StatusServiceProperties {

	public StatusServiceProperties() {
		super();
		setPrefix("ingestion.status");
	}

}
