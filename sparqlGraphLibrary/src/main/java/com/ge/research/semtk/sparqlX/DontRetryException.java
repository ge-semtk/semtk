package com.ge.research.semtk.sparqlX;

public class DontRetryException extends Exception {
	public DontRetryException(String message){
		super(message);
	}
}
