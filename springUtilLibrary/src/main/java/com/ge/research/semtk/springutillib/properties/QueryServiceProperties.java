package com.ge.research.semtk.springutillib.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="query", ignoreUnknownFields = true)
public class QueryServiceProperties extends ServiceProperties {

	public QueryServiceProperties() {
		super();
		setPrefix("query");
	}

}
