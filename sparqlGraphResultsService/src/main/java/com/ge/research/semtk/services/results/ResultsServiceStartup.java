/**
 ** Copyright 2017 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.services.results;

import java.io.File;

import org.mortbay.jetty.security.ClientCertAuthenticator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.ge.research.semtk.services.results.cleanUp.DeleteThread;

@Component
public class ResultsServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  private static final Integer DEFAULT_CLEANUP_FREQUENCY = 120; // time in minutes.

  ResultsEdcConfigProperties edcProp;
  
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
	  System.out.println("results.cleanUpThreadEnabled: " + event.getApplicationContext().getEnvironment().getProperty("results.cleanUpThreadEnabled"));
	  System.out.println("results.cleanUpThreadFrequency: " + event.getApplicationContext().getEnvironment().getProperty("results.cleanUpThreadFrequency"));
	  System.out.println("-----------------------");
	  
	  initializeResultsLocation(event);
	  cleanUpFileLocation(event);
	  
	  return;
  }
 
  private void initializeResultsLocation(final ApplicationReadyEvent event) {
	  
	  String resultsTempStoreLocation = event.getApplicationContext().getEnvironment().getProperty("results.fileLocation");
	  // try to create the output location, if possible. warn if failed...
	  
	  System.err.println("attempting to create results location");
	  
	  File dirToCreate = new File(resultsTempStoreLocation);
	  if( dirToCreate.mkdirs() ){ System.err.println("requested temp storage directory (" + resultsTempStoreLocation + ") successfully created."); }
	  else if( dirToCreate.isDirectory() ){ System.out.println("requested temp storage directory (" + resultsTempStoreLocation + ") already exists."); }
	  else{ 
		  System.out.println("temp storage directory (" + resultsTempStoreLocation + ") could not be created. Exiting..."); 
		  System.exit(-1);
	   }	  
	  
  }
  
  private void cleanUpFileLocation(final ApplicationReadyEvent event){
	  
	  this.createResultsEdcConfigProperties(event);
	  
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
			  cleanUpFreq = Integer.parseInt(event.getApplicationContext().getEnvironment().getProperty("results.cleanUpThreadFrequency"));
			  System.err.println("Declared cleanup frequency is " + cleanUpFreq + " minutes.");
			  
			                                                                                               
			  if(cleanUpFreq == null){ 
				  cleanUpFreq = this.DEFAULT_CLEANUP_FREQUENCY; 
				  System.err.println("Declared cleanup frequency is null. Overriding to " + cleanUpFreq + " minutes.");
			  }
		  }
		  catch(Exception eee){
			  System.err.println( eee.getMessage() );
			  cleanUpFreq = this.DEFAULT_CLEANUP_FREQUENCY;
			  System.err.println("Declared cleanup frequency is null. Overriding to " + cleanUpFreq + " minutes.");
		  }
		  
		  // get the file storage location:
		  String fileStore = event.getApplicationContext().getEnvironment().getProperty("results.fileLocation");
			  
		  // setup and run the actual thread. 
		  DeleteThread ripper = new DeleteThread(fileStore, cleanUpFreq, edcProp);
		  ripper.start();
	  }
	  else{
		  System.err.println("cleanup disabled. no cleanup will be performed.");
		  return;		  
	  }
	  
  }
  
  private void createResultsEdcConfigProperties(final ApplicationReadyEvent event){
	  
	  this.edcProp = new ResultsEdcConfigProperties();
	  
	  this.edcProp.setJobEndpointDataset( event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointDataset") );
	  this.edcProp.setJobEndpointDomain( event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointDomain") );
	  this.edcProp.setJobEndpointServerUrl( event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointServerUrl") );
	  this.edcProp.setJobEndpointType( event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointType") );
	  this.edcProp.setJobEndpointUsername( event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointUsername") );
	  this.edcProp.setJobEndpointPassword( event.getApplicationContext().getEnvironment().getProperty("results.edc.services.jobEndpointPassword") );
	  
	  
  }
  
}
