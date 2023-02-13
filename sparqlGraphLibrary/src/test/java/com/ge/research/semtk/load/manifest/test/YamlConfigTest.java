package com.ge.research.semtk.load.manifest.test;

import com.ge.research.semtk.test.TestGraph;

/**
 * Superclass for other tests.
 */
public abstract class YamlConfigTest {

	public final String MODEL_GRAPH;
	public final String DATA_GRAPH;
	public final String FALLBACK_MODEL_GRAPH;
	public final String FALLBACK_DATA_GRAPH;
	
	public YamlConfigTest() throws Exception{
		MODEL_GRAPH = TestGraph.getDataset() + "/model";
		DATA_GRAPH = TestGraph.getDataset() + "/data";
		FALLBACK_MODEL_GRAPH = TestGraph.getDataset() + "/model/fallback";
		FALLBACK_DATA_GRAPH = TestGraph.getDataset() + "/data/fallback";
	}

}
