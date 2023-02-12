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
import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;
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
		super(yamlFile, Utility.getResourceAsFile(IngestCsvConfig.class, "manifest/ingest_csv_config_schema.json"), fallbackModelGraph, fallbackDataGraph);

		// populate ingestion steps
		JsonNode stepsNode = configNode.get("ingestion-steps");
		if(stepsNode != null) {
			for(JsonNode stepNode : stepsNode){
				if(stepNode.has("class") && stepNode.has("csv")) {
					addStep(new ClassCsvIngestionStep(stepNode.get("class").asText(), baseDir + File.separator + stepNode.get("csv").asText()));
				}else {
					//TODO support 4 more steps from the schema
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

	
	/**
	 * Ingest data
	 * @param modelGraph		a model graph (optional - overrides if present)  // TODO not sure if this is correct
	 * @param dataGraph			// TODO not sure if this is correct  TODO should be multiple?
	 * @param server 			triple store location
	 * @param serverTypeString 	triple store type
	 * @param clear				if true, clears before loading
	 * @param progressWriter 	writer for reporting progress
	 */
	public void load(String modelGraph, String dataGraph, String server, String serverType, boolean clear, PrintWriter progressWriter) throws Exception {
		try {

			// TODO get connection using model/data graph logic
			SparqlConnection conn = new SparqlConnection();

			if(clear) {
				// TODO call SemTK to clear the connection
			}

			// execute each step
			for(IngestionStep step : this.getSteps()) {
				if(step instanceof ClassCsvIngestionStep) {
					((ClassCsvIngestionStep)step).run(conn, progressWriter);
				}
				// TODO add other step types
			}

		}catch(Exception e) {
			LocalLogger.printStackTrace(e);
			throw new Exception("Error ingesting data: " + e.getMessage());
		}
	}



	// **************  ingestion step types below ************************

	public static abstract class IngestionStep{
	}

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
		public void run(SparqlConnection conn, PrintWriter progressWriter) {
			progressWriter.println("Load CSV " + csv + " using class " + clazz);
			progressWriter.flush();
			// TODO call SemTK to load CSV using class template with the given connection
		}
	}

}
