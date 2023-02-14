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

			IngestOwlConfig config = new IngestOwlConfig(Utility.getResourceAsFile(this, "/manifest/IngestionPackage/RACK-Ontology/OwlModels/import.yaml"), MODEL_FALLBACK_GRAPH);

			// test that model gets loaded to the graph provided
			modelSei.clearGraph();
			modelFallbackSei.clearGraph();
			config.load(MODEL_GRAPH, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
			assertEquals(modelSei.getNumTriples(), 1439);
			assertEquals(modelFallbackSei.getNumTriples(), 0);

			// test that model gets loaded to fallback model graph if no model graph is provided (also not specified in YAML)
			clearGraphs();
			config.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
			assertEquals(modelSei.getNumTriples(), 0);
			assertEquals(modelFallbackSei.getNumTriples(), 1439);

			// TODO test that gets loaded to YAML-specified graph

		}catch(Exception e) {
			throw e;
		}finally {
			clearGraphs();
		}
	}

}
