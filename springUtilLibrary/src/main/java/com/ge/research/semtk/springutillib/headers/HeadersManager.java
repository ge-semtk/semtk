/**
 ** Copyright 2018 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
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
	
	public static void clearHeaders() {
		ThreadAuthenticator.unAuthenticateThisThread();	
	}
	
}