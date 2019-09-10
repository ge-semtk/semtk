package com.ge.research.semtk.services.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;


@Configuration
@ConfigurationProperties(prefix="dispatch.nodegroupstore", ignoreUnknownFields = true)
public class DispatchNGStoreProperties extends ServiceProperties {

	public DispatchNGStoreProperties() {
		super();
		setPrefix("dispatch.nodegroupstore");
	}

}
