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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;

import com.ge.research.semtk.load.manifest.Manifest;
import com.ge.research.semtk.load.manifest.Manifest.Step;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class ManifestTest extends YamlConfigTest {

	public ManifestTest() throws Exception {
		super();
	}

	@Test
	public void test() throws Exception{

		Manifest manifest = new Manifest(Utility.getResourceAsFile(this, "/manifest/manifest_animals.yaml"), FALLBACK_MODEL_GRAPH, FALLBACK_DATA_GRAPH);
		assertEquals(manifest.getFallbackModelGraph(), FALLBACK_MODEL_GRAPH);
		assertEquals(manifest.getFallbackDataGraph(), FALLBACK_DATA_GRAPH);

		assertEquals(manifest.getName(), "Animals");
		assertEquals(manifest.getDescription(), "Load information about animals");
		assertTrue(manifest.getCopyToDefaultGraph());
		assertTrue(manifest.getPerformEntityResolution());

		// test footprint
		assertEquals(manifest.getModelgraphsFootprint().size(), 1);
		assertEquals(manifest.getModelgraphsFootprint().get(0).toString(), "http://junit/animals/model");
		assertEquals(manifest.getDatagraphsFootprint().size(), 2);
		assertEquals(manifest.getDatagraphsFootprint().get(0).toString(), "http://junit/animals/data1");
		assertEquals(manifest.getDatagraphsFootprint().get(1).toString(), "http://junit/animals/data2");
		assertEquals(manifest.getGraphsFootprint().size(), 3);
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
		assertEquals(steps.get(6).getValue().toString(), "[http://junit/animals/domestic, http://junit/animals/wild]");

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

		// note this test does not actually perform load - but using something unique anyway
		final String FALLBACK_MODEL_GRAPH = TestGraph.getDataset() + "/model";
		final String FALLBACK_DATA_GRAPH = TestGraph.getDataset() + "/data";

		File file = new File("src/test/resources/manifest/manifest_animals_minimal.yaml");
		Manifest manifest = new Manifest(file, FALLBACK_MODEL_GRAPH, FALLBACK_DATA_GRAPH);
		assertEquals(manifest.getName(), "Animals");
	}

}
