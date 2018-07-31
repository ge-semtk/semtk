package com.ge.research.semtk.auth;

import java.util.ArrayList;
import java.util.List;



public class ThreadAuthenticator {
	public static final String ANONYMOUS = "anonymous";
	public static final String ADMIN = "admin";

	public static final String USERNAME_KEY = "user_name";
	
	private static int admin = 0;
	private static ThreadLocal<HeaderTable> threadHeaderTable = null;
	
	/**
	 * Authenticate this thread with headers
	 * @param headerTable - can be null (equivalent to un-authenticated)
	 */
	public static void authenticateThisThread(HeaderTable headerTable) {
		
		threadHeaderTable = new ThreadLocal<>();
		threadHeaderTable.set(headerTable);
	}
	
	public static void authenticateThisThread(String userName) {
		HeaderTable tab = new HeaderTable();
		ArrayList<String> vals = new ArrayList<String>();
		vals.add(userName);
		tab.put(ThreadAuthenticator.USERNAME_KEY, vals);
		ThreadAuthenticator.authenticateThisThread(tab);
	}
	
	/**
	 * Allows isAdmin() to return true ONCE before resetting
	 */
	public static void setAdmin() {
		admin = 1;
	}
	
	public static boolean isAdmin() {
		return admin-- > 0;
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
