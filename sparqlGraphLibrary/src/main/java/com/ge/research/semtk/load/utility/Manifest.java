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
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Class representing a manifest (YAML file with specification for loading content into triplestore)
 */
public class Manifest {

	private String baseDir;
	private String fallbackModelGraph;
	private String fallbackDataGraph;

	private String name;
	private String description;
	private boolean copyToDefaultGraph = false;
	private boolean performEntityResolution = false;
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
	public String getBaseDir() {
		return baseDir;
	}
	public String getFallbackModelGraph() {
		return fallbackModelGraph;
	}
	public String getFallbackDataGraph() {
		return fallbackDataGraph;
	}
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
	private void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}
	public void setFallbackModelGraph(String fallbackModelGraph) {
		this.fallbackModelGraph = fallbackModelGraph;
	}
	public void setFallbackDataGraph(String fallbackDataGraph) {
		this.fallbackDataGraph = fallbackDataGraph;
	}
	public void setCopyToDefaultGraph(boolean b) {
		this.copyToDefaultGraph = b;
	}
	public void setPerformEntityResolution(boolean b) {
		this.performEntityResolution = b;
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
	 * @param server 			the triple store location (e.g. "http://localhost:3030/DATASET")
	 * @param serverTypeString 	the triple store type (e.g. "fuseki")
	 * @param clear 			if true, clears the footprint graphs (before loading)
	 * @param defaultGraph 		if true, loads everything to the default graph instead of the target graphs
	 * @param topLevel 			true if this is a top-level manifest, false for recursively calling sub-manifests
	 * @param progressWriter 	writer for reporting progress
	 */
	public void load(String server, String serverTypeString, boolean clear, boolean defaultGraph, boolean topLevel, PrintWriter progressWriter) throws Exception {
		progressWriter.println("Loading '" + getName() + "'...");

		// clear graphs first, if wanted
		if(clear) {
			// TODO
		}

		// if loading to default graph, then set targetGraph
		String[] targetGraph = null;
		if(defaultGraph) {
			targetGraph = new String[]{SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME};
		}

		for(Step step : getSteps()) {
			StepType type = step.getType();

			if(type == StepType.DATA) {
				File stepFile = new File(baseDir, (String)step.getValue());
				progressWriter.println("Load data " + stepFile.getAbsolutePath());
				// TODO implement and test

			}else if(type == StepType.MODEL) {
				File stepFile = new File(baseDir, (String)step.getValue());
				progressWriter.println("Load model " + stepFile.getAbsolutePath());
				IngestOwlConfig config = IngestOwlConfig.fromYaml(stepFile, this.fallbackModelGraph);  // read the config YAML file
				config.ingest(targetGraph, server, serverTypeString, progressWriter);

			}else if(type == StepType.NODEGROUPS) {
				File stepFile = new File(baseDir, (String)step.getValue());
				progressWriter.println("Load nodegroups " + stepFile.getAbsolutePath());
				// TODO implement and test

			}else if(type == StepType.MANIFEST) {
				File stepFile = new File(baseDir, (String)step.getValue());
				progressWriter.println("Load manifest " + stepFile.getAbsolutePath());
				Manifest subManifest = Manifest.fromYaml(stepFile, fallbackModelGraph, fallbackDataGraph);
				subManifest.load(server, serverTypeString, clear, defaultGraph, false, progressWriter);

			}else if(type == StepType.COPYGRAPH) {
				progressWriter.println("Copy graph X to Y");
				// TODO implement and test

			}else {
				throw new Exception("Unrecognized manifest step: " + type);
			}
			progressWriter.flush();
		}

		// actions to be performed on top-level manifests only: copy to default graph, entity resolution
		if(topLevel) {
			if(this.getCopyToDefaultGraph()) {
				if(clear) {
					// TODO clear default graph
				}
				// TODO copy each model/data graph to default graph
			}
			if(this.getPerformEntityResolution()) {
				// TODO entity resolution
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
		File manifestFile = new File(baseDir.getAbsoluteFile() + File.separator + Manifest.DEFAULT_FILE_NAME);
		if(!manifestFile.exists()) {
			throw new Exception(Manifest.DEFAULT_FILE_NAME + " does not exist in " + baseDir);
		}
		return manifestFile;
	}

	/**
	 * Instantiate a manifest from YAML
	 * @param yamlFile 				a YAML file
	 * @param fallbackModelGraph 	use this model graph if not specified anywhere else
	 * @param fallbackDataGraph 	use this data graph if not specified anywhere else
	 * @return 						the manifest object
	 * @throws Exception 			e.g. if fails validation
	 */
	public static Manifest fromYaml(File yamlFile, String fallbackModelGraph, String fallbackDataGraph) throws Exception{

		String yamlStr = Utility.getStringFromFilePath(yamlFile.getAbsolutePath());

		// validate manifest YAML against schema
		String manifestSchema = Utility.getResourceAsString(Manifest.class, "manifest_schema.json");
		Utility.validateYaml(yamlStr, manifestSchema);

		// populate the manifest

		JsonNode manifestJsonNode = (new ObjectMapper(new YAMLFactory())).readTree(yamlStr);
		String name = manifestJsonNode.get("name").asText();  // required
		String description = manifestJsonNode.get("description") != null ? manifestJsonNode.get("description").asText() : null; // optional
		Manifest manifest = new Manifest(name, description);
		manifest.setBaseDir(yamlFile.getParent());
		manifest.setFallbackModelGraph(fallbackModelGraph);
		manifest.setFallbackDataGraph(fallbackDataGraph);

		// 3 optional boolean properties
		if(manifestJsonNode.get("copy-to-default-graph") != null) { manifest.setCopyToDefaultGraph(manifestJsonNode.get("copy-to-default-graph").booleanValue()); }
		if(manifestJsonNode.get("perform-entity-resolution") != null) { manifest.setPerformEntityResolution(manifestJsonNode.get("perform-entity-resolution").booleanValue()); }

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
	
	/**
	 * Configuration for loading a set of OWL files
	 *
	 * // TODO the python schema allows model-graphs to be a string or an array.  We support array here - make it support both
	 */
	public static class IngestOwlConfig{

		private String baseDir;
		private String fallbackModelGraph;

		@JsonProperty("model-graphs")
		private List<String> modelgraphs;
		private List<String> files;

		/**
		 * Get methods
		 */
		public String getBaseDir() {
			return baseDir;
		}
		public String getFallbackModelGraph() {
			return fallbackModelGraph;
		}
		public List<String> getModelgraphs() {
			return modelgraphs;
		}
		public List<String> getFiles() {
			return files;
		}

		/**
		 * Set methods
		 */
		public void setBaseDir(String baseDir) {
			this.baseDir = baseDir;
		}
		public void setFallbackModelGraph(String fallbackModelGraph) {
			this.fallbackModelGraph = fallbackModelGraph;
		}
		public void setModelgraphs(List<String> modelgraphs) {
			this.modelgraphs = modelgraphs;
		}
		public void setFiles(List<String> files) {
			this.files = files;
		}

		/**
		 * Get an instance from YAML
		 * @param yamlFile 				a YAML file
		 * @param fallbackModelGraph 	use this model graph if not specified anywhere else
		 * @return 						the config object
		 */
		public static IngestOwlConfig fromYaml(File yamlFile, String fallbackModelGraph) throws Exception {
			try {
				ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
				mapper.findAndRegisterModules();
				IngestOwlConfig config = mapper.readValue(yamlFile, IngestOwlConfig.class); // validates/populates IngestOwlConfig instance using YAML
				config.setBaseDir(yamlFile.getParent());
				config.setFallbackModelGraph(fallbackModelGraph);
				return config;
			}catch(Exception e) {
				LocalLogger.printStackTrace(e);
				throw new Exception("Error populating IngestOwlConfig from " + yamlFile.getName() + ": " + e.getMessage());
			}
		}

		/**
		 * Ingest the OWL files
		 * @param modelGraphs		a set of model graphs (optional - if present, loads to these instead of the model graphs in the config file)
		 * @param server 			triple store location
		 * @param serverTypeString 	triple store type
		 * @param progressWriter 	writer for reporting progress
		 */
		public void ingest(String[] modelGraphs, String server, String serverType, PrintWriter progressWriter) throws Exception {
			try {
				// if modelGraphs parameter not given, then use model graphs from the config file
				if(modelGraphs == null || modelGraphs.length == 0) {
					if(this.getModelgraphs() != null) {
						modelGraphs = this.getModelgraphs().toArray(new String[0]);
					}
				}
				// if no model graphs found, use the fallback
				if(modelGraphs == null || modelGraphs.length == 0) {
					modelGraphs = new String[]{ this.fallbackModelGraph };
				}

				// upload each OWL file to each model graph
				for(String fileStr : this.getFiles()) {
					File file = new File(this.baseDir, fileStr);
					for(String graph : modelGraphs) {
						progressWriter.println("Load file " + file.getAbsolutePath() + " to " + graph + " in " + server);
						progressWriter.flush();
						SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(serverType, server, graph);
						sei.uploadOwl(Files.readAllBytes(file.toPath()));
					}
				}

			}catch(Exception e) {
				LocalLogger.printStackTrace(e);
				throw new Exception("Error ingesting OWL: " + e.getMessage());
			}
		}
	}

}
