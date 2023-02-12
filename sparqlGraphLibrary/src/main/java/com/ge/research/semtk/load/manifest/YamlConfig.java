package com.ge.research.semtk.load.manifest;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.research.semtk.utility.Utility;

/**
 * Class representing YAML file for configuring items to load to triplestore
 */
public abstract class YamlConfig {

	protected String baseDir;				// the directory containing the YAML file
	protected String fallbackModelGraph;	// load to this model graph if not otherwise specified
	protected String fallbackDataGraph;		// load to this data graph if not otherwise specified
	protected JsonNode configNode;			// this file as a JsonNode

	/**
	 * Constructor
	 */
	public YamlConfig(File yamlFile, File schemaFile, String fallbackModelGraph, String fallbackDataGraph) throws Exception {
		setBaseDir(yamlFile.getParent());
		setFallbackModelGraph(fallbackModelGraph);
		setFallbackDataGraph(fallbackDataGraph);

		// validate manifest YAML against schema
		String yamlStr = Utility.getStringFromFilePath(yamlFile.getAbsolutePath());
		String manifestSchema = Utility.getStringFromFilePath(schemaFile.getAbsolutePath());
		Utility.validateYaml(yamlStr, manifestSchema);
		// get the config object
		configNode = Utility.getJsonNodeFromYaml(yamlStr);
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
