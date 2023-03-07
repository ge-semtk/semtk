package com.ge.research.semtk.services.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="ingestion.results", ignoreUnknownFields = true)
public class ResultsServiceProperties extends com.ge.research.semtk.properties.ResultsServiceProperties {

	public ResultsServiceProperties() {
		super();
		setPrefix("ingestion.results");
	}

}
