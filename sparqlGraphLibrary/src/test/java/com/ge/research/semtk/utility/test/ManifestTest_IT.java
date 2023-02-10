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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ge.research.semtk.load.utility.Manifest;
import com.ge.research.semtk.load.utility.Manifest.IngestOwlConfig;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;


public class ManifestTest_IT {

	/**
	 * Test loading a set of OWL files via YAML (e.g. import.yaml)
	 */
	@Test
	public void testIngestOwl() throws Exception{

		IngestOwlConfig config = IngestOwlConfig.fromYaml(new File("src/test/resources/IngestionPackage/RACK-Ontology/OwlModels/import.yaml"));
		String[] modelGraphs;

		// error if provide null model graphs (none are specified in YAML either)
		modelGraphs = null;
		try {
			config.ingest(modelGraphs, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
			fail(); // should not get here
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("No model graphs provided for OWL ingestion"));
		}

		// error if provide null model graphs (none are specified in YAML either)
		modelGraphs = new String[] {};
		try {
			config.ingest(modelGraphs, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
			fail(); // should not get here
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("No model graphs provided for OWL ingestion"));
		}

		// provide a valid model graph
		TestGraph.clearGraph();
		modelGraphs = new String[]{TestGraph.getDataset()};
		config.ingest(modelGraphs, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));
		assertEquals(TestGraph.getNumTriples(), 1439);
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
			Manifest manifest = Manifest.fromYaml(manifestFile);
			assertEquals(manifest.getName(), "Entity Resolution");

			// TODO this will fail until we resolve what to do about the CLI's rack001 default graphs (import.yaml does not specify model graph)
			manifest.load(tempDir.toString(), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, false, true, new PrintWriter(System.out));

			// TODO asserts

		}catch(Exception e) {
			throw e;
		}finally{
			FileUtils.deleteDirectory(tempDir);
		}
	}

}
