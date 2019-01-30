package com.ge.research.semtk.properties;

import java.util.TreeMap;

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
	private String prefix = "<unset>";
	
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
	public void setPrefix(String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			this.prefix = "";
		} else {
			this.prefix = prefix + ".";
		}
	}
	
	/****** Individual field validators *******/
	
	protected void notEmpty(String name, Integer i) throws Exception {
		if (i == null) {
			throw new Exception(this.getPrefixedName(name) + " is null");
		}
		outputProperty(name, i);

	}
	
	protected void notEmpty(String name, String s) throws Exception {
		if (s == null) {
			throw new Exception(this.getPrefixedName(name) + " is null");
		} else if (s.isEmpty()) {
			throw new Exception(this.getPrefixedName(name) + " is empty");
		}
		outputProperty(name, s);
	}
	
	protected void notEmptyNoPrint(String name, String s) throws Exception {
		if (s == null) {
			throw new Exception(this.getPrefixedName(name) + " is null");
		} else if (s.isEmpty()) {
			throw new Exception(this.getPrefixedName(name) + " is empty");
		}
		outputProperty(name, "xxxxxx");
	}
	
	protected void rangeInclusive(String name, int val, int min, int max) throws Exception {
		if (val < min || val > max) {
			throw new Exception(this.getPrefixedNameValue(name, (Integer) val) + " must be between " + String.valueOf(min) + " and " + String.valueOf(max));
		} 
		outputProperty(name, val);
	}
	protected void noValidate(String name, String s) { 
		outputProperty(name, s);
	}
	
	protected void noValidateNoPrint(String name, String s) { 
		outputProperty(name, "xxxxxx");
	}
	
	private void outputProperty(String name, Object v) {
		System.out.println(this.getPrefixedNameValue(name, v));
	}
	
	private String getPrefixedName(String name) {
		return this.prefix + name;
	}
	
	private String getPrefixedNameValue(String name, Object v) {
		return this.prefix + name + "=" + (v == null ? "<null>" : v.toString());
	}
	
}
