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

import org.apache.curator.framework.recipes.locks.PredicateResults;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestConnection;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.load.utility.UriResolver;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.ontologyTools.PredicateStats;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;

/** 
 * Path-finding tests
 * only _IT for the TestGraph
 * 
 * @author 200001934
 *
 */
public class PathFindingTests_IT {
	private static OntologyInfo oInfo;
	
	private final static String REQUIREMENT = "http://arcos.rack/REQUIREMENTS#REQUIREMENT";
	private final static String TEST_RESULT = "http://arcos.rack/TESTING#TEST_RESULT";
	private final static String THING = "http://arcos.rack/PROV-S#THING";
	private final static String WAS_DERIVED_FROM = "http://arcos.rack/PROV-S#wasDerivedFrom";
	private final static String WAS_IMPACTED_BY = "http://arcos.rack/PROV-S#wasImpactedBy";



	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		TestGraph.clearGraph();
		
		// open source RACK is a great example 
		// of an ontology with lots of extraneous superclass-related paths 
		TestGraph.uploadOwlResource(PathFindingTests_IT.class,  "RACK/PROV-S.owl");
		TestGraph.uploadOwlResource(PathFindingTests_IT.class,  "RACK/REQUIREMENTS.owl");
		TestGraph.uploadOwlResource(PathFindingTests_IT.class,  "RACK/TESTING.owl");
		oInfo = TestGraph.getOInfo();

	}
	
	@AfterClass
	public static void cleanup() throws Exception {
	}

	
	
	@Test
	public void testOne() throws Exception {

		TestGraph.clearPrefix(UriResolver.DEFAULT_URI_PREFIX);
		TestGraph.ingest(this.getClass(), "RACK/ingest_req_test_result.json", "RACK/ingest_req_test_result.csv");
		
		//ArrayList<OntologyPath> paths = oInfo.findAllPaths(TEST_RESULT, REQUIREMENT);
		//System.out.println("paths: " + paths.size());
		
		PredicateStats stats = new PredicateStats(TestGraph.getSparqlConn(), oInfo);
		
		oInfo.setPredicateStats(stats);
		ArrayList<OntologyPath> paths2 = oInfo.findAllPaths(TEST_RESULT, REQUIREMENT);
		for (int i=0; i < paths2.size(); i++) {
			System.out.println(paths2.get(i).asString());
		}
		
		assertTrue(true);

	}
	
	@Test
	/**
	 * The RACK ontology has lots of super/sub stuff and it is used heavily by path-finding
	 * so test it here.
	 * 
	 * @throws Exception
	 */
	public void testSubSuperClassesProps() throws Exception {

		HashSet<String> set = oInfo.getSubclassNames(THING);
		assertEquals("THING has wrong number of subclasses", 12, set.size());
		
		set = oInfo.getSubclassNames(REQUIREMENT);
		assertEquals("REQUIREMENT has wrong number of subclasses", 0, set.size());
		
		set = oInfo.getSuperclassNames(REQUIREMENT);
		assertEquals("REQUIREMENT has wrong number of super classes", 2, set.size());
		
		set = oInfo.getSuperclassNames(THING);
		assertEquals("THING has wrong number of super classes", 0, set.size());
		
		set = oInfo.getSuperPropNames(WAS_DERIVED_FROM);
		assertEquals("wasDerivedFrom has wrong number of super properties", 0, set.size());
		
		set = oInfo.getSuperPropNames(WAS_IMPACTED_BY);
		assertEquals("wasImpactedBy has wrong number of super properties", 1, set.size());

	}
}
	