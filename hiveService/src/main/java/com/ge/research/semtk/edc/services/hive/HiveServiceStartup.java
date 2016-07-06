package com.ge.research.semtk.edc.services.hive;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HiveServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  System.out.println("hive.username: " + event.getApplicationContext().getEnvironment().getProperty("hive.username"));	  
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
