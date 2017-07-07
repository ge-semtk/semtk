package com.ge.research.semtk.services.results;

import org.mortbay.jetty.security.ClientCertAuthenticator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.ge.research.semtk.services.results.cleanUp.DeleteThread;

@Component
public class ResultsServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  private static final Integer DEFAULT_CLEANUP_FREQUENCY = 120; // time in minutes.
	
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
	  
	  cleanUpFileLocation(event);
	  
	  return;
  }
 
  private void cleanUpFileLocation(final ApplicationReadyEvent event){
	  
	  // check for the presence of the "cleanUpThreadEnabled" property and 
	  System.err.println("set up for cleanup job");
	  
	  String runCleanUp = null;
	  try{
		  runCleanUp = event.getApplicationContext().getEnvironment().getProperty("results.cleanUpThreadEnabled");
	  }
	  catch(Exception eee){
		  System.err.println("Unable to convert results.cleanUpThreadEnabled to a boolean value. no cleanup will be performed.");
		  return;
	  }
	  Integer cleanUpFreq = null;
	  if(runCleanUp.equalsIgnoreCase("yes")){
		  try{
			  cleanUpFreq = Integer.getInteger(event.getApplicationContext().getEnvironment().getProperty("results.cleanUpThreadFrequency"));
			  if(cleanUpFreq == null){ cleanUpFreq = this.DEFAULT_CLEANUP_FREQUENCY; }
		  }
		  catch(Exception eee){
			  cleanUpFreq = this.DEFAULT_CLEANUP_FREQUENCY;
		  }
		  
		  // get the file storage location:
		  String fileStore = event.getApplicationContext().getEnvironment().getProperty("results.fileLocation");
			  
		  // setup and run the actual thread. 
		  DeleteThread ripper = new DeleteThread(fileStore, cleanUpFreq);
		  ripper.run();
	  }
	  else{
		  System.err.println("cleanup disabled. no cleanup will be performed.");
		  return;		  
	  }
	  
  }
  
}
