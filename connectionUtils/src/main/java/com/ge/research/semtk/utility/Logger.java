package com.ge.research.semtk.utility;

import java.io.PrintWriter;

public class Logger {

	public enum Levels {INFO, WARNING, ERROR};
	private PrintWriter printWriter;

	/**
	 * Constructor
	 */
	public Logger(PrintWriter printWriter) {
		this.printWriter = printWriter;
	}

	/**
	 * Log an info message
	 */
	public void info(String s) {
		log(s, Levels.INFO);
	}

	/**
	 * Log a warning message
	 */
	public void warning(String s) {
		log(s, Levels.WARNING);
	}

	/**
	 * Log an error message
	 */
	public void error(String s) {
		log(s, Levels.ERROR);
	}

	/**
	 * Get the logging level from a logged line
	 * @param line (e.g. "INFO: performed an action")
	 * @return the level (e.g. Levels.INFO)
	 */
	public static Levels getLevel(String line) throws Exception {
		String levelStr = line.substring(0, line.indexOf(":"));
		if(levelStr.equals(Levels.INFO.name())) { return Levels.INFO; }
		if(levelStr.equals(Levels.WARNING.name())) { return Levels.WARNING; }
		if(levelStr.equals(Levels.ERROR.name())) { return Levels.ERROR; }
		throw new Exception("Unrecognized level: " + levelStr);
	}

	/**
	 * Get the message from a logged line
	 * @param line (e.g. "INFO: performed an action")
	 * @return the message, e.g. "performed an action"
	 */
	public static String getMessage(String line) {
		return line.substring(line.indexOf(": ") + 2);
	}

	// log the message
	private void log(String s, Levels level) {
		printWriter.println(level.name() + ": " + s);
		printWriter.flush();
	}

}
