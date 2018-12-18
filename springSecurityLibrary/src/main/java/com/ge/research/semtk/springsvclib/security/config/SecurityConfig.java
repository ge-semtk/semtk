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
// your service needs a class that extends this with annotations like:
//@Configuration
//@EnableResourceServer

/*-------------- sample extension----------------
 
@Configuration
@EnableResourceServer
public class My_SecurityConfig extends SecurityConfig {

	//@Autowired
	//private Environment environment; 

	@Autowired(required = false)
	private My_LocalNetworkProperties localNetworkProperties;
	
	@Autowired
	private My_CredentialsProperties credentialsProperties;

	@Autowired 
	private My_Properties utilityProperties;
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		
		this.configureProxy(http, localNetworkProperties);
		this.httpRequiresChannel(http);

		
		if (utilityProperties.useSecurity()) {
			LocalLogger.logToStdOut("Launching with security");
			http.authorizeRequests()
				.antMatchers(HttpMethod.POST, "/serviceInfo/**").permitAll()
				.antMatchers(HttpMethod.POST, "/utility/serviceInfo/**").permitAll()
				.antMatchers(HttpMethod.GET, "/utility/**").authenticated()
				.antMatchers(HttpMethod.POST, "/utility/**").authenticated()
				;
		} else {
			this.noSecurity(http);
		}

	}
	
	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		this.configureResources(resources, credentialsProperties);
	}

}

------------------------------------------*/


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

	/* // this is not compatible with springboot 2
	protected void httpRequiresChannel(HttpSecurity http) throws Exception {
		if (securityProperties.isRequireSsl()) {
			http.requiresChannel().anyRequest().requiresSecure();
		}
	}
	*/
	
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
