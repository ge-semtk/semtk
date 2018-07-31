package com.ge.research.semtk.auth;

public class AuthorizationProperties {
	int refreshFreqSeconds = 300;

	public int getRefreshFreqSeconds() {
		return refreshFreqSeconds;
	}

	public void setRefreshFreqSeconds(int refreshFreqSeconds) {
		this.refreshFreqSeconds = refreshFreqSeconds;
	}
}
