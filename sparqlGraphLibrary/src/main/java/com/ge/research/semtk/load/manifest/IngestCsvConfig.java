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
import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.research.semtk.utility.Utility;

/**
 * Configuration for loading data to the triplestore
 * Populated from a YAML file conforming to schema ingest_csv_config_schema.json
 */
public class IngestCsvConfig extends YamlConfig {
	
	private LinkedList<IngestionStep> steps = new LinkedList<IngestionStep>();
	private String datagraph;			// optional

	/**
	 * Constructor
	 */
	public IngestCsvConfig(File yamlFile, String fallbackModelGraph, String fallbackDataGraph) throws Exception {
		super(yamlFile, fallbackModelGraph, fallbackDataGraph);
		String yamlStr = Utility.getStringFromFilePath(yamlFile.getAbsolutePath());

		// validate YAML against schema
		String configSchema = Utility.getResourceAsString(IngestCsvConfig.class, "manifest/ingest_csv_config_schema.json");
		Utility.validateYaml(yamlStr, configSchema);

		// read YAML
		JsonNode configNode = Utility.getJsonNodeFromYaml(yamlStr);

		// populate ingestion steps
		JsonNode stepsNode = configNode.get("ingestion-steps");
		if(stepsNode != null) {
			for(JsonNode stepNode : stepsNode){
				if(stepNode.has("class") && stepNode.has("csv")) {
					addStep(new ClassCsvIngestionStep(stepNode.get("class").asText(), stepNode.get("csv").asText()));
				}else {
					//TODO support
					throw new Exception("Ingestion step is not yet supported: " + stepNode.asText());
				}
			}
		}

		// populate data graph
		if(configNode.has("data-graph")){
			setDatagraph(configNode.get("data-graph").asText());
		}

		// TODO populate model graph(s) - or error
		// TODO populate extra datagraphs - or error
	}

	/**
	 * Get methods
	 */
	public LinkedList<IngestionStep> getSteps(){
		return steps;
	}
	public String getDatagraph() {
		return datagraph;
	}

	/**
	 * Set methods
	 */
	public void addStep(IngestionStep step) {
		this.steps.add(step);
	}
	public void setDatagraph(String datagraph) {
		this.datagraph = datagraph;
	}

	public static abstract class IngestionStep{}
	
	public static class ClassCsvIngestionStep extends IngestionStep{
		private String clazz;
		private String csv;
		public ClassCsvIngestionStep(String clazz, String csv) {
			this.clazz = clazz;
			this.csv = csv;
		}
		public String getClazz() {
			return clazz;
		}
		public String getCsv() {
			return csv;
		}
	}

}
