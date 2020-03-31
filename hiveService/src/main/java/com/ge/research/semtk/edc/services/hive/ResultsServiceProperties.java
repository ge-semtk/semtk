package com.ge.research.semtk.edc.services.hive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="hive-service.results", ignoreUnknownFields = true)
public class ResultsServiceProperties extends ServiceProperties {
	public ResultsServiceProperties() {
		super();
		setPrefix("hive-service.results");
	}
}
