/**
 * 
 */
package com.ge.research.semtk.springsvclib.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

import com.ge.research.semtk.springsvclib.security.props.CredentialsProperties;
import com.ge.research.semtk.springsvclib.security.props.LocalNetworkProperties;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * 
 * Provides help for a SecurityConfig with the annotations commented out below.
 * To get beans to work properly, service and child class must autowire their own
 * 	     LocalNetworkProperties
 *       CredentialProperties
 *       SecurityProperties
 * ...and call these functions
 *
 */
//@Configuration
//@EnableResourceServer
public class SecurityConfig extends ResourceServerConfigurerAdapter {
	
	@Autowired
	private SecurityProperties securityProperties;
	
	protected void configureProxy(HttpSecurity http, LocalNetworkProperties localNetworkProperties) throws Exception {
		if (localNetworkProperties.getProxyHost() != null) {
			LocalLogger.logToStdOut("Setting local proxies");
			System.setProperty("http.proxyHost", localNetworkProperties.getProxyHost());
			System.setProperty("http.proxyPort", localNetworkProperties.getProxyPort());
			System.setProperty("https.proxyHost", localNetworkProperties.getProxyHost());
			System.setProperty("https.proxyPort", localNetworkProperties.getProxyPort());
			System.setProperty("http.nonProxyHosts", localNetworkProperties.getNonProxyHosts());
		}
	}
	
	protected void httpRequiresChannel(HttpSecurity http) throws Exception {
		if (securityProperties.isRequireSsl()) {
			http.requiresChannel().anyRequest().requiresSecure();
		}
	}
	
	protected void noSecurity(HttpSecurity http) throws Exception {
		LocalLogger.logToStdOut("Launching with NO SECURITY");
		http.authorizeRequests()
			.antMatchers(HttpMethod.POST, "/nothing").authenticated()
			;
	}

	protected void configureResources(ResourceServerSecurityConfigurer resources, CredentialsProperties credentialsProperties) {
		// note that null resourceId works fine here.  Won't require anything.
		resources
			.resourceId(credentialsProperties.getResourceId())
			.stateless(true);
	}
}
