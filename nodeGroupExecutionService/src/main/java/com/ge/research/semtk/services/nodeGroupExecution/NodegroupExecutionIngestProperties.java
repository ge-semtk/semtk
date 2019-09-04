package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.ingest", ignoreUnknownFields = true)
public class NodegroupExecutionIngestProperties extends ServiceProperties {
	public NodegroupExecutionIngestProperties() {
		super();
		setPrefix("node-group-execution.ingest");
	}
}
