package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.ontologyTools.DataDictionaryGenerator;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.TestGraph;


public class DataDictionaryTest {
	
	@Test
	public void test() throws Exception {
		
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/annotationBattery.owl");		
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn("http://"));

		// generate a dictionary with stripped namespaces
		Table dataDictionaryTable = DataDictionaryGenerator.generate(oInfo, true);		
		assertEquals(dataDictionaryTable.getNumRows(),14);
		assertTrue(dataDictionaryTable.getCell(0, 0).equals("Battery"));	
		assertTrue(dataDictionaryTable.getCell(0,3).equals("duracell"));
		
		// generate a dictionary with full namespaces
		dataDictionaryTable = DataDictionaryGenerator.generate(oInfo, false);	
		assertEquals(dataDictionaryTable.getNumRows(),14);
		assertTrue(dataDictionaryTable.getCell(0, 0).equals("http://kdl.ge.com/batterydemo#Battery")); 	
		assertTrue(dataDictionaryTable.getCell(0,3).equals("duracell"));
		
		// TODO MORE ASSERTS (shared property, enumeration, etc)
	}
	
}
