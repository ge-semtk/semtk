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

package com.ge.research.semtk.services.filestaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.ServiceProperties;

@Configuration
@ConfigurationProperties(prefix="filestaging", ignoreUnknownFields = true)
public class FileStagingProperties extends ServiceProperties {
	
	public FileStagingProperties() {
		super();
		setPrefix("filestaging");
	}
	
	private String storeType;
	private String stageDirectory;
	
	public void validate() throws Exception {
		checkNotEmpty("stageDirectory", stageDirectory);
		checkNotEmpty("storeType", storeType);
	}
	
	public String getStoreType() {
		return storeType;
	}

	public void setStoreType(String storeType) {
		this.storeType = storeType;
	}
	
	public String getStageDirectory() {
		return stageDirectory;
	}
	
	public void setStageDirectory(String stageDirectory) {
		this.stageDirectory = stageDirectory;
	}
	
}
