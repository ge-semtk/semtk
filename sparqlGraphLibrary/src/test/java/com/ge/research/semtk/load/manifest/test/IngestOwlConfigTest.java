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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.ge.research.semtk.load.manifest.IngestOwlConfig;
import com.ge.research.semtk.test.TestGraph;

public class IngestOwlConfigTest {

	/**
	 * Test populating an IngestOwlConfig instance from a YAML file
	 */
	@Test
	public void test() throws Exception{

		final String FALLBACK_MODEL_GRAPH = TestGraph.getDataset() + "/model";	// this test does not contact triplestore - but using something unique anyway

		IngestOwlConfig config;

		// this config has owl files only (no model graph)
		config = IngestOwlConfig.fromYaml(new File("src/test/resources/manifest/ingest_owl_config_1.yaml"), FALLBACK_MODEL_GRAPH);
		assertTrue(config.getBaseDir().matches("src(.*)test(.*)resources(.*)manifest"));
		assertEquals(config.getFallbackModelGraph(), FALLBACK_MODEL_GRAPH);
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getFiles().get(0),"woodchuck.owl");
		assertEquals(config.getFiles().get(1),"hedgehog.owl");
		assertEquals(config.getFiles().get(2),"raccoon.owl");
		assertNull(config.getModelgraph());

		// this config has model graph as array size 1
		config = IngestOwlConfig.fromYaml(new File("src/test/resources/manifest/ingest_owl_config_2.yaml"), FALLBACK_MODEL_GRAPH);
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getModelgraph(),"http://junit/animals/model");

		// this config has model graph as string
		config = IngestOwlConfig.fromYaml(new File("src/test/resources/manifest/ingest_owl_config_3.yaml"), FALLBACK_MODEL_GRAPH);
		assertEquals(config.getFiles().size(), 3);
		assertEquals(config.getModelgraph(),"http://junit/animals/model");

		// this config has multiple model graphs.  Legacy schema supports this (likely unused), disallowing it here
		try {
			config = IngestOwlConfig.fromYaml(new File("src/test/resources/manifest/ingest_owl_config_4.yaml"), FALLBACK_MODEL_GRAPH);
			fail();
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("Not currently supporting multiple model graphs"));
		}
	}
	
}
