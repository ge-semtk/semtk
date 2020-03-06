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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.ge.research.semtk.services.results.cleanUp.DeleteThread;
import com.ge.research.semtk.utility.LocalLogger;

@Component
public class ResultsServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

	private static final Integer DEFAULT_CLEANUP_FREQUENCY = 120; // time in minutes.
	
	@Autowired
	ResultsSemtkEndpointProperties servicesgraph_prop;
	
	/**
	 * Code to run after the service starts up.
	 */
	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {

		// initialize and clean up results location
		initializeResultsLocation(event);
		cleanUpFileLocation(event);
		
		return;
	}

	private void initializeResultsLocation(final ApplicationReadyEvent event) {

		String resultsTempStoreLocation = event.getApplicationContext().getEnvironment().getProperty("results.fileLocation");
		// try to create the output location, if possible. warn if failed...

		LocalLogger.logToStdErr("attempting to create results location");

		File dirToCreate = new File(resultsTempStoreLocation);
		if( dirToCreate.mkdirs() ){ LocalLogger.logToStdErr("requested temp storage directory (" + resultsTempStoreLocation + ") successfully created."); }
		else if( dirToCreate.isDirectory() ){ LocalLogger.logToStdOut("requested temp storage directory (" + resultsTempStoreLocation + ") already exists."); }
		else{ 
			LocalLogger.logToStdErr("temp storage directory (" + resultsTempStoreLocation + ") could not be created. Exiting..."); 
			System.exit(-1);
		}	  

	}

	private void cleanUpFileLocation(final ApplicationReadyEvent event){

		// check for the presence of the "cleanUpThreadEnabled" property and 
		LocalLogger.logToStdErr("set up for cleanup job");

		String runCleanUp = null;
		try{
			runCleanUp = event.getApplicationContext().getEnvironment().getProperty("results.cleanUpThreadEnabled");
		}
		catch(Exception eee){
			LocalLogger.logToStdErr("Unable to convert results.cleanUpThreadEnabled to a boolean value. no cleanup will be performed.");
			return;
		}
		Integer cleanUpFreq = null;
		if(runCleanUp.equalsIgnoreCase("yes")){
			try{
				cleanUpFreq = Integer.parseInt(event.getApplicationContext().getEnvironment().getProperty("results.cleanUpThreadFrequency"));
				LocalLogger.logToStdErr("Declared cleanup frequency is " + cleanUpFreq + " minutes.");


				if(cleanUpFreq == null){ 
					cleanUpFreq = DEFAULT_CLEANUP_FREQUENCY; 
					LocalLogger.logToStdErr("Declared cleanup frequency is null. Overriding to " + cleanUpFreq + " minutes.");
				}
			}
			catch(Exception eee){
				LocalLogger.logToStdErr( eee.getMessage() );
				cleanUpFreq = DEFAULT_CLEANUP_FREQUENCY;
				LocalLogger.logToStdErr("Declared cleanup frequency is null. Overriding to " + cleanUpFreq + " minutes.");
			}

			// get the file storage location:
			String fileStore = event.getApplicationContext().getEnvironment().getProperty("results.fileLocation");

			// setup and run the actual thread. 
			DeleteThread ripper = new DeleteThread(fileStore, cleanUpFreq, servicesgraph_prop);
			ripper.start();
		}
		else{
			LocalLogger.logToStdErr("cleanup disabled. no cleanup will be performed.");
			return;		  
		}

	}

}
