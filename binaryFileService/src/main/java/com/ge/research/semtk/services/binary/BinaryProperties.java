/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.services.binary;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="binary.hdfs", ignoreUnknownFields = true)
public class BinaryProperties extends ServiceProperties {

	// PEC HERE
	
	// add auth_props to rest controller
	// add log_props to rest controller
	
	public BinaryProperties() {
		super();
		setPrefix("binary.hdfs");
	}
	
	private String sharedDirectory;				// path to location accessible by the cluster and services
	
	public void validate() throws Exception {
		checkNotEmpty("sharedDirectory", sharedDirectory);
	}
	
	public String getSharedDirectory() {
		return sharedDirectory;
	}
	
	public void setSharedDirectory(String sharedDirectory) {
		this.sharedDirectory = sharedDirectory;
	}
	
}
