package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.LocalLogger;


public class OntologyDatatypeTest_IT {
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	public void testLoadingAndValidatingDAL() throws Exception {
		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<String> warnings = new ArrayList<String>();
		
		LocalLogger.logToStdErr("119: ----------- START testLoadingAndValidatingDAL ---------------");
		// load model
		TestGraph.clearGraph();
		
		// 119 test
		Table tab = TestGraph.runQuery("select ?s ?p ?o FROM <http://junit/GG2NQYY2E/200001934/both> where { ?s ?p ?o }");
		LocalLogger.logToStdErr("119 post clearGraph() triples:\n" + tab.toCSVString());
				
		TestGraph.uploadOwlResource(this, "datatype_dal.owl");	
		
		// 119 test
		tab = TestGraph.runQuery("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
				+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> \n"
				+ "PREFIX owl:<http://www.w3.org/2002/07/owl#> \n"
				+ "SELECT DISTINCT ?dataType ?equivType ?r_pred ?r_obj \n"
				+ "		FROM <http://junit/GG2NQYY2E/200001934/both> \n"
				+ "WHERE { \n"
				+ "	?dataType rdf:type rdfs:Datatype . \n"
				+ "filter (!regex(str(?dataType),'^(nodeID://|_:)') ) \n"
				+ "   ?dataType owl:equivalentClass* ?e . \n"
				+ "   { \n"
				+ "        ?e owl:onDatatype ?equivType \n"
				+ "    } UNION { \n"
				+ "        ?e owl:unionOf ?u . \n"
				+ "	    ?u rdf:rest* ?r . \n"
				+ "	    ?r rdf:first ?equivType . \n"
				+ "	} \n"
				+ "   optional {  \n"
				+ "     ?e owl:withRestrictions ?rlist . \n"
				+ "     ?rlist rdf:rest* ?r2 . \n"
				+ "     ?r2 rdf:first ?restriction . \n"
				+ "     ?restriction ?r_pred ?r_obj . \n"
				+ "   } \n"
				+ "} ");
		LocalLogger.logToStdErr("119 post load owl datatype query:\n" + tab.toCSVString());
		
		OntologyInfo oInfo = TestGraph.getOInfo();
		assertTrue("http://testy#DAL is not found in oInfo.getDatatype", oInfo.getDatatype("http://testy#DAL") != null);
		
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(getClass(), "datatype_dal_deflated.json");
		assertEquals("Expected dal property to be deflated, but it showed up.", null, sgjson.getNodeGroupNoInflateNorValidate(oInfo).getNode(0).getPropertyByKeyname("dal"));
		
		// inflate from deflated nodegroup
		PropertyItem pItem = sgjson.getNodeGroupInflateAndValidate(oInfo, errors, null, warnings).getNode(0).getPropertyByKeyname("dal");
		assertTrue("Inflating datatype_dal_deflated.json valueType error expected [INT] found:  " + pItem.getValueTypes().toString(),  pItem.getValueTypes().contains(XSDSupportedType.INT) && pItem.getValueTypes().size() == 1);
		assertEquals("Inflating datatype_dal_deflated.json valueTypeURI error", "http://testy#DAL", pItem.getRangeURI());
		assertEquals("Inflating datatype_dal_deflated.json produced errors", 0, errors.size());
		assertEquals("Inflating datatype_dal_deflated.json produced warnings", 0, warnings.size());
		
		// loading a normal nodegroup
		sgjson = TestGraph.getSparqlGraphJsonFromResource(getClass(), "datatype_dal_ok.json");
		pItem = sgjson.getNodeGroupInflateAndValidate(oInfo, errors, null, warnings).getNode(0).getPropertyByKeyname("dal");
		assertTrue("Inflating datatype_dal_ok.json valueType error", pItem.getValueTypes().contains(XSDSupportedType.INT) && pItem.getValueTypes().size() == 1);
		assertEquals("Inflating datatype_dal_ok.json valueTypeURI error", "http://testy#DAL", pItem.getRangeURI());
		assertEquals("Inflating datatype_dal_ok.json produced errors", 0, errors.size());
		assertEquals("Inflating datatype_dal_ok.json produced warnings", 0, warnings.size());
		
		// loading invalid wrong ValueType should produce a warning and correct to int
		sgjson = TestGraph.getSparqlGraphJsonFromResource(getClass(), "datatype_dal_wrong_xsd.json");
		pItem = sgjson.getNodeGroupInflateAndValidate(oInfo, errors, null, warnings).getNode(0).getPropertyByKeyname("dal");
		assertTrue("Inflating datatype_dal_wrong_xsd.json valueType error", pItem.getValueTypes().contains(XSDSupportedType.INT) && pItem.getValueTypes().size() == 1);
		assertEquals("Inflating datatype_dal_wrong_xsd.json valueTypeURI error", "http://testy#DAL", pItem.getRangeURI());
		assertEquals("Inflating datatype_dal_wrong_xsd.json valueTypeURI error", 0, errors.size());
		assertEquals("Inflating datatype_dal_wrong_xsd.json number of warnings", 1, warnings.size());
		assertTrue("Inflating datatype_dal_wrong_xsd.json error produced a warning without the word 'date'", warnings.get(0).contains("date"));
		
		// loading invalid datatype should produce 2 warnings and correct to DAL int
		sgjson = TestGraph.getSparqlGraphJsonFromResource(getClass(), "datatype_dal_unknown.json");
		pItem = sgjson.getNodeGroupInflateAndValidate(oInfo, errors, null, warnings).getNode(0).getPropertyByKeyname("dal");
		assertTrue("Inflating datatype_dal_unknown.json valueType error", pItem.getValueTypes().contains(XSDSupportedType.INT) && pItem.getValueTypes().size() == 1);
		assertEquals("Inflating datatype_dal_unknown.json with unknown datatype", "http://testy#DAL", pItem.getRangeURI());
		assertEquals("Inflating datatype_dal_unknown.json valueTypeURI error ", 0, errors.size());
		assertEquals("Inflating datatype_dal_unknown.json number of warnings", 2, warnings.size());
		String warn = warnings.get(0) + warnings.get(1);
		assertTrue("Inflating datatype_dal_unknown.json missing warning with the word 'DAL'", warn.contains("DAL"));
		assertTrue("Inflating datatype_dal_unknown.json missing warning with the word 'int'", warn.contains("int"));
		
		LocalLogger.logToStdErr("119: ----------- END testLoadingAndValidatingDAL ---------------");
		
	}
	
	@Test
	public void testLoadDataDAL() throws Exception {

		// load model
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "datatype_dal.owl");		
		
		// load three rows of data
		int rows = TestGraph.ingestFromResources(getClass(), "datatype_dal_ingest.json", "datatype_dal_ingest.csv");
		assertEquals("Ingestion produced wrong number of rows", 3, rows);
		
		// check the round trip results
		TestGraph.queryAndCheckResults(this, "datatype_dal_ingest.json", "datatype_dal_ingest.csv");
		
		// test a load with a bad value for the datatype
		try {
			TestGraph.ingestFromResources(getClass(), "datatype_dal_ingest.json", "datatype_dal_ingest_fail.csv");
			assertTrue("Missing error when loading 'a' as an integer datatype.", false);
		} catch (Exception e) {
			String mustHave = "\"a\" as type \"INT\"";
			assertTrue("Load error doesn't contain " + mustHave, e.getMessage().contains(mustHave));
		}
	}
	
	@Test
	public void testLoadDataMultiTypesSuccess() throws Exception {
		// datatypetest.SADL has examples of complex datatypes with restrictions etc. from SADL 3 documentation
		// This test loads a variety of legal values
		
		LocalLogger.logToStdErr("119: ----------- START testLoadDataMultiTypesSuccess ---------------");
		
		// load model
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "datatypetest.owl");		
		
		// load three rows of data
		int rows = TestGraph.ingestFromResources(getClass(), "datatype_exampleA.json", "datatype_exampleA_pass.csv");
		assertEquals("Ingestion produced wrong number of rows", 4, rows);
		
		// check the round trip results
		TestGraph.queryAndCheckResults(this, "datatype_exampleA.json", "datatype_exampleA_pass.csv");
		
		LocalLogger.logToStdErr("119: ----------- END testLoadDataMultiTypesSuccess ---------------");
	}
	
}
