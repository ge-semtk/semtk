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
package com.ge.research.semtk.load.config.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

import com.ge.research.semtk.load.config.LoadDataConfig;
import com.ge.research.semtk.load.config.LoadOwlConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class LoadDataConfigTest_IT extends YamlConfigTest{

	public LoadDataConfigTest_IT() throws Exception {
		super();
	}

	/**
	 * Test loading data via YAML file
	 */
	@Test
	public void test() throws Exception{

		try {

			final int NUM_EXPECTED_TRIPLES = 20;		// TODO verify that this is correct

			// TODO uniquifyJunitGraphName
			LoadOwlConfig loadOwlConfig = new LoadOwlConfig(Utility.getResourceAsFile(this, "/manifest/IngestionPackage/RACK-Ontology/OwlModels/import.yaml"), modelFallbackSei.getGraph());
			LoadDataConfig loadDataConfig = new LoadDataConfig(Utility.getResourceAsFile(this, "/manifest/IngestionPackage/TestData/Package-1/import.yaml"), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());
			LoadDataConfig loadDataConfigWithNoDatagraphInYaml = new LoadDataConfig(Utility.getResourceAsFile(this, "/manifest/IngestionPackage/TestData/Package-1/import-withNoDatagraph.yaml"), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());

			// Case 1: if load() data graph parameter, then confirm loads there
			clearGraphs();
			loadOwlConfig.load(modelSei.getGraph(), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));  // loads OWL
			loadDataConfig.load(modelSei.getGraph(), new LinkedList<String>(Arrays.asList(dataSei.getGraph())), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), new PrintWriter(System.out));
			assertEquals(dataSei.getNumTriples(), NUM_EXPECTED_TRIPLES);
			assertEquals(dataFallbackSei.getNumTriples(), 0);

			// Case 2: if no load() data graph parameter, then confirm loads to YAML data graph if present    "http://junit/rack001/data"
			clearGraphs();
			loadOwlConfig.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));  // loads OWL to fallback
			loadDataConfig.load(null, null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), new PrintWriter(System.out));
			SparqlEndpointInterface dataSeiFromYaml = TestGraph.getSei(loadDataConfig.getDatagraphs().get(0));		// this is what's in the YAML
			assertEquals(dataSei.getNumTriples(), 0);
			assertEquals(dataSeiFromYaml.getNumTriples(), NUM_EXPECTED_TRIPLES);
			assertEquals(dataFallbackSei.getNumTriples(), 0);

			// Case 3: if no load() data graph parameter, and no YAML data graph, then confirm loads to fallback
			clearGraphs();
			loadOwlConfig.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));  // loads OWL to fallback
			loadDataConfigWithNoDatagraphInYaml.load(null, null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), new PrintWriter(System.out));
			assertEquals(dataSei.getNumTriples(), 0);
			assertEquals(dataFallbackSei.getNumTriples(), NUM_EXPECTED_TRIPLES);

		}catch(Exception e) {
			throw e;
		}finally {
			clearGraphs();
		}
	}

}
