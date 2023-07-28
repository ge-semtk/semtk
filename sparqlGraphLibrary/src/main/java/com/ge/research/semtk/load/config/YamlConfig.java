package com.ge.research.semtk.load.config;

import java.io.File;
import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.ge.research.semtk.utility.Utility;

/**
 * Class representing YAML file for configuring items to load to triplestore
 */
public abstract class YamlConfig {

	protected String fileName;				// the YAML file name
	protected String baseDir;				// the directory containing the YAML file
	protected String defaultModelGraph;		// load to this model graph if not otherwise specified
	protected String defaultDataGraph;		// load to this data graph if not otherwise specified
	protected JsonNode configNode;			// this file as a JsonNode
	protected String username = "YamlConfigUser";    // no user or password functionality yet.
	protected String password = "YamlConfigPassword";

	/**
	 * Constructor
	 */
	public YamlConfig(File yamlFile, File schemaFile, String defaultModelGraph, String defaultDataGraph) throws Exception {
		this.fileName = yamlFile.getName();
		this.baseDir = yamlFile.getParent();
		this.defaultModelGraph = defaultModelGraph;
		this.defaultDataGraph = defaultDataGraph;

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
	public String getFileName() {
		return fileName;
	}
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
	 * Utility method for a node that may be a string or a string array.
	 * Returns a list containing the items from the string or string array.
	 */
	protected static LinkedList<String> getAsStringList(JsonNode node) throws Exception {
		LinkedList<String> ret = new LinkedList<String>();
		if(node == null) {
			return null;
		}else if(node.isTextual()) {
			ret.add(node.asText()); // array with single element
		}else if(node.isArray()){
			ArrayNode array = (ArrayNode)node;
			for(int i = 0; i < array.size(); i++) {
				if(array.get(i).getNodeType() != JsonNodeType.STRING) {
					throw new Exception("Expected an array of strings for this node: " + node.toString());
				}
				ret.add(array.get(i).asText());
			}
		}
		return ret;
	}
	
}
