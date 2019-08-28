package com.ge.research.semtk.services.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;


@Configuration
@ConfigurationProperties(prefix="dispatch.nodegroupexec", ignoreUnknownFields = true)
public class DispatcherNGEServiceProperties extends ServiceProperties {

	public DispatcherNGEServiceProperties() {
		super();
		setPrefix("dispatch.nodegroupexec");
	}

}
