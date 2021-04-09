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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import com.ge.research.semtk.plotting.PlotSpec;
import com.ge.research.semtk.plotting.PlotlyPlotSpec;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.Utility;

public class PlotSpecTest {

	private String tableJsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"2020-01-24T00:00:00\",\"22.2\",\"33.3\"],[\"2020-01-25T00:00:00\",\"22.5\",\"33.5\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
	
	@SuppressWarnings("unchecked")
	@Test
	public void test() throws Exception {
		JSONObject plotSpecJson = Utility.getResourceAsJson(this, "plotly.json");		
		PlotlyPlotSpec spec = new PlotlyPlotSpec(plotSpecJson);
		assertTrue(spec.getType().equals(PlotlyPlotSpec.TYPE));
		assertTrue(spec.getName().equals("Plotly Chart 1"));
		assertTrue(spec.getSpec().containsKey("data"));
		assertTrue(spec.getSpec().containsKey("layout"));
		assertTrue(spec.getSpec().containsKey("config"));
		
		spec.setName("Plotly Modified Name");
		assertEquals(spec.getName(),"Plotly Modified Name");
		
		// confirm errors if try to create PlotlyPlotSpec with type "visjs"
		JSONObject plotSpecJsonWrongType = (JSONObject) plotSpecJson.clone();
		plotSpecJsonWrongType.remove(PlotSpec.JKEY_TYPE);
		plotSpecJsonWrongType.put(PlotSpec.JKEY_TYPE, "visjs");
		try{
			new PlotlyPlotSpec(plotSpecJsonWrongType);
			fail("Missed expected exception"); // shouldn't get here
		}catch(Exception e){
			assertTrue(e.getMessage().contains("Cannot create PlotlyPlotSpec with type visjs"));
		}
	}
	
	@Test
	public void testGetSample() throws Exception{
		PlotlyPlotSpec sample = PlotlyPlotSpec.getSample("Sample Plot", "scatter", new String[]{"timestamp", "score"});
				
		System.out.println(sample.toJson());
		assertEquals(sample.getName(), "Sample Plot");
		assertEquals(sample.getType(), "plotly");
		assertEquals(((JSONArray)sample.getSpec().get(PlotlyPlotSpec.JKEY_DATA)).size(), 1); // one trace
		assertTrue(sample.toJson().toJSONString().contains("SEMTK_TABLE.col[timestamp]"));
		assertTrue(sample.toJson().toJSONString().contains("SEMTK_TABLE.col[score]"));
	}
	
	
	@Test
	public void testApplyTable() throws Exception {

		// the plot spec
		JSONObject plotSpecJson = Utility.getResourceAsJson(this, "plotly.json");
	
		// a table
		Table table = Table.fromJson((JSONObject) new JSONParser().parse(tableJsonStr));
		
		PlotlyPlotSpec spec = new PlotlyPlotSpec(plotSpecJson);
		spec.applyTable(table);
		
		JSONObject resultJson = spec.toJson();	
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

		PlotlyPlotSpec spec = new PlotlyPlotSpec(plotSpecJson);		
		try{
			spec.applyTable(table);
			fail();  // should not get here
		}catch(Exception e){
			assertTrue(e.getMessage().contains("Unsupported data specification"));
		}
		
	}
	
}
