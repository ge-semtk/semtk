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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ge.research.semtk.load.utility.Manifest;
import com.ge.research.semtk.load.utility.Manifest.IngestOwlConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;


public class ManifestTest_IT {

	/**
	 * Test loading a set of OWL files via YAML (e.g. import.yaml)
	 */
	@Test
	public void testIngestOwl() throws Exception{

		IngestOwlConfig config = IngestOwlConfig.fromYaml(new File("src/test/resources/IngestionPackage/RACK-Ontology/OwlModels/import.yaml"), ManifestTest.FALLBACK_MODEL_GRAPH);

		// provide a valid model graph
		TestGraph.clearGraph();
		config.ingest(TestGraph.getDataset(), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
		assertEquals(TestGraph.getNumTriples(), 1439);

		// ------------------ test that model gets loaded to fallback model graph if no model graph is provided ----------------

		SparqlEndpointInterface seiFallback = SparqlEndpointInterface.getInstance(TestGraph.getSparqlServerType(), TestGraph.getSparqlServer(), ManifestTest.FALLBACK_MODEL_GRAPH);

		// model graphs null (none are specified in YAML either)
		seiFallback.clearGraph();
		config.ingest(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
		assertEquals(seiFallback.getNumTriples(), 1439);

		// model graphs empty (none are specified in YAML either)
		seiFallback.clearGraph();
		config.ingest(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
		assertEquals(seiFallback.getNumTriples(), 1439);
	}

	/**
	 * Test loading a manifest via YAML
	 */
	@Test
	public void testLoadManifest() throws Exception{

		File tempDir = null;
		try {
			// unzip ingestion package
			ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream("src/test/resources/IngestionPackage.zip"));
			tempDir = Utility.createTempDirectory();
			Utility.unzip(zipInputStream, tempDir);

			// get manifest
			File manifestFile = Manifest.getTopLevelManifestFile(tempDir);
			Manifest manifest = Manifest.fromYaml(manifestFile, ManifestTest.FALLBACK_MODEL_GRAPH, ManifestTest.FALLBACK_DATA_GRAPH);
			assertEquals(manifest.getName(), "Entity Resolution");

			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, false, true, new PrintWriter(System.out));

			// TODO asserts

		}catch(Exception e) {
			throw e;
		}finally{
			FileUtils.deleteDirectory(tempDir);
		}
	}

}
