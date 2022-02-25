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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.load.utility.UriResolver;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.PredicateStats;


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
	private final static String TEST = "http://arcos.rack/TESTING#TEST";
	private final static String THING = "http://arcos.rack/PROV-S#THING";
	private final static String WAS_DERIVED_FROM = "http://arcos.rack/PROV-S#wasDerivedFrom";
	private final static String WAS_IMPACTED_BY = "http://arcos.rack/PROV-S#wasImpactedBy";



	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		TestGraph.clearGraph();
		
		// open source RACK is a great example 
		// of an ontology with lots of extraneous superclass-related paths 
		TestGraph.uploadOwlResource(PathFindingRangeTests_IT.class,  "RACK/PROV-S.owl");
		TestGraph.uploadOwlResource(PathFindingRangeTests_IT.class,  "RACK/REQUIREMENTS.owl");
		TestGraph.uploadOwlResource(PathFindingRangeTests_IT.class,  "RACK/TESTING.owl");
		oInfo = TestGraph.getOInfo();

	}
	
	@AfterClass
	public static void cleanup() throws Exception {
	}

	
	
	@Test
	public void testWithPredicateStats() throws Exception {

		TestGraph.clearPrefix(UriResolver.DEFAULT_URI_PREFIX);
		TestGraph.ingest(this.getClass(), "RACK/ingest_req_test_result.json", "RACK/ingest_req_test_result.csv");
		
		PredicateStats stats = new PredicateStats(TestGraph.getSparqlConn(), oInfo);
		
		ArrayList<OntologyPath> paths2 = oInfo.findAllPaths(TEST_RESULT, REQUIREMENT, stats);
		
		assertEquals("Number of paths found", 1, paths2.size());
		OntologyPath path = paths2.get(0);
		assertEquals("Path start", TEST_RESULT, path.getStartClassName());
		assertEquals("Path end", REQUIREMENT, path.getEndClassName());
		assertTrue(path.containsClass(TEST));

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
	
	
