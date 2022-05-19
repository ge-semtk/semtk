/**
 ** Copyright 2017-2018 General Electric Company
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

package com.ge.research.semtk.load;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.DataLoadBatchHandler;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.InMemoryInterface;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.LocalLogger;

public class InMemoryInterfaceUploadThread extends Thread {

	private InMemoryInterface cacheSei;
	private SparqlEndpointInterface endpoint;
	private HeaderTable headerTable;
	private Exception e = null;
	private AtomicInteger uploadSize;
	
	/**
	 * 
	 * @param cacheSei
	 * @param endpoint
	 * @param headerTable
	 * @param uploadSize - an AtomicInteger (reference) to set to the number of chars uploaded if successful
	 */
	public InMemoryInterfaceUploadThread(InMemoryInterface cacheSei, SparqlEndpointInterface endpoint, HeaderTable headerTable, AtomicInteger uploadSize) {
		super();
		this.cacheSei = cacheSei;
		this.endpoint = endpoint;
		this.headerTable = headerTable;
		this.uploadSize = uploadSize;  // this is pass-by-reference 
	}
	
	/**
	 *  Sets e if exception, else null
	 */
	public void run(){

		ThreadAuthenticator.authenticateThisThread(this.headerTable);
		
		LocalLogger.logToStdErr("Generating temporary graph turtle...");
		String s = this.cacheSei.dumpToTurtle();
		int len = s.length();
		if (len > 0) {
			int tryCount = 1;
			boolean done = false;
			while (! done) {
				try {
					LocalLogger.logToStdErr(String.format("Uploading %,d chars of ttl", len));
					
					this.endpoint.authUploadTurtle(s.getBytes());
					this.uploadSize.set(len);
					LocalLogger.logToStdErr("upload complete");
					done = true;
					
				} catch (Exception e) {
					if (tryCount < 4) {
						try {
							this.endpoint.logFailureAndSleep(e, tryCount);
						} catch (InterruptedException ie) {
							// ignore
						} finally {
							tryCount ++;
						}
					} else { 
						this.e = new Exception("Giving up uploading temp graph", e);
						done = true;
					}
				}
			}
		}
	}
	
	/**
	 * Return the exception thrown during running of this thread, or null
	 * @return
	 */
	public Exception getException() {
		return this.e;
	}
	
}
