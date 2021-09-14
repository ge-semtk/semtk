package com.ge.research.semtk.load.test;

import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.ImportSpec;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class ImportSpecTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}

	@Test
	public void test_createSpecFromReturns() throws Exception {
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
		
		// load data and query it back
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromResource(this, "sampleBatteryNameCellColor.json");
		CSVDataset csvDataset = new CSVDataset("src/test/resources/sampleBattery.csv", false);
		DataLoader dl = new DataLoader(sgJson, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		assertEquals(dl.getTotalRecordsProcessed(), 4);
		Table resTable1 = TestGraph.execTableSelect(sgJson);

		// createSpecFromReturns()
		ImportSpec specFromRet = ImportSpec.createSpecFromReturns(sgJson.getNodeGroup());
		sgJson.setImportSpecJson(specFromRet.toJson());
		
		// clear, then load data from first round using new import spec
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(this, "/sampleBattery.owl");
		csvDataset = new CSVDataset(resTable1.toCSVString(), true);
		dl = new DataLoader(sgJson, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		assertEquals(dl.getTotalRecordsProcessed(), 4);
		Table resTable2 = TestGraph.execTableSelect(sgJson);
		
		// the createSpecFromReturns() import spec should have imported all the data.
		// results should equal the input data
		assertTrue(resTable2.toCSVString().equals(resTable1.toCSVString()));
	}	
	
	@Test
	public void test_createEmptySpec() throws Exception {


		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromResource(this, "sampleBatteryNameCellColor.json");
	
		// createSpecFromReturns()
		ImportSpec emptySpec = ImportSpec.createEmptySpec(sgJson.getNodeGroup());
		
		assertEquals("Empty spec has wrong number of nodes", 3, emptySpec.getNumNodes());
		int i = emptySpec.getNodeIndex("?Battery");
		assertTrue("Battery node was not found", i > -1);
		assertEquals("Battery node has properties", 0, emptySpec.getNodeNumProperties(i));
		assertEquals("Battery node has mappings", 0, emptySpec.getNodeNumMappings(i));
		
	}	
}
