/**
 ** Copyright 2016 General Electric Company
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestConnection;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.test.QueryGenTest_IT;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.load.utility.UriResolver;
import com.ge.research.semtk.ontologyTools.ConnectedDataConstructor;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.ontologyTools.PredicateStats;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;

/** 
 * Path-finding tests
 * only _IT for the TestGraph
 * 
 * @author 200001934
 *
 */
public class ConnectedDataConstructorTests_IT {
	private static OntologyInfo oInfo;
	
	private final static String REQUIREMENT = "http://arcos.rack/REQUIREMENTS#REQUIREMENT";
	private final static String TEST_RESULT = "http://arcos.rack/TESTING#TEST_RESULT";
	private final static String TEST = "http://arcos.rack/TESTING#TEST";
	private final static String THING = "http://arcos.rack/PROV-S#THING";
	private final static String WAS_DERIVED_FROM = "http://arcos.rack/PROV-S#wasDerivedFrom";
	private final static String WAS_IMPACTED_BY = "http://arcos.rack/PROV-S#wasImpactedBy";



	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		// load Batttery
		SparqlGraphJson sgJsonBattery = TestGraph.addModelAndData(QueryGenTest_IT.class, "sampleBattery");

	}
	
	@AfterClass
	public static void cleanup() throws Exception {
	}

	
	
	@Test
	public void test_createConstructAllConnectedBattery_DELETE_ME() throws Exception {	
		final String CELL300 = "http://semtk.research.ge.com/generated#Cell_cell300";
		
		ConnectedDataConstructor constructor = new ConnectedDataConstructor(
				CELL300, XSDSupportedType.NODE_URI,
				TestGraph.getSparqlConn());
		
		JSONObject jObj = constructor.queryJsonLd();
		String j = jObj.toJSONString();
		for (String lookup : new String [] {"Cell_cell300", "Battery_battA", "cell", "color", "blue", "cellId", "cell300"}) {
			assertTrue("Results are missing: " + lookup, j.contains(lookup));
		}
		
		constructor = new ConnectedDataConstructor(
				"1966-01-01T12:00:00", XSDSupportedType.DATETIME,
				TestGraph.getSparqlConn());
		
		jObj = constructor.queryJsonLd();
		j = jObj.toJSONString();
		for (String lookup : new String [] {"birthday", "Battery_battA"}) {
			assertTrue("Results are missing: " + lookup, j.contains(lookup));
		}
	}
	
}
	
