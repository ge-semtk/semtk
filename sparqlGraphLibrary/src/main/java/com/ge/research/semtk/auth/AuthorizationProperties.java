/**
 ** Copyright 2018 General Electric Company
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
package com.ge.research.semtk.auth;

public class AuthorizationProperties {
	int refreshFreqSeconds = 300;
	String graphName = "http://research.ge.com/semtk/services";

	public String getGraphName() {
		return graphName;
	}

	public void setGraphName(String graphName) {
		this.graphName = graphName;
	}

	public int getRefreshFreqSeconds() {
		return refreshFreqSeconds;
	}

	public void setRefreshFreqSeconds(int refreshFreqSeconds) {
		this.refreshFreqSeconds = refreshFreqSeconds;
	}
}
