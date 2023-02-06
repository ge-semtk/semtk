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

import java.io.BufferedReader;
import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.services.client.UtilityClient;
import com.ge.research.semtk.test.IntegrationTestUtility;

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
		BufferedReader reader = client.execLoadIngestionPackage(new File("src/test/resources/IngestionPackage.zip"));
		StringBuffer statusBuffer = new StringBuffer();
		String s;
		while ((s = reader.readLine()) != null) {
			statusBuffer.append(s);
			System.out.println(s);
			// TODO confirm that status updates are arriving in real time
		}
		reader.close();
		assert(statusBuffer.toString().contains("Load complete"));
		// TODO when implemented, confirm that content loaded to triplestore as expected
	}


	// Test sending something other than an ingestion package, confirm get useful error message
	@Test
	public void testLoadIngestionPackage_nonZip() throws Exception {
		BufferedReader reader = client.execLoadIngestionPackage(new File("src/test/resources/animalQuery.json"));
		StringBuffer statusBuffer = new StringBuffer();
		String s;
		while ((s = reader.readLine()) != null) {
			statusBuffer.append(s);
		}
		reader.close();
		assert(statusBuffer.toString().contains("Error: This endpoint only accepts ingestion packages in zip file format"));
	}

}
