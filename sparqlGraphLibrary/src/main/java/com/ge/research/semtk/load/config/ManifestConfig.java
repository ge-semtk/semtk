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
package com.ge.research.semtk.load.config;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.utility.Utility;

/**
 * Class representing a manifest for loading content into triplestore
 * Populated from a YAML file conforming to schema manifest_config_schema.json
 */
public class ManifestConfig extends YamlConfig {

	private String name;
	private String description;
	private String copyToGraph;
	private String entityResolutionGraph;
	private LinkedList<String> modelgraphsFootprint = new LinkedList<String>();
	private LinkedList<String> datagraphsFootprint = new LinkedList<String>();
	private LinkedList<String> nodegroupsFootprint = new LinkedList<String>();
	private LinkedList<Step> steps = new LinkedList<Step>();

	private static String DEFAULT_FILE_NAME = "manifest.yaml";	// the default manifest file name

	/**
	 * Constructor
	 * @param yamlFile			the YAML file
	 * @param defaultModelGraph	model graph to use if not otherwise specified
	 * @param defaultDataGraph	data graph to use if not otherwise specified
	 * @throws Exception
	 */
	public ManifestConfig(File yamlFile, String defaultModelGraph, String defaultDataGraph) throws Exception {
		super(yamlFile, Utility.getResourceAsTempFile(ManifestConfig.class, "/configSchema/manifest_config_schema.json"), defaultModelGraph, defaultDataGraph);

		// populate the manifest
		String name = configNode.get("name").asText();  // required
		String description = configNode.get("description") != null ? configNode.get("description").asText() : null; // optional

		setName(name);
		setDescription(description);

		if(configNode.get("copy-to-graph") != null && !configNode.get("copy-to-graph").asText().trim().isEmpty()) { setCopyToGraph(configNode.get("copy-to-graph").asText()); }
		if(configNode.get("perform-entity-resolution") != null && !configNode.get("perform-entity-resolution").asText().trim().isEmpty()) { setEntityResolutionGraph(configNode.get("perform-entity-resolution").asText()); }

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
	/* Returns the graph to copy to (after load complete) */
	public String getCopyToGraph() {
		return copyToGraph;
	}
	/* Returns the graph in which to perform entity resolution (after load complete) */
	public String getEntityResolutionGraph() {
		return entityResolutionGraph;
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
	public void setCopyToGraph(String s) {
		this.copyToGraph = s;
	}
	public void setEntityResolutionGraph(String s) {
		this.entityResolutionGraph = s;
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
	public SparqlConnection getFootprintConnection(String server, String serverTypeString) throws Exception {
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
	 * Load the contents specified in the manifest
	 * @param server 				the triple store location (e.g. "http://localhost:3030/DATASET")
	 * @param serverTypeString 		the triple store type (e.g. "fuseki")
	 * @param clear 				if true, clears the footprint graphs (before loading)
	 * @param topLevel 				true if this is a top-level manifest, false for recursively calling sub-manifests
	 * @param ingestClient			ingestionRestClient
	 * @param progressWriter 		writer for reporting progress
	 */
	public void load(String server, String serverTypeString, boolean clear, boolean topLevel, IngestorRestClient ingestClient, NodeGroupExecutionClient ngeClient, NodeGroupStoreRestClient ngStoreClient, SparqlQueryClient queryClient, PrintWriter progressWriter) throws Exception {

		writeProgress("Loading manifest for '" + getName() + "'...", progressWriter);

		// clear graphs first
		if(clear) {
			// clear each model and data graph in the footprint
			for(String g : getGraphsFootprint()) {
				writeProgress("Clear graph " + g, progressWriter);
				queryClient.setSei(SparqlEndpointInterface.getInstance(serverTypeString, server, g, username, password));
				queryClient.clearAll();
			}
			// no need to delete nodegroups, they will get overwritten below
		}

		// execute each manifest step
		for(Step step : getSteps()) {
			StepType type = step.getType();

			if(type == StepType.MODEL) {
				// load via an owl ingestion YAML
				File stepFile = new File(baseDir, (String)step.getValue());
				LoadOwlConfig config = new LoadOwlConfig(stepFile, this.defaultModelGraph);
				config.load(null, server, serverTypeString, queryClient, progressWriter);

			}else if(type == StepType.DATA) {
				// load content using CSV ingestion YAML
				File stepFile = new File(baseDir, (String)step.getValue());
				LoadDataConfig config = new LoadDataConfig(stepFile, this.defaultModelGraph, this.defaultDataGraph);
				config.load(null, null, server, serverTypeString, false, ingestClient, ngeClient, queryClient, progressWriter);

			}else if(type == StepType.NODEGROUPS) {
				// load nodegroups/reports from a directory
				writeProgress("Store nodegroups", progressWriter);
				File nodegroupsDirectory = new File(baseDir, (String)step.getValue());
				File csvFile = new File(nodegroupsDirectory, "store_data.csv");
				ngStoreClient.loadStoreDataCsv(csvFile.getAbsolutePath(), null, progressWriter);

			}else if(type == StepType.MANIFEST) {
				// load content using sub-manifest
				File stepFile = new File(baseDir, (String)step.getValue());
				ManifestConfig subManifest = new ManifestConfig(stepFile, defaultModelGraph, defaultDataGraph);
				subManifest.load(server, serverTypeString, false, false, ingestClient, ngeClient, ngStoreClient, queryClient, progressWriter);

			}else if(type == StepType.COPYGRAPH) {
				// perform the copy
				String fromGraph = (String)((Pair)step.getValue()).getFirst();
				String toGraph = (String)((Pair)step.getValue()).getSecond();
				writeProgress("Copy " + fromGraph + " to " + toGraph, progressWriter);
				ngeClient.copyGraph(server, serverTypeString, fromGraph, server, serverTypeString, toGraph);

			}else {
				throw new Exception("Unrecognized manifest step: " + type);
			}
		}

		// actions to be performed on top-level manifests only: copy to graph, entity resolution
		if(topLevel) {

			// copy to graph
			String copyToGraph = this.getCopyToGraph();
			if(copyToGraph != null) {
				if(clear) {
					writeProgress("Clear graph " + copyToGraph, progressWriter);
					queryClient.setSei(SparqlEndpointInterface.getInstance(serverTypeString, server, copyToGraph, username, password));
					queryClient.clearAll();
				}
				for(String graph : this.getGraphsFootprint()) {  // copy each model/data footprint graph to the given graph
					writeProgress("Copy graph " + graph + " to " + copyToGraph, progressWriter);
					String msg = ngeClient.copyGraph(server, serverTypeString, graph, server, serverTypeString, copyToGraph);
					writeProgress(msg, progressWriter);
				}
			}

			// entity resolution
			String entityResolutionGraph = this.getEntityResolutionGraph();
			if(entityResolutionGraph != null) {
				writeProgress("Perform entity resolution in " + entityResolutionGraph, progressWriter);
				try {
					SparqlConnection conn = new SparqlConnection("Entity Resolution", serverTypeString, server, entityResolutionGraph);
					ngeClient.combineEntitiesInConn(conn);
				}catch(Exception e) {
					if(!e.getMessage().contains("No SameAs instances")) {  // if exception is about no entities to resolve, suppress it
						throw e;
					}
				}
			}
		}
	}

	/**
	 * Gets the default top-level manifest file in an unzipped ingestion package
	 * @param baseDir the directory of the unzipped ingestion package
	 * @return the file, if it exists
	 * @throws Exception if the file is not found
	 */
	public static File getTopLevelManifestFile(File baseDir) throws Exception {
		File manifestFile = new File(baseDir.getAbsoluteFile() + File.separator + ManifestConfig.DEFAULT_FILE_NAME);
		if(!manifestFile.exists()) {
			throw new Exception(ManifestConfig.DEFAULT_FILE_NAME + " does not exist in " + baseDir);
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

}
