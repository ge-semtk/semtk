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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Logger;
import com.ge.research.semtk.utility.Utility;

/**
 * Configuration for loading data to the triplestore
 * Populated from a YAML file conforming to schema load_data_config_schema.json
 */
public class LoadDataConfig extends YamlConfig {

	private LinkedList<IngestionStep> steps = new LinkedList<IngestionStep>();
	private LinkedList<String> modelgraphs;
	private LinkedList<String> datagraphs;
	private LinkedList<String> allowedGraphs;	// if not null, only allow loads to these graphs

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
					addStep(new CsvByClassIngestionStep(stepNode.get("class").asText(), baseDir, stepNode.get("csv").asText()));
				} else if (stepNode.has("nodegroup") && stepNode.has("csv")) {
					addStep(new CsvByNodegroupIngestionStep(stepNode.get("nodegroup").asText(), baseDir, stepNode.get("csv").asText()));
				} else if (stepNode.has("owl")) {
					addStep(new OwlIngestionStep(baseDir, stepNode.get("owl").asText()));
				}else {
					throw new Exception("Ingestion step not supported: " + stepNode);
				}
			}
		}

		// add model graphs (from either String or String array)
		setModelgraphs(getAsStringList(configNode.get("model-graphs")));

		// add data graphs
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
	public LinkedList<String> getModelgraphs() {
		return modelgraphs;
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
	public void setModelgraphs(LinkedList<String> modelgraphs) {
		this.modelgraphs = modelgraphs;
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
	public void setAllowedGraphs(LinkedList<String> allowedGraphs) {
		this.allowedGraphs = allowedGraphs;
	}

	
	/**
	 * Ingest data
	 * @param modelGraphs      model graphs (optional)
	 * @param dataGraphs       data graphs (optional)
	 * @param server           triple store location
	 * @param serverTypeString triple store type
	 * @param clear            if true, clears before loading
	 * @param ingestClient     Ingestor rest client
	 * @param logger           logger for reporting progress (may be null)
	 */
	public void load(LinkedList<String> modelGraphs, LinkedList<String> dataGraphs, String server, String serverType, boolean clear, IngestorRestClient ingestClient, NodeGroupExecutionClient ngeClient, SparqlQueryClient queryClient, Logger logger) throws Exception {
		try {

			// determine which model/data graphs to use
			// use if provided as method parameter, else use from config YAML, else use default
			modelGraphs = (modelGraphs != null) ? modelGraphs : (this.getModelgraphs() != null ? this.getModelgraphs() : new LinkedList<String>(Arrays.asList(this.defaultModelGraph)) );
			dataGraphs = (dataGraphs != null) ? dataGraphs : (this.getDatagraphs() != null ? this.getDatagraphs() : new LinkedList<String>(Arrays.asList(this.defaultDataGraph)) );
			if(modelGraphs == null || modelGraphs.isEmpty()) { throw new Exception ("No model graph found"); }
			if(dataGraphs == null || dataGraphs.isEmpty()) { throw new Exception ("No data graph found"); }

			// create a connection
			SparqlConnection conn = new SparqlConnection();
			for(String mg : modelGraphs) {
				if(allowedGraphs != null && !allowedGraphs.contains(mg)) { throw new Exception("Cannot load model from " + getFileName() + " in " + getBaseDir()  + ": " + mg + " is not in list of allowed graphs: " + allowedGraphs); }
				conn.addModelInterface(serverType, server, mg);
			}
			for(String dg : dataGraphs) {
				if(allowedGraphs != null && !allowedGraphs.contains(dg)) { throw new Exception("Cannot load data from " + getFileName() + " in " + getBaseDir()  + ": " + dg + " is not in list of allowed graphs: " + allowedGraphs); }
				conn.addDataInterface(serverType, server, dg);
			}

			// clear if appropriate
			if(clear) {
				for (SparqlEndpointInterface sei : conn.getAllInterfaces()) {
					queryClient.setSei(sei);
					queryClient.clearAll();
				}
			}

			// execute each step
			for(IngestionStep step : this.getSteps()) {
				if(step instanceof CsvByClassIngestionStep) {
					((CsvByClassIngestionStep)step).run(conn, ingestClient, ngeClient, logger);
				}else if(step instanceof CsvByNodegroupIngestionStep) {
					((CsvByNodegroupIngestionStep)step).run(conn, ngeClient, logger);
				}else if(step instanceof OwlIngestionStep) {
					((OwlIngestionStep)step).run(conn, queryClient, logger);
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
		String fileBaseDir;
		String filePath;		// path relative to base dir
		public FileIngestionStep(String fileBaseDir, String filePath) {
			this.fileBaseDir = fileBaseDir;
			this.filePath = filePath;
		}
		public String getFileBaseDir() {
			return fileBaseDir;
		}
		public String getFilePath() {
			return filePath;
		}
		public File getFile() {
			return new File(fileBaseDir + File.separator + filePath);
		}
		public String getDisplayableFilePath() {
			return (new File(fileBaseDir)).getName() + File.separator + filePath;  // show final directory of the base path, for context
		}
	}

	/* Ingestion step: load CSV by class */
	public static class CsvByClassIngestionStep extends FileIngestionStep{
		private String clazz;
		public CsvByClassIngestionStep(String clazz, String baseDir, String csvPath) {
			super(baseDir, csvPath);
			this.clazz = clazz;
		}
		public String getClazz() {
			return clazz;
		}
		public void run(SparqlConnection conn, IngestorRestClient ingestClient, NodeGroupExecutionClient ngeClient, Logger logger) throws Exception {
			if(logger != null) {
				logger.info("Load CSV " + getDisplayableFilePath() + " as class " + clazz);
			}
			String jobId = ingestClient.execFromCsvUsingClassTemplate(clazz, null, Files.readString(getFile().toPath()), conn, false, null);
			if(ingestClient.getWarnings() != null) {
				for (String warning : ingestClient.getWarnings()) {
					if(logger != null) {
						logger.warning(warning);
					}
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
		public CsvByNodegroupIngestionStep(String nodegroupId, String baseDir, String csvPath) {
			super(baseDir, csvPath);
			this.nodegroupId = nodegroupId;
		}
		public String getNodegroupId() {
			return nodegroupId;
		}
		public void run(SparqlConnection conn, NodeGroupExecutionClient ngeClient, Logger logger) throws Exception {
			if(logger != null) {
				logger.info("Load CSV " + getDisplayableFilePath() + " using nodegroup \"" + nodegroupId + "\"");
			}
			ngeClient.dispatchIngestFromCsvStringsByIdSync(this.nodegroupId, Files.readString(getFile().toPath()), conn);
		}
	}

	/* Ingestion step: load OWL to data (not model) interface */
	public static class OwlIngestionStep extends FileIngestionStep{
		public OwlIngestionStep(String baseDir, String owlPath) {
			super(baseDir, owlPath);
		}
		public void run(SparqlConnection conn, SparqlQueryClient queryClient, Logger logger) throws Exception {
			if(conn.getDataInterfaceCount() != 1) {
				throw new Exception("Error: cannot load OWL because 0 or multiple data interfaces are specified");
			}
			SparqlEndpointInterface sei = conn.getDataInterface(0);
			if(logger != null) {
				logger.info("Load OWL " + getDisplayableFilePath() + " to " + sei.getGraph());
			}
			queryClient.setSei(conn.getDataInterface(0));
			SimpleResultSet res = queryClient.uploadOwl(getFile());
			res.throwExceptionIfUnsuccessful();
			
			// not ok to use SEI anymore due to security
			//sei.uploadOwl(Files.readAllBytes(getFile().toPath()));  // OK to use SEI (instead of client) because uploading data (not model)
		}
	}

}
