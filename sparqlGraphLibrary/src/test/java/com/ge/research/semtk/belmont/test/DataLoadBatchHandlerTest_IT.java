/**
 ** Copyright 2016 General Electric Company
 **
 */

package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.DataLoadBatchHandler;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class DataLoadBatchHandlerTest_IT {
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	@Test
	public void checkSpecHandlerFunction() throws Exception {

		String[] headers = { "Battery", "Cell", "color", "birthday" };
		Dataset ds = new CSVDataset("src/test/resources/sampleBattery.csv",	headers);

		TestGraph.clearGraph();
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/sampleBattery.json");
		TestGraph.uploadOwl("src/test/resources/sampleBattery.owl");
		DataLoadBatchHandler dtmx = new DataLoadBatchHandler(sgJson, 6, ds, null);  // TODO temporary null endpoint
		ArrayList<ArrayList<String>> recordList = dtmx.getNextRecordsFromDataSet();
		ArrayList<NodeGroup> ngArr = dtmx.convertToNodeGroups(recordList, 1, false);	

		assertEquals(4, ngArr.size());		
		assertEquals(3, ngArr.get(0).getNodeCount());
		assertEquals("http://kdl.ge.com/batterydemo#Color", ngArr.get(0).getNodeList().get(0).getFullUriName());
		assertEquals("http://kdl.ge.com/batterydemo#Cell", ngArr.get(0).getNodeList().get(1).getFullUriName());
		assertEquals("http://kdl.ge.com/batterydemo#Battery", ngArr.get(0).getNodeList().get(2).getFullUriName());
		assertEquals("http://kdl.ge.com/batterydemo#red", ngArr.get(0).getNodeList().get(0).getInstanceValue());
		assertEquals("belmont/generateSparqlInsert#Cell_cell200", ngArr.get(0).getNodeList().get(1).getInstanceValue());
		assertEquals("belmont/generateSparqlInsert#Battery_battA", ngArr.get(0).getNodeList().get(2).getInstanceValue());
		assertEquals(1, ngArr.get(0).getNodeList().get(1).getPropertyItems().size());
		assertEquals("cell200", ngArr.get(0).getNodeList().get(1).getPropertyItems().get(0).getInstanceValues().get(0));
		assertEquals(2, ngArr.get(0).getNodeList().get(2).getPropertyItems().size());
		assertEquals("1966-01-01T12:00:00", ngArr.get(0).getNodeList().get(2).getPropertyItems().get(0).getInstanceValues().get(0));
		assertEquals("battA", ngArr.get(0).getNodeList().get(2).getPropertyItems().get(1).getInstanceValues().get(0));
	}

}
