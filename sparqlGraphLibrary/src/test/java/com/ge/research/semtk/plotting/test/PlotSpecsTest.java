package com.ge.research.semtk.plotting.test;

import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.plotting.PlotSpecs;
import com.ge.research.semtk.plotting.PlotlyPlotSpec;
import com.ge.research.semtk.utility.Utility;

public class PlotSpecsTest {

	@Test
	public void test_getPlotNames() throws Exception {
		
		PlotSpecs specs = new PlotSpecs(Utility.getJSONArrayFromFilePath("src/test/resources/plots-spec.json"));
		assertEquals(specs.getNumPlotSpecs(), 2);
		assertEquals(specs.getPlotSpecNames().size(), 2);
		assertEquals(specs.getPlotSpecNames().get(0), "Plotly Chart 1");
		assertEquals(specs.getPlotSpecNames().get(1), "Plotly Chart 2");
		
		
		try{
			specs = new PlotSpecs(Utility.getJSONArrayFromFilePath("src/test/resources/plots-spec-missingname.json"));
			specs.getPlotSpecNames();
			fail("Missing expected exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Plot spec is missing 'name'"));
		}
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void test_addPlot() throws Exception{
		
		JSONObject plotSpecJson = Utility.getResourceAsJson(this, "plotly.json");
		PlotlyPlotSpec plotSpec = new PlotlyPlotSpec(plotSpecJson);

		// add the plot spec
		PlotSpecs plotSpecs = new PlotSpecs(null);
		plotSpecs.addPlotSpec(plotSpec);
		assertEquals(plotSpecs.getNumPlotSpecs(), 1);
		
		// confirm can add it with a different name
		plotSpecJson = (JSONObject) plotSpecJson.clone();  // TODO clone this in the method?
		plotSpecJson.remove("name");
		plotSpecJson.put("name", "Plot 2");
		plotSpecs.addPlotSpec(new PlotlyPlotSpec(plotSpecJson));
		assertEquals(plotSpecs.getNumPlotSpecs(), 2);
		
		// confirm can't re-add it (same name)
		try{
			plotSpecs.addPlotSpec(plotSpec);
			fail("Missing an expected exception");
		}catch(Exception e){
			assertTrue(e.getMessage().contains("a plot with this name already exists"));
		}
		
	}
	
}
