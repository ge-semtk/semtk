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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

import org.junit.Test;

import com.ge.research.semtk.load.config.LoadOwlConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class LoadOwlConfigTest_IT extends YamlConfigTest {

	public LoadOwlConfigTest_IT() throws Exception {
		super();
	}

	/**
	 * Test populating an LoadOwlConfig instance from a YAML file
	 */
	@Test
	public void testInstantiate() throws Exception{

		LoadOwlConfig config;

		// this config has owl files only (no model graph)
		config = new LoadOwlConfig(TestGraph.getYamlAndUniquifyJunitGraphs(this, "/config/load_owl_config_1.yaml"), modelFallbackSei.getGraph());
		assertEquals(config.getDefaultModelGraph(), modelFallbackSei.getGraph());
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getFiles().get(0),"woodchuck.owl");
		assertEquals(config.getFiles().get(1),"hedgehog.owl");
		assertEquals(config.getFiles().get(2),"raccoon.owl");
		assertNull(config.getModelgraph());

		// this config has model graph as array size 1
		config = new LoadOwlConfig(TestGraph.getYamlAndUniquifyJunitGraphs(this, "/config/load_owl_config_2.yaml"), modelFallbackSei.getGraph());
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getModelgraph(), TestGraph.uniquifyJunitGraphs("http://junit/animals/model"));

		// this config has model graph as string
		config = new LoadOwlConfig(TestGraph.getYamlAndUniquifyJunitGraphs(this, "/config/load_owl_config_3.yaml"), modelFallbackSei.getGraph());
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getModelgraph(), TestGraph.uniquifyJunitGraphs("http://junit/animals/model"));

		// this config has multiple model graphs.  Legacy schema supports this (likely unused), disallowing it here
		try {
			config = new LoadOwlConfig(TestGraph.getYamlAndUniquifyJunitGraphs(this, "/config/load_owl_config_4.yaml"), modelFallbackSei.getGraph());
			fail();
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("Not currently supporting multiple entries for this node"));
		}
	}

	/**
	 * Test loading OWL via YAML file
	 */
	@Test
	public void testLoad() throws Exception{

		try {

			final int NUM_EXPECTED_TRIPLES = 1439;

			File tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/IngestionPackage.zip");
			LoadOwlConfig config = new LoadOwlConfig(
					Paths.get(tempDir.getAbsolutePath(), "RACK-Ontology","OwlModels","import.yaml").toFile(),  // this YAML file has no model
					modelFallbackSei.getGraph());

			// Case 1: if load() model graph parameter, then confirm loads there
			clearGraphs();
			config.load(modelSei.getGraph(), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);
			assertEquals(modelSei.getNumTriples(), NUM_EXPECTED_TRIPLES);
			assertEquals(modelFallbackSei.getNumTriples(), 0);

			// Case 2: if no load() model graph parameter, then confirm loads to YAML data graph if present
			clearGraphs();
			config.setModelgraph(TestGraph.uniquifyJunitGraphs("http://junit/rack001/model"));  // add model graph to YAML config
			SparqlEndpointInterface modelSeiFromYaml = TestGraph.getSei(config.getModelgraph());
			IntegrationTestUtility.clearGraph(modelSeiFromYaml);
			config.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);
			assertEquals(modelSei.getNumTriples(), 0);
			assertEquals(modelFallbackSei.getNumTriples(), 0);
			assertEquals(modelSeiFromYaml.getNumTriples(), NUM_EXPECTED_TRIPLES);

			// Case 3: if no load() model graph parameter, and no YAML data graph, then confirm loads to fallback
			clearGraphs();
			config.setModelgraph(null);  // remove model graph from YAML config
			config.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);
			assertEquals(modelSei.getNumTriples(), 0);
			assertEquals(modelFallbackSei.getNumTriples(), NUM_EXPECTED_TRIPLES);

			// test specifying allowed graphs, where graphs to be loaded are present in the allowed graphs
			config.setAllowedGraphs(new LinkedList<String>(Arrays.asList(modelSei.getGraph())));
			config.load(modelSei.getGraph(), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);

			// test specifying allowed graphs, where graphs to be loaded are NOT present in the allowed graphs
			config.setAllowedGraphs(new LinkedList<String>(Arrays.asList(modelSei.getGraph() + "-123")));
			try {
				config.load(modelSei.getGraph(), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);
				fail();
			}catch(Exception e) {
				assertTrue(e.getMessage().contains(modelSei.getGraph() + " is not in list of allowed graphs: [" + modelSei.getGraph() + "-123]"));
			}

		}catch(Exception e) {
			throw e;
		}finally {
			clearGraphs();
		}
	}

}
