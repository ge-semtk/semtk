package com.ge.research.semtk.auth;

import java.util.List;



public class ThreadAuthenticator {
	public static final String ANONYMOUS = "anonymous";
	public static final String USERNAME_KEY = "user_name";
	
	private static boolean admin = false;
	private static ThreadLocal<HeaderTable> threadHeaderTable = null;
	
	/**
	 * Authenticate this thread with headers
	 * @param headerTable - can be null (equivalent to un-authenticated)
	 */
	public static void authenticateThisThread(HeaderTable headerTable) {
		
		threadHeaderTable = new ThreadLocal<>();
		threadHeaderTable.set(headerTable);
	}
	
	public static void setAdmin() {
		admin = true;
	}
	
	public static boolean isAdmin() {
		return admin;
	}
	/**
	 * Get username on this thread
	 * @return
	 */
	public static String getThreadUserName() {
		if (threadHeaderTable != null) {
			HeaderTable headerTable = threadHeaderTable.get();
			if (headerTable != null) {
				List<String> vals = headerTable.get(USERNAME_KEY);
				if (vals != null && vals.size() == 1) {
					return vals.get(0);
				}
			}
		}
		return ANONYMOUS;
	}
	
	/**
	 * Get entire headerTable. Could be null.  
	 * Used to authenticate a sub-thread like this:
	 * 
	 * MyThread thread = new MyThread(..., ..,  ThreadAuthenticator.getThreadHeaderTable());
	 * 
	 * MyThread 
	 *    - constructor has:     this.headerTable = headerTableParam;
	 *    - first line of run(): ThreadAuthenticator.aurthenticateThisThread(this.headerTable)
	 *    
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
