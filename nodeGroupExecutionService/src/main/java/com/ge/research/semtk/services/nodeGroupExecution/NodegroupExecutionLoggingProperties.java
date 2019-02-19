package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.logging.easyLogger.EasyLogEnabledConfigProperties;


@Configuration
@ConfigurationProperties(prefix="node-group-execution.logging", ignoreUnknownFields = true)
public class NodegroupExecutionLoggingProperties extends EasyLogEnabledConfigProperties {
	public NodegroupExecutionLoggingProperties() {
		setPrefix("node-group-execution.logging");
	}
}