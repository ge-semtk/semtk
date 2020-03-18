package com.ge.research.semtk.springutillib.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.SemtkEndpointProperties;

// subclass simply adds prefix and spring annotations

@Configuration
@ConfigurationProperties(prefix="servicesgraph", ignoreUnknownFields = true)
public class ServicesGraphProperties extends SemtkEndpointProperties {

	public ServicesGraphProperties() {
		super();
		setPrefix("servicesgraph");
	}

}
