package com.ge.research.semtk.security;

/**
 * Abstract class describing a class that maintains Principal information per thread.
 * @author 200001934
 *
 */
public abstract class PrincipalAwareClass {
	public static final String ANONYMOUS = "anonymous";
	private static ThreadLocal<String> userName = new ThreadLocal<String>();
	
	/**
	 * Get username on this thread
	 * @return
	 */
	public static String getPrincipalUserName() {
		if (userName.get() == null) {
			return ANONYMOUS;
		} else {
			return userName.get();
		}
	}
	
	/**
	 * Set username for this thread
	 * @param userName
	 */
	public static void setPrincipalUserName(String userName) {
		PrincipalAwareClass.userName.set(userName);
	}
	
}
