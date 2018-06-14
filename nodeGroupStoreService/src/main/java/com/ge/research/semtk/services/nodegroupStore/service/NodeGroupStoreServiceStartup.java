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

import java.util.TreeMap;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.ge.research.semtk.utility.Utility;

@Component
public class NodeGroupStoreServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  // print and validate properties - and exit if invalid
	  String[] propertyNames = {
			  "store.ingestorLocation",
			  "store.ingestorProtocol",
			  "store.ingestorPort",
			  "store.sparqlServiceServer",
			  "store.sparqlServicePort",
			  "store.sparqlServiceProtocol",
			  "store.sparqlServiceEndpoint",
			  "store.sparqlConnServerAndPort",
			  "store.sparqlConnDataDataset",
			  "store.sparqlConnModelDataset",
			  "store.sparqlConnDomain",
			  "store.sparqlConnType",
			  "store.sparqlServiceUser"
	  };
	  TreeMap<String,String> properties = new TreeMap<String,String>();
	  for(String propertyName : propertyNames){
		  properties.put(propertyName, event.getApplicationContext().getEnvironment().getProperty(propertyName));
	  }
	  Utility.validatePropertiesAndExitOnFailure(properties); 
	  
	  return;
  }
 
}
