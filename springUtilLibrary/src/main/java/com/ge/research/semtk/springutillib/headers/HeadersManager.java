package com.ge.research.semtk.springutillib.headers;

import org.springframework.http.HttpHeaders;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;

public class HeadersManager {
	
	/**
	 * DESIGN NOTE:  This maven module has a spring dependency.
	 *               It exists so that SparqlGraphLibrary does NOT have a spring dependency.
	 *               
	 * @param headers
	 */
	
	
	
	/**
	 * Accept the headers from the http call that kicked off this thread.
	 * Send those headers to the semTK ThreadAuthenticator
	 * 
	 * @param headers
	 */
	public static void setHeaders(HttpHeaders headers) {		
		// change headers into a non-spring type: 
		//      Hashtable<String,List<String> - hash of name to value list
		HeaderTable headerTable = new HeaderTable();	
		
		// grab only principal-related headers
		for (String key : headers.keySet()) {
			
			// grab user_name if there is exactly one
			if (	key.equals("user_name") ||
					key.equals("authorization")) {
				headerTable.put(key, headers.get(key));
			}
		}
		
		ThreadAuthenticator.authenticateThisThread(headerTable);
		
	}
	
}