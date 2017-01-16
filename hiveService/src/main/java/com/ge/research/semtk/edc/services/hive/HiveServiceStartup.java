package com.ge.research.semtk.edc.services.hive;

@Component
public class HiveServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  System.out.println("hive.username: " + event.getApplicationContext().getEnvironment().getProperty("hive.username"));
	  System.out.println("hive.executionEngine: " + event.getApplicationContext().getEnvironment().getProperty("hive.executionEngine"));
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
