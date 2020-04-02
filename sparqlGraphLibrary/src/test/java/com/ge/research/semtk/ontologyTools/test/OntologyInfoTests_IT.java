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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestConnection;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;

public class OntologyInfoTests_IT {
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		// upload the "http://semtk.junit/*  graphs
		TestGraph.syncOwlToItsGraph("src/test/resources/owl_import_Imported.owl");
		TestGraph.syncOwlToItsGraph("src/test/resources/owl_import_Leaf.owl");
		TestGraph.syncOwlToItsGraph("src/test/resources/owl_import_LeafRange.owl");
		
	}

	
	@Test
	public void testFullLoad() throws Exception {

		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/Pet.owl");		

		// attempt the load
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());

		assertTrue("Expecting 0 enums.  found: " + oInfo.getNumberOfEnum(),              oInfo.getNumberOfEnum() == 0);
		assertTrue("Expecting 2 classes.  found: " + oInfo.getNumberOfClasses(),        oInfo.getNumberOfClasses() == 3);
		assertTrue("Expecting 6 properties.  found: " + oInfo.getNumberOfProperties(), oInfo.getNumberOfProperties() == 6);

		OntologyClass cat = oInfo.getClass("http://research.ge.com/kdl/pet#Cat");
		OntologyClass dog = oInfo.getClass("http://research.ge.com/kdl/pet#Dog");

		// dog and cat each have four properties
		assertTrue(oInfo.getInheritedProperties(dog).size() == 4);
		assertTrue(oInfo.getInheritedProperties(cat).size() == 4);
		// "name" is a property of dog and cat
		assertTrue(oInfo.getInheritedPropertyByKeyname(dog, "name") != null);
		assertTrue(oInfo.getInheritedPropertyByKeyname(cat, "name") != null);

	}
	
	@Test
	public void testFullLoadAnnotated() throws Exception {

		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/annotationBattery.owl");		

		// attempt the load
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());

		// count things
		assertTrue("Expecting 1 enums.  found: " + oInfo.getNumberOfEnum(),              oInfo.getNumberOfEnum() == 1);
		assertTrue("Expecting 4 classes.  found: " + oInfo.getNumberOfClasses(),        oInfo.getNumberOfClasses() == 4);
		assertTrue("Expecting 6 properties.  found: " + oInfo.getNumberOfProperties(), oInfo.getNumberOfProperties() == 6);

		// look for a random annotation
		assertTrue(oInfo.getClass("http://kdl.ge.com/batterydemo#Battery").getAnnotationLabels().get(0).equals("duracell"));

	}
	
	@Test
	public void testEnumerations() throws Exception {

		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/annotationBattery.owl");		
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());
		
		assertEquals(oInfo.getNumberOfEnum(), 1);
		assertTrue(oInfo.classIsEnumeration("http://kdl.ge.com/batterydemo#Color"));
		assertEquals(oInfo.getEnumerationStrings("http://kdl.ge.com/batterydemo#Color").size(),3);
		assertTrue(oInfo.getEnumerationStrings("http://kdl.ge.com/batterydemo#Color").contains("http://kdl.ge.com/batterydemo#blue"));
		assertFalse(oInfo.getEnumerationStrings("http://kdl.ge.com/batterydemo#Color").contains("http://kdl.ge.com/batterydemo#ochre"));

		// invalid enumerations
		assertFalse(oInfo.classIsEnumeration("http://kdl.ge.com/batterydemo#Colorful"));
		assertNull(oInfo.getEnumerationStrings("http://kdl.ge.com/batterydemo#Colorful"));
	
	}
	
	@Test
	public void testOwlRoundTrip() throws Exception {
		// load owl from file to triplestore and into oInfo1, 
		// generate owl, 
		// load THAT through triplestore into oInfo2
		// and compare results
		
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/annotationBattery.owl");		

		// load from graph
		OntologyInfo oInfo1 = new OntologyInfo(TestGraph.getSparqlConn());
		
		// generate owl and upload it
		String owl = oInfo1.generateRdfOWL("http://kdl.ge.com/batterydemo");
		TestGraph.clearGraph();
		TestGraph.uploadOwlString(owl);		
		
		// load new version from graph
		OntologyInfo oInfo2 = new OntologyInfo(TestGraph.getSparqlConn());
		
		// count things
		assertEquals(oInfo1.getNumberOfEnum(),       oInfo2.getNumberOfEnum());
		assertEquals(oInfo1.getNumberOfClasses(),    oInfo2.getNumberOfClasses());
		assertEquals(oInfo1.getNumberOfProperties(), oInfo2.getNumberOfProperties());

		// look for a random annotation
		assertTrue(oInfo1.getClass("http://kdl.ge.com/batterydemo#Battery").getAnnotationLabels().get(0).equals("duracell"));
		assertTrue(oInfo2.getClass("http://kdl.ge.com/batterydemo#Battery").getAnnotationLabels().get(0).equals("duracell"));
		
		String labels1 = oInfo1.getClass("http://kdl.ge.com/batterydemo#Battery").getProperty("http://kdl.ge.com/batterydemo#id").getAnnotationLabelsString();
		String labels2 = oInfo2.getClass("http://kdl.ge.com/batterydemo#Battery").getProperty("http://kdl.ge.com/batterydemo#id").getAnnotationLabelsString();
		assertEquals(labels1, labels2);
		
		String comments1 = oInfo1.getClass("http://kdl.ge.com/batterydemo#Battery").getProperty("http://kdl.ge.com/batterydemo#id").getAnnotationCommentsString();
		String comments2 = oInfo2.getClass("http://kdl.ge.com/batterydemo#Battery").getProperty("http://kdl.ge.com/batterydemo#id").getAnnotationCommentsString();
		assertEquals(comments1, comments2);

		System.out.println("oInfo1.toJson()\n" + oInfo1.toJson());
	}
	
	@Test
	public void testFullLoadViaService() throws Exception {


		SparqlQueryClientConfig conf = new SparqlQueryClientConfig(
				IntegrationTestUtility.get("protocol"), 
				IntegrationTestUtility.get("sparqlqueryservice.server"), 
				IntegrationTestUtility.getInt("sparqlqueryservice.port"), 
				"sparqlQueryService/query",
				TestGraph.getSparqlServer(), 
				TestGraph.getSparqlServerType(), 
				TestGraph.getDataset());
			
			
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/Pet.owl");		

		// attempt the load
		OntologyInfo oInfo = new OntologyInfo(conf, TestGraph.getSparqlConn());

		assertTrue("Expecting 0 enums.  found: " + oInfo.getNumberOfEnum(),              oInfo.getNumberOfEnum() == 0);
		assertTrue("Expecting 2 classes.  found: " + oInfo.getNumberOfClasses(),        oInfo.getNumberOfClasses() == 3);
		assertTrue("Expecting 6 properties.  found: " + oInfo.getNumberOfProperties(), oInfo.getNumberOfProperties() == 6);

		OntologyClass cat = oInfo.getClass("http://research.ge.com/kdl/pet#Cat");
		OntologyClass dog = oInfo.getClass("http://research.ge.com/kdl/pet#Dog");

		// dog and cat each have four properties
		assertTrue(oInfo.getInheritedProperties(dog).size() == 4);
		assertTrue(oInfo.getInheritedProperties(cat).size() == 4);
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
				IntegrationTestUtility.get("protocol"), 
				IntegrationTestUtility.get("sparqlqueryservice.server"), 
				IntegrationTestUtility.getInt("sparqlqueryservice.port"), 
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
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());

		assertTrue("Expecting 0 enums.  found: " + oInfo.getNumberOfEnum(), oInfo.getNumberOfEnum() == 0);
		assertTrue("Expecting 2 classes.  found: " + oInfo.getNumberOfClasses(), oInfo.getNumberOfClasses() == 3);
		assertTrue("Expecting 6 properties.  found: " + oInfo.getNumberOfProperties(), oInfo.getNumberOfProperties() == 6);

		OntologyClass cat = oInfo.getClass("http://research.ge.com/kdl/pet#Cat");
		OntologyClass dog = oInfo.getClass("http://research.ge.com/kdl/pet#Dog");

		// dog and cat each have four properties
		assertTrue(oInfo.getInheritedProperties(dog).size() == 4);
		assertTrue(oInfo.getInheritedProperties(cat).size() == 4);
		// "name" is a property of dog and cat
		assertTrue(oInfo.getInheritedPropertyByKeyname(dog, "name") != null);
		assertTrue(oInfo.getInheritedPropertyByKeyname(cat, "name") != null);

	}

	@Test
	/**
	 * Superclass is not in the ontology
	 * @throws Exception
	 */
	public void testMissingImportSuperClass() throws Exception {

		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/owl_import_Leaf.owl");		

		// load should fail
		try {
			OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());
			fail("Missing exception for unknown superclass");
		} catch (Exception e) {
			assertTrue("Unknown superclass exception doesn't contain name of missing class:\n" + e.getMessage(),
					e.getMessage().contains("#Imported"));
		}
	}
	
	@Test
	/**
	 * Property range is not in the ontology
	 */
	public void testMissingRangeClass() throws Exception {

		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/owl_import_LeafRange.owl");		

		// load should succeed
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());
		
		OntologyClass oClass = oInfo.getClass("http://semtk.junit/leafrange#LeafRange");
		OntologyProperty prop = oClass.getProperties().get(0);
		OntologyRange oRange = prop.getRange();
		
		OntologyClass errClass = oInfo.getClass(oRange.getFullName());
		assertTrue("Non-existent range class 'Imported' did not come back null", errClass == null);
	}
	
	@Test
	/**
	 * Owl import from the leaf graph
	 */
	public void testOwlImport() throws Exception {

		SparqlEndpointInterface sei = TestGraph.getSei();
		sei.setGraph("http://semtk.junit/leaf");

		// load should succeed
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(sei, "http://semtk.junit", true);
		
		OntologyClass oClass = oInfo.getClass("http://semtk.junit/imported#Imported");
		
		assertTrue("http://semtk.junit/imported#Imported was not imported by owl import", oClass != null);
		
		ArrayList<String> loadWarnings = oInfo.getLoadWarnings();
		ArrayList<String> importedGraphs = oInfo.getImportedGraphs();
		
		assertTrue("http://semtk.junit/imported#Imported is not in importedGraphs list", importedGraphs.contains("http://semtk.junit/imported"));
		assertEquals("Graph was either not imported or double imported", 4, importedGraphs.size());
		assertTrue("No warnings were found for empty default graphs such as sadlbasemodel", loadWarnings.size() > 0);
	}
	
	@Test
	public void testOwlImportRoundTrip() throws Exception {
		// clear data
		TestGraph.clearGraph();
		
		// set up custom TestGraph sparql connection
		SparqlConnection conn = TestGraph.getSparqlAuthConn();
		conn.setOwlImportsEnabled(true);
		SparqlEndpointInterface sei = TestGraph.getSei();
		sei.setGraph("http://semtk.junit/leaf");
		conn.clearModelInterfaces();
		conn.addModelInterface(sei);
		
		// put connection into SGJson instead of using normal TestGraph override
		// because we need to use the graph above
		// and ingesting an owl model with imports into the wrong graph doesn't work because the name of the graph is the subject of the import triple
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/owl_import_leaf.json");
		sgjson.setSparqlConn(conn);
		OntologyInfo oInfo = new OntologyInfo(conn);
		
		// load the data
		Dataset ds = new CSVDataset("src/test/resources/owl_import_leaf.csv", false);
		DataLoader dl = new DataLoader(sgjson, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		
		// run query
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/owl_import_leaf.json");
		sgJson.setSparqlConn(conn);
		String query = sgJson.getNodeGroup(oInfo).generateSparqlSelect();
		Table table = TestGraph.execTableSelect(query);
	
		assertEquals("wrong number of rows returned by:\n" + query, 5, table.getNumRows());
		
		// now try using superclass from import in the query (makes sure we got the superclass/subclass from an owl import)
		sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/owl_import_imported_select.json");
		sgJson.setSparqlConn(conn);
		query = sgJson.getNodeGroup(oInfo).generateSparqlSelect();
		table = TestGraph.execTableSelect(query);
	
		NodeGroup ng = sgJson.getNodeGroup(oInfo);

		
		assertEquals("wrong number of rows returned by:\n" + query, 5, table.getNumRows());
	}
	@Test
	/**
	 * Owl import from the leaf graph
	 */
	public void testOwlImportConnection() throws Exception {

		SparqlEndpointInterface sei = TestGraph.getSei();
		sei.setGraph("http://semtk.junit/leaf");

		SparqlConnection conn = new SparqlConnection();
		conn.setName("junit testOwlImportConnection");
		conn.setDomain("http://semtk.junit");
		conn.addDataInterface(sei);
		conn.addModelInterface(sei);
		
		// load should fail without owl imports
		try {
			OntologyInfo oInfo = new OntologyInfo();
			oInfo.loadSparqlConnection(conn);
			
			fail("No failure was encountered when followOwlImports wasn't set to true.");
		} catch (Exception e) {}
		
		// now set owl imports and it should succeed and class should be imported
		conn.setOwlImportsEnabled(true);
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.loadSparqlConnection(conn);
		OntologyClass oClass = oInfo.getClass("http://semtk.junit/imported#Imported");
		
		// run just the basic test on loadSparqlConnection
		assertTrue("http://semtk.junit/imported#Imported was not imported by owl import", oClass != null);

	}
	
	@Test
	public void testContainsClassesWithBase() throws Exception {
		TestGraph.clearGraph();
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false);
		assertFalse("oInfo magically contains made-up-class", oInfo.containsClassWithBase("http://made/up"));
		
		TestGraph.uploadOwl("src/test/resources/Pet.owl");
		oInfo.load(TestGraph.getSei(), false);
		InputStream is = this.getClass().getResourceAsStream("/Pet.owl");
		assertTrue("can't find class with base that was just loaded", oInfo.containsClassWithBase(is));
	}
}
