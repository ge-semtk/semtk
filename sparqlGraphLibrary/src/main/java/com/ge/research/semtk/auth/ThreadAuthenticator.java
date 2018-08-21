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
package com.ge.research.semtk.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ge.research.semtk.utility.LocalLogger;


/**
 * IMPORTANT:  strange behavior has been observed in sub-threads.
 *             Best strategy is to instantiate any RestClients and send them into sub-thread constructor.
 *             That way they know their authentication.
 * @author 200001934
 *
 */

public class ThreadAuthenticator {
	public static final String ANONYMOUS = "anonymous";
	
	public static final String USERNAME_KEY = "user_name";
	
	private static ThreadLocal<HeaderTable> threadHeaderTable = new ThreadLocal<>();
	private static ThreadLocal<Boolean> jobAdmin = new ThreadLocal<>();
	
	/**
	 * Authenticate this thread with headers
	 * @param headerTable - can be null (equivalent to un-authenticated)
	 */
	public static void authenticateThisThread(HeaderTable headerTable) {
		
		/******* logging ********/		
		LocalLogger.logToStdErr(Thread.currentThread().getName() + 
								" is authenticating from: " + getThreadUserName() + 
								" to: " + getUserName(headerTable));
		
		/****** real work ********/
		threadHeaderTable.set(headerTable);
		
		jobAdmin.set(false);
		
		/******* logging ********/		
		LocalLogger.logToStdErr(Thread.currentThread().getName() + 
								" is authenticated to: " + getThreadUserName());
	}
	
	public static void authenticateThisThread(String userName) {
		HeaderTable tab = new HeaderTable();
		ArrayList<String> vals = new ArrayList<String>();
		vals.add(userName);
		tab.put(ThreadAuthenticator.USERNAME_KEY, vals);
		ThreadAuthenticator.authenticateThisThread(tab);
	}
	
	public static void unAuthenticateThisThread() {
		threadHeaderTable = null;
		jobAdmin = null;
	}
	
	/**
	 * Makes this thread admin.
	 * 
	 *    // When you're not entirely sure the thread should alwasy be ADMIN, best usage is:
	 * 	  setAdmin(true);
	 *    try {
	 *        do stuff;
	 *    } finally {
	 *    	setAdmin(false);
	 *    }
	 */
	public static void setJobAdmin(boolean a) {
		jobAdmin = new ThreadLocal<Boolean>();
		jobAdmin.set(a);
	}
	
	public static boolean isJobAdmin() {
		if (jobAdmin != null && jobAdmin.get() != null) {
			return jobAdmin.get();
		} else {
			return false;
		}
	}
	
	/**
	 * Get username on this thread
	 * @return
	 */
	public static String getThreadUserName() {
		if (threadHeaderTable != null) {
			return getUserName(threadHeaderTable.get());
		} else {
			LocalLogger.logToStdErr(Thread.currentThread().getName() +" ThreadAuthenticator.getThreadUserName() says threadHeaderTable == null");
		}
		return ANONYMOUS;
	}
	
	public static String getUserName(HeaderTable headerTable) {
		if (headerTable != null) {
			List<String> vals = headerTable.get(USERNAME_KEY);
			if (vals != null && vals.size() == 1) {
				LocalLogger.logToStdErr(Thread.currentThread().getName() + " ThreadAuthenticator.getUserName is " + vals.get(0));
				return vals.get(0);
			} else if (vals == null) {
				LocalLogger.logToStdErr(Thread.currentThread().getName() + " ThreadAuthenticator.getUserName() says vals is null");
			} else if (vals.size() != 1) {
				LocalLogger.logToStdErr(Thread.currentThread().getName() + " ThreadAuthenticator.getUserName() says vals size != 1 It is: " + Integer.toString(vals.size()));
			}
		} else {
			LocalLogger.logToStdErr(Thread.currentThread().getName() + " ThreadAuthenticator.getUserName() says headerTable == null");
		}
		
		return ANONYMOUS;
	}
	
	/**
	 * Get entire headerTable. Could be null.  
	 * @return
	 */
	public static HeaderTable getThreadHeaderTable() {
		if (threadHeaderTable == null) {
			return null;
		} else {
			return threadHeaderTable.get();
		}
	}
	
}
