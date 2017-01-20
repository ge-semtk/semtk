package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.test.TestGraph;

public class NodeGroupTest {

	private static SparqlGraphJson sgJson = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		sgJson = TestGraph.initGraphWithData("sampleBattery");
	}

	@Test
	public void nodeGroupFromJson() throws Exception {		

		NodeGroup ng = NodeGroup.getInstanceFromJson(sgJson.getSNodeGroupJson());

		assertEquals(3, ng.getNodeList().size());
		assertEquals(ng.getNodeList().get(0).getFullUriName(),"http://kdl.ge.com/batterydemo#Color");
		assertEquals(ng.getNodeList().get(1).getFullUriName(),"http://kdl.ge.com/batterydemo#Cell");
		assertEquals(ng.getNodeList().get(2).getFullUriName(),"http://kdl.ge.com/batterydemo#Battery");	
	}
	
}
