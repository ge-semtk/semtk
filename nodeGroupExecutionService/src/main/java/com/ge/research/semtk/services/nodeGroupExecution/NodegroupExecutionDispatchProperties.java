package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.DispatchServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.dispatch", ignoreUnknownFields = true)
public class NodegroupExecutionDispatchProperties extends DispatchServiceProperties {
	public NodegroupExecutionDispatchProperties() {
		super();
		setPrefix("node-group-execution.dispatch");
	}
}
