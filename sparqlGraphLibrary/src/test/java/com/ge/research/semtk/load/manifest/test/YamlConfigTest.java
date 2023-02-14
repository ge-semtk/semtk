package com.ge.research.semtk.load.manifest.test;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;

/**
 * Superclass for other tests.
 */
public abstract class YamlConfigTest {

	protected final String MODEL_GRAPH = TestGraph.getDataset() + "/model";
	protected final String DATA_GRAPH = TestGraph.getDataset() + "/data";
	protected final String MODEL_FALLBACK_GRAPH = TestGraph.getDataset() + "/model/fallback";
	protected final String DATA_FALLBACK_GRAPH  = TestGraph.getDataset() + "/data/fallback";
	protected final String DEFAULT_GRAPH = SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME;

	protected SparqlEndpointInterface modelSei = TestGraph.getSei(MODEL_GRAPH);
	protected SparqlEndpointInterface dataSei = TestGraph.getSei(DATA_GRAPH);
	protected SparqlEndpointInterface modelFallbackSei = TestGraph.getSei(MODEL_FALLBACK_GRAPH);
	protected SparqlEndpointInterface dataFallbackSei = TestGraph.getSei(DATA_FALLBACK_GRAPH);
	protected SparqlEndpointInterface defaultGraphSei = TestGraph.getSei(DEFAULT_GRAPH);
	
	public YamlConfigTest() throws Exception{
	}

	protected void clearGraphs() throws Exception{
		modelSei.clearGraph();
		dataSei.clearGraph();
		modelFallbackSei.clearGraph();
		dataFallbackSei.clearGraph();
		// not clearing default graph - can do this in individual tests if appropriate (e.g. using Fuseki)
	}

}
