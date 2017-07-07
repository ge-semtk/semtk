package com.ge.research.semtk.services.results.cleanUp;

import java.io.File;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.services.results.ResultsEdcConfigProperties;

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
			System.err.println("unable to get a job tracker instance. reason given: " + e.getMessage());
			e.printStackTrace();
		}
	}
			
    public void run() {
    	
    	System.err.println("Clean up initialized...");
    	
    	while(true){
    		try{
    			
    			// cleanup files.
    	    	
    			long now = new Date().getTime();
    			long cutoff = now - this.runFrequencyInMilliseconds;
    			System.err.println("Clean up started...");
    			
    			for (File f : locationToDeleteFrom.listFiles()) {
	                 if (f.lastModified() < cutoff) {
	                	 f.delete();
	                 }
                }
    			
    			// cleanup meta data.
    			this.jTracker.deleteJobsBeforeGivenMinutesAgo(frequencyInMinutes);
    			
    		}
    		catch(Exception iei){
    			System.err.println(iei.getMessage());
    		}
    		try {
    			System.err.println("Clean up about to sleep...");
				Thread.sleep(runFrequencyInMilliseconds);
			} catch (InterruptedException e) {
				System.err.println("Sleep failed");
			}
    	}
   
    }

}
