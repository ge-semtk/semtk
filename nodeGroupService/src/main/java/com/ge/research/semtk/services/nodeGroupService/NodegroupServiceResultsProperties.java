package com.ge.research.semtk.services.nodeGroupService;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;
@Configuration
@ConfigurationProperties(prefix="nodegroup.results", ignoreUnknownFields = true)
public class NodegroupServiceResultsProperties extends ServiceProperties {

	public NodegroupServiceResultsProperties() {
		super();
		this.setPrefix("nodegroup.results");
	}
	
}
