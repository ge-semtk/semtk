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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Configuration for loading data to the triplestore
 * Populated from a YAML file conforming to schema load_data_config_schema.json
 */
public class LoadDataConfig extends YamlConfig {

	private LinkedList<IngestionStep> steps = new LinkedList<IngestionStep>();
	private String modelgraph;   			// schema supports multiple model graphs, but functionality not needed
	private LinkedList<String> datagraphs;

	/**
	 * Constructor
	 */
	public LoadDataConfig(File yamlFile, String defaultModelGraph, String defaultDataGraph) throws Exception {
		super(yamlFile, Utility.getResourceAsTempFile(LoadDataConfig.class, "/configSchema/load_data_config_schema.json"), defaultModelGraph, defaultDataGraph);

		if(defaultModelGraph == null) {
			throw new Exception("Default model graph not provided");
		}
		if(defaultDataGraph == null) {
			throw new Exception("Default data graph not provided");
		}

		// populate ingestion steps
		JsonNode stepsNode = configNode.get("ingestion-steps");
		if(stepsNode != null) {
			for(JsonNode stepNode : stepsNode){
				if(stepNode.has("class") && stepNode.has("csv")) {
					addStep(new CsvByClassIngestionStep(stepNode.get("class").asText(), baseDir + File.separator + stepNode.get("csv").asText()));
				} else if (stepNode.has("nodegroup") && stepNode.has("csv")) {
					addStep(new CsvByNodegroupIngestionStep(stepNode.get("nodegroup").asText(), baseDir + File.separator + stepNode.get("csv").asText()));
				} else if (stepNode.has("owl")) {
					addStep(new OwlIngestionStep(baseDir + File.separator + stepNode.get("owl").asText()));
				}else {
					throw new Exception("Ingestion step not supported: " + stepNode);
				}
			}
		}

		// add model graph (may be either String or array size 1 to support pre-existing schema)
		setModelgraph(getStringOrFirstArrayEntry(configNode.get("model-graphs")));

		// add data graph
		if(configNode.has("data-graph")){
			addDatagraph(configNode.get("data-graph").asText());
		}
		if(configNode.has("extra-data-graphs")){
			for(JsonNode n : configNode.get("extra-data-graphs")) {
				addDatagraph(n.asText());
			}
		}
	}

	/**
	 * Get methods
	 */
	public LinkedList<IngestionStep> getSteps(){
		return steps;
	}
	public String getModelgraph() {
		return modelgraph;
	}
	public LinkedList<String> getDatagraphs() {
		return datagraphs;
	}
	/**
	 * Set methods
	 */
	public void addStep(IngestionStep step) {
		this.steps.add(step);
	}
	public void setModelgraph(String modelgraph) {
		this.modelgraph = modelgraph;
	}
	public void setDataGraphs(LinkedList<String> datagraphs) {
		this.datagraphs = datagraphs;
	}
	public void addDatagraph(String datagraph) {
		if(datagraphs == null) {
			this.datagraphs = new LinkedList<String>();
		}
		this.datagraphs.add(datagraph);
	}

	
	/**
	 * Ingest data
	 * @param modelGraph		a model graph (optional)
	 * @param dataGraphs		a data graph (optional)
	 * @param server 			triple store location
	 * @param serverTypeString 	triple store type
	 * @param clear				if true, clears before loading
	 * @param ingestClient	    Ingestor rest client
	 * @param progressWriter 	writer for reporting progress
	 */
	public void load(String modelGraph, LinkedList<String> dataGraphs, String server, String serverType, boolean clear, IngestorRestClient ingestClient, NodeGroupExecutionClient ngeClient, PrintWriter progressWriter) throws Exception {
		try {

			// determine which model/data graphs to use
			// use if provided as method parameter, else use from config YAML, else use default
			modelGraph = (modelGraph != null) ? modelGraph : (this.getModelgraph() != null ? this.getModelgraph() : this.defaultModelGraph );
			dataGraphs = (dataGraphs != null) ? dataGraphs : (this.getDatagraphs() != null ? this.getDatagraphs() : new LinkedList<String>(Arrays.asList(this.defaultDataGraph)) );
			if(modelGraph == null) { throw new Exception ("No model graph found"); }
			if(dataGraphs == null || dataGraphs.isEmpty()) { throw new Exception ("No data graph found"); }

			// create a connection
			SparqlConnection conn = new SparqlConnection();
			conn.addModelInterface(serverType, server, modelGraph);
			for(String dg : dataGraphs) {
				conn.addDataInterface(serverType, server, dg);
			}

			// clear if appropriate
			if(clear) {
				for (SparqlEndpointInterface sei : conn.getAllInterfaces()) {
					sei.clearGraph();
				}
			}

			// execute each step
			for(IngestionStep step : this.getSteps()) {
				if(step instanceof CsvByClassIngestionStep) {
					((CsvByClassIngestionStep)step).run(conn, ingestClient, ngeClient, progressWriter);
				}else if(step instanceof CsvByNodegroupIngestionStep) {
					((CsvByNodegroupIngestionStep)step).run(conn, ngeClient, progressWriter);
				}else if(step instanceof OwlIngestionStep) {
					((OwlIngestionStep)step).run(conn, progressWriter);
				}else {
					throw new Exception("Unrecognized ingestion step");		// should not get here
				}
			}
		}catch(Exception e) {
			LocalLogger.printStackTrace(e);
			throw new Exception("Error ingesting data: " + e.getMessage());
		}
	}



	// **************  ingestion step types below ************************

	public static abstract class IngestionStep{
	}
	public static abstract class FileIngestionStep extends IngestionStep{
		String filePath;
		public FileIngestionStep(String filePath) {
			this.filePath = filePath;
		}
		public String getFilePath() {
			return filePath;
		}
	}

	/* Ingestion step: load CSV by class */
	public static class CsvByClassIngestionStep extends FileIngestionStep{
		private String clazz;
		public CsvByClassIngestionStep(String clazz, String csvPath) {
			super(csvPath);
			this.clazz = clazz;
		}
		public String getClazz() {
			return clazz;
		}
		public void run(SparqlConnection conn, IngestorRestClient ingestClient, NodeGroupExecutionClient ngeClient, PrintWriter progressWriter) throws Exception {
			writeProgress("Load CSV " + (new File(filePath)).getName() + " as " + clazz, progressWriter);
			String jobId = ingestClient.execFromCsvUsingClassTemplate(clazz, null, Files.readString(Path.of(filePath)), conn, false, null);
			if(ingestClient.getWarnings() != null) {
				for (String warning : ingestClient.getWarnings()) {
					writeProgress("Load CSV warning: " + warning, progressWriter);
				}
			}
			ngeClient.waitForCompletion(jobId);
			if (!ngeClient.getJobSuccess(jobId)) {
				throw new Exception("Failed loading CSV by class:\n" + ngeClient.getResultsTable(jobId).toCSVString());
			}
		}
	}

	/* Ingestion step: load CSV by nodegroup */
	public static class CsvByNodegroupIngestionStep extends FileIngestionStep{
		private String nodegroupId;
		public CsvByNodegroupIngestionStep(String nodegroupId, String csvPath) {
			super(csvPath);
			this.nodegroupId = nodegroupId;
		}
		public String getNodegroupId() {
			return nodegroupId;
		}
		public void run(SparqlConnection conn, NodeGroupExecutionClient ngeClient, PrintWriter progressWriter) throws Exception {
			writeProgress("Load CSV " + filePath + " using nodegroup " + nodegroupId, progressWriter);
			ngeClient.dispatchIngestFromCsvStringsByIdSync(this.nodegroupId, Files.readString(Path.of(filePath))); // TODO junit
		}
	}

	/* Ingestion step: load OWL to data (not model) interface */
	public static class OwlIngestionStep extends FileIngestionStep{
		public OwlIngestionStep(String owlPath) {
			super(owlPath);
		}
		public void run(SparqlConnection conn, PrintWriter progressWriter) throws Exception {
			if(conn.getDataInterfaceCount() != 1) {
				throw new Exception("Error: cannot load OWL because 0 or multiple data interfaces are specified");
			}
			File file = (new File(filePath));
			SparqlEndpointInterface sei = conn.getDataInterface(0);
			writeProgress("Load OWL " + file.getName() + " to " + sei.getGraph(), progressWriter);
			sei.uploadOwl(Files.readAllBytes(file.toPath()));
		}
	}

}
