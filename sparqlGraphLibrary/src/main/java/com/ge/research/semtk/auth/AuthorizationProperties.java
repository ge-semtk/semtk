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

import org.json.simple.JSONObject;

import com.ge.research.semtk.properties.Properties;

public class AuthorizationProperties extends Properties {
	int refreshFreqSeconds = 300;
	String settingsFilePath = "";
	String logPath = "";
	String usernameKey = "user_name";
	String groupKey = "group";

	/**
	 * Return json version so that SpringUtilLibrary isn't compiled into SparqlGraphLibrary
	 * @return
	 */
	
	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public String getSettingsFilePath() {
		return settingsFilePath;
	}

	public void setSettingsFilePath(String path) {
		this.settingsFilePath = path;
	}

	public int getRefreshFreqSeconds() {
		return refreshFreqSeconds;
	}

	public void setRefreshFreqSeconds(int refreshFreqSeconds) {
		this.refreshFreqSeconds = refreshFreqSeconds;
	}
	
	public String getUsernameKey() {
		return usernameKey;
	}
	
	public void setUsernameKey(String key) {
		this.usernameKey = key;
	}
	
	public String getGroupKey() {
		return groupKey;
	}
	
	public void setGroupKey(String key) {
		this.groupKey = key;
	}
	
	public void validate() throws Exception {
		super.validate();
		checkRangeInclusive("refreshFreqSeconds", refreshFreqSeconds, 1, 3600);
		checkNone("settingsFilePath", settingsFilePath);
		checkNone("logPath", logPath);
		checkNone("usernameKey", usernameKey);
		checkNone("groupKey", groupKey);
	}
}
