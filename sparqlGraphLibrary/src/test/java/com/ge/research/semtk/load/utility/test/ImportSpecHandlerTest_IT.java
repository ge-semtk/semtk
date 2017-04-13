package com.ge.research.semtk.load.utility.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.test.TestGraph;

public class ImportSpecHandlerTest_IT {

	@Test
	public void test_getMappedPropItems() throws Exception {
		// load test data
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/sampleBattery.owl");		
		
		// load in ImportSpecHandler
		String jsonPath = "src/test/resources/sampleBatteryClearedColorMapping.json";
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath);
		OntologyInfo oInfo = sgJson.getOntologyInfo();
		NodeGroup nodegroup = sgJson.getNodeGroupCopy();
		ImportSpecHandler handler = new ImportSpecHandler(sgJson.getImportSpecJson(), oInfo);
		
		// Test
		ArrayList<PropertyItem> pItems = handler.getMappedPropItems(nodegroup);
		assertTrue(pItems.size() == 3);
	}

}
