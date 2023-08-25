/**
 ** Copyright 2017 General Electric Company
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

package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;


public class SparqlToXUtilsTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	public void testGetInstanceData_IT() throws Exception {
		TestGraph.clearGraph();
		SparqlGraphJson sgJson = TestGraph.initGraphWithData(this.getClass(), "sampleBattery");
		SparqlConnection conn = sgJson.getSparqlConn();
		OntologyInfo oInfo = sgJson.getOntologyInfo(IntegrationTestUtility.getOntologyInfoClient());
		
		// get color classes
		ArrayList<String> classList = new ArrayList<String>();
		classList.add("http://kdl.ge.com/batterydemo#Color");
		String query = SparqlToXLibUtil.generateSelectInstanceDataSubjects(conn, oInfo,classList, -1, -1, false);
		Table resTab = TestGraph.execQueryToTable(query);
		assertEquals(resTab.toCSVString() + "\nWrong number of rows.", 3, resTab.getNumRows());
		
		// count query
		query = SparqlToXLibUtil.generateSelectInstanceDataSubjects(conn, oInfo,classList, -1, -1, true);
		resTab = TestGraph.execQueryToTable(query);
		assertEquals("wrong subject count", 3, resTab.getCellAsInt(0, 0));
		
		
		// try retrieving predicates
		ArrayList<String[]> predList = new ArrayList<String[]>();
		predList.add(new String [] {"http://kdl.ge.com/batterydemo#Battery", "http://kdl.ge.com/batterydemo#cell"});
		predList.add(new String [] {"http://kdl.ge.com/batterydemo#Battery", "http://kdl.ge.com/batterydemo#name"});
		query = SparqlToXLibUtil.generateSelectInstanceDataPredicates(conn, oInfo, predList, -1, -1, false);
		resTab = TestGraph.execQueryToTable(query);
		assertEquals(resTab.toCSVString() + "\nWrong number of rows.", 6, resTab.getNumRows());
		
		// get just the count
		query = SparqlToXLibUtil.generateSelectInstanceDataPredicates(conn, oInfo, predList, -1, -1, true);
		resTab = TestGraph.execQueryToTable(query);
		assertEquals("wrong predicate count", 6, resTab.getCellAsInt(0, 0));
		
		
	}
	
	@Test
	public void testCountInstances() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "/AnimalSubProps.owl");
		TestGraph.uploadOwlResource(this, "/AnimalsToCombineData.owl");
		
		String query = SparqlToXLibUtil.generateCountInstances(TestGraph.getSparqlConn(), TestGraph.getOInfo(), "http://AnimalSubProps#Tiger");
		Table resTab = TestGraph.execQueryToTable(query);
		assertEquals("Wrong number of rows ", 1, resTab.getNumRows());
		assertEquals("Wrong number of cols ", 1, resTab.getNumColumns());
		assertEquals("Wrong number of Tigers ", 6, resTab.getCellAsInt(0, 0));
		
		query = SparqlToXLibUtil.generateCountInstances(TestGraph.getSparqlConn(), TestGraph.getOInfo(), "http://AnimalSubProps#Animal");
		resTab = TestGraph.execQueryToTable(query);
		assertEquals("Wrong number of Tigers ", 7, resTab.getCellAsInt(0, 0));
	}
}