package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.ngstore", ignoreUnknownFields = true)
public class NodegroupExecutionStoreProperties extends ServiceProperties {
	public NodegroupExecutionStoreProperties() {
		super();
		setPrefix("node-group-execution.ngStore");
	}
}
