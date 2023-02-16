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
package com.ge.research.semtk.load.manifest;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.Utility;

/**
 * Class representing a manifest for loading content into triplestore
 * Populated from a YAML file conforming to schema manifest_schema.json
 */
public class Manifest extends YamlConfig {

	private String name;
	private String description;
	private boolean copyToDefaultGraph = false;
	private boolean performEntityResolution = false;
	private LinkedList<String> modelgraphsFootprint = new LinkedList<String>();
	private LinkedList<String> datagraphsFootprint = new LinkedList<String>();
	private LinkedList<String> nodegroupsFootprint = new LinkedList<String>();
	private LinkedList<Step> steps = new LinkedList<Step>();

	private static String DEFAULT_FILE_NAME = "manifest.yaml";	// the default manifest file name

	public Manifest(File yamlFile, String fallbackModelGraph, String fallbackDataGraph) throws Exception {
		super(yamlFile, Utility.getResourceAsFile(Manifest.class, "/manifest/manifest_schema.json"), fallbackModelGraph, fallbackDataGraph);

		// populate the manifest
		String name = configNode.get("name").asText();  // required
		String description = configNode.get("description") != null ? configNode.get("description").asText() : null; // optional

		setName(name);
		setDescription(description);

		// 3 optional boolean properties
		if(configNode.get("copy-to-default-graph") != null) { setCopyToDefaultGraph(configNode.get("copy-to-default-graph").booleanValue()); }
		if(configNode.get("perform-entity-resolution") != null) { setPerformEntityResolution(configNode.get("perform-entity-resolution").booleanValue()); }

		// footprint
		JsonNode footprintJsonNode = configNode.get("footprint");
		if(footprintJsonNode != null) {
			JsonNode nodes;
			nodes = footprintJsonNode.get("model-graphs");
			if(nodes != null) {
				for(JsonNode node : nodes){
					addModelgraphFootprint(node.asText());
				}
			}
			nodes = footprintJsonNode.get("data-graphs");
			if(nodes != null) {
				for(JsonNode node : nodes){
					addDatagraphFootprint(node.asText());
				}
			}
			nodes = footprintJsonNode.get("nodegroups");
			if(nodes != null) {
				for(JsonNode node : nodes){
					addNodegroupFootprint(new String(node.asText()));
				}
			}
		}

		// steps
		JsonNode stepsJsonNode = configNode.get("steps");
		if(stepsJsonNode != null) {
			for(JsonNode stepNode : stepsJsonNode){
				if(stepNode.has(StepType.MODEL.toString())) {
					addStep(new Step(StepType.MODEL, stepNode.get(StepType.MODEL.toString()).asText()));
				}else if(stepNode.has(StepType.DATA.toString())) {
					addStep(new Step(StepType.DATA, stepNode.get(StepType.DATA.toString()).asText()));
				}else if(stepNode.has(StepType.NODEGROUPS.toString())) {
					addStep(new Step(StepType.NODEGROUPS, stepNode.get(StepType.NODEGROUPS.toString()).asText()));
				}else if(stepNode.has(StepType.MANIFEST.toString())) {
					addStep(new Step(StepType.MANIFEST, stepNode.get(StepType.MANIFEST.toString()).asText()));
				}else if(stepNode.has(StepType.COPYGRAPH.toString())) {
					String fromGraph = stepNode.get(StepType.COPYGRAPH.toString()).get("from-graph").asText();
					String toGraph = stepNode.get(StepType.COPYGRAPH.toString()).get("to-graph").asText();
					addStep(new Step(StepType.COPYGRAPH, new Pair<String, String>(fromGraph, toGraph)));
				}else {
					throw new Exception("Unsupported manifest step: " + stepNode);
				}
			}
		}
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
	public LinkedList<String> getModelgraphsFootprint() {
		return modelgraphsFootprint;
	}
	public LinkedList<String> getDatagraphsFootprint() {
		return datagraphsFootprint;
	}
	/* Gets all model and data graphs in the footprint */
	public LinkedList<String> getGraphsFootprint(){
		LinkedList<String> ret = new LinkedList<String>();
		if (getModelgraphsFootprint() != null) {
			ret.addAll(getModelgraphsFootprint());
		}
		if (getDatagraphsFootprint() != null) {
			ret.addAll(getDatagraphsFootprint());
		}
		return ret;
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
	public void setName(String name) {
		this.name = name;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setCopyToDefaultGraph(boolean b) {
		this.copyToDefaultGraph = b;
	}
	public void setPerformEntityResolution(boolean b) {
		this.performEntityResolution = b;
	}
	public void addModelgraphFootprint(String s) {
		this.modelgraphsFootprint.add(s);
	}
	public void addDatagraphFootprint(String s) {
		this.datagraphsFootprint.add(s);
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
		for(String graph : modelgraphsFootprint) {
			conn.addModelInterface(SparqlEndpointInterface.getInstance(serverTypeString, server, graph.toString()));
		}
		for(String graph : datagraphsFootprint) {
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
	 * @param server 				the triple store location (e.g. "http://localhost:3030/DATASET")
	 * @param serverTypeString 		the triple store type (e.g. "fuseki")
	 * @param clear 				if true, clears the footprint graphs (before loading)
	 * @param loadToDefaultGraph 	if true, loads everything to the default graph instead of the target graphs
	 * @param topLevel 				true if this is a top-level manifest, false for recursively calling sub-manifests
	 * @param ingestClient			ingestionRestClient
	 * @param progressWriter 		writer for reporting progress
	 */
	public void load(String server, String serverTypeString, boolean clear, boolean loadToDefaultGraph, boolean topLevel, IngestorRestClient ingestClient, NodeGroupExecutionClient ngeClient, NodeGroupStoreRestClient ngStoreClient, PrintWriter progressWriter) throws Exception {

		progressWriter.println("Loading manifest '" + getName() + "'...");

		// clear graphs first
		if(clear) {
			if(loadToDefaultGraph) {
				clearDefaultGraph(serverTypeString, server);
			} else {
				// clear each model and data graph in the footprint
				for(String g : getGraphsFootprint()) {
					SparqlEndpointInterface.getInstance(serverTypeString, server, g).clearGraph();
				}
			}
			// no need to delete nodegroups, they will get overwritten below
		}

		// if loading to default graph, then set targetGraph
		String targetGraph = null;
		if(loadToDefaultGraph) {
			targetGraph = SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME;
		}

		// execute each manifest step
		for(Step step : getSteps()) {
			StepType type = step.getType();

			if(type == StepType.MODEL) {
				// load via an owl ingestion YAML
				File stepFile = new File(baseDir, (String)step.getValue());
				progressWriter.println("Load model " + stepFile.getAbsolutePath());
				IngestOwlConfig config = new IngestOwlConfig(stepFile, this.fallbackModelGraph);
				config.load(targetGraph, server, serverTypeString, progressWriter);

			}else if(type == StepType.DATA) {
				// load content using CSV ingestion YAML
				File stepFile = new File(baseDir, (String)step.getValue());
				progressWriter.println("Load data " + stepFile.getAbsolutePath());
				IngestCsvConfig config = new IngestCsvConfig(stepFile, this.fallbackModelGraph, this.fallbackDataGraph);
				config.load(targetGraph, (targetGraph == null ? null : new LinkedList<String>(Arrays.asList(targetGraph))), server, serverTypeString, clear, ingestClient, ngeClient, progressWriter);

			}else if(type == StepType.NODEGROUPS) {
				// load nodegroups/reports from a directory
				File nodegroupsDirectory = new File(baseDir, (String)step.getValue());
				progressWriter.println("Load nodegroups from " + nodegroupsDirectory.getAbsolutePath());
				File csvFile = new File(nodegroupsDirectory, "store_data.csv");
				ngStoreClient.loadStoreDataCsv(csvFile.getAbsolutePath(), null, progressWriter);

			}else if(type == StepType.MANIFEST) {
				// load content using sub-manifest
				File stepFile = new File(baseDir, (String)step.getValue());
				progressWriter.println("Load manifest " + stepFile.getAbsolutePath());
				Manifest subManifest = new Manifest(stepFile, fallbackModelGraph, fallbackDataGraph);
				subManifest.load(server, serverTypeString, clear, loadToDefaultGraph, false, ingestClient, ngeClient, ngStoreClient, progressWriter);

			}else if(type == StepType.COPYGRAPH) {
				// perform the copy		TODO junit
				String fromGraph = ((String[])step.getValue())[0];
				String toGraph = ((String[])step.getValue())[1];
				ngeClient.copyGraph(server, serverTypeString, fromGraph, server, serverTypeString, toGraph);

			}else {
				throw new Exception("Unrecognized manifest step: " + type);
			}
			progressWriter.flush();
		}

		// actions to be performed on top-level manifests only: copy to default graph, entity resolution
		if(topLevel) {
			if(this.getCopyToDefaultGraph()) {
				if(clear) {
					clearDefaultGraph(serverTypeString, server);
				}
				// call SemTK to copy each model/data footprint graph to default graph
				for(String graph : this.getGraphsFootprint()) {
					progressWriter.println("Copy graph " + graph + " to default graph");
					progressWriter.flush();
					String msg = ngeClient.copyGraph(server, serverTypeString, graph, server, serverTypeString, SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);
					progressWriter.println(msg);
					progressWriter.flush();
				}
			}
			if(this.getPerformEntityResolution()) {
				// TODO call SemTK to perform entity resolution
			}
		}
		progressWriter.flush();
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

	/**
	 * Clear the default graph
	 */
	private void clearDefaultGraph(String serverTypeString, String server) throws Exception {
		SparqlEndpointInterface.getInstance(serverTypeString, server, SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME).clearGraph();
	}

}
