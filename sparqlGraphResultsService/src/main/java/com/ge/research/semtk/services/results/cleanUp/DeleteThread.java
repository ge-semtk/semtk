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

package com.ge.research.semtk.services.results.cleanUp;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.resultsStorage.TableResultsStorage;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.utility.LocalLogger;

public class DeleteThread extends Thread {

	private int runFrequencyInMilliseconds;
	private int frequencyInMinutes;
	private File locationToDeleteFrom;	
	private TableResultsStorage trstore;
	private ServicesGraphProperties edcProp;
	
	public DeleteThread(String fileStorageLocation, int frequencyInMinutes, ServicesGraphProperties edcProp){
		this.locationToDeleteFrom = new File(fileStorageLocation);
		this.runFrequencyInMilliseconds = frequencyInMinutes * 60 * 1000;
		this.frequencyInMinutes = frequencyInMinutes;
		this.trstore = new TableResultsStorage(fileStorageLocation);
		this.edcProp = edcProp;
		
	}
			
    public void run() {
    	
    	LocalLogger.logToStdErr("Clean up initialized...");
    	
    	while(true){
    		try{
    			JobTracker jTracker = new JobTracker(this.edcProp.buildSei());

    			Calendar cal = Calendar.getInstance();
    			cal.add(Calendar.MILLISECOND, -1 * this.runFrequencyInMilliseconds);
    			Date cutoff = cal.getTime();
    			long cutoffMsec = cutoff.getTime();
    			
    			// cleanup files.   			
    			
    			LocalLogger.logToStdErr("Clean up started...");
    			
    			// clean up the official way
    			try {
	    			AuthorizationManager.setSemtkSuper();
	    			jTracker.deleteJobsAndFiles(cutoff, this.trstore);
    			} finally {
    				AuthorizationManager.clearSemtkSuper();
    			}
    			
    			// look for leftovers
    			for (File f : locationToDeleteFrom.listFiles()) {
    				 // metaFile.deleteTargetPath() could delete files out of order, 
    				 //  so check f.exists()
	                 if (f.exists() && f.lastModified() < cutoffMsec) {
	                	 try {	       
	                		 f.delete();
	                		 LocalLogger.logToStdOut("Deleted leftover: " + f.getAbsolutePath());
	                	 } catch (Exception e1) {
	                		 LocalLogger.printStackTrace(e1);
	                	 }
	                 }
                }
    		}
    		catch(Exception iei){
    			LocalLogger.printStackTrace(iei);
    		}
    		
    		// Sleep before repeating
    		try {
    			LocalLogger.logToStdErr("Clean up about to sleep for " + (double)runFrequencyInMilliseconds/(60 * 1000) + " minutes. ");
				Thread.sleep(runFrequencyInMilliseconds);
			} catch (InterruptedException e) {
				LocalLogger.logToStdErr("Sleep failed");
			}
    	}
   
    }

}
