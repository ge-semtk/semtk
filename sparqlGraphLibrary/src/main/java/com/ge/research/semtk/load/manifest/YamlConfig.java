package com.ge.research.semtk.load.manifest;

import java.io.File;

/**
 * Class representing YAML file for configuring items to load to triplestore
 */
public abstract class YamlConfig {

	protected String baseDir;				// the directory containing the YAML file
	protected String fallbackModelGraph;	// load to this model graph if not otherwise specified
	protected String fallbackDataGraph;		// load to this data graph if not otherwise specified

	/**
	 * Constructor
	 */
	public YamlConfig(File yamlFile, String fallbackModelGraph, String fallbackDataGraph) throws Exception {
		setBaseDir(yamlFile.getParent());
		setFallbackModelGraph(fallbackModelGraph);
		setFallbackDataGraph(fallbackDataGraph);
	}
	
	/**
	 * Get methods
	 */
	public String getBaseDir() {
		return baseDir;
	}
	public String getFallbackModelGraph() {
		return fallbackModelGraph;
	}
	public String getFallbackDataGraph() {
		return fallbackDataGraph;
	}
	
	/**
	 * Set methods
	 */
	protected void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}
	protected void setFallbackModelGraph(String fallbackModelGraph) {
		this.fallbackModelGraph = fallbackModelGraph;
	}
	protected void setFallbackDataGraph(String fallbackDataGraph) {
		this.fallbackDataGraph = fallbackDataGraph;
	}

}
