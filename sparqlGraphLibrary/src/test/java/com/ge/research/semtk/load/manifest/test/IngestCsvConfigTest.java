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
import org.junit.Test;

import java.io.File;

import com.ge.research.semtk.load.manifest.IngestCsvConfig;
import com.ge.research.semtk.load.manifest.IngestCsvConfig.ClassCsvIngestionStep;
import com.ge.research.semtk.test.TestGraph;

// TODO test variations, valid and invalid
// TODO test model graph (when implemented)
// TODO test extra data graphs (when implemented)
public class IngestCsvConfigTest {

	/**
	 * Test populating an IngestCsvConfigTest instance from a YAML file
	 */
	@Test
	public void test() throws Exception{

		final String FALLBACK_MODEL_GRAPH = TestGraph.getDataset() + "/model";  // this test does not contact triplestore - but using something unique anyway
		final String FALLBACK_DATA_GRAPH = TestGraph.getDataset() + "/data";

		IngestCsvConfig config;
		
		// this config has a datagraph + steps
		config = new IngestCsvConfig(new File("src/test/resources/manifest/ingest_csv_config_1.yaml"), FALLBACK_MODEL_GRAPH, FALLBACK_DATA_GRAPH);
		assertTrue(config.getBaseDir().matches("src(.*)test(.*)resources(.*)manifest"));
		assertEquals(config.getFallbackModelGraph(), FALLBACK_MODEL_GRAPH);
		assertEquals(config.getSteps().size(), 2);
		assertEquals(((ClassCsvIngestionStep)config.getSteps().get(0)).getClazz(), "http://animals/woodland#WOODCHUCK");
		assertEquals(((ClassCsvIngestionStep)config.getSteps().get(0)).getCsv(), "woodchucks.csv");
		assertEquals(((ClassCsvIngestionStep)config.getSteps().get(1)).getClazz(), "http://animals/woodland#HEDGEHOG");
		assertEquals(((ClassCsvIngestionStep)config.getSteps().get(1)).getCsv(), "hedgehogs.csv");
		assertEquals(config.getDatagraph(), "http://junit/animals/data");
		
		// this config has steps only (no graphs)
		config = new IngestCsvConfig(new File("src/test/resources/manifest/ingest_csv_config_2.yaml"), FALLBACK_MODEL_GRAPH, FALLBACK_DATA_GRAPH);
		assertEquals(config.getDatagraph(), null);
	}
	
}
