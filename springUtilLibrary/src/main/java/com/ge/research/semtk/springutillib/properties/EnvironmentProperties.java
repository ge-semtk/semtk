package com.ge.research.semtk.springutillib.properties;

import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import com.ge.research.semtk.properties.Properties;


public class EnvironmentProperties extends Properties {
	
	public static final String [] STANDARD_PROPERTIES = (new String []{
			  "ssl.enabled"
	});
	private String[] propertyNames;
	private ApplicationContext context = null;;
	
	public EnvironmentProperties(ApplicationContext context, String[] propertyNames) {
		this.context = context;
		this.setPrefix("");
		this.propertyNames = propertyNames;
	}
	
	public void validate() throws Exception {
		super.validate();
		
		if (this.context == null) {
			throw new Exception("Internal error: application context is null");
		}
		TreeMap<String,String> properties = new TreeMap<String,String>();
		for(String propName : this.propertyNames){
			String propValue = context.getEnvironment().getProperty(propName);
			this.notEmpty(propName, propValue);
		}
	}
}
