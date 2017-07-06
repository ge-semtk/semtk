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

package com.ge.research.semtk.services.nodeGroupExecution;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class NodegroupExecutionServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  
	  System.out.println("nodeGroupExecution.ngStoreProtocol: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.ngStoreProtocol"));
	  System.out.println("nodeGroupExecution.ngStoreServer: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.ngStoreServer"));
	  System.out.println("nodeGroupExecution.ngStorePort: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.ngStorePort"));

	  System.out.println("nodeGroupExecution.dispatchProtocol: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.dispatchProtocol"));
	  System.out.println("nodeGroupExecution.dispatchServer: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.dispatchServer"));
	  System.out.println("nodeGroupExecution.dispatchPort: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.dispatchPort"));

	  System.out.println("nodeGroupExecution.resultsProtocol: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.resultsProtocol"));
	  System.out.println("nodeGroupExecution.resultsServer: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.resultsServer"));
	  System.out.println("nodeGroupExecution.resultsPort: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.resultsPort"));

	  System.out.println("nodeGroupExecution.statusProtocol: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.statusProtocol"));
	  System.out.println("nodeGroupExecution.statusServer: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.statusServer"));
	  System.out.println("nodeGroupExecution.statusPort: " + event.getApplicationContext().getEnvironment().getProperty("nodeGroupExecution.statusPort"));
	  
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
