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

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ge.research.semtk.load.config.ManifestConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class ManifestConfigTest_LoadTurnstile_IT {

	SparqlEndpointInterface modelFallbackSei = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/model"));
	SparqlEndpointInterface dataFallbackSei = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/data"));
	SparqlEndpointInterface dataSeiFromYaml1 = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/do-178c"));
	SparqlEndpointInterface dataSeiFromYaml2 = TestGraph.getSei(TestGraph.uniquifyJunitGraphs("http://junit/rack001/turnstiledata"));
	SparqlEndpointInterface defaultGraphSei = TestGraph.getSei(SparqlEndpointInterface.SEMTK_DEFAULT_GRAPH_NAME);

	public ManifestConfigTest_LoadTurnstile_IT() throws Exception {
		super();
	}

	/**
	 * Test loading the Turnstile ingestion package
	 */
	@Test
	public void test() throws Exception{
		// Note: no copy to default graph, so don't have to limit to Fuseki only

		File tempDir = null;
		try {
			// get manifest from ingestion package, perform load
			tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/turnstile.zip");
			ManifestConfig manifest = new ManifestConfig(ManifestConfig.getTopLevelManifestFile(tempDir), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());

			reset();
			manifest.setCopyToDefaultGraph(false);
			manifest.setPerformEntityResolution(false);
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), true, false, true, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getNodeGroupStoreRestClient(), new PrintWriter(System.out));
			assertEquals("Number of triples loaded to model fallback graph", 1986, modelFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to data graph 1", 338, dataSeiFromYaml1.getNumTriples());
			assertEquals("Number of triples loaded to data graph 2", 1386, dataSeiFromYaml2.getNumTriples());
			assertEquals("Number of nodegroups", 32, IntegrationTestUtility.countItemsInStoreByCreator("junit"));	
			assertEquals("Number of triples loaded to data fallback graph", 0, dataFallbackSei.getNumTriples());
			assertEquals("Number of triples loaded to default graph", 0, defaultGraphSei.getNumTriples());
		}finally{
			if(tempDir != null) { FileUtils.deleteDirectory(tempDir); }
		}
	}

	// clear graphs and nodegroup store
	private void reset() throws Exception {
		modelFallbackSei.clearGraph();
		dataFallbackSei.clearGraph();
		dataSeiFromYaml1.clearGraph();
		dataSeiFromYaml2.clearGraph();
		defaultGraphSei.clearGraph();
		IntegrationTestUtility.cleanupNodegroupStore("junit");
	}

}
