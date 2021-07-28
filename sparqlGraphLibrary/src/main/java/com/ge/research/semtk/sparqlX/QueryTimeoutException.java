package com.ge.research.semtk.sparqlX;

public class QueryTimeoutException extends DontRetryException {
	public QueryTimeoutException(String message){
		super(message);
	}
}
