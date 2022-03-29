/**
 ** Copyright 2020 General Electric Company
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

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.ontologyTools.ClassInstance;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.PathExplorer;
import com.ge.research.semtk.ontologyTools.PathItemRequest;
import com.ge.research.semtk.ontologyTools.ReturnRequest;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;

public class PathExplorerTest_IT {
	private static SparqlConnection conn = null;
	private static OntologyInfo oInfo = null;
	private static PathExplorer explorer = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		
		IntegrationTestUtility.setupFdcTests(PathExplorerTest_IT.class);
		
		conn = TestGraph.getSparqlConn();
		oInfo = new OntologyInfo(conn);
		
		// create a nodegroup cache graph based on the jUnit test graph name
		String cacheName = TestGraph.generateGraphName("ngCache");
		SparqlEndpointInterface cacheSei = TestGraph.getSei(cacheName);
		cacheSei.clearGraph();
		
		explorer = new PathExplorer(oInfo, conn, IntegrationTestUtility.getNodeGroupExecutionRestClient(), cacheSei);
		
	}
	
	
	@Test
	public void addClassAirportToAircraft() throws Exception {
		// link Aircraft to Airport though FDC Aircraft Location and FDC Distance chain.
		// many-to-one FDC calls shouldn't be too overwhelming
		
		NodeGroup ng = new NodeGroup();
		ng.getOrAddNode("http://research.ge.com/semtk/fdcSample/test#Airport", oInfo);
		
		// Airport -> Aircraft
		// this should succeed through Distance
		Node add = explorer.addClassFirstPath(ng, "http://research.ge.com/semtk/fdcSample/test#Aircraft");
		
		assertTrue("addClassFirstPathWithData failed to link Aircraft to Airport", add != null);
		
		// check each link
		for (String sparqlId : new String [] {"?AircraftLocation", "?Distance", "?Location"} ) {
			assertTrue("Didn't link through " + sparqlId + " : " + ng.getNodeSparqlIds().toString(), ng.getNodeBySparqlID(sparqlId) != null);
		}
		
	}
	
	@Test
	public void addClassAirportToAirport_SLOW() throws Exception {
		// distance between two airports.
		// FDC calls could take a long time
		
		NodeGroup ng = new NodeGroup();
		ng.getOrAddNode("http://research.ge.com/semtk/fdcSample/test#Airport", oInfo);
		
		// Airport -> Aircraft
		// this should succeed through Distance
		Node add = explorer.addClassFirstPath(ng, "http://research.ge.com/semtk/fdcSample/test#Airport");
		
		assertTrue("addClassFirstPathWithData failed to link Airport to Airport", add != null);
		
		// check each link
		for (String sparqlId : new String [] {"?Airport_0", "?Location_0", "?Distance", "?Location", "?Airport"} ) {
			assertTrue("Didn't link through " + sparqlId + " : " + ng.getNodeSparqlIds().toString(), ng.getNodeBySparqlID(sparqlId) != null);
		}
		
	}
	
	@Test
	public void addClassAirportToAirportInstances() throws Exception {
		// distance between two airports.
		// FDC calls could take a long time
		
		NodeGroup ng = new NodeGroup();
		ng.setSparqlConnection(TestGraph.getSparqlConn());
		Node airport = ng.addNode("http://research.ge.com/semtk/fdcSample/test#Airport", TestGraph.getOInfo());
		airport.setIsReturned(true);
		ng.setLimit(2);
		Table airportTable = TestGraph.execTableSelect(ng.generateSparqlSelect());
		String airportUri1 = airportTable.getCell(0, 0);
		String airportUri2 = airportTable.getCell(1, 0);
		
		ng = new NodeGroup();
		ng.addNodeInstance("http://research.ge.com/semtk/fdcSample/test#Airport", oInfo, airportUri1);
		
		// Airport -> Aircraft
		// this should succeed through Distance
		Node add = explorer.addClassFirstPath(ng, "http://research.ge.com/semtk/fdcSample/test#Airport", airportUri2);
		
		assertTrue("addClassFirstPathWithData failed to link Airport to Airport", add != null);
		
		// check each link
		for (String sparqlId : new String [] {"?Airport_0", "?Location_0", "?Distance", "?Location", "?Airport"} ) {
			assertTrue("Didn't link through " + sparqlId + " : " + ng.getNodeSparqlIds().toString(), ng.getNodeBySparqlID(sparqlId) != null);
		}
		
	}

	@Test
	public void addClassFdcFail() throws Exception {
		// linking only half of parameters for FDC.
		// will never find instance data using addClassFirstPathWithData()
		
		NodeGroup ng = new NodeGroup();
		ng.getOrAddNode("http://research.ge.com/semtk/fdcSample/test#Airport", oInfo);
		
		// Airport -> Distance  
		// this should fail because distance requires two locations to ever work
		Node add = explorer.addClassFirstPath(ng, "http://research.ge.com/semtk/fdcSample/test#Distance");
		assertTrue("Adding distance with only one location did not fail", add == null);
		
	}
	
	@Test
	public void buildNgAirportToAirportInstances() throws Exception {
		// distance between two airports.
		// FDC calls could take a long time
		
		NodeGroup ng = new NodeGroup();
		Node airport = ng.addNode("http://research.ge.com/semtk/fdcSample/test#Airport", TestGraph.getOInfo());
		ng.setSparqlConnection(TestGraph.getSparqlConn());
		ng.setIsReturned(airport, true);
		ng.setLimit(2);
		String query = ng.generateSparqlSelect();
		
		Table airportTable = TestGraph.execTableSelect(query);
		String airportUri1 = airportTable.getCell(0, 0);
		String airportUri2 = airportTable.getCell(1, 0);
		
		ArrayList<PathItemRequest> requestList = new ArrayList<PathItemRequest>();
		PathItemRequest req;
		
		req = new PathItemRequest("http://research.ge.com/semtk/fdcSample/test#Airport");
		req.setInstanceUri(airportUri1);
		requestList.add(req);
		
		req = new PathItemRequest("http://research.ge.com/semtk/fdcSample/test#Airport");
		req.setInstanceUri(airportUri2);
		requestList.add(req);
		
		req = new PathItemRequest("http://research.ge.com/semtk/fdcSample/test#Distance");
		req.addPropUri("http://research.ge.com/semtk/fdcSample/test#distanceNm");
		requestList.add(req);
				
		ng = explorer.buildNgWithData(requestList);
		
		assertTrue("buildNgWithData failed to link Airport to Airport", ng != null);
		
		// check each link
		for (String sparqlId : new String [] {"?Airport_0", "?Location_0", "?Distance", "?Location", "?Airport"} ) {
			assertTrue("Didn't link through " + sparqlId + " : " + ng.getNodeSparqlIds().toString(), ng.getNodeBySparqlID(sparqlId) != null);
		}
		
		for (String sparqlId : new String [] {"?distanceNm"} ) {
			assertTrue("Didn't return " + sparqlId + " : " + ng.getReturnedSparqlIDs().toString(), ng.getReturnedSparqlIDs().contains(sparqlId));
		}
		
	}
}
