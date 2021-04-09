package com.ge.research.semtk.plotting.test;

import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.plotting.PlotSpecsHandler;
import com.ge.research.semtk.plotting.PlotlyPlotSpec;
import com.ge.research.semtk.utility.Utility;

public class PlotSpecsHandlerTest {

	@Test
	public void test_getPlotNames() throws Exception {
		
		PlotSpecsHandler plotSpecsHandler = new PlotSpecsHandler(Utility.getJSONArrayFromFilePath("src/test/resources/plots-spec.json"));
		assertEquals(plotSpecsHandler.getNumPlotSpecs(), 2);
		assertEquals(plotSpecsHandler.getPlotSpecNames().size(), 2);
		assertEquals(plotSpecsHandler.getPlotSpecNames().get(0), "Plotly Chart 1");
		assertEquals(plotSpecsHandler.getPlotSpecNames().get(1), "Plotly Chart 2");
		
		
		try{
			plotSpecsHandler = new PlotSpecsHandler(Utility.getJSONArrayFromFilePath("src/test/resources/plots-spec-missingname.json"));
			plotSpecsHandler.getPlotSpecNames();
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
		PlotSpecsHandler plotSpecs = new PlotSpecsHandler(null);
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
