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
package com.ge.research.semtk.load.config.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ge.research.semtk.load.config.ManifestConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class ManifestConfigTest_IT {

	// not extending YamlConfigTest because need to match ingestion package footprint
	SparqlEndpointInterface modelFallbackSei = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/model"));
	SparqlEndpointInterface dataFallbackSei = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/data"));
	SparqlEndpointInterface dataSeiFromYaml = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/data"));  // for this package, same as footprint/fallback
	SparqlEndpointInterface defaultGraphSei = TestGraph.getSei(SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);

	public final static int NUM_EXPECTED_TRIPLES_MODEL = 1439;
	public final static int NUM_EXPECTED_TRIPLES_DATA = 80;
	public final static int NUM_NET_CHANGE_ENTITY_RESOLUTION_TRIPLES = -12;	// note: entity resolution results in net loss of triples
	public final static int NUM_EXPECTED_NODEGROUPS = 31;

	public ManifestConfigTest_IT() throws Exception {
		super();
	}

	/**
	 * Test loading a manifest via YAML, without using default graph
	 */
	@Test
	public void testLoadManifest_NoDefaultGraph() throws Exception{
		// Note: no copy to default graph, so don't have to limit to Fuseki only

		File tempDir = null;
		final boolean LOAD_TO_DEFAULT_GRAPH = false;
		try {
			// get manifest from ingestion package, perform load
			tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/IngestionPackage.zip");
			ManifestConfig manifest = new ManifestConfig(ManifestConfig.getTopLevelManifestFile(tempDir), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());

			// no data going to default graph.  clear=false
			reset();
			manifest.setCopyToGraph(null);
			manifest.setEntityResolutionGraph(null);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, LOAD_TO_DEFAULT_GRAPH, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", NUM_EXPECTED_TRIPLES_MODEL, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", NUM_EXPECTED_TRIPLES_DATA, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", 0, defaultGraphSei.getNumTriples());
			assertEquals("Number of nodegroups", NUM_EXPECTED_NODEGROUPS, IntegrationTestUtility.countItemsInStoreByCreator("junit"));

			// no data going to default graph.  clear=true
			reset();
			manifest.setCopyToGraph(null);
			manifest.setEntityResolutionGraph(null);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), true, LOAD_TO_DEFAULT_GRAPH, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", NUM_EXPECTED_TRIPLES_MODEL, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", NUM_EXPECTED_TRIPLES_DATA, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", 0, defaultGraphSei.getNumTriples());

		}finally{
			if(tempDir != null) { FileUtils.deleteDirectory(tempDir); }
			reset();
		}
	}

	/**
	 * Test loading a manifest via YAML, with data loaded to the default graph in 2 different ways:
	 * 1) via copy-to-graph (set to default graph) in the manifest YAML
	 * 2) via load() with loadToDefaultGraph=true
	 */
	@Test
	public void testLoadManifest_UsesDefaultGraph() throws Exception{

		// this ingestion package copies to the default graph - only allow to run on Fuseki
		assumeTrue("Skipping test: only use default graph on Fuseki triplestore", TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.FUSEKI_SERVER));

		File tempDir = null;
		boolean loadToDefaultGraph;
		try {
			// get manifest from ingestion package, perform load
			tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/IngestionPackage.zip");
			ManifestConfig manifest = new ManifestConfig(ManifestConfig.getTopLevelManifestFile(tempDir), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());

			// load directly (not copy) to default graph, no entity resolution
			reset();
			loadToDefaultGraph = true;
			manifest.setCopyToGraph(null);
			manifest.setEntityResolutionGraph(null);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, loadToDefaultGraph, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", 0, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", 0, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", NUM_EXPECTED_TRIPLES_MODEL + NUM_EXPECTED_TRIPLES_DATA, defaultGraphSei.getNumTriples());

			// load directly (not copy) to default graph, perform entity resolution
			reset();
			loadToDefaultGraph = true;
			manifest.setCopyToGraph(null);
			manifest.setEntityResolutionGraph(SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, loadToDefaultGraph, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", 0, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", 0, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", NUM_EXPECTED_TRIPLES_MODEL + NUM_EXPECTED_TRIPLES_DATA + NUM_NET_CHANGE_ENTITY_RESOLUTION_TRIPLES, defaultGraphSei.getNumTriples());

			// copy (not load directly) to default graph, perform entity resolution
			reset();
			loadToDefaultGraph = false;
			manifest.setCopyToGraph(SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);
			manifest.setEntityResolutionGraph(SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, loadToDefaultGraph, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", NUM_EXPECTED_TRIPLES_MODEL, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", NUM_EXPECTED_TRIPLES_DATA, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", NUM_EXPECTED_TRIPLES_MODEL + NUM_EXPECTED_TRIPLES_DATA + NUM_NET_CHANGE_ENTITY_RESOLUTION_TRIPLES, defaultGraphSei.getNumTriples());

		}finally{
			if(tempDir != null) { FileUtils.deleteDirectory(tempDir); }
			reset();
		}
	}

	// clear graphs and nodegroup store
	private void reset() throws Exception {
		modelFallbackSei.clearGraph();
		dataFallbackSei.clearGraph();
		dataSeiFromYaml.clearGraph();
		defaultGraphSei.clearGraph();
		IntegrationTestUtility.cleanupNodegroupStore("junit");
	}

}
