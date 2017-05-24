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
package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestConnection;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;

public class OntologyInfoTests_IT {
	
	@Test
	public void testFullLoad() throws Exception {

		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/Pet.owl");		

		// attempt the load
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn("http://research.ge.com/"));

		assertTrue("Expecting 0 enums.  found: " + oInfo.getNumberOfEnum(),              oInfo.getNumberOfEnum() == 0);
		assertTrue("Expecting 2 classes.  found: " + oInfo.getNumberOfClasses(),        oInfo.getNumberOfClasses() == 2);
		assertTrue("Expecting 5 properties.  found: " + oInfo.getNumberOfProperties(), oInfo.getNumberOfProperties() == 5);

		OntologyClass cat = oInfo.getClass("http://research.ge.com/kdl/pet#Cat");
		OntologyClass dog = oInfo.getClass("http://research.ge.com/kdl/pet#Dog");

		// dog and cat each have three properties
		assertTrue(oInfo.getInheritedProperties(dog).size() == 3);
		assertTrue(oInfo.getInheritedProperties(cat).size() == 3);
		// "name" is a property of dog and cat
		assertTrue(oInfo.getInheritedPropertyByKeyname(dog, "name") != null);
		assertTrue(oInfo.getInheritedPropertyByKeyname(cat, "name") != null);

	}
	
	@Test
	public void testFullLoadViaService() throws Exception {


		SparqlQueryClientConfig conf = new SparqlQueryClientConfig(
				IntegrationTestUtility.getServiceProtocol(), 
				IntegrationTestUtility.getSparqlQueryServiceServer(), 
				IntegrationTestUtility.getSparqlQueryServicePort(), 
				"sparqlQueryService/query",
				TestGraph.getSparqlServer(), 
				TestGraph.getSparqlServerType(), 
				TestGraph.getDataset());
			
			
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/Pet.owl");		

		// attempt the load
		OntologyInfo oInfo = new OntologyInfo(conf, TestGraph.getSparqlConn("http://research.ge.com/"));

		assertTrue("Expecting 0 enums.  found: " + oInfo.getNumberOfEnum(),              oInfo.getNumberOfEnum() == 0);
		assertTrue("Expecting 2 classes.  found: " + oInfo.getNumberOfClasses(),        oInfo.getNumberOfClasses() == 2);
		assertTrue("Expecting 5 properties.  found: " + oInfo.getNumberOfProperties(), oInfo.getNumberOfProperties() == 5);

		OntologyClass cat = oInfo.getClass("http://research.ge.com/kdl/pet#Cat");
		OntologyClass dog = oInfo.getClass("http://research.ge.com/kdl/pet#Dog");

		// dog and cat each have three properties
		assertTrue(oInfo.getInheritedProperties(dog).size() == 3);
		assertTrue(oInfo.getInheritedProperties(cat).size() == 3);
		// "name" is a property of dog and cat
		assertTrue(oInfo.getInheritedPropertyByKeyname(dog, "name") != null);
		assertTrue(oInfo.getInheritedPropertyByKeyname(cat, "name") != null);

	}

	@Test
	public void testDoubleModel() throws Exception {
		TestConnection testConn = setupDoubleModel();
		OntologyInfo oInfo = new OntologyInfo(testConn.getSparqlConn());
		OntologyClass cat = oInfo.getClass("http://research.ge.com/kdl/pet#Cat");
		OntologyClass flower = oInfo.getClass("http://research.ge.com/kdl/plant#Flower");
		assertTrue(cat != null);
		assertTrue(flower != null);
	}
	
	@Test
	public void testDoubleModelViaQueryService() throws Exception {
		SparqlQueryClientConfig config = new SparqlQueryClientConfig(
				IntegrationTestUtility.getServiceProtocol(), 
				IntegrationTestUtility.getSparqlQueryServiceServer(), 
				IntegrationTestUtility.getSparqlQueryServicePort(), 
				"sparqlQueryService/query",
				"empty", 
				"empty", 
				"empty");
		
		TestConnection testConn = setupDoubleModel();
		OntologyInfo oInfo = new OntologyInfo(config, testConn.getSparqlConn());
		OntologyClass cat = oInfo.getClass("http://research.ge.com/kdl/pet#Cat");
		OntologyClass flower = oInfo.getClass("http://research.ge.com/kdl/plant#Flower");
		assertTrue(cat != null);
		assertTrue(flower != null);
	}
	
	private TestConnection setupDoubleModel() throws Exception {
		TestConnection conn = new TestConnection(2, 1, "http://research.ge.com/kdl/");
		conn.uploadOwl(0, "src/test/resources/Pet.owl");
		conn.uploadOwl(1, "src/test/resources/Plant.owl");
		return conn;
	}
	
	@Test
	public void testFullLoadDuplicateProp() throws Exception {

		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/Pet.owl");

		// attempt the load
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn("http://research.ge.com/"));

		assertTrue("Expecting 0 enums.  found: " + oInfo.getNumberOfEnum(), oInfo.getNumberOfEnum() == 0);
		assertTrue("Expecting 2 classes.  found: " + oInfo.getNumberOfClasses(), oInfo.getNumberOfClasses() == 2);
		assertTrue("Expecting 5 properties.  found: " + oInfo.getNumberOfProperties(), oInfo.getNumberOfProperties() == 5);

		OntologyClass cat = oInfo.getClass("http://research.ge.com/kdl/pet#Cat");
		OntologyClass dog = oInfo.getClass("http://research.ge.com/kdl/pet#Dog");

		// dog and cat each have three properties
		assertTrue(oInfo.getInheritedProperties(dog).size() == 3);
		assertTrue(oInfo.getInheritedProperties(cat).size() == 3);
		// "name" is a property of dog and cat
		assertTrue(oInfo.getInheritedPropertyByKeyname(dog, "name") != null);
		assertTrue(oInfo.getInheritedPropertyByKeyname(cat, "name") != null);

	}

	@Test
	public void testVisJs() throws Exception {

		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/Plant.owl");
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn("http://research.ge.com/"));

		String res = oInfo.toVisJs().toString();
		System.out.print(res);
		// label without namespace
		assertTrue(res.contains("\"label\":\"Flower\""));
		// property val without namespace
		assertTrue(res.contains("\"name\":\"color\","));
		// property type without namespace
		assertTrue(res.contains("\"type\":\"Color\""));
	}
	


}
