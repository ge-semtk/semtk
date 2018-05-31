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
	 * Default to anonymous user
	 */
	public PrincipalAwareClass() {
		setPrincipalUserName(ANONYMOUS);
	}
	
	/**
	 * Get username on this thread
	 * @return
	 */
	public static String getPrincipalUserName() {
		return userName.get();
	}
	
	/**
	 * Set username for this thread
	 * @param userName
	 */
	public static void setPrincipalUserName(String userName) {
		PrincipalAwareClass.userName.set(userName);
	}
	
}
