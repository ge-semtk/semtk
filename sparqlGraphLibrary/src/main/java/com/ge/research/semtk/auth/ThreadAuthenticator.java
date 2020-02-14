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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.ge.research.semtk.utility.LocalLogger;


/**
 * @author 200001934
 *
 *  USAGE:
 *  >> Get headers from incoming REST call.   Authenticate.
 *		rest controller endpoint needs parameter:   @RequestHeader HttpHeaders headers
 *		first line of rest controller endpoint:     HeadersManager.setHeaders(headers);
 *
 *  >> RestClient automatically passes Authentication on to other calls
 *
 *  >> Manually passing authentication into threads:
 *  	thread constructor should have:     this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
 *      and thread run should start with:   ThreadAuthenticator.authenticateThisThread(this.headerTable);
 *
 *  >> Use AuthorizationManager to decide what this authenticated thread is allowed to do.
 */

public class ThreadAuthenticator {
	public static final String ANONYMOUS = "anonymous";
	
	private static String usernameKey = "user_name";
	private static String groupKey = "group";
	
	private static ThreadLocal<HeaderTable> threadHeaderTables = new ThreadLocal<>();
	private static ThreadLocal<Boolean> threadJobAdmins = new ThreadLocal<>();
	
	public static void setUsernameKey(String k) {
		usernameKey = k;
	}
	
	public static String getUsernameKey() {
		return usernameKey;
	}
	
	public static void setGroupKey(String k) {
		groupKey = k;
	}
	
	public static String getGroupKey() {
		return groupKey;
	}
	
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
		final boolean LOG_TEST = false;
		
		/****** real work ********/
		threadHeaderTables.set(headerTable);
		threadJobAdmins.set(false);
		
		/******* logging ********/	
		/** getting overwhelming, so left just for manual debugging **/
		if (LOG_TEST) {
			String oldName = getThreadUserName();
			LocalLogger.logToStdErr(Thread.currentThread().getName() + 
									" has authenticated  from: " + oldName + "  to: " + getThreadUserName());
		}
	}
	
	/**
	 * For testing
	 * @param userName
	 */
	public static void authenticateThisThread(String userName) {
		HeaderTable tab = new HeaderTable();
		ArrayList<String> vals = new ArrayList<String>();
		vals.add(userName);
		tab.put(ThreadAuthenticator.usernameKey, vals);
		ThreadAuthenticator.authenticateThisThread(tab);
	}
	
	/**
	 * For testing
	 * @param userName
	 * @param groups
	 */
	public static void authenticateThisThread(String userName, ArrayList<String> groups) {
		HeaderTable tab = new HeaderTable();
		ArrayList<String> vals = new ArrayList<String>();
		vals.add(userName);
		tab.put(ThreadAuthenticator.usernameKey, vals);
		tab.put(ThreadAuthenticator.groupKey, groups);
		ThreadAuthenticator.authenticateThisThread(tab);
	}
	
	public static void authenticateThisThread(String userName, String group) {
		ArrayList<String> groups = new ArrayList<String>();
		groups.add(group);
		authenticateThisThread(userName, groups);
	}
	
	public static void unAuthenticateThisThread() {
		threadHeaderTables.set(null);;
		threadJobAdmins.set(null);
	}
	
	/**
	 * Makes this thread a job admin.
	 * 
	 *    // When you're not entirely sure the thread should always be ADMIN, best usage is:
	 *    try {
	 * 	      setAdmin(true);
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
			List<String> vals = headerTable.get(usernameKey);
			if (vals != null && vals.size() == 1) {
				return vals.get(0);
			} 
		} 
		
		return ANONYMOUS;
	}
	
	/**
	 * Get idm groups on this thread
	 * @return List<String> (never null)
	 */
	public static List<String> getThreadGroups() {
		if (threadHeaderTables != null) {
			return getGroups(threadHeaderTables.get());
		} 
		return new ArrayList<String>();
	}
	
	/**
	 * Get idm groups from header table
	 * @return List<String> (never null)
	 */
	public static List<String> getGroups(HeaderTable headerTable) {
		List<String> ret = new ArrayList<String>();
		
		if (headerTable != null) {
			List<String> vals = headerTable.get(groupKey);
			if (vals != null) {
				for (String v : vals) {
					// each value might be a comma-separated list
					Collections.addAll(ret, v.split(","));
				}
			} 
		} 
		
		return ret;
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
