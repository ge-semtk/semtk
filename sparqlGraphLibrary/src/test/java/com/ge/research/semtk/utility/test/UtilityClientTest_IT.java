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

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.services.client.UtilityClient;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.Utility;

public class UtilityClientTest_IT {

	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	private static UtilityClient client = null;

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		SERVICE_PROTOCOL = IntegrationTestUtility.get("protocol");
		SERVICE_SERVER = IntegrationTestUtility.get("utilityservice.server");
		SERVICE_PORT = IntegrationTestUtility.getInt("utilityservice.port");
		client = new UtilityClient(new RestClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, "fake"));  
	}

	@Test
	public void testLoadIngestionPackage() throws Exception {
		String response = Utility.readToString(client.execLoadIngestionPackage(new File("src/test/resources/IngestionPackage.zip")));
		// check the response stream
		assert(response.contains("Loading 'Entity Resolution'..."));
		assert(response.matches("(.*)Load manifest (.*)manifests(.*)rack.yaml(.*)"));
		assert(response.contains("Loading 'RACK ontology'..."));
		assert(response.matches("(.*)Load model (.*)manifests(.*)RACK-Ontology(.*)OwlModels(.*)import.yaml(.*)"));
		assert(response.matches("(.*)Load file (.*)manifests(.*)RACK-Ontology(.*)OwlModels(.*)AGENTS.owl(.*)"));
		assert(response.matches("(.*)Load file (.*)manifests(.*)RACK-Ontology(.*)OwlModels(.*)ANALYSIS.owl(.*)"));
		assert(response.matches("(.*)Load nodegroups (.*)manifests(.*)nodegroups(.*)queries(.*)"));
		assert(response.matches("(.*)Load data (.*)TestData(.*)Package-1(.*)import.yaml(.*)"));
		assert(response.matches("(.*)Load data (.*)TestData(.*)Package-2(.*)import.yaml(.*)"));
		assert(response.matches("(.*)Load data (.*)TestData(.*)Package-3(.*)import.yaml(.*)"));
		assert(response.matches("(.*)Load data (.*)TestData(.*)Resolutions-1(.*)import.yaml(.*)"));
		assert(response.matches("(.*)Load data (.*)TestData(.*)Resolutions-1(.*)import.yaml(.*)"));
		assert(response.contains("Load complete"));
	}

	// Test error conditions
	@Test
	public void testLoadIngestionPackage_errorConditions() throws Exception {

		String response;

		// not a zip file
		response = Utility.readToString(client.execLoadIngestionPackage(new File("src/test/resources/animalQuery.json")));
		assert(response.contains("Error: This endpoint only accepts ingestion packages in zip file format"));

		// contains no top-level manifest.yaml
		response = Utility.readToString(client.execLoadIngestionPackage(new File("src/test/resources/IngestionPackageNoManifest.zip")));
		assert(response.contains("Error: Cannot find a top-level manifest in IngestionPackageNoManifest.zip"));
	}

}
