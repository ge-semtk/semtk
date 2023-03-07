package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.IngestorServiceProperties;

@Configuration
@ConfigurationProperties(prefix="node-group-execution.ingest", ignoreUnknownFields = true)
public class NodegroupExecutionIngestProperties extends IngestorServiceProperties {
	public NodegroupExecutionIngestProperties() {
		super();
		setPrefix("node-group-execution.ingest");
	}
}
