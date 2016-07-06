package com.ge.research.semtk.services.ingestion;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class IngestionServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  System.out.println("ingestion.sparqlUserName: " + event.getApplicationContext().getEnvironment().getProperty("ingestion.sparqlUserName"));
	  System.out.println("ingestion.batchSize: " + event.getApplicationContext().getEnvironment().getProperty("ingestion.batchSize"));
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
