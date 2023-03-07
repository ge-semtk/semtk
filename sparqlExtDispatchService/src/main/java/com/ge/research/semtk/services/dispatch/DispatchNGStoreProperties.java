package com.ge.research.semtk.services.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.NgStoreServiceProperties;

@Configuration
@ConfigurationProperties(prefix="dispatch.nodegroupstore", ignoreUnknownFields = true)

public class DispatchNGStoreProperties extends NgStoreServiceProperties {

	public DispatchNGStoreProperties() {
		super();
		setPrefix("dispatch.nodegroupstore");
	}

}
