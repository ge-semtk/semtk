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
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Configuration for loading a set of OWL files to the triplestore
 * Populated from a YAML file conforming to schema ingest_owl_config_schema.json
 */
public class IngestOwlConfig{

	private String baseDir;
	private String fallbackModelGraph;
	private String modelgraph;   // schema supports multiple model graphs, but functionality not needed
	private LinkedList<String> files = new LinkedList<String>();

	/**
	 * Get methods
	 */
	public String getBaseDir() {
		return baseDir;
	}
	public String getFallbackModelGraph() {
		return fallbackModelGraph;
	}
	public String getModelgraph() {
		return modelgraph;
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
	public void setModelgraph(String modelgraph) {
		this.modelgraph = modelgraph;
	}
	public void addFile(String file) {
		this.files.add(file);
	}

	/**
	 * Get an instance from YAML
	 * @param yamlFile 				a YAML file
	 * @param fallbackModelGraph 	use this model graph if not specified anywhere else
	 * @return 						the config object
	 */
	public static IngestOwlConfig fromYaml(File yamlFile, String fallbackModelGraph) throws Exception {
		try {
			String yamlStr = Utility.getStringFromFilePath(yamlFile.getAbsolutePath());

			// validate YAML against schema
			String configSchema = Utility.getResourceAsString(IngestOwlConfig.class, "manifest/ingest_owl_config_schema.json");
			Utility.validateYaml(yamlStr, configSchema);

			// populate the config item
			IngestOwlConfig config = new IngestOwlConfig();
			config.setBaseDir(yamlFile.getParent());
			config.setFallbackModelGraph(fallbackModelGraph);

			JsonNode configJsonNode = Utility.getJsonNodeFromYaml(yamlStr);
			// add files
			JsonNode filesNode = configJsonNode.get("files");
			if(filesNode != null) {
				for(JsonNode fileNode : filesNode){
					config.addFile(fileNode.asText());
				}
			}
			// add model graph (may be either String or array size 1 to support pre-existing schema)
			JsonNode modelGraphNode = configJsonNode.get("model-graphs");
			if(modelGraphNode != null) {
				if(modelGraphNode.isTextual()) {
					config.setModelgraph(modelGraphNode.asText());
				}else if(modelGraphNode.isArray()){
					ArrayNode array = (ArrayNode)modelGraphNode;
					if(array.size() > 1) {
						throw new Exception("Not currently supporting multiple model graphs");
					}
					config.setModelgraph(array.get(0).asText());
				}
			}

			return config;
		}catch(Exception e) {
			LocalLogger.printStackTrace(e);
			throw new Exception("Error populating IngestOwlConfig from " + yamlFile.getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Ingest the OWL files
	 * @param modelGraph		a model graph (optional - overrides if present)
	 * @param server 			triple store location
	 * @param serverTypeString 	triple store type
	 * @param progressWriter 	writer for reporting progress
	 */
	public void load(String modelGraph, String server, String serverType, PrintWriter progressWriter) throws Exception {
		try {
			// use modelGraph from method parameter if present.  Else use from config YAML if present.  Else use fallback.
			if(modelGraph == null) {
				if(this.getModelgraph() != null) {
					modelGraph = this.getModelgraph();		// use from config
				}else{
					modelGraph = this.fallbackModelGraph;	// use the fallback
				}
			}

			// upload each OWL file to model graph
			for(String fileStr : this.getFiles()) {
				File file = new File(this.baseDir, fileStr);
				progressWriter.println("Load file " + file.getAbsolutePath() + " to " + modelGraph + " in " + server);
				progressWriter.flush();
				SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(serverType, server, modelGraph);
				sei.uploadOwl(Files.readAllBytes(file.toPath()));
			}

		}catch(Exception e) {
			LocalLogger.printStackTrace(e);
			throw new Exception("Error ingesting OWL: " + e.getMessage());
		}
	}
}
