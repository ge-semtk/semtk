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

// PEC LICENSE NOTE test.json is not open source

package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;

public class LoadCSVToVirtuosoTest_IT {
	
	private static SparqlGraphJson sgJson = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		sgJson = TestGraph.initGraphWithData("sampleBattery");
	}
	
	@Test
	public void loadCSVToVirtuoso() throws Exception {
		String[] headers = { "Battery", "Cell", "color", "birthday" };
		Dataset ds = new CSVDataset("src/test/resources/sampleBattery.csv", headers);
		DataLoader dl = new DataLoader(sgJson, 2, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		assertEquals(dl.getTotalRecordsProcessed(),4);
	}
	
	@Test 
	public void loadCSVToVirtuoso_BadFile() {

		boolean thrown = false;
		try {			
			// load JSON object from file
			
			String[] headers = {"Battery","Cell","color","birthday"};
			Dataset ds = new CSVDataset("src/test/resources/bad/path.csv", headers);
			fail("Did not throw expected exception");
		
		} catch (Exception e) {

		}	
		
	}
}
