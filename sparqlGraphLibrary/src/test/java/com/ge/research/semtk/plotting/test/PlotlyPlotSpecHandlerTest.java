package com.ge.research.semtk.plotting.test;

import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;


import com.ge.research.semtk.plotting.PlotlyPlotSpecHandler;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.Utility;

public class PlotlyPlotSpecHandlerTest {

	private String tableJsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"11.1\",\"22.2\",\"33.3\"],[\"11.5\",\"22.5\",\"33.5\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";

	
	@Test
	public void test() throws Exception {

		// the plot spec
		JSONObject plotSpecJson = Utility.getResourceAsJson(this, "plotly.json");
	
		// a table
		Table table = Table.fromJson((JSONObject) new JSONParser().parse(tableJsonStr));
		
		PlotlyPlotSpecHandler specHandler = new PlotlyPlotSpecHandler(plotSpecJson);
		specHandler.applyTable(table);
		
		JSONObject resultJson = specHandler.getJSON();	
		assertFalse(resultJson.toJSONString().contains("SEMTK_TABLE"));
		assertTrue(resultJson.toJSONString().contains("\"x\":[11.1,11.5]"));
		assertTrue(resultJson.toJSONString().contains("\"y\":[22.2,22.5]"));
		assertTrue(resultJson.toJSONString().contains("\"y\":[33.3,33.5]"));
	}
	
	
	@Test
	public void testTimestampValues() throws Exception {

		// the plot spec
		JSONObject plotSpecJson = Utility.getResourceAsJson(this, "plotly.json");
	
		// a table
		String tableJsonStr_Timestamps = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"2020-01-24T00:00:00\",\"22.2\",\"33.3\"],[\"2020-01-25T00:00:00\",\"22.5\",\"33.5\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
		Table table = Table.fromJson((JSONObject) new JSONParser().parse(tableJsonStr_Timestamps));
		
		PlotlyPlotSpecHandler specHandler = new PlotlyPlotSpecHandler(plotSpecJson);
		specHandler.applyTable(table);
		
		JSONObject resultJson = specHandler.getJSON();	
		System.out.println(resultJson.toJSONString());
		assertFalse(resultJson.toJSONString().contains("SEMTK_TABLE"));
		assertTrue(resultJson.toJSONString().contains("\"x\":[\"2020-01-24T00:00:00\",\"2020-01-25T00:00:00\"]"));
		assertTrue(resultJson.toJSONString().contains("\"y\":[22.2,22.5]"));
		assertTrue(resultJson.toJSONString().contains("\"y\":[33.3,33.5]"));
	}
	
	@Test
	public void testError() throws Exception {
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
