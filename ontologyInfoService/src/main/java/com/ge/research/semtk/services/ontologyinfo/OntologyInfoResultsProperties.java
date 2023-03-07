package com.ge.research.semtk.services.ontologyinfo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ResultsServiceProperties;

@Configuration
@ConfigurationProperties(prefix="oinfo.results", ignoreUnknownFields = true)
public class OntologyInfoResultsProperties extends ResultsServiceProperties {
	public OntologyInfoResultsProperties() {
		super();
		this.setPrefix("oinfo.results");
	}
}
