package com.ge.research.semtk.services.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.StatusServiceProperties;

@Configuration
@ConfigurationProperties(prefix="dispatch.status", ignoreUnknownFields = true)
public class DispatchStatusServiceProperties extends StatusServiceProperties {

	public DispatchStatusServiceProperties() {
		super();
		this.setPrefix("dispatch.status");
	}
	
}
