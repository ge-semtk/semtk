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
package com.ge.research.semtk.utility.test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;

import org.apache.commons.math3.util.Pair;

import com.ge.research.semtk.load.utility.Manifest;
import com.ge.research.semtk.load.utility.Manifest.IngestOwlConfig;
import com.ge.research.semtk.load.utility.Manifest.Step;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.test.TestGraph;

public class ManifestTest {

		public final static String FALLBACK_MODEL_GRAPH = "http://junit/ManifestTest/fallback/model";
		public final static String FALLBACK_DATA_GRAPH = "http://junit/ManifestTest/fallback/data";

		@Test
		public void test() throws Exception{
			Manifest manifest = Manifest.fromYaml(new File("src/test/resources/manifest_animals.yaml"), FALLBACK_MODEL_GRAPH, FALLBACK_DATA_GRAPH);
			assertTrue(manifest.getBaseDir().matches("src(.*)test(.*)resources"));
			assertEquals(manifest.getFallbackModelGraph(),ManifestTest.FALLBACK_MODEL_GRAPH);
			assertEquals(manifest.getFallbackDataGraph(),ManifestTest.FALLBACK_DATA_GRAPH);

			assertEquals(manifest.getName(), "Animals");
			assertEquals(manifest.getDescription(), "Load information about animals");
			assertTrue(manifest.getCopyToDefaultGraph());
			assertTrue(manifest.getPerformEntityResolution());

			// test footprint
			assertEquals(manifest.getModelgraphsFootprint().size(), 1);
			assertEquals(manifest.getModelgraphsFootprint().get(0), new URL("http://junit/animals/model"));
			assertEquals(manifest.getDatagraphsFootprint().size(), 2);
			assertEquals(manifest.getDatagraphsFootprint().get(0), new URL("http://junit/animals/data1"));
			assertEquals(manifest.getDatagraphsFootprint().get(1), new URL("http://junit/animals/data2"));
			assertEquals(manifest.getNodegroupsFootprint().size(), 1);
			assertEquals(manifest.getNodegroupsFootprint().get(0), "animals/nodegroups");

			// test steps
			LinkedList<Step> steps = manifest.getSteps();
			assertEquals(steps.size(), 7);
			assertEquals(steps.get(0).getType(), Manifest.StepType.MODEL );
			assertEquals(steps.get(0).getValue(), "animals/import.yaml" );
			assertEquals(steps.get(1).getType(), Manifest.StepType.MANIFEST );
			assertEquals(steps.get(1).getValue(), "animals.yaml" );
			assertEquals(steps.get(2).getType(), Manifest.StepType.NODEGROUPS );
			assertEquals(steps.get(2).getValue(), "animals/nodegroups" );
			assertEquals(steps.get(3).getType(), Manifest.StepType.DATA );
			assertEquals(steps.get(3).getValue(), "animals/mammals/import.yaml" );
			assertEquals(steps.get(6).getType(), Manifest.StepType.COPYGRAPH );
			assertEquals(steps.get(6).getValue(), new Pair<URL, URL>(new URL("http://junit/animals/domestic"), new URL("http://junit/animals/wild")));

			// test connections
			final String SERVER = TestGraph.getSparqlServer();
			final String SERVER_TYPE = TestGraph.getSparqlServerType();
			// test regular connection
			SparqlConnection conn = manifest.getConnection(SERVER, SERVER_TYPE);
			assertEquals(conn.getName(), "Animals");
			assertEquals(conn.getModelInterfaces().size(), 1);
			assertEquals(conn.getModelInterface(0).getGraph(), "http://junit/animals/model");
			assertEquals(conn.getModelInterface(0).getServerAndPort(), SERVER);
			assertEquals(conn.getModelInterface(0).getServerType(), SERVER_TYPE);
			assertEquals(conn.getDataInterfaces().size(), 2);
			assertEquals(conn.getDataInterface(0).getGraph(), "http://junit/animals/data1");
			assertEquals(conn.getDataInterface(0).getServerAndPort(), SERVER);
			assertEquals(conn.getDataInterface(0).getServerType(), SERVER_TYPE);
			assertEquals(conn.getDataInterface(1).getGraph(), "http://junit/animals/data2");
			assertEquals(conn.getDataInterface(1).getServerAndPort(), SERVER);
			assertEquals(conn.getDataInterface(1).getServerType(), SERVER_TYPE);
			// test default connection
			conn = manifest.getDefaultGraphConnection(SERVER, SERVER_TYPE);
			assertEquals(conn.getName(), "Default Graph");
			assertEquals(conn.getModelInterfaces().size(), 1);
			assertTrue(conn.getModelInterface(0).isDefaultGraph());
			assertEquals(conn.getModelInterface(0).getGraph(), "urn:x-arq:DefaultGraph");
			assertEquals(conn.getModelInterface(0).getServerAndPort(), SERVER);
			assertEquals(conn.getModelInterface(0).getServerType(), SERVER_TYPE);
			assertEquals(conn.getDataInterfaces().size(), 1);
			assertTrue(conn.getDataInterface(0).isDefaultGraph());
			assertEquals(conn.getDataInterface(0).getGraph(), "urn:x-arq:DefaultGraph");
			assertEquals(conn.getDataInterface(0).getServerAndPort(), SERVER);
			assertEquals(conn.getDataInterface(0).getServerType(), SERVER_TYPE);
		}

		/**
		 * Test that bare bones manifest does not error
		 * Test signature with file parameter
		 */
		@Test
		public void testMinimalManifest_andFromFile() throws Exception{
			File file = new File("src/test/resources/manifest_animals_minimal.yaml");
			Manifest manifest = Manifest.fromYaml(file, FALLBACK_MODEL_GRAPH, FALLBACK_DATA_GRAPH);
			assertEquals(manifest.getName(), "Animals");
		}

		/**
		 * Test populating an IngestOwlConfig instance from a YAML file
		 */
		@Test
		public void testIngestOwlConfig() throws Exception{

			IngestOwlConfig config;

			// this config has owl files only (no model graph)
			config = IngestOwlConfig.fromYaml(new File("src/test/resources/ingest_owl_config_1.yaml"), ManifestTest.FALLBACK_MODEL_GRAPH);
			assertTrue(config.getBaseDir().matches("src(.*)test(.*)resources"));
			assertEquals(config.getFallbackModelGraph(),ManifestTest.FALLBACK_MODEL_GRAPH);
			assertEquals(config.getFiles().size(), 3);
			assertEquals(config.getFiles().get(0),"woodchuck.owl");
			assertEquals(config.getFiles().get(1),"hedgehog.owl");
			assertEquals(config.getFiles().get(2),"raccoon.owl");
			assertNull(config.getModelgraph());

			// this config has model graph as array size 1
			config = IngestOwlConfig.fromYaml(new File("src/test/resources/ingest_owl_config_2.yaml"), ManifestTest.FALLBACK_MODEL_GRAPH);
			assertEquals(config.getFiles().size(), 3);
			assertEquals(config.getModelgraph(),"http://junit/animals/model");

			// this config has model graph as string
			config = IngestOwlConfig.fromYaml(new File("src/test/resources/ingest_owl_config_3.yaml"), ManifestTest.FALLBACK_MODEL_GRAPH);
			assertEquals(config.getFiles().size(), 3);
			assertEquals(config.getModelgraph(),"http://junit/animals/model");

			// this config has multiple model graphs.  Legacy schema supports this (likely unused), disallowing it here
			try {
				config = IngestOwlConfig.fromYaml(new File("src/test/resources/ingest_owl_config_4.yaml"), ManifestTest.FALLBACK_MODEL_GRAPH);
				fail();
			}catch(Exception e) {
				assertTrue(e.getMessage().contains("Not currently supporting multiple model graphs"));
			}
		}

}
