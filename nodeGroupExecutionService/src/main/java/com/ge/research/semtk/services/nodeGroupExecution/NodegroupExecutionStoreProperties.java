package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.NgStoreServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.ngstore", ignoreUnknownFields = true)
public class NodegroupExecutionStoreProperties extends NgStoreServiceProperties {
	public NodegroupExecutionStoreProperties() {
		super();
		setPrefix("node-group-execution.ngStore");
	}
}
