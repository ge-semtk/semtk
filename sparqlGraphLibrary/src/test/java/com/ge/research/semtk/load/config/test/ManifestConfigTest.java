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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.ge.research.semtk.load.config.ManifestConfig;
import com.ge.research.semtk.test.TestGraph;

public class ManifestConfigTest {

	public ManifestConfigTest() throws Exception {
		super();
	}

	@Test
	public void testGetTopLevelManifest() throws Exception{

		File tempDir;
		File manifestFile;
		
		// manifest found at top level
		tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/IngestionPackage.zip");
		manifestFile = ManifestConfig.getTopLevelManifestFile(tempDir);
		assertEquals(manifestFile.getName(), "manifest.yaml");

		// manifest found at next-to top level
		tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/IngestionPackage-ExtraDirectory.zip");
		manifestFile = ManifestConfig.getTopLevelManifestFile(tempDir);
		assertEquals(manifestFile.getName(), "manifest.yaml");

		// manifest exists at next-to top level, but reject because there are 2 directories at top level
		File dummyDir = new File(tempDir + File.separator + "dummyDir");
		dummyDir.createNewFile();
		try {
			manifestFile = ManifestConfig.getTopLevelManifestFile(tempDir);
			fail(); // should not get here
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("Top-level manifest.yaml does not exist in"));
		}

		// manifest not found
		try {
			tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/IngestionPackage-NoManifest.zip");
			manifestFile = ManifestConfig.getTopLevelManifestFile(tempDir);
			fail(); // should not get here
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("Top-level manifest.yaml does not exist in"));
		}
	}

}
