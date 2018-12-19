package com.ge.research.semtk.services.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.edc.ServiceProperties;
@Configuration
@ConfigurationProperties(prefix="ingestion.results", ignoreUnknownFields = true)
public class ResultsServiceProperties extends ServiceProperties {

}
