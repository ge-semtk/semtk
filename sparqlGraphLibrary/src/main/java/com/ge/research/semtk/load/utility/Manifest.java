/**
 ** Copyright 2023 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
package com.ge.research.semtk.load.utility;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.LinkedList;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.Utility;

/**
 * Class representing a manifest (YAML file with specification for loading content into triplestore)
 */
public class Manifest {

	private String name;
	private String description;
	private boolean copyToDefaultGraph = false;
	private boolean performEntityResolution = false;
	private boolean performOptimization = false;
	private LinkedList<URL> modelgraphsFootprint = new LinkedList<URL>();
	private LinkedList<URL> datagraphsFootprint = new LinkedList<URL>();
	private LinkedList<String> nodegroupsFootprint = new LinkedList<String>();
	private LinkedList<Step> steps = new LinkedList<Step>();

	private static String DEFAULT_FILE_NAME = "manifest.yaml";	// the default manifest file name

	/**
	 * Constructor
	 */
	public Manifest(String name, String description) {
		this.name = name;
		this.description = description;
	}

	/**
	 * Get methods
	 */
	/* Returns the name specified in the manifest */
	public String getName() {
		return name;
	}
	/*Returns the description specified in the manifest */
	public String getDescription() {
		return description;
	}
	/* Returns true when this manifest prescribes copying the footprint to the default graph */
	public boolean getCopyToDefaultGraph() {
		return copyToDefaultGraph;
	}
	/* Returns true when this manifest prescribes running entity resolution */
	public boolean getPerformEntityResolution() {
		return performEntityResolution;
	}
	/* Returns true when this manifest file prescribes running the triplestore optimizer */
	public boolean getPerformOptimization() {
		return performOptimization;
	}
	public LinkedList<URL> getModelgraphsFootprint() {
		return modelgraphsFootprint;
	}
	public LinkedList<URL> getDatagraphsFootprint() {
		return datagraphsFootprint;
	}
	public LinkedList<String> getNodegroupsFootprint() {
		return nodegroupsFootprint;
	}
	public LinkedList<Step> getSteps() {
		return steps;
	}


	/**
	 * Set methods
	 */
	public void setCopyToDefaultGraph(boolean b) {
		this.copyToDefaultGraph = b;
	}
	public void setPerformEntityResolution(boolean b) {
		this.performEntityResolution = b;
	}
	public void setPerformOptimization(boolean b) {
		this.performOptimization = b;
	}
	public void addModelgraphFootprint(URL url) {
		this.modelgraphsFootprint.add(url);
	}
	public void addDatagraphFootprint(URL url) {
		this.datagraphsFootprint.add(url);
	}
	public void addNodegroupFootprint(String s) {
		this.nodegroupsFootprint.add(s);
	}
	public void addStep(Step step) {
		this.steps.add(step);
	}

	/**
	 * Build a connection using the graphs defined in the footprint
	 * @param server the triple store location (e.g. "http://localhost:3030/DATASET")
	 * @param serverTypeString the triple store type (e.g. "fuseki")
	 * @return the connection object
	 */
	public SparqlConnection getConnection(String server, String serverTypeString) throws Exception {
		SparqlConnection conn = new SparqlConnection();
		conn.setName(this.name);
		for(URL graph : modelgraphsFootprint) {
			conn.addModelInterface(SparqlEndpointInterface.getInstance(serverTypeString, server, graph.toString()));
		}
		for(URL graph : datagraphsFootprint) {
			conn.addDataInterface(SparqlEndpointInterface.getInstance(serverTypeString, server, graph.toString()));
		}
		return conn;
	}

	/**
	 * Build a connection to the default graph
	 * @param server the triple store location (e.g. "http://localhost:3030/DATASET")
	 * @param serverTypeString the triple store type (e.g. "fuseki")
	 * @return the connection object
	 */
	public SparqlConnection getDefaultGraphConnection(String server, String serverTypeString) throws Exception {
		SparqlConnection conn = new SparqlConnection();
		conn.setName("Default Graph");
		conn.addModelInterface(SparqlEndpointInterface.getInstance(serverTypeString, server, SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME));
		conn.addDataInterface(SparqlEndpointInterface.getInstance(serverTypeString, server, SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME));
		return conn;
	}

	/**
	 * Load the contents specified in the manifest
	 */
	public void load(String basePath, PrintWriter progressWriter) throws Exception {  // TODO change basePath to Path?
		progressWriter.println("Loading '" + getName() + "'...");
		for(Step step : getSteps()) {
			StepType type = step.getType();
			Object value = step.getValue();

			switch(type) {
				case MANIFEST:
					File subManifestFile = new File(basePath, (String)step.getValue());
					progressWriter.println("Load manifest " + subManifestFile.getAbsolutePath());
					Manifest subManifest = Manifest.fromYaml(subManifestFile);
					subManifest.load(subManifestFile.getParent(), progressWriter);
					// TODO implement and test
					break;
				case DATA:
					File dataFile = new File(basePath, (String)step.getValue());
					progressWriter.println("Load data " + dataFile.getAbsolutePath());
					// TODO implement and test
					break;
				case MODEL:
					File modelFile = new File(basePath, (String)step.getValue());
					progressWriter.println("Load model " + modelFile.getAbsolutePath());
					// TODO implement and test
					break;
				case NODEGROUPS:
					File nodegroupsPath = new File(basePath, (String)step.getValue());
					progressWriter.println("Load nodegroups " + nodegroupsPath.getAbsolutePath());
					// TODO implement and test
					break;
				case COPYGRAPH:
					progressWriter.println("Copy graph X to Y");
					// TODO implement and test
					break;
				default:
					throw new Exception("Unrecognized manifest step: " + type);
			}
			Thread.sleep(3*1000); // TODO REMOVE
			progressWriter.flush();
		}
	}

	/**
	 * Gets the default top-level manifest file in an unzipped ingestion package
	 * @param baseDir the directory of the unzipped ingestion package
	 * @return the file, if it exists
	 * @throws Exception if the file is not found
	 */
	public static File getTopLevelManifestFile(File baseDir) throws Exception {
		File manifestFile = new File(baseDir.getAbsoluteFile() + File.separator + Manifest.DEFAULT_FILE_NAME);
		if(!manifestFile.exists()) {
			throw new Exception(Manifest.DEFAULT_FILE_NAME + " does not exist in " + baseDir);
		}
		return manifestFile;
	}

	/**
	 * Instantiate a manifest from YAML
	 * @param yamlFile a YAML file
	 * @return the manifest object
	 * @throws Exception e.g. if fails validation
	 */
	public static Manifest fromYaml(File yamlFile) throws Exception{
		return fromYaml(Utility.getStringFromFilePath(yamlFile.getAbsolutePath()));
	}

	/**
	 * Instantiate a manifest from YAML
	 * @param yamlStr a YAML string
	 * @return the manifest object
	 * @throws Exception e.g. if fails validation
	 */
	public static Manifest fromYaml(String yamlStr) throws Exception {

		// validate manifest YAML against schema
		String manifestSchema = Utility.getResourceAsString(Manifest.class, "manifest_schema.json");
		Utility.validateYaml(yamlStr, manifestSchema);

		// populate the manifest

		JsonNode manifestJsonNode = (new ObjectMapper(new YAMLFactory())).readTree(yamlStr);
		String name = manifestJsonNode.get("name").asText();
		String description = manifestJsonNode.get("description") != null ? manifestJsonNode.get("description").asText() : null; // optional
		Manifest manifest = new Manifest(name, description);

		// 3 optional boolean properties
		if(manifestJsonNode.get("copy-to-default-graph") != null) { manifest.setCopyToDefaultGraph(manifestJsonNode.get("copy-to-default-graph").booleanValue()); }
		if(manifestJsonNode.get("perform-entity-resolution") != null) { manifest.setPerformEntityResolution(manifestJsonNode.get("perform-entity-resolution").booleanValue()); }
		if(manifestJsonNode.get("perform-triplestore-optimization") != null) { manifest.setPerformOptimization(manifestJsonNode.get("perform-triplestore-optimization").booleanValue()); }

		// footprint
		JsonNode footprintJsonNode = manifestJsonNode.get("footprint");
		if(footprintJsonNode != null) {
			JsonNode nodes;
			nodes = footprintJsonNode.get("model-graphs");
			if(nodes != null) {
				for(JsonNode node : nodes){
					manifest.addModelgraphFootprint(new URL(node.asText()));
				}
			}
			nodes = footprintJsonNode.get("data-graphs");
			if(nodes != null) {
				for(JsonNode node : nodes){
					manifest.addDatagraphFootprint(new URL(node.asText()));
				}
			}
			nodes = footprintJsonNode.get("nodegroups");
			if(nodes != null) {
				for(JsonNode node : nodes){
					manifest.addNodegroupFootprint(new String(node.asText()));
				}
			}
		}

		// steps
		JsonNode stepsJsonNode = manifestJsonNode.get("steps");
		if(stepsJsonNode != null) {
			for(JsonNode stepNode : stepsJsonNode){
				if(stepNode.has(StepType.MODEL.toString())) {
					manifest.addStep(new Step(StepType.MODEL, stepNode.get(StepType.MODEL.toString()).asText()));
				}else if(stepNode.has(StepType.DATA.toString())) {
					manifest.addStep(new Step(StepType.DATA, stepNode.get(StepType.DATA.toString()).asText()));
				}else if(stepNode.has(StepType.NODEGROUPS.toString())) {
					manifest.addStep(new Step(StepType.NODEGROUPS, stepNode.get(StepType.NODEGROUPS.toString()).asText()));
				}else if(stepNode.has(StepType.MANIFEST.toString())) {
					manifest.addStep(new Step(StepType.MANIFEST, stepNode.get(StepType.MANIFEST.toString()).asText()));
				}else if(stepNode.has(StepType.COPYGRAPH.toString())) {
					String fromGraph = stepNode.get(StepType.COPYGRAPH.toString()).get("from-graph").asText();
					String toGraph = stepNode.get(StepType.COPYGRAPH.toString()).get("to-graph").asText();
					manifest.addStep(new Step(StepType.COPYGRAPH, new Pair<URL, URL>(new URL(fromGraph), new URL(toGraph))));
				}else {
					throw new Exception("Unsupported manifest step: " + stepNode);
				}
			}
		}

		return manifest;
	}

	/**
	 * Enumeration for manifest step types, with String values corresponding to the manifest YAML
	 */
	public static enum StepType { 
		MODEL		{public String toString(){return "model";}}, 
		DATA		{public String toString(){return "data";}}, 
		NODEGROUPS	{public String toString(){return "nodegroups";}}, 
		MANIFEST	{public String toString(){return "manifest";}}, 
		COPYGRAPH	{public String toString(){return "copygraph";}}
	}

	/**
	 * Class representing a manifest step
	 */
	public static class Step{
		private StepType type;
		private Object value;
		public Step(StepType type, Object o){
			this.type = type;
			this.value = o;
		}
		public StepType getType(){
			return type;
		}
		public Object getValue(){
			return value;
		}
	}
	
}
