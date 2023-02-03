package com.ge.research.semtk.load.utility;

import java.net.URL;
import java.util.LinkedList;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.Utility;

/**
 * Class representing a manifest (YAML file with specification for loading content into triplestore)
 */
public class Manifest {

	private String name;
	private String description;
	private boolean copyToDefaultGraph = false;
	private boolean performEntityResolution = false;
	private boolean performOptimization = false;
	private LinkedList<URL> modelgraphsFootprint = new LinkedList<URL>();
	private LinkedList<URL> datagraphsFootprint = new LinkedList<URL>();
	private LinkedList<String> nodegroupsFootprint = new LinkedList<String>();
	private LinkedList<Step> steps = new LinkedList<Step>();

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
	/* Returns true when this manifest file prescribes running the triplestore optimizer */
	public boolean getPerformOptimization() {
		return performOptimization;
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
	public void setCopyToDefaultGraph(boolean b) {
		this.copyToDefaultGraph = b;
	}
	public void setPerformEntityResolution(boolean b) {
		this.performEntityResolution = b;
	}
	public void setPerformOptimization(boolean b) {
		this.performOptimization = b;
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
		conn.addModelInterface(SparqlEndpointInterface.getInstance(serverTypeString, server, "uri://DefaultGraph"));
		conn.addDataInterface(SparqlEndpointInterface.getInstance(serverTypeString, server, "uri://DefaultGraph"));
		return conn;
	}

	/**
	 * Instantiate a manifest from a YAML file
	 * @param yamlStr a YAML string
	 * @return the manifest object
	 * @throws Exception e.g. if fails validation
	 */
	public static Manifest fromYaml(String yamlStr) throws Exception {

		// validate manifest YAML against schema
		String manifestSchema = Utility.getResourceAsString(Manifest.class, "manifest_schema.json");
		Utility.validateYaml(yamlStr, manifestSchema);

		// populate the manifest

		JsonNode manifestJsonNode = (new ObjectMapper(new YAMLFactory())).readTree(yamlStr);
		String name = manifestJsonNode.get("name").asText();
		String description = manifestJsonNode.get("description") != null ? manifestJsonNode.get("description").asText() : null; // optional
		Manifest manifest = new Manifest(name, description);

		// 3 optional boolean properties
		if(manifestJsonNode.get("copy-to-default-graph") != null) { manifest.setCopyToDefaultGraph(manifestJsonNode.get("copy-to-default-graph").booleanValue()); }
		if(manifestJsonNode.get("perform-entity-resolution") != null) { manifest.setPerformEntityResolution(manifestJsonNode.get("perform-entity-resolution").booleanValue()); }
		if(manifestJsonNode.get("perform-triplestore-optimization") != null) { manifest.setPerformOptimization(manifestJsonNode.get("perform-triplestore-optimization").booleanValue()); }

		// footprint
		JsonNode footprintJsonNode = manifestJsonNode.get("footprint");
		if(footprintJsonNode != null) {
			for(JsonNode node : footprintJsonNode.get("model-graphs")){
				manifest.addModelgraphFootprint(new URL(node.asText()));
			}
			for(JsonNode node : footprintJsonNode.get("data-graphs")){
				manifest.addDatagraphFootprint(new URL(node.asText()));
			}
			for(JsonNode node : footprintJsonNode.get("nodegroups")){
				manifest.addNodegroupFootprint(new String(node.asText()));
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
	
}
