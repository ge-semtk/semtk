package com.ge.research.semtk.services.results;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ResultsServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  System.out.println("results.baseURL: " + event.getApplicationContext().getEnvironment().getProperty("results.baseURL"));
	  System.out.println("results.fileLocation: " + event.getApplicationContext().getEnvironment().getProperty("results.fileLocation"));	  
	  System.out.println("results.edc.services.jobEndpointType: " + event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointType"));
	  System.out.println("results.edc.services.jobEndpointDomain: " + event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointDomain"));
	  System.out.println("results.edc.services.jobEndpointServerUrl: " + event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointServerUrl"));
	  System.out.println("results.edc.services.jobEndpointDataset: " + event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointDataset"));
	  System.out.println("results.edc.services.jobEndpointUsername: " + event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointUsername"));
	  System.out.println("results.edc.services.jobEndpointPassword: " + event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointPassword"));	  
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
