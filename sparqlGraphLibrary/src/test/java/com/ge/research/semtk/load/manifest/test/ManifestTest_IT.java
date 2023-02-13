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

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ge.research.semtk.load.manifest.Manifest;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;


public class ManifestTest_IT extends YamlConfigTest {

	public ManifestTest_IT() throws Exception {
		super();
	}

	/**
	 * Test loading a manifest via YAML
	 */
	@Test
	public void testLoadManifest() throws Exception{

		SparqlEndpointInterface modelSeiFallback = TestGraph.getSei(FALLBACK_MODEL_GRAPH);
		SparqlEndpointInterface dataSeiFallback = TestGraph.getSei(FALLBACK_DATA_GRAPH);

		File tempDir = null;
		try {
			
			tempDir = TestGraph.unzipIngestionPackageToTemp(this, "/manifest/IngestionPackage.zip");

			// get manifest
			File manifestFile = Manifest.getTopLevelManifestFile(tempDir);
			Manifest manifest = new Manifest(manifestFile, FALLBACK_MODEL_GRAPH, FALLBACK_DATA_GRAPH);  // should only load to model, not data
			assertEquals(manifest.getName(), "Entity Resolution");

			modelSeiFallback.clearGraph();
			dataSeiFallback.clearGraph();
			manifest.load(TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, false, true, new PrintWriter(System.out));
			assertEquals(modelSeiFallback.getNumTriples(), 1439);  	// TODO this will change when manifest.load is fully implemented
			assertEquals(dataSeiFallback.getNumTriples(), 0);  		// TODO this will change when manifest.load is fully implemented

			// clean up
			modelSeiFallback.dropGraph();
			dataSeiFallback.dropGraph();

		}catch(Exception e) {
			throw e;
		}finally{
			if(tempDir != null) {
				FileUtils.deleteDirectory(tempDir);
			}
		}
	}

}
