package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.Utility;

public class ValueConstraintTest {

	@Test
	public void test() throws Exception {
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/sampleBattery.json"));
		NodeGroup ng = sgJson.getNodeGroup();
		
		// uri
		Returnable r = ng.getItemBySparqlID("?Color");
		String v = ValueConstraint.buildFilterConstraint(r, ">", "http://kdl.ge.com/batterydemo#testuri");
		assertTrue(v.equals("FILTER(?Color > <http://kdl.ge.com/batterydemo#testuri>)"));
		
		// string
		r = ng.getItemBySparqlID("?CellId");
		v = ValueConstraint.buildFilterConstraint(r, "=", "teststring");
		assertTrue(v.equals("FILTER(?CellId = \"teststring\"^^<http://www.w3.org/2001/XMLSchema#string>)"));
	}

}
