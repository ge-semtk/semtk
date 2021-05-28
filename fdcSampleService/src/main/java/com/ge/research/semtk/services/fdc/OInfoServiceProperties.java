package com.ge.research.semtk.services.fdc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.OntologyInfoServiceProperties;
import com.ge.research.semtk.properties.ServiceProperties;
@Configuration
@ConfigurationProperties(prefix="fdcsample.oinfo", ignoreUnknownFields = true)
public class OInfoServiceProperties extends OntologyInfoServiceProperties {

	public OInfoServiceProperties() {
		super();
		this.setPrefix("fdcsample.oinfo");
	}
	
}
