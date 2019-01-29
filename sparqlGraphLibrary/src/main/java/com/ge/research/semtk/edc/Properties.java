package com.ge.research.semtk.edc;

import javax.annotation.PostConstruct;

import com.ge.research.semtk.utility.LocalLogger;

/**
 * Problem: I don't know how to validate properties in @spring when...
 *          I want to use inheritance to get consistent property names.
 *          So this class does logging (for startup) and validation
 *          
 *          Check out SparqlQueryServiceRestController for usage.
 *          
 * @author 200001934
 *
 */
public class Properties {
	private String prefix = "";
	
	/**
	 * Designed to be called from @PostConstruct in a @RestController
	 * Validate and exit with message instead of giant long stack trace.
	 * On success: prints all values
	 */
	public void validateWithExit() {
		try {
			this.validate();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.println("exiting.");
			System.exit(1);
		}
	}
	
	/** 
	 * Validate entire properties object
	 * @throws Exception
	 */
	public void validate() throws Exception {
		System.out.println("---- Properties ----");
	}
	
	/**
	 * Call this from your top level annotated properties class
	 * @param prefix
	 */
	protected void setPrefix(String prefix) {
		this.prefix = prefix + ".";
	}
	
	/****** Individual field validators *******/
	
	protected void notEmpty(String name, Integer i) throws Exception {
		if (i == null) {
			throw new Exception(this.prefix + name + " is null");
		}
		outputProperty(name, i);

	}
	
	protected void notEmpty(String name, String s) throws Exception {
		if (s == null) {
			throw new Exception(this.prefix + name + " is null");
		} else if (s.isEmpty()) {
			throw new Exception(this.prefix + name + " is empty");
		}
		outputProperty(name, s);
	}
	
	protected void noValidate(String name, String s) { 
		outputProperty(name, s);
	}
	
	private void outputProperty(String name, Object s) {
		System.out.println(this.prefix + name + "=" + (s == null ? "" : s.toString()));
	}
	
}
