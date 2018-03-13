package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.ontologyTools.DataDictionaryGenerator;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.TestGraph;


public class DataDictionaryTest_IT {
	
	@Test
	public void test() throws Exception {
		
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/annotationBattery.owl");		
		OntologyInfo oInfo = new OntologyInfo(TestGraph.getSparqlConn("http://"));

		// generate a dictionary with stripped namespaces
		Table dataDictionaryTable = DataDictionaryGenerator.generate(oInfo, true);		
		
		// check the table structure
		assertEquals(dataDictionaryTable.getNumRows(),14);
		assertEquals(dataDictionaryTable.getNumColumns(),5);
		
		// check some labels and comments, including for shared properties and inherited properties
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Battery").getSubsetWhereMatches("PROPERTY", "").getCell(0,3).equals("duracell")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Battery").getSubsetWhereMatches("PROPERTY", "cell").getCell(0,2).equals("Cell")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Battery").getSubsetWhereMatches("PROPERTY", "id (SHARED PROPERTY)").getCell(0,2).equals("string")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "BatteryChild").getSubsetWhereMatches("PROPERTY", "id (SHARED PROPERTY)").getCell(0,2).equals("string")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Battery").getSubsetWhereMatches("PROPERTY", "id (SHARED PROPERTY)").getCell(0,3).equals("alias added by Battery ; alias added by Cell (SHARED PROPERTY)")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Cell").getSubsetWhereMatches("PROPERTY", "color").getCell(0,4).equals("you know,like red")));

		// check the enumeration
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Color", new String[]{"DATA TYPE"}).getCell(0,0).contains("enumeration")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Color", new String[]{"DATA TYPE"}).getCell(0,0).contains("red")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Color", new String[]{"DATA TYPE"}).getCell(0,0).contains("white")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "Color", new String[]{"DATA TYPE"}).getCell(0,0).contains("blue")));
		
		// generate a dictionary with full namespaces
		dataDictionaryTable = DataDictionaryGenerator.generate(oInfo, false);	
		assertEquals(dataDictionaryTable.getNumRows(),14);
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "http://kdl.ge.com/batterydemo#Battery").getSubsetWhereMatches("PROPERTY", "").getCell(0,3).equals("duracell")));
		assertTrue((dataDictionaryTable.getSubsetWhereMatches("CLASS", "http://kdl.ge.com/batterydemo#Battery").getSubsetWhereMatches("PROPERTY", "http://kdl.ge.com/batterydemo#cell").getCell(0,2).equals("http://kdl.ge.com/batterydemo#Cell")));
	}
	
}
