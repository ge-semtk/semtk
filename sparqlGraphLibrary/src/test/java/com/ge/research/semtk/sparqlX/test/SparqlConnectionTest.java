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

package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.sparqlX.SparqlConnection;

public class SparqlConnectionTest {

	@Test
	public void SparqlConnectionTest1() throws Exception {
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/MeasurementAndBattery.json"));
		NodeGroup ng = sgJson.getNodeGroup();
		String sparql = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 0, null);
		
		String expect = "prefix externalDataConnection:<http://research.ge.com/kdl/sparqlgraph/externalDataConnection#>\r\n" + 
				"prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" +
				"prefix timeseries:<http://research.ge.com/timeseries#>\r\n" + 
				"prefix generateSparqlInsert:<belmont/generateSparqlInsert#>\r\n" + 
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>\r\n" + 
				"prefix batterydemo:<http://kdl.ge.com/batterydemo#>\r\n" + 
				"select distinct ?cellId ?mapName ?tag\r\n" + 
				"		from <http://measurement>\r\n" + 
				"		from <http://demo/battery>\r\n" + 
				" where {" + 
				"	?Table a timeseries:Table .\r\n" + 
				"\r\n" + 
				"	?Table externalDataConnection:map ?MeasurementMap .\r\n" + 
				"		?MeasurementMap a timeseries:MeasurementMap .\r\n" + 
				"		?MeasurementMap externalDataConnection:mapName ?mapName .\r\n" + 
				"\r\n" + 
				"		?MeasurementMap externalDataConnection:hasMeasurement ?Measurement .\r\n" + 
				"			?Measurement a timeseries:Measurement .\r\n" + 
				"			?Measurement externalDataConnection:tag ?tag .\r\n" + 
				"	?Cell a batterydemo:Cell .\r\n" + 
				"	?Cell batterydemo:cellId ?cellId .\r\n" + 
				"	FILTER regex(?cellId, ?mapName) .\r\n" + 
				"}\r\n";
		sparql = sparql.replaceAll("\\s+", " ").toLowerCase();
		assertEquals("Graph didn't appear in sparql exactly once: <http://measurement>", StringUtils.countMatches(sparql,  "from <http://measurement>"), 1);
		assertEquals("Graph didn't appear in sparql exactly once: <http://demo/battery>", StringUtils.countMatches(sparql,  "from <http://demo/battery>"), 1);
		assertEquals("Wrong number of model interfaces", sgJson.getSparqlConn().getModelInterfaceCount(), 2);
	}
	
	@Test
	public void IsSingleServerURLTest() throws Exception {
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/MeasurementAndBattery.json"));
		SparqlConnection conn = sgJson.getSparqlConn();
		assertTrue(conn.isSingleServerURL());
		assertEquals(2, conn.getAllGraphsForServer(conn.getModelInterface(0).getServerAndPort()).size());
		assertEquals(2, conn.getDataDatasetsForServer(conn.getModelInterface(0).getServerAndPort()).size());
		assertEquals(2, conn.getModelDatasetsForServer(conn.getModelInterface(0).getServerAndPort()).size());
	}
	
	@Test
	public void JsonAndEquals() throws Exception {
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/MeasurementAndBattery.json"));
		SparqlConnection conn = sgJson.getSparqlConn();
		SparqlConnection copy = new SparqlConnection(conn.toString());
		assertTrue(copy.equals(conn, false));
	}
	
	@Test
	public void DeepCopyTest() throws Exception {
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getJSONObjectFromFilePath("src/test/resources/MeasurementAndBattery.json"));
		SparqlConnection conn = sgJson.getSparqlConn();
		SparqlConnection copy = SparqlConnection.deepCopy(conn);
		assertTrue(conn.equals(copy, true));
	}

}
