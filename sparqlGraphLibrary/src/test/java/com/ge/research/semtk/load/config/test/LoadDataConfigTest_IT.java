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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

import com.ge.research.semtk.load.config.LoadDataConfig;
import com.ge.research.semtk.load.config.LoadOwlConfig;
import com.ge.research.semtk.load.config.LoadDataConfig.CsvByClassIngestionStep;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class LoadDataConfigTest_IT extends YamlConfigTest{

	public LoadDataConfigTest_IT() throws Exception {
		super();
	}

	/**
	 * Test populating an LoadDataConfig instance from a YAML file
	 */
	@Test
	public void testInstantiate() throws Exception{

		LoadDataConfig config;

		// this config has a datagraph + steps
		config = new LoadDataConfig(Utility.getResourceAsTempFile(this, "/config/load_data_config_1.yaml"), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());
		assertEquals(config.getDefaultModelGraph(), modelFallbackSei.getGraph());
		assertEquals(config.getSteps().size(), 2);
		assertEquals(((CsvByClassIngestionStep)config.getSteps().get(0)).getClazz(), "http://animals/woodland#WOODCHUCK");
		assertTrue(((CsvByClassIngestionStep)config.getSteps().get(0)).getFilePath().endsWith("woodchucks.csv"));
		assertEquals(((CsvByClassIngestionStep)config.getSteps().get(1)).getClazz(), "http://animals/woodland#HEDGEHOG");
		assertTrue(((CsvByClassIngestionStep)config.getSteps().get(1)).getFilePath().endsWith("hedgehogs.csv"));
		assertEquals(config.getModelgraph(), null);
		assertEquals(config.getDatagraphs().size(), 1);
		assertEquals(config.getDatagraphs().get(0), "http://junit/animals/data");

		// this config has steps only (no graphs)
		config = new LoadDataConfig(Utility.getResourceAsTempFile(this, "/config/load_data_config_2.yaml"), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());
		assertEquals(config.getDatagraphs(), null);

		// this config test has extra-graphs
		config = new LoadDataConfig(Utility.getResourceAsTempFile(this, "/config/load_data_config_3.yaml"), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());
		assertEquals(3, config.getDatagraphs().size());
		assertEquals(config.getDatagraphs().get(0), "http://junit/animals/data");
		assertEquals(config.getDatagraphs().get(1), "http://junit/animals/data2");
		assertEquals(config.getDatagraphs().get(2), "http://junit/animals/data3");

		// this config has a model-graph
		config = new LoadDataConfig(Utility.getResourceAsTempFile(this, "/config/load_data_config_4.yaml"), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());
		assertEquals(config.getModelgraph(), "http://junit/animals/model");
	}

	@Test
	public void testInstantiate_Fail() throws Exception{
		try {
			new LoadDataConfig(Utility.getResourceAsTempFile(this, "/config/load_data_config_1.yaml"), null, dataFallbackSei.getGraph());
			fail(); // should not get here
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("Default model graph not provided"));
		}
		try {
			new LoadDataConfig(Utility.getResourceAsTempFile(this, "/config/load_data_config_1.yaml"), modelFallbackSei.getGraph(), null);
			fail(); // should not get here
		}catch(Exception e) {
			assertTrue(e.getMessage().contains("Default data graph not provided"));
		}
	}

	/**
	 * Test loading data via YAML file
	 */
	@Test
	public void testLoad() throws Exception{
		try {
			final int NUM_EXPECTED_TRIPLES = 20;

			File tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/IngestionPackage.zip");
			LoadOwlConfig loadOwlConfig = new LoadOwlConfig(
					Paths.get(tempDir.getAbsolutePath(), "RACK-Ontology","OwlModels","import.yaml").toFile(), 
					modelFallbackSei.getGraph());
			LoadDataConfig loadDataConfig = new LoadDataConfig(
					Paths.get(tempDir.getAbsolutePath(), "TestData","Package-1","import.yaml").toFile(), 
					modelFallbackSei.getGraph(), dataFallbackSei.getGraph());

			// Case 1: if load() data graph parameter, then confirm loads there
			clearGraphs();
			loadOwlConfig.load(modelSei.getGraph(), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);  // loads OWL
			loadDataConfig.load(modelSei.getGraph(), new LinkedList<String>(Arrays.asList(dataSei.getGraph())), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);
			assertEquals(dataSei.getNumTriples(), NUM_EXPECTED_TRIPLES);
			assertEquals(dataFallbackSei.getNumTriples(), 0);

			// Case 2: if no load() data graph parameter, then confirm loads to YAML data graph if present    "http://junit/rack001/data"
			clearGraphs();
			loadOwlConfig.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);  // loads OWL to fallback
			loadDataConfig.load(null, null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);
			SparqlEndpointInterface dataSeiFromYaml = TestGraph.getSei(loadDataConfig.getDatagraphs().get(0));		// this is what's in the YAML
			assertEquals(dataSei.getNumTriples(), 0);
			assertEquals(dataSeiFromYaml.getNumTriples(), NUM_EXPECTED_TRIPLES);
			assertEquals(dataFallbackSei.getNumTriples(), 0);

			// Case 3: if no load() data graph parameter, and no YAML data graph, then confirm loads to fallback
			clearGraphs();
			loadOwlConfig.load(null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);  // loads OWL to fallback
			loadDataConfig.setDataGraphs(null);  // no datagraph in YAML
			loadDataConfig.load(null, null, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);
			assertEquals(dataSei.getNumTriples(), 0);
			assertEquals(dataFallbackSei.getNumTriples(), NUM_EXPECTED_TRIPLES);

		}finally {
			clearGraphs();
		}
	}

	/**
	 * Test loading data via YAML file: load by nodegroup
	 */
	@Test
	public void testLoad_ByNodegroup() throws Exception{
		try {
			TestGraph.clearGraph();

			// store a nodegroup to use for loading
			JSONObject nodegroupJson = Utility.getResourceAsJson(this, "/RACK/ingest_req_test_result.json");
			IntegrationTestUtility.getNodeGroupStoreRestClient().executeStoreNodeGroup("JUNIT LoadDataConfig-byNodegroup", "", "junit", nodegroupJson);

			// upload OWL
			TestGraph.uploadOwlResource(this, "/RACK/PROV-S.owl");
			TestGraph.uploadOwlResource(this, "/RACK/REQUIREMENTS.owl");
			TestGraph.uploadOwlResource(this, "/RACK/TESTING.owl");

			// load data by nodegroup
			File tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/config/LoadDataConfig-byNodegroup.zip");
			LoadDataConfig loadDataConfig = new LoadDataConfig(Paths.get(tempDir.getAbsolutePath(), "import.yaml").toFile(), TestGraph.getSei().getGraph(), TestGraph.getSei().getGraph());
			loadDataConfig.load(TestGraph.getSei().getGraph(), new LinkedList<String>(Arrays.asList(TestGraph.getSei().getGraph())), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), IntegrationTestUtility.getSparqlQueryAuthClient(), null);
			assertEquals(TestGraph.getSei().getNumTriples(), 334);

		} finally{
			IntegrationTestUtility.cleanupNodegroupStore("junit");
			TestGraph.clearGraph();
		}
	}

}
