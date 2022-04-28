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
		
		// ingest some data without cells:  error
		String data = 	"birthday,name,cell_cellId\n" +
						"03/23/1966, batt1, cell1\n" +
						"03/23/1966, batt2, cell2\n" +
						"null      , batt2, cell3\n";
	
		SparqlGraphJson batterySGJson = battryBuilder.getSgjson();
		
		Dataset ds = new CSVDataset(data, true);
		DataLoader dl = new DataLoader(batterySGJson, ds, TestGraph.getUsername(), TestGraph.getPassword());
		int records = dl.importData(true);
		assertEquals("Error if missing did not occur as expected on cells", 0, records);
		
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

	
	
}


// PEC HERE:  test the four smapleBattery Inflate/Deflate files