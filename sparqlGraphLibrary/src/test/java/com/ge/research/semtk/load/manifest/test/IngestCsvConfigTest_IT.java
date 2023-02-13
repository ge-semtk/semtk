/**
 ** Copyright 2023 General Electric Company
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
package com.ge.research.semtk.load.manifest.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

import com.ge.research.semtk.load.manifest.IngestCsvConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.TestGraph;

public class IngestCsvConfigTest_IT extends YamlConfigTest{

	public IngestCsvConfigTest_IT() throws Exception {
		super();
	}

	/**
	 * Test loading data via YAML file
	 */
	@Test
	public void test() throws Exception{

		SparqlEndpointInterface dataSei = TestGraph.getSei(DATA_GRAPH);
		SparqlEndpointInterface dataSeiFallback = TestGraph.getSei(FALLBACK_DATA_GRAPH);
		
		IngestCsvConfig config = new IngestCsvConfig(new File("src/test/resources/manifest/IngestionPackage/TestData/Package-1/import.yaml"), null, FALLBACK_DATA_GRAPH);

		// test that data gets loaded to the graph provided
		dataSei.clearGraph();
		dataSeiFallback.clearGraph();

		config.load(MODEL_GRAPH, new LinkedList<String>(Arrays.asList(DATA_GRAPH)), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, new PrintWriter(System.out));
		assertEquals(dataSei.getNumTriples(), 9999999);  // TODO add real number when load is implemented
		assertEquals(dataSeiFallback.getNumTriples(), 0);

		// TODO more tests - including fallback

		// clean up
		dataSei.dropGraph();
		dataSeiFallback.dropGraph();
	}

}
