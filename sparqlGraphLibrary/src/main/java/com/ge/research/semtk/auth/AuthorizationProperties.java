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
