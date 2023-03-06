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

import java.util.Arrays;

import org.json.simple.JSONObject;

public class AuthorizationProperties {
	private String prefix = "<unset>";
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
	
	/**
	 * Designed to be called from @PostConstruct in a @RestController
	 * Validate and exit with message instead of giant long stack trace.
	 * On success: prints all values
	 */
	public void validateWithExit() {
		try {
			this.validate();
		} catch (Exception e) {
			System.err.println(e.toString());
			System.err.println("exiting.");
			System.exit(1);
		}
	}
	
	
	/**
	 * Call this from your top level annotated properties class
	 * @param prefix
	 */
	public void setPrefix(String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			this.prefix = "";
		} else {
			this.prefix = prefix + ".";
		}
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public void validate() throws Exception {
		System.out.println("---- Properties ----");
		if (refreshFreqSeconds < 1 || refreshFreqSeconds > 3600) {
			throw new Exception(this.getPrefixedNameValue("refreshFreqSeconds", (Integer) refreshFreqSeconds) + " must be between " + String.valueOf(1) + " and " + String.valueOf(3600));

		}
		System.out.println(this.getPrefixedNameValue("refreshFreqSeconds", refreshFreqSeconds));
		System.out.println(this.getPrefixedNameValue("settingsFilePath", settingsFilePath));
		System.out.println(this.getPrefixedNameValue("logPath", logPath));
		System.out.println(this.getPrefixedNameValue("usernameKey", usernameKey));
		System.out.println(this.getPrefixedNameValue("groupKey", groupKey));
	}
	
	/**
	 * Get printable prevfix.name=value
	 * Blocks out "password" fields' values
	 * @param name
	 * @param v
	 * @return
	 */
	private String getPrefixedNameValue(String name, Object v) {
		String out;
		if (v == null) {
			out = "<null>";
		} else if (v.getClass().isArray()) {
			out = Arrays.toString((String []) v);
		} else {
			out = v.toString();
		}
		return this.prefix + name + "=" + out;
	}
}
