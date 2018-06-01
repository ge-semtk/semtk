package com.ge.research.semtk.springutillib.headers;

import org.springframework.http.HttpHeaders;

import com.ge.research.semtk.security.PrincipalAwareClass;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

import java.util.Hashtable;
import java.util.List;

public class HeadersManager extends PrincipalAwareClass {
	
	/**
	 * DESIGN NOTE:  This maven module has a spring dependency.
	 *               It exists so that SparqlGraphLibrary does NOT have a spring dependency.
	 *               This means that HeadersManager should call OUT and set values in SparqlGraphLibrary.
	 *               Because if SparqlGraphLibrary calls IN to HeadersManager it will inherit the spring dependency.
	 *               Sidenote: not even sure the spring dependency matters.
	 *               
	 * WHAT THIS DOES:	stores headers from the http call that started this rest service thread.
	 *               
	 * @param headers
	 */
	
	
	
	/**
	 * Accept the headers from the http call that kicked off this thread.
	 * Send those headers to any SemTK class that needs them
	 * but doesn't want to call HeadersManager because it doesn't want the spring dependency in its pom.
	 * 
	 * @param headers
	 */
	public static void setHeaders(HttpHeaders headers) {		
		// change headers into a non-spring type: 
		//      Hashtable<String,List<String> - hash of name to value list
		Hashtable<String,List<String>> headerTable = new Hashtable<String,List<String>>();	
		
		// default user is anon
		String userName = PrincipalAwareClass.ANONYMOUS;
		
		// grab only principal-related headers
		for (String key : headers.keySet()) {
			
			// grab user_name if there is exactly one
			if (key.equals("user_name") && headers.get(key).size() == 1) {
				headerTable.put(key, headers.get(key));
				userName = headers.get(key).get(0);
			
			// grab other principal-related tokens
			} else if (key.equals("authorization")) {
				headerTable.put(key, headers.get(key));
			}
		}
		
		// send headers to RestClient
		RestClient.setDefaultHeaders(headerTable);
		
		// send userName to PrincipalAware classes
		HeadersManager.setPrincipalUserName(userName);
		SparqlEndpointInterface.setPrincipalUserName(userName);
	}
	
}