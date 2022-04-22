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
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestConnection;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.test.QueryGenTest_IT;
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
	
	@AfterClass
	public static void cleanup() throws Exception {
		TestGraph.clearSyncedGraph("src/test/resources/owl_import_Imported.owl");
		TestGraph.clearSyncedGraph("src/test/resources/owl_import_Leaf.owl");
		TestGraph.clearSyncedGraph("src/test/resources/owl_import_LeafRange.owl");
	}

	
	@Test
	public void testFullLoad() throws Exception {

		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "Pet.owl");		

		// attempt the load
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());

		assertTrue("Expecting 0 enums.  found: " + oInfo.getNumberOfEnum(),              oInfo.getNumberOfEnum() == 0);
		assertTrue("Expecting 3 classes.  found: " + oInfo.getNumberOfClasses(),        oInfo.getNumberOfClasses() == 3);
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
		TestGraph.uploadOwlResource(this, "annotationBattery.owl");		

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
		TestGraph.uploadOwlResource(this, "annotationBattery.owl");		
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
		// and compare results\
		// NOTE:    this is used lightly if at all
		// CAREFUL: only the features in annotationBattery.owl are tested
		
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "annotationBattery.owl");		

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
		TestGraph.uploadOwlResource(this, "Pet.owl");		

		// attempt the load
		@SuppressWarnings("deprecation")
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
		@SuppressWarnings("deprecation")
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
		TestGraph.uploadOwlResource(this, "Pet.owl");

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
		TestGraph.uploadOwlResource(this, "owl_import_Leaf.owl");		

		// load should fail
		try {
			new OntologyInfo(TestGraph.getSparqlConn());
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
		TestGraph.uploadOwlResource(this, "owl_import_LeafRange.owl");		

		// load should succeed
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn());
		
		OntologyClass oClass = oInfo.getClass("http://semtk.junit/leafrange#LeafRange");
		OntologyProperty prop = oClass.getProperties().get(0);
		OntologyRange oRange = prop.getRange(oClass, oInfo);
		
		OntologyClass errClass = oInfo.getClass(oRange.getSimpleUri());
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
		
		TestGraph.uploadOwlResource(this, "Pet.owl");
		oInfo.load(TestGraph.getSei(), false);
		InputStream is = this.getClass().getResourceAsStream("/Pet.owl");
		assertTrue("can't find class with base that was just loaded", oInfo.containsClassWithBase(is));
	}
	
	@Test
	/**
	 * Load ontology with subproperties
	 * 		superProp
	 *      subPropDomainRange
	 *      subPropDomainOnly
	 *      subPropRangeOnly
	 *      subPropOnly
	 */
	public void testSubProperties() throws Exception {

		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "subproperties.owl");		
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false);
		
		// Jira:  PESQS-724
		// Range should not be inferred, but it is.
		// So *PropOnly and *DomainOnly should eventually be "Class" or something generic.
		String propArr[] = new String[]   { "superProp", "subPropDomainRange", "subPropDomainOnly", "subPropRangeOnly", "subPropOnly", "superDataProp", "subDataPropDomainRange", "subDataPropDomainOnly", "subDataPropRangeOnly", "subDataPropOnly"};
		String domainArr[] = new String[] { "ENTITY",    "SUBENTITY",          "SUBENTITY",         "ENTITY",            "ENTITY",         "ENTITY",        "SUBENTITY",              "SUBENTITY",          "ENTITY",                   "ENTITY"};
		String rangeArr[] = new String[]  { "ENTITY",    "SUBENTITY",          "ENTITY",            "SUBENTITY",        "ENTITY",      "double",        "int",                    "double",                 "int",               "double"};
		
		for (int i=0; i < propArr.length; i++) {
			String propName = "http://paul/subprop#" + propArr[i];
			String domainName = "http://paul/subprop#" + domainArr[i];
			OntologyProperty p = oInfo.getProperty(propName);
			assertTrue("Can't find property: " + propName, p != null);
			OntologyClass oDomain = oInfo.getClass(domainName);
			String r = p.getRange(oDomain, oInfo).getSimpleUri();
			assertTrue(propArr[i] + " expected range to end in #" + rangeArr[i] + " found " + r, r.endsWith("#"+rangeArr[i]));
			
			int expectDomainSize = 1;
			
			// check oInfo.getPropertyDomain()
			ArrayList<OntologyClass> domainList = oInfo.getPropertyDomain(p);
			assertEquals("Size of domain of property " + propArr[i], expectDomainSize, domainList.size());
			
			// check the property's domains
			Set<String> domainList1 = p.getRangeDomains();
			assertEquals("Size of domain of property " + propArr[i], expectDomainSize, domainList1.size());
			
			if (expectDomainSize > 0) {
				String d0 = domainList.get(0).getNameString(true);
				assertTrue(propArr[i] + " expected domain " + domainArr[i] + " found " + d0, d0.equals(domainArr[i]));
			}
		}
	}
	
	/**
	 * Complex and restricted ranges   RangeTest.sadl
	 * @throws Exception
	 */
	@Test
	public void testRanges() throws Exception {
		final String PREFIX = "http://rangetest#";
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "RangeTest.owl");		
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false);
		
		// the entire ontology expectation:  className,  colon-separated-props,  colon-separated lists of ranges for each prop
		// See RangeTest.sadl and it's comments for an explanation
		String table[][] = new String[][] {
			new String[] {"Animal",     "hasChild:hasEgg:hasAnotherEgg",             "Another,Animal,Separate:Egg:Egg"},
			new String[] {"Bird",       "hasChild:hasEgg:hasAnotherEgg",             "Another,Animal,Separate:Egg:Egg"},
			new String[] {"Duck",       "hasChild:hasDuckling:hasEgg:hasAnotherEgg", "Duck:Duck:Egg:Egg"},
			new String[] {"WeirdBird",  "hasChild:hasDuckling:hasEgg:hasAnotherEgg", "Duck,Unusual:Duck,Unusual:Egg:Egg"},
			new String[] {"Rabbit",     "hasChild:hasBunny:hasEgg:hasAnotherEgg",    "Rabbit:Rabbit:Egg:Egg"},
			new String[] {"Another",    "",                            ""},
			new String[] {"Unusual",    "",                            ""},
			new String[] {"Egg",        "",                            ""},
			new String[] {"Separate",   "hasChild:hasEgg:hasAnotherEgg",             "Another,Animal,Separate:Egg:Egg"}
		};
		
		for (String row[] : table) {
			// get class and prop from ontology
			String tableDomainUri = PREFIX + row[0];
			String tableDomainKeyname = row[0];
			OntologyClass oClass = oInfo.getClass(tableDomainUri);
			ArrayList <OntologyProperty> oProps = oInfo.getInheritedProperties(oClass);
			
			// get class and props from table
			String tableProps[] = row[1].isBlank() ? new String[]{} : row[1].split(":");
			String tableRanges[] = row[2].isBlank() ? new String[]{} : row[2].split(":");
			
			assertEquals(row[0] + " has incorrect number of properties", tableProps.length, oProps.size());
			
			// loop through expected props
			for (int i=0; i < tableProps.length; i++) {
				// get the uriList (ranges)
				OntologyProperty oProp = oInfo.getInheritedPropertyByUri(oClass, PREFIX + tableProps[i]);
				OntologyRange oRange = oProp.getRange(oClass, oInfo);
				HashSet<String> actualRangeList = oRange.getUriList();
				
				// compare to table values
				String [] tableRangeUris = tableRanges[i].split(",");
				assertEquals(tableDomainKeyname + "->" + tableProps[i] + " of {" + String.join(",", actualRangeList) + "} has incorrect number of range uris", tableRangeUris.length, actualRangeList.size());
				
				for (String expectedRange : tableRangeUris) {
					assertTrue(tableDomainKeyname + "->" + tableProps[i] + " of {" + String.join(",", actualRangeList) + "} does not contain range " + expectedRange, actualRangeList.contains(PREFIX + expectedRange));
				}
			}
		}
	}
	
	@Test
	public void testList() throws Exception {
		// SADL3 List is not supported.
		// Make sure it is handled slightly gracefully
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "ListTest.owl");		
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false);
		
		OntologyClass oClass = oInfo.getClass("http://list#ListTest");
		OntologyProperty oProp = oClass.getProperty("http://list#Pets");
		assertTrue("Property with range List did not show up in ontology when SadlListModel.owl IS NOT loaded", oProp != null);
		
		TestGraph.uploadOwlResource(this, "SadlListModelTest.owl");		// SadlListModel.owl is in .gitignore, so use a different name.
		oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false);
		
		oClass = oInfo.getClass("http://list#ListTest");
		oProp = oClass.getProperty("http://list#Pets");
		assertTrue("Property with range List did not show up in ontology when SadlListModel.owl IS loaded", oProp != null);
		
	}
	@Test
	public void testGetLowestCommonSuperclass() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwlContents(Utility.getResourceAsString(QueryGenTest_IT.class, 
				"AnimalSubProps.owl"));
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(TestGraph.getSei(), false);
		
		String animal = "http://AnimalSubProps#Animal";
		String cat = "http://AnimalSubProps#Cat";
		String tiger = "http://AnimalSubProps#Tiger";
		String dog = "http://AnimalSubProps#Dog";
		
		assertEquals(oInfo.findLowestCommonSuperclass(tiger, dog), animal);
		assertEquals(oInfo.findLowestCommonSuperclass(tiger, cat), cat);
		assertEquals(oInfo.findLowestCommonSuperclass(dog, cat), animal);
	}
}
