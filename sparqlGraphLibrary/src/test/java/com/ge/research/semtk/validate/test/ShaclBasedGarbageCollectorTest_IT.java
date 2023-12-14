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
package com.ge.research.semtk.validate.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.validate.ShaclBasedGarbageCollector;
import com.ge.research.semtk.resultSet.Table;

public class ShaclBasedGarbageCollectorTest_IT {
	
	SparqlConnection sparqlConn;
	String shaclStr;
	
	@Test
	public void test() throws Exception{
		setup();
		assertEquals(sparqlConn.getDataInterface(0).getNumTriples(), 151);
		Table table = (new ShaclBasedGarbageCollector()).run(shaclStr, sparqlConn);  // runs 2 rounds of garbage collection.  2nd round is Fruits that are available for GC after their basket is deleted.
		assertEquals(sparqlConn.getDataInterface(0).getNumTriples(), 127);
		assertEquals(table.getNumRows(), 10);
		assertEquals(table.getSubsetWhereMatches(ShaclBasedGarbageCollector.HEADER_DELETED_INSTANCES, "http://DeliveryBasketExample#fruit100d").getNumRows(), 1);
	}
	
	@Test
	// perform one round of garbage collection
	public void testRunOnce() throws Exception{
		setup();
		assertEquals(sparqlConn.getDataInterface(0).getNumTriples(), 151);
		Table table = (new ShaclBasedGarbageCollector()).run(shaclStr, sparqlConn, false);  // runs 1 round of garbage collection
		assertEquals(sparqlConn.getDataInterface(0).getNumTriples(), 133);
		assertEquals(table.getNumRows(), 5);
	}
	
	@Test
	// provide good error message if attempt to run on multiple datasets
	public void testErrorIfMultipleDatasets() throws Exception {
		setup();
		sparqlConn.addDataInterface(TestGraph.getSei("aaa"));
		try {
			(new ShaclBasedGarbageCollector()).run(shaclStr, sparqlConn, false);
			fail(); // should not get here
		}catch(Exception e) {
			assertTrue(e.getMessage().equals("Connection has multiple data interfaces: this is not yet supported"));
		}
	}
	
	private void setup() throws Exception {

		// create a SPARQL connection, populate it
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "DeliveryBasketExample.owl");
		sparqlConn = TestGraph.getSparqlConn();
		
		// get SHACL for DeliveryBasket garbage collection
		shaclStr = Files.readString(Utility.getResourceAsTempFile(this, "DeliveryBasketExample-GarbageCollection-shacl.ttl").toPath(), StandardCharsets.UTF_8);
	}
	
}
