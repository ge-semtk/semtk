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
package com.ge.research.semtk.load.manifest.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ge.research.semtk.load.manifest.Manifest;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;


public class ManifestTest_IT {

	// not extending YamlConfigTest because need to match ingestion package footprint
	SparqlEndpointInterface modelFallbackSei = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/model"));
	SparqlEndpointInterface dataFallbackSei = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/data"));
	SparqlEndpointInterface dataSeiFromYaml = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/data"));  // for this package, same as footprint/fallback
	SparqlEndpointInterface defaultGraphSei = TestGraph.getSei(SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);

	public final static int NUM_EXPECTED_TRIPLES_MODEL = 1439;				// TODO verify that this is correct
	public final static int NUM_EXPECTED_TRIPLES_DATA = 80;					// TODO verify that this is correct
	public final static int NUM_NET_CHANGE_ENTITY_RESOLUTION_TRIPLES = -12;	// TODO verify that this is correct  (entity resolution results in net loss of triples)
	public final static int NUM_EXPECTED_NODEGROUPS = 31;

	public ManifestTest_IT() throws Exception {
		super();
	}

	/**
	 * Test loading a manifest via YAML, without using default graph
	 */
	@Test
	public void testLoadManifest_NoDefaultGraph() throws Exception{
		// Note: no copy to default graph, so don't have to limit to Fuseki only

		File tempDir = null;
		boolean loadToDefaultGraph;
		try {
			// get manifest from ingestion package, perform load
			tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/manifest/IngestionPackage.zip");
			Manifest manifest = new Manifest(Manifest.getTopLevelManifestFile(tempDir), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());

			// load to non-default graphs, do not copy to default graph
			reset();
			loadToDefaultGraph = false;
			manifest.setCopyToDefaultGraph(false); // sets copy-to-default-graph=false
			manifest.setPerformEntityResolution(false);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, loadToDefaultGraph, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", NUM_EXPECTED_TRIPLES_MODEL, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", NUM_EXPECTED_TRIPLES_DATA, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", 0, defaultGraphSei.getNumTriples());
			assertEquals("Number of nodegroups", NUM_EXPECTED_NODEGROUPS, IntegrationTestUtility.countItemsInStoreByCreator("junit"));

			// error if try to perform entity resolution when not using default graph
			reset();
			loadToDefaultGraph = false;
			manifest.setCopyToDefaultGraph(false);
			manifest.setPerformEntityResolution(true);
			try {
				manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, loadToDefaultGraph, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
				fail(); // should not get here
			}catch(Exception e) {
				assertTrue(e.getMessage().contains("Cannot perform entity resolution because not populating default graph"));
			}

		}finally{
			if(tempDir != null) { FileUtils.deleteDirectory(tempDir); }
			reset();
		}
	}

	/**
	 * Test loading a manifest via YAML, with data loaded to the default graph in 2 different ways:
	 * 1) via copy-to-default-graph=true in the manifest YAML
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
			tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/manifest/IngestionPackage.zip");
			Manifest manifest = new Manifest(Manifest.getTopLevelManifestFile(tempDir), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());

			// load directly (not copy) to default graph, no entity resolution
			reset();
			loadToDefaultGraph = true;
			manifest.setCopyToDefaultGraph(false);
			manifest.setPerformEntityResolution(false);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, loadToDefaultGraph, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", 0, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", 0, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", NUM_EXPECTED_TRIPLES_MODEL + NUM_EXPECTED_TRIPLES_DATA, defaultGraphSei.getNumTriples());

			// load directly (not copy) to default graph, perform entity resolution
			reset();
			loadToDefaultGraph = true;
			manifest.setCopyToDefaultGraph(false);
			manifest.setPerformEntityResolution(true);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, loadToDefaultGraph, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", 0, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", 0, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", NUM_EXPECTED_TRIPLES_MODEL + NUM_EXPECTED_TRIPLES_DATA + NUM_NET_CHANGE_ENTITY_RESOLUTION_TRIPLES, defaultGraphSei.getNumTriples());

			// copy (not load directly) to default graph, perform entity resolution
			reset();
			loadToDefaultGraph = false;
			manifest.setCopyToDefaultGraph(true);   // it's already true, but making it explicit
			manifest.setPerformEntityResolution(true);
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
