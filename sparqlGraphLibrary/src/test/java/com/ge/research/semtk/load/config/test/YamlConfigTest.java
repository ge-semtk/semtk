package com.ge.research.semtk.load.config.test;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;

/**
 * Superclass for other tests.
 */
public abstract class YamlConfigTest {

	protected SparqlEndpointInterface modelSei = TestGraph.getSei(TestGraph.getDataset() + "/model");
	protected SparqlEndpointInterface dataSei = TestGraph.getSei(TestGraph.getDataset() + "/data");
	protected SparqlEndpointInterface modelFallbackSei = TestGraph.getSei(TestGraph.getDataset() + "/model/fallback");
	protected SparqlEndpointInterface dataFallbackSei = TestGraph.getSei(TestGraph.getDataset() + "/data/fallback");
	protected SparqlEndpointInterface defaultGraphSei = TestGraph.getSei(SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);
	
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
