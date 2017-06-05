package com.ge.research.semtk.services.dispatch;

import java.net.ConnectException;

import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.asynchronousQuery.AsynchronousNodeGroupBasedQueryDispatcher;


public class WorkThread extends Thread {
	AsynchronousNodeGroupBasedQueryDispatcher dsp;
	Boolean test;
	JSONObject constraintJson;
	
	public WorkThread(AsynchronousNodeGroupBasedQueryDispatcher dsp, Boolean test, JSONObject constraintJson){
		this.dsp = dsp;
		this.test = test;
		this.constraintJson = constraintJson;
	}
	
    public void run() {

    	if(!test){
    		try {
				TableResultSet trs = this.dsp.execute(constraintJson);
					
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
    	}
    	
    	else{
    		String fakecsv = "col1,col2,col3\n01,02,03\n001,002,003";
    		
    		try{
    		Thread.sleep(10000);
    		dsp.getStatusClient().execSetPercentComplete(10);
    		Thread.sleep(10000);
    		dsp.getStatusClient().execSetPercentComplete(30);
    		Thread.sleep(10000);
    		dsp.getStatusClient().execSetPercentComplete(40);
    		Thread.sleep(10000);
    		dsp.getStatusClient().execSetPercentComplete(50);
    		Thread.sleep(10000);
    		dsp.getStatusClient().execSetPercentComplete(80);
    		Thread.sleep(10000);
    		dsp.getStatusClient().execSetPercentComplete(90);
    		Thread.sleep(10000);
    		dsp.getResultsClient().execStoreCsvResults(dsp.getJobId(), fakecsv);
    		dsp.getStatusClient().execSetSuccess();
    		
    		
    		}catch(Exception eee){ 
    			System.err.println(eee.getMessage()); 
    			try {
					this.dsp.getStatusClient().execSetFailure("Job failed during initial spin up of service thread. message was: " + eee.getMessage()  
							+ " with a trace : " + eee.getStackTrace().toString());
				} catch (Exception e) {
					System.err.println("attempt to write failure status failed.");
					e.printStackTrace();
				}
    		
    		}
    	}
    	
    	System.out.println("Setting dispatcher to null in WorkThread");
    	this.dsp = null;
    }


}