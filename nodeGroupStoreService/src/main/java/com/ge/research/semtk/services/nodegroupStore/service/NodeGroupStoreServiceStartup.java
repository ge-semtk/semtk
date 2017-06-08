/**
 ** Copyright 2016 General Electric Company
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

package com.ge.research.semtk.services.nodegroupStore.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

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
