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
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Logger;
import com.ge.research.semtk.utility.Utility;

/**
 * Configuration for loading a set of OWL files to the triplestore
 * Populated from a YAML file conforming to schema load_owl_config_schema.json
 */
public class LoadOwlConfig extends YamlConfig {

	public LoadOwlConfig(File yamlFile, String defaultModelGraph) throws Exception {
		super(yamlFile, Utility.getResourceAsTempFile(LoadOwlConfig.class, "/configSchema/load_owl_config_schema.json"), defaultModelGraph, null);

		if(defaultModelGraph == null) {
			throw new Exception("Default model graph not provided");
		}

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
	 * @param logger		 	logger for reporting progress (may be null)
	 */
	public void load(String modelGraph, String server, String serverType, SparqlQueryClient queryClient, Logger logger) throws Exception {
		try {
			// use modelGraph from method parameter if present.  Else use from config YAML if present.  Else use default.
			modelGraph = (modelGraph != null) ? modelGraph : (this.getModelgraph() != null ? this.getModelgraph() : this.defaultModelGraph );
			if(modelGraph == null) { throw new Exception ("No model graph found"); }

			// upload each OWL file to model graph
			for(String fileStr : this.getFiles()) {
				File file = new File(this.baseDir, fileStr);
				if(logger != null) {
					logger.info("Load OWL " + new File(this.baseDir).getName() + File.separator + file.getName() + " to " + modelGraph);
				}
				SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(serverType, server, modelGraph, username, password);
				queryClient.setSei(sei);
				queryClient.uploadOwl(file);
			}

		}catch(Exception e) {
			LocalLogger.printStackTrace(e);
			throw new Exception("Error ingesting OWL: " + e.getMessage());
		}
	}
}
