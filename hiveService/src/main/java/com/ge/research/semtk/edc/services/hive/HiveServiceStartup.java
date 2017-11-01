package com.ge.research.semtk.edc.services.hive;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;

import com.ge.research.semtk.utility.Utility;

@Component
public class HiveServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  // print and validate properties - and exit if invalid
	  String[] propertyNames = {
			  "hive.username",
			  "hive.executionEngine"
	  };
	  HashMap<String,String> properties = new HashMap<String,String>();
	  for(String propertyName : propertyNames){
		  properties.put(propertyName, event.getApplicationContext().getEnvironment().getProperty(propertyName));
	  }
	  HashSet<String> propertiesSkipValidation = new HashSet<String>();
	  propertiesSkipValidation.add("hive.executionEngine"); 	// this property can be unspecified
	  Utility.validatePropertiesAndExitOnFailure(properties, propertiesSkipValidation); 
	    
	  return;
  }
 
}
