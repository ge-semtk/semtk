/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.edc.client.test;

import static org.junit.Assert.*;

import java.net.ConnectException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.OntologyInfoClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class OntologyInfoClientTest_IT {
	
	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	
	private final String JOB_ID1 = "client_test_job_1";
	
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		SERVICE_PROTOCOL = IntegrationTestUtility.getServiceProtocol();
		SERVICE_SERVER = IntegrationTestUtility.getOntologyInfoServiceServer();
		SERVICE_PORT = IntegrationTestUtility.getOntologyInfoServicePort();
	}
	
	private OntologyInfoClient getClient() throws Exception {
		OntologyInfoClientConfig config = new OntologyInfoClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT);
		return new OntologyInfoClient(config);
	}
	
	@Test
	public void testGetOInfoFromConn() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/sampleBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		SparqlConnection conn = sgJson.getSparqlConn();
		
		OntologyInfoClient client = this.getClient();
		OntologyInfo oInfo = client.getOntologyInfo(conn);
		
		assertEquals(3, oInfo.getNumberOfClasses());
		
	}
}
