package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.dispatch", ignoreUnknownFields = true)
public class NodegroupExecutionDispatchProperties extends ServiceProperties {
	public NodegroupExecutionDispatchProperties() {
		super();
		setPrefix("node-group-execution.dispatch");
	}
}
