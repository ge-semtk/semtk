/**
 ** Copyright 2016 General Electric Company
 **
 */

package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.DataToModelTransformer;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.Utility;

public class DataToModelTransformerTest_IT {

	@Test
	public void checkSpecHandlerFunction() throws Exception {

		String[] headers = { "Battery", "Cell", "color", "birthday" };
		Dataset ds = new CSVDataset("src/test/resources/sampleBattery.csv",	headers);

		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/sampleBattery.json"));
		DataToModelTransformer dtmx = new DataToModelTransformer(sgJson, 6, ds);
		ArrayList<NodeGroup> ngArr = dtmx.getNextBatch();
	
//		for (NodeGroup n : ngArr) {
//			for (Node nd : n.getNodeList()) {
//				System.out.println(nd.getSparqlID() + " instance value was " + nd.getInstanceValue());
//				for (PropertyItem prop : nd.getPropertyItems()) {
//					String ret = "";
//					for (String inst : prop.getInstanceValues()) {
//						ret += inst + ",";
//					}
//					if (ret != "") {
//						System.out.println("\t" + prop.getUriRelationship()	+ " ----> " + ret);
//					}
//				}
//			}
//		}		

		assertEquals(ngArr.size(),4);		
		assertEquals(ngArr.get(0).getNodeCount(),3);
		assertEquals(ngArr.get(0).getNodeList().get(0).getFullUriName(),"http://kdl.ge.com/batterydemo#Color");
		assertEquals(ngArr.get(0).getNodeList().get(1).getFullUriName(),"http://kdl.ge.com/batterydemo#Cell");
		assertEquals(ngArr.get(0).getNodeList().get(2).getFullUriName(),"http://kdl.ge.com/batterydemo#Battery");
		assertEquals(ngArr.get(0).getNodeList().get(0).getInstanceValue(),"belmont/generateSparqlInsert#red");
		assertEquals(ngArr.get(0).getNodeList().get(1).getInstanceValue(),"belmont/generateSparqlInsert#Cell_cell200");
		assertEquals(ngArr.get(0).getNodeList().get(2).getInstanceValue(),"belmont/generateSparqlInsert#Battery_battA");
		assertEquals(ngArr.get(0).getNodeList().get(1).getPropertyItems().size(),1);
		assertEquals(ngArr.get(0).getNodeList().get(1).getPropertyItems().get(0).getInstanceValues().get(0),"cell200");
		assertEquals(ngArr.get(0).getNodeList().get(2).getPropertyItems().size(),2);
		assertEquals(ngArr.get(0).getNodeList().get(2).getPropertyItems().get(0).getInstanceValues().get(0),"1966-01-01T12:00:00");
		assertEquals(ngArr.get(0).getNodeList().get(2).getPropertyItems().get(1).getInstanceValues().get(0),"battA");
	}

}
