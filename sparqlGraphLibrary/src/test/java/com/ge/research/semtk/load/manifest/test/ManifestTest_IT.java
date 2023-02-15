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

	final int NUM_EXPECTED_TRIPLES_MODEL = 1439;		// TODO verify that this is correct
	final int NUM_EXPECTED_TRIPLES_DATA = 80;			// TODO verify that this is correct
	final int NUM_EXPECTED_NODEGROUPS = 31;

	public ManifestTest_IT() throws Exception {
		super();
	}

	/**
	 * Test loading a manifest via YAML, with copy-to-default-graph=false
	 */
	@Test
	public void testLoadManifest_NoDefaultGraph() throws Exception{
		// Note: no copy to default graph, so don't have to limit to Fuseki only

		File tempDir = null;
		try {
			// get manifest from ingestion package, perform load
			tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/manifest/IngestionPackage.zip");
			Manifest manifest = new Manifest(Manifest.getTopLevelManifestFile(tempDir), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());  // should only load to model, not data
			manifest.setCopyToDefaultGraph(false); // copy-to-default-graph=false

			reset();
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, false, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", NUM_EXPECTED_TRIPLES_MODEL, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", NUM_EXPECTED_TRIPLES_DATA, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", 0, defaultGraphSei.getNumTriples());
			assertEquals("Number of nodegroups", NUM_EXPECTED_NODEGROUPS, IntegrationTestUtility.countItemsInStoreByCreator("junit"));

		}finally{
			if(tempDir != null) { FileUtils.deleteDirectory(tempDir); }
			reset();
		}
	}

	/**
	 * Test loading a manifest via YAML, with copy-to-default-graph=true
	 */
	@Test
	public void testLoadManifest() throws Exception{

		// this ingestion package copies to the default graph - only allow to run on Fuseki
		assumeTrue("Skipping test: only use default graph on Fuseki triplestore", TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.FUSEKI_SERVER));

		File tempDir = null;
		try {
			// get manifest from ingestion package, perform load
			tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/manifest/IngestionPackage.zip");
			Manifest manifest = new Manifest(Manifest.getTopLevelManifestFile(tempDir), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());  // should only load to model, not data

			reset();
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, false, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model graph", NUM_EXPECTED_TRIPLES_MODEL, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph", NUM_EXPECTED_TRIPLES_DATA, dataSeiFromYaml.getNumTriples());
			assertEquals("Number of triples loaded to default graph", NUM_EXPECTED_TRIPLES_MODEL + NUM_EXPECTED_TRIPLES_DATA, defaultGraphSei.getNumTriples());
			assertEquals("Number of nodegroups", NUM_EXPECTED_NODEGROUPS, IntegrationTestUtility.countItemsInStoreByCreator("junit"));

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
