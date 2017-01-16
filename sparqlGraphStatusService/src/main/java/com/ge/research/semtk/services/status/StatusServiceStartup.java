package com.ge.research.semtk.services.status;

@Component
public class StatusServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  System.out.println("status.edc.services.jobEndpointType: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointType"));
	  System.out.println("status.edc.services.jobEndpointDomain: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointDomain"));
	  System.out.println("status.edc.services.jobEndpointServerUrl: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointServerUrl"));
	  System.out.println("status.edc.services.jobEndpointDataset: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointDataset"));
	  System.out.println("status.edc.services.jobEndpointUsername: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointUsername"));
	  System.out.println("status.edc.services.jobEndpointPassword: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointPassword"));	  
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
