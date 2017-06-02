package com.ge.research.semtk.services.storedNodegroupExecution;

public class StatusRequestBody {
	private String jobID;
	
	public void setJobID(String jobID){
		this.jobID = jobID;
	}
	public String getJobID(){
		return this.jobID;
	}
}
