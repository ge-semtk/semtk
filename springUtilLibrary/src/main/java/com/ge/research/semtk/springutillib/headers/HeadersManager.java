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
		for (String key : headers.keySet()) {
			headerTable.put(key, headers.get(key));
		}
		
		// send headers to RestClient
		RestClient.setDefaultHeaders(headerTable);
		
		// send userName to PrincipalAware classes
		String userName = HeadersManager.getUserName(headers);
		
		HeadersManager.setPrincipalUserName(userName);
		SparqlEndpointInterface.setPrincipalUserName(userName);
	}
	
	private static String getUserName(HttpHeaders headers) {
		if (headers != null && headers.containsKey("user_name") && headers.get("user_name").size() == 1) {
			return headers.get("user_name").get(0);
		} else {
			return "anonymous";
		}
	}
}