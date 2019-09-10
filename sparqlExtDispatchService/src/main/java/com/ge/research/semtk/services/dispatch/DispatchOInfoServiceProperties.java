package com.ge.research.semtk.services.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;
@Configuration
@ConfigurationProperties(prefix="dispatch.oinfo", ignoreUnknownFields = true)
public class DispatchOInfoServiceProperties extends ServiceProperties {

	public DispatchOInfoServiceProperties() {
		super();
		this.setPrefix("dispatch.oinfo");
	}
	
}
