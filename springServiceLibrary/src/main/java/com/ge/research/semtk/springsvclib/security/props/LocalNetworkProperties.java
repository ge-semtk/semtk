package com.ge.research.semtk.springsvclib.security.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * proxy properties
 *
 */

// Your service needs a class that extends this with annotations like:
// @Configuration 
// @EnableConfigurationProperties 
// @ConfigurationProperties(prefix="local.network") 

public class LocalNetworkProperties {
	private String proxyHost; 
	private String proxyPort;	
	private String nonProxyHosts;
	
	public String getProxyHost() {
		return proxyHost;
	}
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}
	public String getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}
	public String getNonProxyHosts() {
		return nonProxyHosts;
	}
	public void setNonProxyHosts(String nonProxyHosts) {
		this.nonProxyHosts = nonProxyHosts;
	}
	
	
}