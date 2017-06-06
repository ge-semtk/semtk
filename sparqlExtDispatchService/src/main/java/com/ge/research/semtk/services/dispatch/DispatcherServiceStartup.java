package com.ge.research.semtk.services.dispatch;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DispatcherServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  
	  System.out.println("dispatch.sparqlServiceServer: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.sparqlServiceServer"));
	  System.out.println("dispatch.sparqlServicePort: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.sparqlServicePort"));
	  System.out.println("dispatch.sparqlServiceEndpoint: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.sparqlServiceEndpoint"));
	
	  System.out.println("dispatch.edcSparqlServerAndPort: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.edcSparqlServerAndPort"));
	  System.out.println("dispatch.edcSparqlServerDataset: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.edcSparqlServerDataset"));
	  System.out.println("dispatch.edcSparqlServerType: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.edcSparqlServerType"));

	  System.out.println("dispatch.resultsServiceServer: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.resultsServiceServer"));
	  System.out.println("dispatch.resultsServicePort: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.resultsServicePort"));
	  
	  System.out.println("dispatch.statusServiceServer: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.statusServiceServer"));
	  System.out.println("dispatch.statusServicePort: " + event.getApplicationContext().getEnvironment().getProperty("dispatch.statusServicePort"));
	  
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
