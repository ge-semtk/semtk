package com.ge.research.semtk.utility;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class Utility {

	
	/**
	 * Retrieve a property from a properties file.
	 * @param propertyFile the property file
	 * @param key the name of the property to retrieve
	 */
	public static String getPropertyFromFile(String propertyFile, String key) throws Exception{

		System.out.println("Loading properties from " + propertyFile);
	
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(new File(propertyFile)));
		} catch (Exception e) {
		    throw new Exception("Cannot load properties file " + propertyFile, e);
		}
		// now read the property		
		String ret = properties.getProperty(key);
		if(ret == null){
		    throw new Exception("Cannot read property '" + key + "' from " + propertyFile);	
		}
		
		return ret;
	}
	
}
