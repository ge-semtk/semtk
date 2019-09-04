package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.results", ignoreUnknownFields = true)
public class NodegroupExecutionResultsProperties extends ServiceProperties {
	public NodegroupExecutionResultsProperties() {
		super();
		setPrefix("node-group-execution.results");
	}
}
