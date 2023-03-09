/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.springutillib.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.Properties;

@Configuration
@ConfigurationProperties(prefix="ngstore", ignoreUnknownFields = true)
public class NodegroupStoreProperties extends Properties {
	
    // sparql server and named graph info
    private String sparqlConnServerAndPort = "";
    private String sparqlConnDataDataset = "";
    private String sparqlConnModelDataset = "";
    private String sparqlConnDomain = "";
    private String sparqlConnType = "";
    
	public String getSparqlConnServerAndPort() {
		return sparqlConnServerAndPort;
	}
	public void setSparqlConnServerAndPort(String sparqlConnServerAndPort) {
		this.sparqlConnServerAndPort = sparqlConnServerAndPort;
	}
	public String getSparqlConnDataDataset() {
		return sparqlConnDataDataset;
	}
	public void setSparqlConnDataDataset(String sparqlConnDataDataset) {
		this.sparqlConnDataDataset = sparqlConnDataDataset;
	}
	public String getSparqlConnModelDataset() {
		return sparqlConnModelDataset;
	}
	public void setSparqlConnModelDataset(String sparqlConnModelDataset) {
		this.sparqlConnModelDataset = sparqlConnModelDataset;
	}
	public String getSparqlConnDomain() {
		return sparqlConnDomain;
	}
	public void setSparqlConnDomain(String sparqlConnDomain) {
		this.sparqlConnDomain = sparqlConnDomain;
	}
	public String getSparqlConnType() {
		return sparqlConnType;
	}
	public void setSparqlConnType(String sparqlConnType) {
		this.sparqlConnType = sparqlConnType;
	}

}
