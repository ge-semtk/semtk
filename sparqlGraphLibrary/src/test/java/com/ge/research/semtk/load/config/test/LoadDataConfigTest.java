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
import org.junit.Test;

import com.ge.research.semtk.load.config.LoadDataConfig;
import com.ge.research.semtk.load.config.LoadDataConfig.CsvByClassIngestionStep;
import com.ge.research.semtk.utility.Utility;

// TODO test variations, valid and invalid
// TODO test extra data graphs (when implemented)
public class LoadDataConfigTest extends YamlConfigTest {

	public LoadDataConfigTest() throws Exception {
		super();
	}

	/**
	 * Test populating an LoadDataConfig instance from a YAML file
	 */
	@Test
	public void test() throws Exception{

		LoadDataConfig config;
		
		// this config has a datagraph + steps
		config = new LoadDataConfig(Utility.getResourceAsFile(this, "/config/load_csv_config_1.yaml"), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());
		assertEquals(config.getFallbackModelGraph(), modelFallbackSei.getGraph());
		assertEquals(config.getSteps().size(), 2);
		assertEquals(((CsvByClassIngestionStep)config.getSteps().get(0)).getClazz(), "http://animals/woodland#WOODCHUCK");
		assertTrue(((CsvByClassIngestionStep)config.getSteps().get(0)).getCsvPath().matches("(.*)config(.*)woodchucks.csv"));
		assertEquals(((CsvByClassIngestionStep)config.getSteps().get(1)).getClazz(), "http://animals/woodland#HEDGEHOG");
		assertTrue(((CsvByClassIngestionStep)config.getSteps().get(1)).getCsvPath().matches("(.*)config(.*)hedgehogs.csv"));
		assertEquals(config.getModelgraph(), null);  	// TODO don't have an example to test yet
		assertEquals(config.getDatagraphs().size(), 1);
		assertEquals(config.getDatagraphs().get(0), "http://junit/animals/data");
		
		// this config has steps only (no graphs)
		config = new LoadDataConfig(Utility.getResourceAsFile(this, "/config/load_csv_config_2.yaml"), modelFallbackSei.getGraph(), dataFallbackSei.getGraph());
		assertEquals(config.getDatagraphs(), null);
	}
	
}
