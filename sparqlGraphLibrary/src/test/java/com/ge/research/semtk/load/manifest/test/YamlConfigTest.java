package com.ge.research.semtk.load.manifest.test;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;

/**
 * Superclass for other tests.
 */
public abstract class YamlConfigTest {

	protected final String MODEL_GRAPH = TestGraph.getDataset() + "/model";
	protected final String DATA_GRAPH = TestGraph.getDataset() + "/data";
	protected final String FALLBACK_MODEL_GRAPH = TestGraph.getDataset() + "/model/fallback";
	protected final String FALLBACK_DATA_GRAPH  = TestGraph.getDataset() + "/data/fallback";

	protected SparqlEndpointInterface modelSei = TestGraph.getSei(MODEL_GRAPH);
	protected SparqlEndpointInterface dataSei = TestGraph.getSei(DATA_GRAPH);
	protected SparqlEndpointInterface modelSeiFallback = TestGraph.getSei(FALLBACK_MODEL_GRAPH);
	protected SparqlEndpointInterface dataSeiFallback = TestGraph.getSei(FALLBACK_DATA_GRAPH);
	
	public YamlConfigTest() throws Exception{
	}

}
