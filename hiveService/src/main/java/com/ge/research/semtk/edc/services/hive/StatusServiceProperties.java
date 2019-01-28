package com.ge.research.semtk.edc.services.hive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.edc.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="hive-service.status", ignoreUnknownFields = true)
public class StatusServiceProperties extends ServiceProperties {

}
