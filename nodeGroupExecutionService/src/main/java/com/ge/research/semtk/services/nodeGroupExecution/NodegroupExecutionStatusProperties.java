package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.status", ignoreUnknownFields = true)
public class NodegroupExecutionStatusProperties extends ServiceProperties {
	public NodegroupExecutionStatusProperties() {
		super();
		setPrefix("node-group-execution.status");
	}
}
