package com.ge.research.semtk.services.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="ingestion.status", ignoreUnknownFields = true)
public class StatusServiceProperties extends ServiceProperties {

}
