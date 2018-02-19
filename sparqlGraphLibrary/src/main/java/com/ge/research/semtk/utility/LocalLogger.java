package com.ge.research.semtk.utility;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A utility class to perform logging to the local console.
 */
public class LocalLogger {
	
	private static SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * Get a timestamp prefix for a log message
	 * Example: "2017-11-27 13:45:01 "
	 */
	private static String getTimestampPrefix() {
		return TIMESTAMP_FORMAT.format(new Date()) + " ";
	}

	/**
	 * Log to standard out.
	 * @throws Exception 
	 */
	public static void logToStdOut(String s) {
		System.out.println(getTimestampPrefix() + s);
	}
	
	/**
	 * Log to standard error.
	 */
	public static void logToStdErr(String s){
		System.err.println(getTimestampPrefix() + s);
	}
	
	/**
	 * Log to standard error, and print stack trace.
	 */
	public static void printStackTrace(Throwable e){
		logToStdErr(e.getMessage());  // need this to get the timestamp.
		e.printStackTrace();		
	}
	
}
