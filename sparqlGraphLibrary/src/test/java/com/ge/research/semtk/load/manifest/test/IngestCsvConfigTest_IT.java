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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

import com.ge.research.semtk.load.manifest.IngestCsvConfig;
import com.ge.research.semtk.load.manifest.IngestOwlConfig;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class IngestCsvConfigTest_IT extends YamlConfigTest{

	public IngestCsvConfigTest_IT() throws Exception {
		super();
	}

	/**
	 * Test loading data via YAML file
	 */
	@Test
	public void test() throws Exception{

		try {
			modelSei.clearGraph();
			dataSei.clearGraph();
			dataSeiFallback.clearGraph();

			// need ontology in place in order to load data
			IngestOwlConfig ingestOwlConfig = new IngestOwlConfig(Utility.getResourceAsFile(this, "/manifest/IngestionPackage/RACK-Ontology/OwlModels/import.yaml"), FALLBACK_MODEL_GRAPH);
			ingestOwlConfig.load(MODEL_GRAPH, TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), new PrintWriter(System.out));

			// load data
			IngestCsvConfig ingestCsvConfig = new IngestCsvConfig(Utility.getResourceAsFile(this, "/manifest/IngestionPackage/TestData/Package-1/import.yaml"), null, FALLBACK_DATA_GRAPH);
			ingestCsvConfig.load(MODEL_GRAPH, new LinkedList<String>(Arrays.asList(DATA_GRAPH)), TestGraph.getSparqlServer(), TestGraph.getSparqlServerType(), false, IntegrationTestUtility.getIngestorRestClient(), IntegrationTestUtility.getNodeGroupExecutionRestClient(), new PrintWriter(System.out));
			assertEquals(dataSei.getNumTriples(), 20);  		// TODO verify that this is correct
			assertEquals(dataSeiFallback.getNumTriples(), 0);	// confirm that nothing was written to fallback

			// TODO more tests - including loading data to fallback

		}catch(Exception e) {
			throw e;
		}finally {
			// clean up
			modelSei.dropGraph();
			dataSei.dropGraph();
			dataSeiFallback.dropGraph();
		}
	}

}
