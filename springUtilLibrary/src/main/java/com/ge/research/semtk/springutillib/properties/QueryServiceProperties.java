package com.ge.research.semtk.springutillib.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.properties.ServiceProperties;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

@Configuration
@ConfigurationProperties(prefix="query.service", ignoreUnknownFields = true)
public class QueryServiceProperties extends ServiceProperties {
	public String user = "";
	public void setUser(String user) {
		this.user = user;
	}
	public QueryServiceProperties() {
		super();
		setPrefix("query.service");
	}
	

	public void setPassword(String password) {
		this.password = password;
	}
	public String password = "";
	
	
	
	public String getUser() { return user; }
	public String getPassword() { return password; }
	
	// ============= functions add user and password if they are blank ===============
	public SparqlEndpointInterface setUserAndPasswordIfMissing(SparqlEndpointInterface sei) {
		if (sei.getUserName() == null || sei.getUserName().isBlank())
			sei.setUserAndPassword(this.user, this.password);
		return sei;
	}
	
	public SparqlConnection setUserAndPasswordIfMissing(SparqlConnection conn) {
		for (SparqlEndpointInterface sei : conn.getAllInterfaces())
			this.setUserAndPasswordIfMissing(sei);
		return conn;
	}
	
	public SparqlGraphJson setUserAndPasswordIfMissing(SparqlGraphJson sgjson) {
		SparqlConnection conn = null;
		try {
			conn = sgjson.getSparqlConn();
		} catch (Exception e) {
			return sgjson;
		}
		this.setUserAndPasswordIfMissing(conn);
		sgjson.setSparqlConn(conn);
		return sgjson;
	}
}
