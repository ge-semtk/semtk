package com.ge.research.semtk.services.nodeGroupExecution.requests;

public class NodegroupRequestBodyPercentMsec {
	public String jobID;
	public int percentComplete;
	public int maxWaitMsec;
	
	public void validate() throws Exception {
		if (jobID == null || jobID.isEmpty()) {
			throw new Exception("jobID is " + jobID==null?"null":"empty");
		}
	}
}
