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

import java.io.PrintWriter;

import org.junit.Test;

import com.ge.research.semtk.load.manifest.IngestOwlConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class IngestOwlConfigTest_IT extends YamlConfigTest {

	public IngestOwlConfigTest_IT() throws Exception {
		super();
	}

	/**
	 * Test loading OWL via YAML file
	 */
	@Test
	public void test() throws Exception{

		try {

			final int NUM_EXPECTED_TRIPLES = 1439;		// TODO verify that this is correct

			// TODO uniquifyJunitGraphName
			IngestOwlConfig configWithoutModelInYaml = new IngestOwlConfig(Utility.getResourceAsFile(this, "/manifest/IngestionPackage/RACK-Ontology/OwlModels/import.yaml"), modelFallbackSei.getGraph());
			IngestOwlConfig configWithModelInYaml = new IngestOwlConfig(Utility.getResourceAsFile(this, "/manifest/IngestionPackage/RACK-Ontology/OwlModels/import-WithModelgraph.yaml"), modelFallbackSei.getGraph());

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
