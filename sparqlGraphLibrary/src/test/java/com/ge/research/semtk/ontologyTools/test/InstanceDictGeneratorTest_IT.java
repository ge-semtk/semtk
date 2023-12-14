/**
 ** Copyright 2020 General Electric Company
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


import org.junit.BeforeClass;
import org.junit.Test;


import com.ge.research.semtk.ontologyTools.InstanceDictGenerator;
import com.ge.research.semtk.ontologyTools.OntologyInfo;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;

import com.ge.research.semtk.test.TestGraph;

public class InstanceDictGeneratorTest_IT {

	@BeforeClass
	public static void setup() throws Exception {

	}
	
	
	@Test
	public void testInstanceDict() throws Exception {
		String data = "Battery,Cell,color,birthday\n"
				+ "battA,cell200,red,1966-01-01T12:00:00\n"
				+ "battA,cell300,blue,1979-01-01T12:00:00-04:00\n"
				+ "battB,cell401,white,01/01/2000 00:00:01\n"
				+ "battB,cell402,white,07-04-2016\n"
				+ "both,both,red,\n"
				+ "batt 2word,cell 2words,white,\n"
				+ "batt three words,cell three words,blue,\n"
				+ "triplet,,,\n";
				
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "sampleBattery.owl");
		TestGraph.ingestCsvString(getClass(), "sampleBatteryGuids.json", data);
		SparqlConnection conn = TestGraph.getSparqlConn();
		OntologyInfo oInfo = TestGraph.getOInfo();
		
		InstanceDictGenerator generator = new InstanceDictGenerator(conn, oInfo, 2, 2);
		Table tab = generator.generate();
		assertEquals("Wrong number of rows", 11, tab.getNumRows());
		
		generator = new InstanceDictGenerator(conn, oInfo, 3, 2);
		tab = generator.generate();
		assertEquals("Wrong number of rows after allowing three words", 13, tab.getNumRows());
		
		generator = new InstanceDictGenerator(conn, oInfo, 2, 1);
		tab = generator.generate();
		assertEquals("Wrong number of rows with specificity set to 1", 9, tab.getNumRows());
	}
	
}
