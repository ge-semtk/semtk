package com.ge.research.semtk.springutillib.properties;

import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import com.ge.research.semtk.properties.Properties;


public class EnvironmentProperties extends Properties {
	
	public static final String [] SEMTK_REQ_PROPS = (new String []{
			  "server.port"
	});
	public static final String [] SEMTK_OPT_PROPS = (new String []{
			  "server.ssl.enabled",
			  "server.ssl.key-store-type",
			  "server.ssl.key-store",
			  "server.ssl.key-store-password"
	});
	
	private String[] requiredProps;
	private String[] optionalProps;
	
	private ApplicationContext context = null;;
	
	public EnvironmentProperties(ApplicationContext context, String[] reqProps, String [] optProps) {
		this.context = context;
		this.setPrefix("");
		this.requiredProps = reqProps;
		this.optionalProps = optProps;
	}
	
	public void validate() throws Exception {
		super.validate();
		
		if (this.context == null) {
			throw new Exception("Internal error: application context is null");
		}
		TreeMap<String,String> properties = new TreeMap<String,String>();
		for(String propName : this.requiredProps){
			String propValue = context.getEnvironment().getProperty(propName);
			if (propName.toLowerCase().contains("password") && !propValue.isEmpty()) {
				propValue = "xxxxxx";
			}
			this.checkNotEmpty(propName, propValue);
		}
		for(String propName : this.optionalProps){
			String propValue = context.getEnvironment().getProperty(propName);
			if (propName.toLowerCase().contains("password") && !propValue.isEmpty()) {
				propValue = "xxxxxx";
			}
			this.checkNone(propName, propValue);
		}
	}
}
