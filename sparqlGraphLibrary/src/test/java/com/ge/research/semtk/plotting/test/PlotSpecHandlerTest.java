/**
 ** Copyright 2021 General Electric Company
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
package com.ge.research.semtk.plotting.test;

import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;


import com.ge.research.semtk.plotting.PlotlyPlotSpecHandler;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.Utility;

public class PlotSpecHandlerTest {

	private String tableJsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"2020-01-24T00:00:00\",\"22.2\",\"33.3\"],[\"2020-01-25T00:00:00\",\"22.5\",\"33.5\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
	
	@Test
	public void test() throws Exception {
		JSONObject plotSpecJson = Utility.getResourceAsJson(this, "plotly.json");		
		PlotlyPlotSpecHandler specHandler = new PlotlyPlotSpecHandler(plotSpecJson);
		assertTrue(specHandler.getType().equals(PlotlyPlotSpecHandler.TYPE));
		assertTrue(specHandler.getName().equals("Plotly Chart 1"));
		assertTrue(specHandler.getSpec().containsKey("data"));
		assertTrue(specHandler.getSpec().containsKey("layout"));
		assertTrue(specHandler.getSpec().containsKey("config"));
		
		specHandler.setName("Plotly Modified Name");
		assertEquals(specHandler.getName(),"Plotly Modified Name");
				
	}
	
	
	@Test
	public void testApplyTable() throws Exception {

		// the plot spec
		JSONObject plotSpecJson = Utility.getResourceAsJson(this, "plotly.json");
	
		// a table
		Table table = Table.fromJson((JSONObject) new JSONParser().parse(tableJsonStr));
		
		PlotlyPlotSpecHandler specHandler = new PlotlyPlotSpecHandler(plotSpecJson);
		specHandler.applyTable(table);
		
		JSONObject resultJson = specHandler.toJson();	
		System.out.println(resultJson.toJSONString());
		assertFalse(resultJson.toJSONString().contains("SEMTK_TABLE"));
		assertTrue(resultJson.toJSONString().contains("\"x\":[\"2020-01-24T00:00:00\",\"2020-01-25T00:00:00\"]"));
		assertTrue(resultJson.toJSONString().contains("\"y\":[22.2,22.5]"));
		assertTrue(resultJson.toJSONString().contains("\"y\":[33.3,33.5]"));
	}
	
	
	@Test
	public void testApplyTableError() throws Exception {
		JSONObject plotSpecJson = Utility.getResourceAsJson(this, "plotly-unsupported.json");  // contains SEMTK_TABLE.unsupported[colA]
		Table table = Table.fromJson((JSONObject) new JSONParser().parse(tableJsonStr));

		PlotlyPlotSpecHandler specHandler = new PlotlyPlotSpecHandler(plotSpecJson);		
		try{
			specHandler.applyTable(table);
			fail();  // should not get here
		}catch(Exception e){
			assertTrue(e.getMessage().contains("Unsupported data specification"));
		}
		
	}
	
}
