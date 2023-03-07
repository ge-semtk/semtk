package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.OntologyInfoServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.oinfo", ignoreUnknownFields = true)
public class OInfoServiceProperties extends OntologyInfoServiceProperties {

	public OInfoServiceProperties() {
		super();
		this.setPrefix("node-group-execution.oinfo");
	}
	
}
