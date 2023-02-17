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

import java.io.PrintWriter;

import org.junit.Test;

import com.ge.research.semtk.load.config.LoadOwlConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

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
		assertEquals(config.getFallbackModelGraph(), modelFallbackSei.getGraph());
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getFiles().get(0),"woodchuck.owl");
		assertEquals(config.getFiles().get(1),"hedgehog.owl");
		assertEquals(config.getFiles().get(2),"raccoon.owl");
		assertNull(config.getModelgraph());

		// this config has model graph as array size 1
		config = new LoadOwlConfig(TestGraph.getYamlAndUniquifyJunitGraphs(this, "/config/load_owl_config_2.yaml"), modelFallbackSei.getGraph());
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getModelgraph(),"http://junit/animals/model");

		// this config has model graph as string
		config = new LoadOwlConfig(TestGraph.getYamlAndUniquifyJunitGraphs(this, "/config/load_owl_config_3.yaml"), modelFallbackSei.getGraph());
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getModelgraph(),"http://junit/animals/model");

		// this config has multiple model graphs.  Legacy schema supports this (likely unused), disallowing it here
		try {
			config = new LoadOwlConfig(TestGraph.getYamlAndUniquifyJunitGraphs(this, "/config/load_owl_config_4.yaml"), modelFallbackSei.getGraph());
			fail();
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("Not currently supporting multiple entries for this node: [\"http://junit/animals/model\",\"http://junit/animals/model2\"]"));
		}
	}

	/**
	 * Test loading OWL via YAML file
	 */
	@Test
	public void testLoad() throws Exception{

		try {

			final int NUM_EXPECTED_TRIPLES = 1439;

			// TODO uniquifyJunitGraphName
			LoadOwlConfig configWithoutModelInYaml = new LoadOwlConfig(Utility.getResourceAsTempFile(this, "/config/IngestionPackage/RACK-Ontology/OwlModels/import.yaml"), modelFallbackSei.getGraph());
			LoadOwlConfig configWithModelInYaml = new LoadOwlConfig(Utility.getResourceAsTempFile(this, "/config/IngestionPackage/RACK-Ontology/OwlModels/import-withModelgraph.yaml"), modelFallbackSei.getGraph());

			// Case 1: if load() model graph parameter, then confirm loads there
			clearGraphs();
			configWithoutModelInYaml.load(modelSei.getGraph(), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
			assertEquals(modelSei.getNumTriples(), NUM_EXPECTED_TRIPLES);
			assertEquals(modelFallbackSei.getNumTriples(), 0);

			// Case 2: if no load() model graph parameter, then confirm loads to YAML data graph if present
			clearGraphs();
			SparqlEndpointInterface modelSeiFromYaml = TestGraph.getSei(configWithModelInYaml.getModelgraph());  // from the YAML, e.g. http://junit/rack001/model
			modelSeiFromYaml.clearGraph();
			configWithModelInYaml.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
			assertEquals(modelSei.getNumTriples(), 0);
			assertEquals(modelFallbackSei.getNumTriples(), 0);
			assertEquals(modelSeiFromYaml.getNumTriples(), NUM_EXPECTED_TRIPLES);

			// Case 3: if no load() model graph parameter, and no YAML data graph, then confirm loads to fallback
			clearGraphs();
			configWithoutModelInYaml.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
			assertEquals(modelSei.getNumTriples(), 0);
			assertEquals(modelFallbackSei.getNumTriples(), NUM_EXPECTED_TRIPLES);

		}catch(Exception e) {
			throw e;
		}finally {
			clearGraphs();
		}
	}

}
