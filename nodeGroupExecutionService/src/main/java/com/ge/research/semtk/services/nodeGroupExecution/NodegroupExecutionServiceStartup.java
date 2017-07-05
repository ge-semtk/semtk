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
	  
	  System.out.println("storedNodegroupExecution.ngStoreProtocol: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.ngStoreProtocol"));
	  System.out.println("storedNodegroupExecution.ngStoreServer: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.ngStoreServer"));
	  System.out.println("storedNodegroupExecution.ngStorePort: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.ngStorePort"));

	  System.out.println("storedNodegroupExecution.dispatchProtocol: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.dispatchProtocol"));
	  System.out.println("storedNodegroupExecution.dispatchServer: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.dispatchServer"));
	  System.out.println("storedNodegroupExecution.dispatchPort: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.dispatchPort"));

	  System.out.println("storedNodegroupExecution.resultsProtocol: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.resultsProtocol"));
	  System.out.println("storedNodegroupExecution.resultsServer: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.resultsServer"));
	  System.out.println("storedNodegroupExecution.resultsPort: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.resultsPort"));

	  System.out.println("storedNodegroupExecution.statusProtocol: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.statusProtocol"));
	  System.out.println("storedNodegroupExecution.statusServer: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.statusServer"));
	  System.out.println("storedNodegroupExecution.statusPort: " + event.getApplicationContext().getEnvironment().getProperty("storedNodegroupExecution.statusPort"));
	  
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
