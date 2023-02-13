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
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Configuration for loading a set of OWL files to the triplestore
 * Populated from a YAML file conforming to schema ingest_owl_config_schema.json
 */
public class IngestOwlConfig extends YamlConfig {

	public IngestOwlConfig(File yamlFile, String fallbackModelGraph) throws Exception {
		super(yamlFile, Utility.getResourceAsFile(IngestOwlConfig.class, "/manifest/ingest_owl_config_schema.json"), fallbackModelGraph, null);

		// add files
		JsonNode filesNode = configNode.get("files");
		if(filesNode != null) {
			for(JsonNode fileNode : filesNode){
				addFile(fileNode.asText());
			}
		}
		// add model graph (may be either String or array size 1 to support pre-existing schema)
		setModelgraph(getStringOrFirstArrayEntry(configNode.get("model-graphs")));
	}

	private String modelgraph;   // schema supports multiple model graphs, but functionality not needed
	private LinkedList<String> files = new LinkedList<String>();

	/**
	 * Get methods
	 */
	public String getModelgraph() {
		return modelgraph;
	}
	public List<String> getFiles() {
		return files;
	}

	/**
	 * Set methods
	 */
	public void setModelgraph(String modelgraph) {
		this.modelgraph = modelgraph;
	}
	public void addFile(String file) {
		this.files.add(file);
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
			modelGraph = (modelGraph != null) ? modelGraph : (this.getModelgraph() != null ? this.getModelgraph() : this.fallbackModelGraph );

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
