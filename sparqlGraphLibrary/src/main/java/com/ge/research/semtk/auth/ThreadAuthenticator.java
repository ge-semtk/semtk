package com.ge.research.semtk.auth;

import java.util.ArrayList;
import java.util.List;



public class ThreadAuthenticator {
	public static final String ANONYMOUS = "anonymous";
	
	public static final String USERNAME_KEY = "user_name";
	
	private static ThreadLocal<HeaderTable> threadHeaderTable = null;
	private static ThreadLocal<Boolean> jobAdmin = null;
	
	/**
	 * Authenticate this thread with headers
	 * @param headerTable - can be null (equivalent to un-authenticated)
	 */
	public static void authenticateThisThread(HeaderTable headerTable) {
		
		threadHeaderTable = new ThreadLocal<>();
		threadHeaderTable.set(headerTable);
		
		jobAdmin = new ThreadLocal<Boolean>();
		jobAdmin.set(false);
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
		if (threadHeaderTable != null && threadHeaderTable.get() != null) {
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
