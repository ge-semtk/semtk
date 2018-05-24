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
import java.io.FileReader;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.services.results.ResultsEdcConfigProperties;
import com.ge.research.semtk.services.results.ResultsMetaFile;
import com.ge.research.semtk.utility.LocalLogger;

public class DeleteThread extends Thread {

	private int runFrequencyInMilliseconds;
	private int frequencyInMinutes;
	private File locationToDeleteFrom;	
	private JobTracker jTracker;
	
	public DeleteThread(String fileStorageLocation, int frequencyInMinutes, ResultsEdcConfigProperties edcProp){
		this.locationToDeleteFrom = new File(fileStorageLocation);
		this.runFrequencyInMilliseconds = frequencyInMinutes * 60 * 1000;
		this.frequencyInMinutes = frequencyInMinutes;
		try {
			this.jTracker = new JobTracker(edcProp);
		} catch (Exception e) {
			// something failed when getting the jobtracker. report it but continue anyway
			LocalLogger.logToStdErr("unable to get a job tracker instance. reason given: " + e.getMessage());
			LocalLogger.printStackTrace(e);
		}
	}
			
    public void run() {
    	
    	LocalLogger.logToStdErr("Clean up initialized...");
    	
    	while(true){
    		try{
    			
    			// cleanup files.
    	    	
    			long now = new Date().getTime();
    			long cutoff = now - this.runFrequencyInMilliseconds;
    			LocalLogger.logToStdErr("Clean up started...");
    			
    			for (File f : locationToDeleteFrom.listFiles()) {
    				 // metaFile.deleteTargetPath() could delete files out of order, 
    				 //  so check f.exists()
	                 if (f.exists() && f.lastModified() < cutoff) {
	                	 try {
	                		 // if this is a meta file, delete results target file too 
		                	 if (ResultsMetaFile.fileIsInstanceOf(f.getName())) {
		                		 ResultsMetaFile metaFile = new ResultsMetaFile(new FileReader(f));
		                		 metaFile.deleteTargetPath();
		                	 }
	                	 
	                		 f.delete();
	                	 } catch (Exception e1) {
	                		 LocalLogger.printStackTrace(e1);
	                	 }
	                 }
                }
    			
    			// cleanup meta data.
    			this.jTracker.deleteJobsBeforeGivenMinutesAgo(frequencyInMinutes);
    			
    		}
    		catch(Exception iei){
    			LocalLogger.logToStdErr(iei.getMessage());
    		}
    		try {
    			LocalLogger.logToStdErr("Clean up about to sleep for " + (double)runFrequencyInMilliseconds/(60 * 1000) + " minutes. ");
				Thread.sleep(runFrequencyInMilliseconds);
			} catch (InterruptedException e) {
				LocalLogger.logToStdErr("Sleep failed");
			}
    	}
   
    }

}
