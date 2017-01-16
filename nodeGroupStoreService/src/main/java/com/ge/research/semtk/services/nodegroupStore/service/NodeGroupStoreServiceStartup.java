package com.ge.research.semtk.services.nodegroupStore.service;

@Component
public class NodeGroupStoreServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  
	  System.out.println("store.templateLocation: " + event.getApplicationContext().getEnvironment().getProperty("store.templateLocation"));
	  System.out.println("store.ingestorLocation: " + event.getApplicationContext().getEnvironment().getProperty("store.ingestorLocation"));
	  System.out.println("store.ingestorProtocol: " + event.getApplicationContext().getEnvironment().getProperty("store.ingestorProtocol"));
	  System.out.println("store.ingestorPort: " + event.getApplicationContext().getEnvironment().getProperty("store.ingestorPort"));
	  
	  System.out.println("store.sparqlServiceServer: " + event.getApplicationContext().getEnvironment().getProperty("store.sparqlServiceServer"));
	  System.out.println("store.sparqlServicePort: " + event.getApplicationContext().getEnvironment().getProperty("store.sparqlServicePort"));
	  System.out.println("store.sparqlServiceProtocol: " + event.getApplicationContext().getEnvironment().getProperty("store.sparqlServiceProtocol"));
	  System.out.println("store.sparqlServiceEndpoint: " + event.getApplicationContext().getEnvironment().getProperty("store.sparqlServiceEndpoint"));
	  
	  System.out.println("store.sparqlServerAndPort: " + event.getApplicationContext().getEnvironment().getProperty("store.sparqlServerAndPort"));
	  System.out.println("store.sparqlServerDataSet: " + event.getApplicationContext().getEnvironment().getProperty("store.sparqlServerDataSet"));
	  System.out.println("store.sparqlServerType: " + event.getApplicationContext().getEnvironment().getProperty("store.sparqlServerType"));
	  
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
