package com.ge.research.semtk.load.config;

import java.io.File;
import java.io.PrintWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.ge.research.semtk.utility.Utility;

/**
 * Class representing YAML file for configuring items to load to triplestore
 */
public abstract class YamlConfig {

	protected String baseDir;				// the directory containing the YAML file
	protected String defaultModelGraph;		// load to this model graph if not otherwise specified
	protected String defaultDataGraph;		// load to this data graph if not otherwise specified
	protected JsonNode configNode;			// this file as a JsonNode

	/**
	 * Constructor
	 */
	public YamlConfig(File yamlFile, File schemaFile, String defaultModelGraph, String defaultDataGraph) throws Exception {
		setBaseDir(yamlFile.getParent());
		setDefaultModelGraph(defaultModelGraph);
		setDefaultDataGraph(defaultDataGraph);

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
	public String getDefaultModelGraph() {
		return defaultModelGraph;
	}
	public String getDefaultDataGraph() {
		return defaultDataGraph;
	}

	/**
	 * Set methods
	 */
	protected void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}
	protected void setDefaultModelGraph(String defaultModelGraph) {
		this.defaultModelGraph = defaultModelGraph;
	}
	protected void setDefaultDataGraph(String defaultDataGraph) {
		this.defaultDataGraph = defaultDataGraph;
	}

	/**
	 * Utility method for a node that may be a string or a string array.
	 * Returns the string or the first array element, and errors if the array has multiple elements.
	 */
	protected static String getStringOrFirstArrayEntry(JsonNode node) throws Exception {
		if(node == null) {
			return null;
		}else if(node.isTextual()) {
			return node.asText();
		}else if(node.isArray()){
			ArrayNode array = (ArrayNode)node;
			if(array.size() > 1) {
				throw new Exception("Not currently supporting multiple entries for this node: " + node.toString());
			}
			if(array.get(0).getNodeType() != JsonNodeType.STRING) {
				throw new Exception("Expected an array of strings for this node: " + node.toString());
			}
			return array.get(0).asText();
		}
		return null;
	}

	/**
	 * Convenience method to write/flush
	 */
	protected static void writeProgress(String s, PrintWriter progressWriter) {
		progressWriter.println(s);
		progressWriter.flush();
	}

}
