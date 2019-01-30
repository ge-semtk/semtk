package com.ge.research.semtk.services.sparql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;
@Configuration
@ConfigurationProperties(prefix="query.oinfo", ignoreUnknownFields = true)
public class OInfoServiceProperties extends ServiceProperties {
	public OInfoServiceProperties() {
		super();
		this.setPrefix("query.oinfo");
	}
}
