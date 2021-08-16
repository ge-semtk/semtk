package com.ge.research.semtk.services.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;
@Configuration
@ConfigurationProperties(prefix="ingestion.oinfo", ignoreUnknownFields = true)
public class OInfoServiceProperties extends ServiceProperties {
	public OInfoServiceProperties() {
		super();
		this.setPrefix("ingestion.oinfo");
	}
}
