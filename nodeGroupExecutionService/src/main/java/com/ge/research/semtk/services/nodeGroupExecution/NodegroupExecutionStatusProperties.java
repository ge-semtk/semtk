package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.StatusServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.status", ignoreUnknownFields = true)
public class NodegroupExecutionStatusProperties extends StatusServiceProperties {
	public NodegroupExecutionStatusProperties() {
		super();
		setPrefix("node-group-execution.status");
	}
}
