package com.ge.research.knowledge.logging.easyLogger;

import java.util.Stack;
import java.util.UUID;

public class Logger {
	
	private static final String VERSION_IDENTIFIER = "000001.EXPERIMENTAL";
	private static final String VERSION_MAKE_AND_MODEL = "JAVA EASY LOG CLIENT";
	
	private String applicationName = "UNKNOWN_APPLICATION";	// should be set by the user at some point.
	private Stack parentEventStack = new Stack<UUID>();		// this may be used in the long run.
	private UUID sessionID = UUID.randomUUID();		// the ID that is used for the logging session. 
	private long sequenceNumber = -1;				// starts at -1 because the first call will result in it being set to zero
	
	public Logger(String applicationName){
		// create a new instance of the logger.
		this.setApplicationName(applicationName);
	}
	
	private void setApplicationName(String aName){
		// remove the whitespace from the location. 
		aName = aName.replaceAll(" ", "_");
		this.applicationName = aName;
	}

	public long getLastSequenceNumber(){
		// what is the current count
		return this.sequenceNumber;
	}
	
	public synchronized long getNextSeqNumber(){
		// increment the sequence number and then return it.
		this.sequenceNumber += 1;
		return this.sequenceNumber;
	}
	
	public void pushParentEvent(UUID pEvent){
		this.parentEventStack.push(pEvent); 	// push a new parent event
	}
	
	public void popParentEvent(){
		this.parentEventStack.pop();			// pop the top event
	}
	
	private UUID generateActionID(){
		return UUID.randomUUID();
	}
	
	
	
}

