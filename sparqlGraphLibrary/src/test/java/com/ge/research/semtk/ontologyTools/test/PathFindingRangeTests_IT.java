 /**
 ** Copyright 2022 General Electric Company
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

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyPath;


/** 
 * Path-finding tests
 * only _IT for the TestGraph
 * 
 * @author 200001934
 *
 */
public class PathFindingRangeTests_IT {
	private static OntologyInfo oInfo;
	
	
	private final static String ANIMAL = "http://rangetest#Animal";
	private final static String DUCK = "http://rangetest#Duck";
	
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		TestGraph.clearGraph();
		
		// RangeTest.owl has many range restrictions and complex ranges 
		TestGraph.uploadOwlResource(PathFindingTests_IT.class,  "RangeTest.owl");
		oInfo = TestGraph.getOInfo();

	}
	
	@AfterClass
	public static void cleanup() throws Exception {
	}

	
	
	@Test
	public void testComplexRangeHasChild() throws Exception {
		// Test model-based path-finding with tricky complex ranges and restrictions
		
		
		
		ArrayList<OntologyPath> paths = oInfo.findAllPaths(DUCK, ANIMAL);
		ArrayList<String> pathStrings = new ArrayList<String>();
		for (OntologyPath path : paths) {
			pathStrings.add(path.asString().trim());
			System.out.println(path.asString());
		}
		
		// This one came back wrong once so test for it
		String [] illegalPaths = new String [] {"Duck.hasChild Animal" };
		for (String pathStr : illegalPaths) {
			assertFalse("Found illegal path: " + pathStr, pathStrings.contains(pathStr));
		}
		
		// Most obvious correct answer
		String [] legalPaths = new String [] {"Animal.hasChild Duck" };
		for (String pathStr : legalPaths) {
			assertTrue("Missing legal path: " + pathStr, pathStrings.contains(pathStr));
		}
		
		// Build each nodegroup and do a validation
		// This at least checks consistency between path-finding and validation
		for (int i=0; i < paths.size(); i++) {
			NodeGroup ng = new NodeGroup();
			ng.addNode(ANIMAL, oInfo);
			ng.addPath(paths.get(i), ng.getNode(0), oInfo);
			ng.validateAgainstModel(oInfo);
		}

	}
	
}