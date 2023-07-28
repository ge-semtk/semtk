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

package com.ge.research.semtk.load.utility.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.IngestionNodegroupBuilder;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class IngestionNodegroupBuilderTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	public void basicTest() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "sampleBattery.owl");		
		
		// build the ingestion template
		IngestionNodegroupBuilder battryBuilder = new IngestionNodegroupBuilder("http://kdl.ge.com/batterydemo#Battery", TestGraph.getSparqlConn(), TestGraph.getOInfo());
		battryBuilder.setIdRegex("(name|Id$)");
		battryBuilder.build();
		
		// check the CSV
		String csv = battryBuilder.getCsvTemplate();
		assertTrue(csv.contains("birthday"));
		assertTrue(csv.contains("name"));
		assertTrue(csv.contains("cell_cellId"));
		
		// ingest some data when cells don't exist yet:  error
		String data = 	"birthday,name,cell_cellId\n" +
						"03/23/1966, batt1, cell1\n" +
						"03/23/1966, batt2, cell2\n" +
						"null      , batt2, cell3\n";
	
		SparqlGraphJson batterySGJson = battryBuilder.getSgjson();
		
		Dataset ds = new CSVDataset(data, true);
		DataLoader dl = new DataLoader(batterySGJson, ds, TestGraph.getUsername(), TestGraph.getPassword());
		int records = dl.importData(true);
		assertEquals("URI lookup error on cells did not occur as expected", 0, records);
		
		// ingest the cells
		String cellData = 	"cellId\n" +
				"cell1\n" +
				"cell2\n" +
				"cell3\n";
		// build the ingestion template
		IngestionNodegroupBuilder cellBuilder = new IngestionNodegroupBuilder("http://kdl.ge.com/batterydemo#Cell", TestGraph.getSparqlConn(), TestGraph.getOInfo());
		cellBuilder.setIdRegex("(name|Id$)");
		cellBuilder.build();
		SparqlGraphJson cellSGJson = cellBuilder.getSgjson();
		
		assertTrue("Missing cellId column", cellBuilder.getCsvTemplate().contains("cellId"));
		assertTrue("Missing color_Color column", cellBuilder.getCsvTemplate().contains("color_Color"));

		
		csv = cellBuilder.getCsvTemplate();
		
		Dataset cellDs = new CSVDataset(cellData, true);
		DataLoader cellDl = new DataLoader(cellSGJson, cellDs, TestGraph.getUsername(), TestGraph.getPassword());
		records = cellDl.importData(true);
		assertEquals(dl.getLoadingErrorReportBrief(), 3, records);
		
		// now try the batteries again
		ds = new CSVDataset(data, true);
		dl = new DataLoader(batterySGJson, ds, TestGraph.getUsername(), TestGraph.getPassword());
		records = dl.importData(true);
		assertEquals(dl.getLoadingErrorReportBrief(), 3, records);
		
		// use same sgJson to query data back out
		batterySGJson.getNodeGroup().orderByAll();
		Table roundTrip = TestGraph.execTableSelect(batterySGJson);
		assertEquals(3, roundTrip.getNumRows());
		// name column
		assertTrue(roundTrip.getCellAsString(0, 1).equals("batt1"));
		assertTrue(roundTrip.getCellAsString(1, 1).equals("batt2"));
		assertTrue(roundTrip.getCellAsString(2, 1).equals("batt2"));
		// cell_id column
		assertTrue(roundTrip.getCellAsString(0, 2).equals("cell1"));
		assertTrue(roundTrip.getCellAsString(1, 2).equals("cell2"));
		assertTrue(roundTrip.getCellAsString(2, 2).equals("cell3"));
		
	}

	@Test
	public void classTypeTest() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "loadTestDuraBattery.owl");		
		
		// build the ingestion template
		IngestionNodegroupBuilder battryBuilder = new IngestionNodegroupBuilder("http://kdl.ge.com/durabattery#Battery", TestGraph.getSparqlConn(), TestGraph.getOInfo());
		battryBuilder.setIdRegex("(Id$)");
		battryBuilder.build();
		
		// check the CSV:   superclass should contain _type column
		String csv = battryBuilder.getCsvTemplate();
		assertTrue(csv.contains("assemblyDate"));
		assertTrue(csv.contains("batteryDesc"));
		assertTrue(csv.contains("batteryId"));
		assertTrue(csv.contains("Battery_type"));

		
		// ingest some data when cells don't exist yet:  error
		String data = 	"assemblyDate,batteryId,Battery_type,batteryDesc\n" +
						"03/23/1966, batt1, Battery,\n" +
						"03/23/1966, batt2, DuraBattery,\n";
	
		SparqlGraphJson batterySGJson = battryBuilder.getSgjson();
		
		Dataset ds = new CSVDataset(data, true);
		DataLoader dl = new DataLoader(batterySGJson, ds, TestGraph.getUsername(), TestGraph.getPassword());
		int records = dl.importData(true);
		assertEquals("Did not ingest proper number of rows", 2, records);
	}
	
	@Test
	public void dataClassTest() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "loadTestDuraBattery.owl");		
		
		// build the ingestion template
		IngestionNodegroupBuilder battryBuilder = new IngestionNodegroupBuilder("http://kdl.ge.com/durabattery#DuraBattery", TestGraph.getSparqlConn(), TestGraph.getOInfo());
		battryBuilder.setIdRegex("(Id$)");
		battryBuilder.setDataClassRegex("#Cell$");
		battryBuilder.build();
		
		// check the CSV: should have data columns including enumerated type color
		String csv = battryBuilder.getCsvTemplate();
		for (String colName : "assemblyDate,batteryDesc,batteryId,cell1_cellId,cell1_color,cell2_cellId,cell2_color,cell3_cellId,cell3_color,cell4_cellId,cell4_color".split(",")) {
			assertTrue(csv.contains(colName));
		}

		
		// ingest data
		String data = 	"batteryId,cell1_cellId,cell1_color\n" +
						"batt01,cell0A,blue\n" +
						"batt02,cell0B,red\n";
	
		SparqlGraphJson batterySGJson = battryBuilder.getSgjson();
		
		Dataset ds = new CSVDataset(data, true);
		DataLoader dl = new DataLoader(batterySGJson, ds, TestGraph.getUsername(), TestGraph.getPassword());
		int records = dl.importData(true);
		assertEquals("Did not ingest proper number of rows", 2, records);
		
		batterySGJson.getNodeGroup().orderByAll();
		Table roundTrip = TestGraph.execTableSelect(batterySGJson);
		assertEquals("wrong number of results", 2, roundTrip.getNumRows());
		assertTrue(roundTrip.getCellAsString(0, "batteryId").equals("batt01"));
		assertTrue(roundTrip.getCellAsString(0, "cell1_cellId").equals("cell0A"));
		assertTrue(roundTrip.getCellAsString(0, "cell1_color").endsWith("blue"));
		assertTrue(roundTrip.getCellAsString(1, "batteryId").equals("batt02"));
		assertTrue(roundTrip.getCellAsString(1, "cell1_cellId").equals("cell0B"));
		assertTrue(roundTrip.getCellAsString(1, "cell1_color").endsWith("red"));

	}
}


// PEC HERE:  test the four smapleBattery Inflate/Deflate files