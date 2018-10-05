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
import java.util.List;

import com.ge.research.semtk.utility.LocalLogger;


/**
 * @author 200001934
 *
 */

public class ThreadAuthenticator {
	public static final String ANONYMOUS = "anonymous";
	
	public static final String USERNAME_KEY = "user_name";
	
	private static ThreadLocal<HeaderTable> threadHeaderTables = new ThreadLocal<>();
	private static ThreadLocal<Boolean> threadJobAdmins = new ThreadLocal<>();
	
	/**
	 * Authenticate this thread with headers
	 * 
	 * Code snippet for passing authentication on to sub-threads:
	 *    - sub thread should have 
	 *    		private HeaderTable headerTable;
	 *    - sub thread constructor should contain
	 *          this.headerTable = ThreadAuthenticator.getHeaderTable()
	 *    - sub thread run() should have
	 *          ThreadAuthenticator.authenticateThisThread(this.headerTable)
	 * 
	 * @param headerTable - can be null (equivalent to un-authenticated)
	 */
	public static void authenticateThisThread(HeaderTable headerTable) {
		
		/******* logging ********/		
		String oldName = getThreadUserName();
		
		/****** real work ********/
		threadHeaderTables.set(headerTable);
		threadJobAdmins.set(false);
		
		/******* logging ********/		
		LocalLogger.logToStdErr(Thread.currentThread().getName() + 
								" has authenticated  from: " + oldName + "  to: " + getThreadUserName());
	}
	
	public static void authenticateThisThread(String userName) {
		HeaderTable tab = new HeaderTable();
		ArrayList<String> vals = new ArrayList<String>();
		vals.add(userName);
		tab.put(ThreadAuthenticator.USERNAME_KEY, vals);
		ThreadAuthenticator.authenticateThisThread(tab);
	}
	
	public static void unAuthenticateThisThread() {
		threadHeaderTables.set(null);;
		threadJobAdmins.set(null);
	}
	
	/**
	 * Makes this thread a job admin.
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
		threadJobAdmins.set(a);
	}
	
	public static boolean isJobAdmin() {
		if (threadJobAdmins != null && threadJobAdmins.get() != null) {
			return threadJobAdmins.get();
		} else {
			return false;
		}
	}
	
	/**
	 * Get username on this thread
	 * @return
	 */
	public static String getThreadUserName() {
		if (threadHeaderTables != null) {
			return getUserName(threadHeaderTables.get());
		} 
		return ANONYMOUS;
	}
	
	public static String getUserName(HeaderTable headerTable) {
		if (headerTable != null) {
			List<String> vals = headerTable.get(USERNAME_KEY);
			if (vals != null && vals.size() == 1) {
				return vals.get(0);
			} 
		} 
		
		return ANONYMOUS;
	}
	
	/**
	 * Get entire headerTable. Could be null.  
	 * @return
	 */
	public static HeaderTable getThreadHeaderTable() {
		if (threadHeaderTables == null) {
			return null;
		} else {
			return threadHeaderTables.get();
		}
	}
	
}
