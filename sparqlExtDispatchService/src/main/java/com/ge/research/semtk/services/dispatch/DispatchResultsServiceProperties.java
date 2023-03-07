package com.ge.research.semtk.services.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ResultsServiceProperties;

@Configuration
@ConfigurationProperties(prefix="dispatch.results", ignoreUnknownFields = true)
public class DispatchResultsServiceProperties extends ResultsServiceProperties {

	public DispatchResultsServiceProperties() {
		super();
		this.setPrefix("dispatch.results");
	}
	
}
