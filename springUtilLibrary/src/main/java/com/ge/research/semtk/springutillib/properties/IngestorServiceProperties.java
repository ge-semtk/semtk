package com.ge.research.semtk.springutillib.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="ingest", ignoreUnknownFields = true)
public class IngestorServiceProperties extends com.ge.research.semtk.properties.IngestorServiceProperties {
	public IngestorServiceProperties() {
		super();
		setPrefix("ingest");
	}
}
